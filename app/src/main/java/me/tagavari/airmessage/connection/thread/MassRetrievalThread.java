package me.tagavari.airmessage.connection.thread;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.activity.ConversationsBase;
import me.tagavari.airmessage.common.Blocks;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.messaging.AttachmentInfo;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.ConversationItem;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.util.Constants;
import me.tagavari.airmessage.util.ConversationUtils;
import me.tagavari.airmessage.util.ShortcutUtils;

public class MassRetrievalThread extends Thread {
	//Creating the reference values
	static final long startTimeout = 40 * 1000; //The timeout duration directly after requesting a mass retrieval - 40 seconds
	static final long intervalTimeout = 10 * 1000; //The timeout duration between message packets - 10 seconds
	
	private static final int stateCreated = 0;
	private static final int stateWaiting = 1;
	private static final int stateRegistered = 2;
	private static final int stateDownloading = 3;
	private static final int stateFailed = 4;
	private static final int stateFinished = 5;
	
	//Creating the start information
	private int currentState = stateCreated;
	private final WeakReference<Context> contextReference;
	private List<Blocks.ConversationInfo> conversationList;
	private int messageCount;
	private final AtomicInteger atomicMessageProgress = new AtomicInteger();
	
	//Creating the timer values
	private final Handler handler = new Handler();
	private final Runnable callbackFail;
	
	//Creating the packet values
	private int lastMessagePackageIndex = 0;
	private final BlockingQueue<DataPackage> messagePacketQueue = new LinkedBlockingQueue<>();
	private String currentFileGUID = null;
	private long currentFileLocalID = -1;
	private File currentFileTarget = null;
	private FileOutputStream currentFileStream = null;
	private int currentFileIndex = -1;
	private ConnectionManager.Packager currentFilePackager = null;
	private final List<ConversationItem> lastAddedItems = new ArrayList<>(); //A list of the items from the last message update, used to associate attachment GUIDs to their local IDs
	
	//Creating the other values
	private short requestID;
	
	public MassRetrievalThread(Context context) {
		//Establishing the references
		contextReference = new WeakReference<>(context);
		
		callbackFail = () -> {
			Context newContext = contextReference.get();
			if(newContext != null) cancel(newContext);
		};
	}
	
	public void setRequestID(short requestID) {
		this.requestID = requestID;
	}
	
