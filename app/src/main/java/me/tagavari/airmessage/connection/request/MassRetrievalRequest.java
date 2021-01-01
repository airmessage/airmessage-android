package me.tagavari.airmessage.connection.request;

import android.content.Context;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.reactivex.rxjava3.schedulers.Schedulers;
import me.tagavari.airmessage.common.Blocks;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.helper.AttachmentStorageHelper;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.ConversationItem;

public class MassRetrievalRequest {
	private static final String TAG = MassRetrievalRequest.class.getName();
	
	private final Scheduler requestScheduler = Schedulers.from(Executors.newSingleThreadExecutor(), true);
	
	//Conversations state
	private boolean initialInfoReceived = false;
	private List<ConversationInfo> conversationList;
	
	//Messages state
	private int totalMessageCount;
	private int messagesReceived = 0;
	private int expectedResponseIndex = 1;
	
	//Attachments state (only to be accessed on background thread)
	private String attachmentGUID;
	private File attachmentTargetFile;
	private int attachmentExpectedRequestIndex = 0;
	private OutputStream attachmentOutputStream;
	
	/**
	 * Handles the initial mass retrieval information sent from the server
	 * @param conversationList The list of conversations to save
	 * @param totalMessageCount The total amount of messages
	 * @return A single to return the list of added conversations
	 */
	public Single<List<ConversationInfo>> handleInitialInfo(Collection<Blocks.ConversationInfo> conversationList, int totalMessageCount) {
		if(initialInfoReceived) {
			return Single.error(new IllegalStateException("Initial info already received"));
		}
		
		initialInfoReceived = true;
		this.totalMessageCount = totalMessageCount;
		
		//Writing the data
		return Single.create((SingleEmitter<List<ConversationInfo>> emitter) -> {
			//Writing the conversations to disk
			List<ConversationInfo> conversationInfoList = new ArrayList<>();
			for(Blocks.ConversationInfo structConversation : conversationList) {
				ConversationInfo item = DatabaseManager.getInstance().addReadyConversationInfoAMBridge(structConversation);
				if(item != null) conversationInfoList.add(item);
			}
			emitter.onSuccess(conversationInfoList);
		}).subscribeOn(requestScheduler).observeOn(AndroidSchedulers.mainThread()).doOnSuccess(conversations -> this.conversationList = conversations);
	}
	
	/**
	 * Handles a group of messages sent from the server
	 * @param context The context to use
	 * @param responseIndex The index of this response data
	 * @param itemList The list of conversation items
	 * @return A single to return the list of added items
	 */
	public Single<List<ConversationItem>> handleMessages(Context context, int responseIndex, Collection<Blocks.ConversationItem> itemList) {
		if(!initialInfoReceived) {
			return Single.error(new IllegalStateException("Initial info not yet received"));
		}
		
		if(responseIndex != expectedResponseIndex) {
			return Single.error(new IllegalStateException("Request out of order: expected #" + expectedResponseIndex + ", received #" + responseIndex));
		}
		expectedResponseIndex++;
		
		return Single.create((SingleEmitter<List<ConversationItem>> emitter) -> {
			List<ConversationItem> addedItemList = new ArrayList<>();
			
			//Adding the messages
			for(Blocks.ConversationItem structItem : itemList) {
				//Finding the parent conversation
				ConversationInfo parentConversation = conversationList.stream().filter(conversation -> structItem.chatGuid.equals(conversation.getGUID())).findAny().orElse(null);
				if(parentConversation == null) {
					Log.w(TAG, "Mass retrieval referenced conversation not found: " + structItem.chatGuid);
					continue;
				}
				
				//Writing the item
				ConversationItem conversationItem = DatabaseManager.getInstance().addConversationStruct(context, parentConversation.getLocalID(), structItem);
				if(conversationItem == null) continue;
				addedItemList.add(conversationItem);
			}
			
			emitter.onSuccess(addedItemList);
		}).subscribeOn(requestScheduler).observeOn(AndroidSchedulers.mainThread()).doOnSuccess(addedItems -> {
			//Updating the total
			messagesReceived += itemList.size();
		});
	}
	