	public void completeInit(Context context) {
		//Sending a start broadcast
		LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ConnectionManager.localBCMassRetrieval).putExtra(Constants.intentParamState, ConnectionManager.intentExtraStateMassRetrievalStarted));
		
		//Starting the timeout timer
		handler.postDelayed(callbackFail, startTimeout);
		
		//Setting the state
		currentState = stateWaiting;
	}
	
	public void registerInfo(Context context, short requestID, List<Blocks.ConversationInfo> conversationList, int messageCount, ConnectionManager.Packager packager) {
		//Ignoring the request if the identifier does not line up
		if(this.requestID != requestID) return;
		
		//Handling the state
		if(currentState != stateWaiting) return;
		currentState = stateRegistered;
		
		//Setting the information
		this.conversationList = conversationList;
		this.messageCount = messageCount;
		currentFilePackager = packager;
		
		//Restarting the timeout timer with the packet delay
		handler.removeCallbacks(callbackFail);
		handler.postDelayed(callbackFail, intervalTimeout);
		
		//Sending a broadcast
		LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ConnectionManager.localBCMassRetrieval).putExtra(Constants.intentParamState, ConnectionManager.intentExtraStateMassRetrievalProgress).putExtra(Constants.intentParamSize, messageCount));
		
		//Starting the thread
		start();
	}
	
	public void addPacket(Context context, short requestID, int index, List<Blocks.ConversationItem> itemList) {
		//Ignoring the request if the identifier does not line up
		if(this.requestID != requestID) return;
		
		//Handling the state
		if(currentState != stateDownloading && currentState != stateRegistered) return;
		currentState = stateDownloading;
		
		//Checking if the indices don't line up or the list is invalid
		if(lastMessagePackageIndex + 1 != index || itemList == null) {
			//Cancelling the task
			cancel(context);
			
			//Returning
			return;
		}
		
		//Updating the counters
		lastMessagePackageIndex = index;
		
		//Restarting the timeout timer
		refreshTimeout();
		
		//Queueing the packet
		messagePacketQueue.add(new MessagePackage(itemList));
	}
	
	public void startFileData(short requestID, String guid, String fileName) {
		//Ignoring the request if the identifier does not line up
		if(this.requestID != requestID) return;
		if(currentState != stateDownloading) return;
		
		//Queueing the packet
		messagePacketQueue.add(new FileDataInitPackage(guid, fileName));
	}
	
	public void appendFileData(short requestID, int index, String guid, byte[] compressedBytes, boolean isLast) {
		//Ignoring the request if the identifier does not line up
		if(this.requestID != requestID) return;
		if(currentState != stateDownloading) return;
		
		//Restarting the timeout timer
		refreshTimeout();
		
		//Queueing the packet
		messagePacketQueue.add(new FileDataContentPackage(index, guid, compressedBytes, isLast));
	}
	
	private void refreshTimeout() {
		handler.removeCallbacks(callbackFail);
		handler.postDelayed(callbackFail, intervalTimeout);
	}
	
	public void finish() {
		//Handling the state
		if(currentState != stateDownloading && currentState != stateRegistered) return;
		currentState = stateFinished;
		
		//Stopping the timeout timer
		handler.removeCallbacks(callbackFail);
		
		//Queueing a finish flag package (to use as a message that the process is finished)
		messagePacketQueue.add(new FinishPackage());
	}
	
	public void cancel(Context context) {
		//Returning if there is no mass retrieval in progress
		if(!isInProgress()) return;
		
		//Setting the state
		currentState = stateFailed;
		
		//Stopping the timeout timer
		handler.removeCallbacks(callbackFail);
		
		//Sending a state broadcast
		LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ConnectionManager.localBCMassRetrieval).putExtra(Constants.intentParamState, ConnectionManager.intentExtraStateMassRetrievalFailed));
		
		//Updating the state
		currentState = stateFailed;
		
		//Interrupting the thread if it is running
		interrupt();
	}
	
	@Override
	public void run() {
		//Writing the conversations to disk
		List<ConversationInfo> conversationInfoList = new ArrayList<>();
		{
			Context context = contextReference.get();
			if(context == null) return;
			
			for(Blocks.ConversationInfo structConversation : conversationList) {
				ConversationInfo item = DatabaseManager.getInstance().addReadyConversationInfoAMBridge(context, structConversation);
				//if(item == null) item = DatabaseManager.getInstance().fetchConversationInfo(context, structConversation.guid);
				if(item != null) conversationInfoList.add(item);
			}
		}
		
		//Reading from the queue
		int messageCountReceived = 0;
		try {
			while(!isInterrupted()) {
				//Getting the list
				DataPackage dataPackage = messagePacketQueue.take();
				
				//Checking if the package is a finish flag
				if(dataPackage instanceof FinishPackage) {
					//Cancelling the current file download
					cancelCurrentFile();
					
					//Sorting the conversations
					//Collections.sort(conversationInfoList, ConversationUtils.conversationComparator);
					
					//Running on the main thread
					handler.post(() -> {
						//Getting the context
						Context context = contextReference.get();
						if(context == null) return;
						
						//Setting the conversations in memory
						ArrayList<ConversationInfo> sharedConversations = ConversationUtils.getConversations();
						if(sharedConversations != null) {
							sharedConversations.addAll(conversationInfoList);
							
							//Sorting the conversations
							Collections.sort(sharedConversations, ConversationUtils.conversationComparator);
							
							//Updating shortcuts
							if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
								ShortcutUtils.updateShortcuts(context, sharedConversations);
								ShortcutUtils.enableShortcuts(context, sharedConversations);
							}
						}
						
						//Sending the mass retrieval broadcast
						LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ConnectionManager.localBCMassRetrieval).putExtra(Constants.intentParamState, ConnectionManager.intentExtraStateMassRetrievalFinished));
						
						//Updating the conversation activity list
						LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ConversationsBase.localBCConversationUpdate));
						
						//Setting the current state
						currentState = stateFinished;
					});
					
					//Returning
					return;
				}
				//Otherwise checking if the package is a message package
				else if(dataPackage instanceof MessagePackage) {
					//Getting the context
					Context context = contextReference.get();
					if(context == null) return;
					
					//Getting the lists
					List<Blocks.ConversationItem> itemList = ((MessagePackage) dataPackage).messageList;
					lastAddedItems.clear();
					
					//Adding the messages
					for(Blocks.ConversationItem structItem : itemList) {
						//Cleaning the conversation item
						ConnectionManager.cleanConversationItem(structItem);
						
						//Decompressing the item data
						if(structItem instanceof Blocks.MessageInfo) {
							Blocks.MessageInfo structMessage = (Blocks.MessageInfo) structItem;
							for(Blocks.StickerModifierInfo stickerInfo : structMessage.stickers)stickerInfo.data = currentFilePackager.unpackageData(stickerInfo.data);
						}
						
						//Finding the parent conversation
						ConversationInfo parentConversation = null;
						for(ConversationInfo conversationInfo : conversationInfoList) {
							if(!structItem.chatGuid.equals(conversationInfo.getGuid())) continue;
							parentConversation = conversationInfo;
						}
						if(parentConversation == null) continue;
						
						//Writing the item
						ConversationItem conversationItem = DatabaseManager.getInstance().addConversationItem(context, structItem, parentConversation);
						if(conversationItem == null) continue;
						lastAddedItems.add(conversationItem);
						
						//Updating the parent conversation's last item
						parentConversation.trySetLastItem(conversationItem.toLightConversationItemSync(context), false);
					}
					
					//Updating the progress
					messageCountReceived += itemList.size();
					atomicMessageProgress.set(messageCountReceived);
					LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ConnectionManager.localBCMassRetrieval).putExtra(Constants.intentParamState, ConnectionManager.intentExtraStateMassRetrievalProgress).putExtra(Constants.intentParamProgress, messageCountReceived));
				}
				//Otherwise checking if the package is an attachment download initiation
				else if(dataPackage instanceof FileDataInitPackage) {
					//Getting the context
					Context context = contextReference.get();
					if(context == null) return;
					
					//Cancelling the file
					cancelCurrentFile();
					
					//Setting the file values
					FileDataInitPackage data = (FileDataInitPackage) dataPackage;
					
					//Finding the local ID
					{
						long localID = -1;
						conversationLoop:
						for(ConversationItem item : lastAddedItems) {
							if(!(item instanceof MessageInfo)) continue;
							for(AttachmentInfo attachment : ((MessageInfo) item).getAttachments()) {
								if(!data.guid.equals(attachment.getGuid())) continue;
								localID = attachment.getLocalID();
								break conversationLoop;
							}
						}
						
						//Ignoring the request if no ID could be found
						if(localID == -1) continue;
						currentFileLocalID = localID;
					}
					
					//Setting the GUID
					currentFileGUID = data.guid;
					
					//Assigning the file
					File targetFileDir = new File(MainApplication.getDownloadDirectory(context), Long.toString(currentFileLocalID));
					if(!targetFileDir.exists()) targetFileDir.mkdir();
					else if(targetFileDir.isFile()) {
						Constants.recursiveDelete(targetFileDir);
						targetFileDir.mkdir();
					}
					currentFileTarget = new File(targetFileDir, data.fileName);
					try {
						currentFileStream = new FileOutputStream(currentFileTarget);
					} catch(IOException exception) {
						//Printing the stack trace
						exception.printStackTrace();
						
						//Cleaning up
						targetFileDir.delete();
						resetFileValues();
					}
				}
				//Otherwise checking if the package is an attachment download
				else if(dataPackage instanceof FileDataContentPackage) {
					//Getting the context
					Context context = contextReference.get();
					if(context == null) return;
					
					FileDataContentPackage data = (FileDataContentPackage) dataPackage;
					
					//Ignoring the message if there is no current attachment or the index does not line up
					if(!data.guid.equals(currentFileGUID) || currentFileIndex + 1 != data.index) continue;
					
					//Decompressing the bytes
					byte[] decompressedBytes = currentFilePackager.unpackageData(data.compressedBytes);
					
					try {
						//Writing the bytes
						currentFileStream.write(decompressedBytes);
					} catch(IOException exception) {
						exception.printStackTrace();
						cancelCurrentFile();
						continue;
					}
					
					//Checking if this is the last file
					if(data.isLast) {
						//Closing the stream
						try {
							currentFileStream.close();
						} catch(IOException exception) {
							exception.printStackTrace();
							cancelCurrentFile();
							continue;
						}
						
						//Updating the database entry
						DatabaseManager.getInstance().updateAttachmentFile(currentFileLocalID, MainApplication.getInstance(), currentFileTarget);
						
						//Cleaning up
						resetFileValues();
					} else {
						//Updating the index
						currentFileIndex = data.index;
					}
				}
			}
		} catch(InterruptedException exception) {
			exception.printStackTrace();
		}
	}
	
	private void cancelCurrentFile() {
		//Returning if there is no current file
		if(currentFileGUID == null) return;
		
		//Killing the stream
		try {
			currentFileStream.close();
		} catch(IOException exception) {
			exception.printStackTrace();
		}
		
		//Deleting the file
		currentFileTarget.delete();
		currentFileTarget.getParentFile().delete();
		
		//Invalidating the values
		resetFileValues();
	}
	
	private void resetFileValues() {
		currentFileGUID = null;
		currentFileLocalID = -1;
		currentFileTarget = null;
		currentFileStream = null;
		currentFileIndex = -1;
	}
	
	public int getProgress() {
		return atomicMessageProgress.get();
	}
	
	public int getProgressCount() {
		return messageCount;
	}
	
	public boolean isInProgress() {
		return currentState == stateWaiting || currentState == stateRegistered || currentState == stateDownloading;
	}
	
	public boolean isWaiting() {
		return currentState == stateWaiting;
	}
	
	private static abstract class DataPackage {
	
	}
	
	private static class FinishPackage extends DataPackage {
	
	}
	
	private static class MessagePackage extends DataPackage {
		final List<Blocks.ConversationItem> messageList;
		
		MessagePackage(List<Blocks.ConversationItem> messageList) {
			this.messageList = messageList;
		}
	}
	
	private static class FileDataInitPackage extends DataPackage {
		final String guid;
		final String fileName;
		
		FileDataInitPackage(String guid, String fileName) {
			this.guid = guid;
			this.fileName = fileName;
		}
	}
	
	private static class FileDataContentPackage extends DataPackage {
		final int index;
		final String guid;
		final byte[] compressedBytes;
		final boolean isLast;
		
		FileDataContentPackage(int index, String guid, byte[] compressedBytes, boolean isLast) {
			this.index = index;
			this.guid = guid;
			this.compressedBytes = compressedBytes;
			this.isLast = isLast;
		}
	}
}