	/**
	 * Initializes the response for an attachment file
	 * @param context The context to use
	 * @param guid The GUID of the attachment
	 * @param fileName The file name of the attachment
	 * @return A completable to represent this task
	 */
	public Completable initializeAttachment(Context context, String guid, String fileName) {
		return Completable.create(emitter -> {
			//Checking if there is another request in progress
			if(attachmentGUID != null) throw new IllegalStateException("Trying to start attachment download for " + attachmentGUID + ", but " + guid + " is already in progress");
			
			//Creating the file and the stream
			attachmentGUID = guid;
			attachmentTargetFile = AttachmentStorageHelper.prepareContentFile(context, AttachmentStorageHelper.dirNameAttachment, fileName);
			attachmentOutputStream = new BufferedOutputStream(new FileOutputStream(attachmentTargetFile));
			
			emitter.onComplete();
		}).subscribeOn(requestScheduler).observeOn(AndroidSchedulers.mainThread());
	}
	
	/**
	 * Performs a validation check and finishes this retrieval request
	 * @return A completable to represent this task
	 */
	public Completable complete() {
		return Completable.create(emitter -> {
			//Checking if there is a file request in progress
			if(attachmentGUID != null) throw new IllegalStateException("Trying to finish mass retrieval, but attachment download " + attachmentGUID + " is still in progress");
			
			emitter.onComplete();
		}).subscribeOn(requestScheduler).observeOn(AndroidSchedulers.mainThread());
	}
	
	/**
	 * Writes a chunk of data to disk for this request
	 * @param guid The GUID of the attachment
	 * @param responseIndex The index of this response data
	 * @param data The attachment's data
	 * @return A completable to represent this task
	 */
	public Completable writeChunkAttachment(String guid, int responseIndex, byte[] data) {
		return Completable.create(emitter -> {
			//Validating and increasing the index
			if(responseIndex != attachmentExpectedRequestIndex) {
				emitter.onError(new IllegalStateException("Request out of order: expected #" + attachmentExpectedRequestIndex + ", received #" + responseIndex));
				return;
			}
			attachmentExpectedRequestIndex++;
			
			//Validating the attachment GUID
			if(!guid.equals(attachmentGUID)) {
				emitter.onError(new IllegalStateException("Mass retrieval file data mismatch: expected " + attachmentTargetFile + ", received" + guid));
				return;
			}
			
			//Writing the data
			attachmentOutputStream.write(data);
		}).subscribeOn(requestScheduler).observeOn(AndroidSchedulers.mainThread());
	}
	
	/**
	 * Completes the download of an attachment by cleaning up and writing the new state to disk
	 * @param context The context to use
	 * @param guid The GUID of the attachment file
	 * @return A completable to represent this task
	 */
	public Completable finishAttachment(Context context, String guid) {
		return Completable.create(emitter -> {
			//Validating the attachment GUID
			if(!guid.equals(attachmentGUID)) {
				emitter.onError(new IllegalStateException("Mass retrieval file data mismatch: expected " + attachmentTargetFile + ", received" + guid));
				return;
			}
			
			//Updating the attachment file location
			DatabaseManager.getInstance().updateAttachmentFile(attachmentGUID, context, attachmentTargetFile);
			
			//Cleaning up
			closeAttachment();
			attachmentGUID = null;
			attachmentTargetFile = null;
			attachmentExpectedRequestIndex = 0;
			attachmentOutputStream = null;
			emitter.onComplete();
		}).subscribeOn(requestScheduler).observeOn(AndroidSchedulers.mainThread());
	}
	
	/**
	 * Gets the total amount of messages scheduled to be downloaded from this retrieval
	 */
	public int getTotalMessageCount() {
		return totalMessageCount;
	}
	
	/**
	 * Gets the amount of messages retrieved so far
	 */
	public int getMessagesReceived() {
		return messagesReceived;
	}
	
	/**
	 * Closes and cleans up any pending tasks
	 */
	public void cancel() throws IOException {
		requestScheduler.shutdown();
		cancelAttachment();
	}
	
	/**
	 * Closes the current attachment request's streams for use when we are done with this request
	 */
	public void closeAttachment() throws IOException {
		if(attachmentOutputStream != null) attachmentOutputStream.close();
	}
	
	/**
	 * Cancels the current attachment request, closing its streams and cleaning up any saved data
	 */
	public void cancelAttachment() throws IOException {
		closeAttachment();
		if(attachmentTargetFile != null) AttachmentStorageHelper.deleteContentFile(AttachmentStorageHelper.dirNameAttachment, attachmentTargetFile);
	}
}