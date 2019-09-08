package me.tagavari.airmessage.connection;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;
import android.util.SparseArray;

import androidx.core.util.Consumer;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.crashlytics.android.Crashlytics;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.activity.Preferences;
import me.tagavari.airmessage.common.Blocks;
import me.tagavari.airmessage.common.SharedValues;
import me.tagavari.airmessage.connection.caladium.ClientCommCaladium;
import me.tagavari.airmessage.connection.request.ChatCreationResponseManager;
import me.tagavari.airmessage.connection.request.ConversationInfoRequest;
import me.tagavari.airmessage.connection.request.FileDownloadRequest;
import me.tagavari.airmessage.connection.request.FileProcessingRequest;
import me.tagavari.airmessage.connection.request.FilePushRequest;
import me.tagavari.airmessage.connection.request.FileRemovalRequest;
import me.tagavari.airmessage.connection.request.MessageResponseManager;
import me.tagavari.airmessage.connection.task.FetchConversationRequestsTask;
import me.tagavari.airmessage.connection.task.MessageUpdateAsyncTask;
import me.tagavari.airmessage.connection.task.ModifierUpdateAsyncTask;
import me.tagavari.airmessage.connection.task.QueueTask;
import me.tagavari.airmessage.connection.task.SaveConversationInfoAsyncTask;
import me.tagavari.airmessage.connection.thread.FileProcessingThread;
import me.tagavari.airmessage.connection.thread.MassRetrievalThread;
import me.tagavari.airmessage.connection.thread.MessageProcessingThread;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.ConversationItem;
import me.tagavari.airmessage.messaging.DraftFile;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.service.ConnectionService;
import me.tagavari.airmessage.util.Constants;
import me.tagavari.airmessage.util.ConversationUtils;

public class ConnectionManager {
	/* COMMUNICATIONS VERSION CHANGES
	 *  1 - Original release
	 *  2 - Serialization changes
	 *  3 - Original rework without WS layer
	 *  4 - Better stability and security, with sub-version support
	 */
	public static final int mmCommunicationsVersion = 4;
	public static final int mmCommunicationsSubVersion = 6;
	
	public static final int maxPacketAllocation = 50 * 1024 * 1024; //50 MB
	
	public static final String localBCStateUpdate = "LocalMSG-ConnectionService-State";
	public static final String localBCMassRetrieval = "LocalMSG-ConnectionService-MassRetrievalProgress";
	
	public static final int intentResultCodeSuccess = 0;
	public static final int intentResultCodeInternalException = 1;
	public static final int intentResultCodeBadRequest = 2;
	public static final int intentResultCodeClientOutdated = 3;
	public static final int intentResultCodeServerOutdated = 4;
	public static final int intentResultCodeUnauthorized = 5;
	public static final int intentResultCodeConnection = 6;
	
	public static final int intentExtraStateMassRetrievalStarted = 0;
	public static final int intentExtraStateMassRetrievalProgress = 1;
	public static final int intentExtraStateMassRetrievalFinished = 2;
	public static final int intentExtraStateMassRetrievalFailed = 3;
	
	public static final int stateDisconnected = 0;
	public static final int stateConnecting = 1;
	public static final int stateConnected = 2;
	
	public static final int largestFileSize = 1024 * 1024 * 100; //100 MB
	public static final int attachmentChunkSize = 1024 * 1024; //1 MiB
	
	public static final Pattern regExValidPort = Pattern.compile("(:([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5]?))$");
	
	//Creating the service values
	private final ServiceCallbacks serviceCallbacks;
	
	//Creating the communications values
	final List<Class> communicationsClassPriorityList = Collections.singletonList(ClientCommCaladium.class);
	final List<ConnectionServiceSource> communicationsInstancePriorityList = Collections.singletonList(ClientCommCaladium::new);
	
	//Creating the file processing values
	private final BlockingQueue<FileProcessingRequest> fileProcessingRequestQueue = new LinkedBlockingQueue<>();
	private final AtomicReference<FileProcessingRequest> fileProcessingRequestCurrent = new AtomicReference<>(null);
	private AtomicBoolean fileProcessingThreadRunning = new AtomicBoolean(false);
	private FileProcessingThread fileProcessingThread = null;
	
	//Creating the message processing values
	private final BlockingQueue<QueueTask<?, ?>> messageProcessingQueue = new LinkedBlockingQueue<>();
	private AtomicBoolean messageProcessingQueueThreadRunning = new AtomicBoolean(false);
	
	//Creating the mass retrieval values
	private MassRetrievalThread massRetrievalThread = null;
	private MassRetrievalParams currentMassRetrievalParams = null;
	
	//Creating the download request values
	private final ArrayList<FileDownloadRequest> fileDownloadRequests = new ArrayList<>();
	
	//Creating the connection values
	public static String hostname = null;
	public static String hostnameFallback = null;
	public static String password = null;
	
	private CommunicationsManager currentCommunicationsManager = null;
	private static byte currentLaunchID = 0;
	private int lastConnectionResult = -1;
	
	private boolean flagMarkEndTime = false; //Marks the time that the connection is closed, so that missed messages can be fetched since that time when reconnecting
	private boolean flagDropReconnect = false; //Automatically starts a new connection when the connection is closed
	private boolean flagShutdownRequested = false; //Disables forwarding when disconnecting
	
	private int activeCommunicationsVersion = -1;
	private int activeCommunicationsSubVersion = -1;
	
	//Creating the quick request variables
	private final SparseArray<MessageResponseManager> messageSendRequests = new SparseArray<>();
	private final SparseArray<ChatCreationResponseManager> chatCreationRequests = new SparseArray<>();
	private short currentRequestID = 0;
	
	private final ArrayList<ConversationInfoRequest> pendingConversations = new ArrayList<>();
	
	public ConnectionManager(ServiceCallbacks serviceCallbacks) {
		//Setting the service information
		this.serviceCallbacks = serviceCallbacks;
	}
	
	public ServiceCallbacks getServiceCallbacks() {
		return serviceCallbacks;
	}
	
	public void init(Context context) {
		//Loading data from the database
		new FetchConversationRequestsTask(context, new ConversationRequestsListener(this)).execute();
	}
	
	private static class ConversationRequestsListener extends StaticInterfaceListener implements Consumer<List<ConversationInfo>> {
		public ConversationRequestsListener(ConnectionManager manager) {
			super(manager);
		}
		
		@Override
		public void accept(List<ConversationInfo> conversations) {
			//Getting the connection manager
			ConnectionManager manager = getManager();
			if(manager == null) return;
			
			//Copying the conversations to the pending list
			synchronized(manager.pendingConversations) {
				for(ConversationInfo conversation : conversations)
					manager.pendingConversations.add(new ConversationInfoRequest(conversation, true));
			}
			
			//Requesting a conversation info fetch
			manager.retrievePendingConversationInfo();
		}
	}
	
	public boolean connect(Context context, byte launchID) {
		//Closing the current connection if it exists
		//if(getCurrentState() != stateDisconnected) disconnect();
		
		//Returning if there is no connection
		{
			NetworkInfo activeNetwork = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
			boolean isConnected = activeNetwork != null && activeNetwork.isConnected();
			if(!isConnected) {
				//Updating the notification
				//postDisconnectedNotification(true);
				
				//Notifying the connection listeners
				broadcastState(context, stateDisconnected, intentResultCodeConnection, launchID);
				
				return false;
			}
		}
		
		//Checking if there is no hostname
		if(hostname == null) {
			//Retrieving the data from the shared preferences
			SharedPreferences sharedPrefs = MainApplication.getInstance().getConnectivitySharedPrefs();
			hostname = sharedPrefs.getString(MainApplication.sharedPreferencesConnectivityKeyHostname, null);
			hostnameFallback = Preferences.getPreferenceFallbackServer(context);
			password = sharedPrefs.getString(MainApplication.sharedPreferencesConnectivityKeyPassword, null);
		}
		
		//Checking if the hostname is invalid (nothing was found in memory or on disk)
		if(hostname == null) {
			//Updating the notification
			//postDisconnectedNotification(true);
			
			//Notifying the connection listeners
			broadcastState(context, stateDisconnected, intentResultCodeConnection, launchID);
			
			return false;
		}
		
		//Connecting through the top of the priority queue
		boolean result = communicationsInstancePriorityList.get(0).get(this, context).connect(launchID);
		
		if(result) {
			//Updating the notification
			//postConnectedNotification(false, false);
			
			//Notifying the connection listeners
			broadcastState(context, stateConnecting, 0, launchID);
		} else {
			//Updating the notification
			//postDisconnectedNotification(false);
			
			//Notifying the connection listeners
			broadcastState(context, stateDisconnected, intentResultCodeInternalException, launchID);
		}
		
		//Returning the result
		return result;
	}
	
	public void disconnect() {
		if(currentCommunicationsManager != null) currentCommunicationsManager.disconnect();
	}
	
	public void reconnect(Context context) {
		connect(context, getNextLaunchID());
	}
	
	void updateCommunicationsManager(CommunicationsManager communicationsManager) {
		//Disconnecting the current communications manager
		if(currentCommunicationsManager != null && currentCommunicationsManager.getState() != stateDisconnected) currentCommunicationsManager.disconnect();
		
		//Updating the communications manager information
		currentCommunicationsManager = communicationsManager;
	}
	
	public boolean getFlagMarkEndTime() {
		return flagMarkEndTime;
	}
	
	public void setFlagMarkEndTime(boolean flagMarkEndTime) {
		this.flagMarkEndTime = flagMarkEndTime;
	}
	
	public boolean getFlagDropReconnect() {
		return flagDropReconnect;
	}
	
	public void setFlagDropReconnect(boolean flagDropReconnect) {
		this.flagDropReconnect = flagDropReconnect;
	}
	
	public boolean getFlagShutdownRequested() {
		return flagShutdownRequested;
	}
	
	public void setFlagShutdownRequested(boolean flagShutdownRequested) {
		this.flagShutdownRequested = flagShutdownRequested;
	}
	
	public boolean isConnectedFallback() {
		if(currentCommunicationsManager == null) return false;
		return currentCommunicationsManager.isConnectedFallback();
	}
	
	public CommunicationsManager getCurrentCommunicationsManager() {
		return currentCommunicationsManager;
	}
	
	public int getCurrentState() {
		if(currentCommunicationsManager == null) return stateDisconnected;
		else return currentCommunicationsManager.getState();
	}
	
	public void setActiveCommunicationsInfo(int version, int subversion) {
		activeCommunicationsVersion = version;
		activeCommunicationsSubVersion = subversion;
	}
	
	public int getActiveCommunicationsVersion() {
		return activeCommunicationsVersion;
	}
	
	public int getActiveCommunicationsSubVersion() {
		return activeCommunicationsSubVersion;
	}
	
	public boolean checkSupportsFeature(String feature) {
		if(currentCommunicationsManager == null) return false;
		return currentCommunicationsManager.checkSupportsFeature(feature);
	}
	
	public short getCurrentRequestID() {
		return currentRequestID;
	}
	
	public void setCurrentRequestID(short currentRequestID) {
		this.currentRequestID = currentRequestID;
	}
	
	public short getNextRequestID() {
		return ++currentRequestID;
	}
	
	public static byte getCurrentLaunchID() {
		return currentLaunchID;
	}
	
	public void setCurrentLaunchID(byte currentLaunchID) {
		this.currentLaunchID = currentLaunchID;
	}
	
	public int getLastConnectionResult() {
		return lastConnectionResult;
	}
	
	public void setLastConnectionResult(int lastConnectionResult) {
		this.lastConnectionResult = lastConnectionResult;
	}
	
	public static byte getNextLaunchID() {
		return ++currentLaunchID;
	}
	
	public int compareLaunchID(byte launchID) {
		return Integer.compare(currentLaunchID, launchID);
	}
	
	public void addMessageSendRequest(short requestID, MessageResponseManager responseManager) {
		messageSendRequests.put(requestID, responseManager);
	}
	
	public void removeMessageSendRequest(MessageResponseManager responseManager) {
		int index = messageSendRequests.indexOfValue(responseManager);
		if(index != -1) messageSendRequests.removeAt(index);
	}
	
	public SparseArray<MessageResponseManager> getMessageSendRequestsList() {
		return messageSendRequests;
	}
	
	public void addMessagingProcessingTask(QueueTask<?, ?> task) {
		//Adding the task
		messageProcessingQueue.add(task);
		
		//Starting the thread if it isn't running
		if(messageProcessingQueueThreadRunning.compareAndSet(false, true)) new MessageProcessingThread(messageProcessingQueue, new AtomicBooleanFinishTrigger(messageProcessingQueueThreadRunning)).start();
	}
	
	public void addFileProcessingRequest(FileProcessingRequest request) {
		//Adding the task
		fileProcessingRequestQueue.add(request);
		
		//Starting the thread if it isn't running
		if(fileProcessingThreadRunning.compareAndSet(false, true)) {
			fileProcessingThread = new FileProcessingThread(MainApplication.getInstance(), fileProcessingRequestQueue, new FileProcessingRequestUpdateConsumer(this), new AtomicBooleanFinishTrigger(fileProcessingThreadRunning));
			fileProcessingThread.start();
		}
	}
	
	public FileProcessingRequest searchFileProcessingQueue(long draftID) {
		List<FileProcessingRequest> queueList = new ArrayList<>(fileProcessingRequestQueue);
		for(ListIterator<FileProcessingRequest> iterator = queueList.listIterator(queueList.size()); iterator.hasPrevious();) {
			FileProcessingRequest request = iterator.previous();
			if(request instanceof FilePushRequest) {
				if(((FilePushRequest) request).getDraftID() == draftID) return request;
			} else if(request instanceof FileRemovalRequest) {
				if(((FileRemovalRequest) request).getDraftFile().getLocalID() == draftID) return request;
			}
		}
		
		FileProcessingRequest request = fileProcessingRequestCurrent.get();
		if(request != null) {
			if(request instanceof FilePushRequest) {
				if(((FilePushRequest) request).getDraftID() == draftID) return request;
			} else if(request instanceof FileRemovalRequest) {
				if(((FileRemovalRequest) request).getDraftFile().getLocalID() == draftID) return request;
			}
		}
		
		return null;
	}
	
	public static void removeDraftFileSync(DraftFile draftFile, long updateTime) {
		//Deleting the file and the file's parent directory (since each draft file is stored in its own folder to prevent name collisions)
		draftFile.getFile().delete();
		draftFile.getFile().getParentFile().delete();
		
		//Removing the draft reference from the database
		DatabaseManager.getInstance().removeDraftReference(draftFile.getLocalID(), updateTime);
	}
	
	/**
	 * Sends a broadcast to the listeners
	 *
	 * @param state the state of the connection
	 * @param code the error code, if the state is disconnected
	 * @param launchID the launch ID of the connection
	 */
	public void broadcastState(Context context, int state, int code, byte launchID) {
		//Notifying the connection listeners
		LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(localBCStateUpdate)
				.putExtra(Constants.intentParamState, state)
				.putExtra(Constants.intentParamCode, code)
				.putExtra(Constants.intentParamLaunchID, launchID));
	}
	
	public void processMessageUpdate(List<Blocks.ConversationItem> structConversationItems, boolean sendNotifications) {
		//Creating and running the task
		//new MessageUpdateAsyncTask(this, getApplicationContext(), structConversationItems, sendNotifications).execute();
		addMessagingProcessingTask(new MessageUpdateAsyncTask(this, MainApplication.getInstance(), structConversationItems, sendNotifications));
	}
	
	public void processChatInfoResponse(List<Blocks.ConversationInfo> structConversations) {
		//Creating the list values
		final ArrayList<ConversationInfo> unavailableConversations = new ArrayList<>();
		final ArrayList<ConversationInfoRequest> availableConversations = new ArrayList<>();
		
		//Iterating over the conversations
		for(Blocks.ConversationInfo structConversationInfo : structConversations) {
			//Finding the conversation in the pending list
			ConversationInfoRequest request = null;
			synchronized(pendingConversations) {
				for(Iterator<ConversationInfoRequest> iterator = pendingConversations.iterator(); iterator.hasNext(); ) {
					//Getting the current request
					ConversationInfoRequest allRequests = iterator.next();
					
					//Skipping the remainder of the iteration if the pending conversation's GUID doesn't match the new conversation information's GUID
					if(!allRequests.getConversationInfo().getGuid().equals(structConversationInfo.guid)) continue;
					
					//Setting the request
					request = allRequests;
					
					//Removing the request (it will be processed no matter what)
					iterator.remove();
					
					//Breaking from the loop
					break;
				}
				
				//Skipping the remainder of the iteration if no matching pending conversation could be found or the conversation is not in a valid state
				if(request == null || request.getConversationInfo().getState() != ConversationInfo.ConversationState.INCOMPLETE_SERVER) continue;
				
				//Checking if the conversation is available
				if(structConversationInfo.available) {
					//Setting the conversation details
					ConversationInfo conversationInfo = request.getConversationInfo();
					conversationInfo.setService(structConversationInfo.service);
					conversationInfo.setTitle(MainApplication.getInstance(), structConversationInfo.name);
					//conversationInfo.setConversationColor(ConversationInfo.getRandomColor());
					conversationInfo.setConversationColor(ConversationInfo.getDefaultConversationColor(request.getConversationInfo().getGuid()));
					conversationInfo.setConversationMembersCreateColors(structConversationInfo.members);
					conversationInfo.setState(ConversationInfo.ConversationState.READY);
					
					//Marking the conversation as valid (and to be saved)
					availableConversations.add(request);
				}
				//Otherwise marking the conversation as invalid
				else unavailableConversations.add(request.getConversationInfo());
			}
		}
		
		//Creating and running the asynchronous task
		//new SaveConversationInfoAsyncTask(getApplicationContext(), unavailableConversations, availableConversations).execute();
		addMessagingProcessingTask(new SaveConversationInfoAsyncTask(MainApplication.getInstance(), unavailableConversations, availableConversations));
	}
	
	public void processModifierUpdate(List<Blocks.ModifierInfo> structModifiers, Packager packager) {
		//Creating and running the task
		//new ModifierUpdateAsyncTask(getApplicationContext(), structModifiers).execute();
		addMessagingProcessingTask(new ModifierUpdateAsyncTask(MainApplication.getInstance(), structModifiers, packager));
	}
	
	/* boolean requestAttachmentInfo(String fileGuid, short requestID) {
		//Preparing to serialize the request
		try(ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
			//Adding the data
			out.writeByte(SharedValues.wsFrameAttachmentReq); //Message type - attachment request
			out.writeShort(requestID); //Request ID
			out.writeUTF(fileGuid); //File GUID
			out.writeInt(attachmentChunkSize); //Chunk size
			out.flush();
			
			//Sending the message
			wsClient.send(bos.toByteArray());
		} catch(IOException | NotYetConnectedException exception) {
			//Printing the stack trace
			exception.printStackTrace();
			Crashlytics.logException(exception);
			
			//Returning false
			return false;
		}
		
		//Returning true
		return true;
	} */
	
	public boolean retrievePendingConversationInfo() {
		//Returning if the connection is not ready
		if(getCurrentState() != stateConnected) return false;
		
		//Sending a request and returning the result
		return currentCommunicationsManager.sendConversationInfoRequest(pendingConversations);
	}
	
	public boolean isMassRetrievalInProgress() {
		return massRetrievalThread != null && massRetrievalThread.isInProgress();
	}
	
	public boolean isMassRetrievalWaiting() {
		return massRetrievalThread != null && massRetrievalThread.isWaiting();
	}
	
	public int getMassRetrievalProgress() {
		if(massRetrievalThread == null) return -1;
		return massRetrievalThread.getProgress();
	}
	
	public int getMassRetrievalProgressCount() {
		if(massRetrievalThread == null) return -1;
		return massRetrievalThread.getProgressCount();
	}
	
	public void setMassRetrievalParams(MassRetrievalParams params) {
		currentMassRetrievalParams = params;
	}
	
	public boolean requestMassRetrieval() {
		//Returning false if the client isn't ready or a mass retrieval is already in progress
		if((massRetrievalThread != null && massRetrievalThread.isInProgress()) || getCurrentState() != stateConnected || currentMassRetrievalParams == null) return false;
		
		//Picking the next request ID
		short requestID = getNextRequestID();
		
		//Creating the mass retrieval manager
		massRetrievalThread = new MassRetrievalThread(MainApplication.getInstance());
		massRetrievalThread.setRequestID(requestID);
		
		//Sending the request
		boolean result = currentCommunicationsManager.requestRetrievalAll(requestID, currentMassRetrievalParams);
		if(!result) return false;
		
		//Starting the thread
		massRetrievalThread.completeInit(MainApplication.getInstance());
		
		//Sending the broadcast
		//LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(localBCMassRetrieval).putExtra(Constants.intentParamState, intentExtraStateMassRetrievalStarted));
		
		//Returning true
		return true;
	}
	
	public void cancelMassRetrieval(Context context) {
		//Forwarding the request
		if(massRetrievalThread != null) massRetrievalThread.cancel(context);
	}
	
	public void finishMassRetrieval() {
		if(massRetrievalThread != null) massRetrievalThread.finish();
	}
	
	public MassRetrievalThread getMassRetrievalThread() {
		return massRetrievalThread;
	}
	
	public boolean addDownloadRequest(FileDownloadRequest.Callbacks callbacks, long attachmentID, String attachmentGUID, String attachmentName) {
		//Getting the request ID
		short requestID = getNextRequestID();
		
		//Returning if there is no connection
		if(currentCommunicationsManager == null || currentCommunicationsManager.getState() != stateConnected) return false;
		
		//Building the tracking request
		FileDownloadRequest request = new FileDownloadRequest(callbacks, new FileDownloadRequestDeregistrationListener(this), requestID, attachmentID, attachmentGUID, attachmentName);
		
		//Sending the request
		boolean result = currentCommunicationsManager.addDownloadRequest(requestID, attachmentGUID, request::startTimer);
		if(!result) return false;
		
		//Recording the tracking request
		fileDownloadRequests.add(request);
		
		//Returning true
		return true;
	}
	
	public FileDownloadRequest.ProgressStruct updateDownloadRequestAttachment(long attachmentID, FileDownloadRequest.Callbacks callbacks) {
		for(FileDownloadRequest request : fileDownloadRequests) {
			if(request.getAttachmentID() == attachmentID) {
				request.setCallbacks(callbacks);
				return request.getProgress();
			}
		}
		
		return null;
	}
	
	public void removeDownloadRequest(FileDownloadRequest fileDownloadRequest) {
		fileDownloadRequests.remove(fileDownloadRequest);
	}
	
	public ArrayList<FileDownloadRequest> getFileDownloadRequests() {
		return fileDownloadRequests;
	}
	
	public boolean sendMessage(String chatGUID, String message, MessageResponseManager responseListener) {
		//Checking if the client isn't ready
		if(getCurrentState() != stateConnected) {
			//Telling the response listener
			responseListener.onFail(Constants.messageErrorCodeLocalNetwork, null);
			
			//Returning false
			return false;
		}
		
		//Getting the request ID
		short requestID = getNextRequestID();
		
		//Sending the message
		boolean result = currentCommunicationsManager.sendMessage(requestID, chatGUID, message);
		
		//Validating the result
		if(!result) {
			//Telling the response listener
			responseListener.onFail(Constants.messageErrorCodeLocalIO, null);
			
			//Returning false
			return false;
		}
		
		//Adding the request
		messageSendRequests.put(requestID, responseListener);
		
		//Starting the timer
		responseListener.startTimer();
		
		//Returning true
		return true;
	}
	
	public boolean sendMessage(String[] chatRecipients, String message, String service, MessageResponseManager responseListener) {
		//Checking if the client isn't ready
		if(getCurrentState() != stateConnected) {
			//Telling the response listener
			responseListener.onFail(Constants.messageErrorCodeLocalNetwork, null);
			
			//Returning false
			return false;
		}
		
		//Getting the request ID
		short requestID = getNextRequestID();
		
		//Sending the message
		boolean result = currentCommunicationsManager.sendMessage(requestID, chatRecipients, message, service);
		
		//Validating the result
		if(!result) {
			//Telling the response listener
			responseListener.onFail(Constants.messageErrorCodeLocalIO, null);
			
			//Returning false
			return false;
		}
		
		//Adding the request
		messageSendRequests.put(requestID, responseListener);
		
		//Starting the timer
		responseListener.startTimer();
		
		//Returning true
		return true;
	}
	
	public void cancelCurrentTasks() {
		//Cancelling the file requests
		List<FileDownloadRequest> requestList = (List<FileDownloadRequest>) fileDownloadRequests.clone();
		for(FileDownloadRequest request : requestList) request.failDownload(FileDownloadRequest.Callbacks.errorCodeCancelled);
		
		//Interrupting the file processing request thread and clearing its contents
		if(fileProcessingThreadRunning.get()) fileProcessingThread.interrupt();
		fileProcessingRequestQueue.clear();
	}
	
	public void failDownloadRequest(short requestID, int errorCode) {
		for(FileDownloadRequest request : fileDownloadRequests) {
			if(request.getRequestID() != requestID/* || !request.attachmentGUID.equals(fileGUID)*/) continue;
			request.failDownload(errorCode);
			break;
		}
	}
	
	public SparseArray<ChatCreationResponseManager> getChatCreationRequests() {
		return chatCreationRequests;
	}
	
	public ArrayList<ConversationInfoRequest> getPendingConversations() {
		return pendingConversations;
	}
	
	public boolean retrieveMessagesSince(long timeLower, long timeUpper) {
		//Returning false if the connection isn't ready
		if(getCurrentState() != stateConnected) return false;
		
		//Sending the request
		return currentCommunicationsManager.requestRetrievalTime(timeLower, timeUpper);
	}
	
	public boolean createChat(String[] members, String service, ChatCreationResponseManager responseListener) {
		//Checking if the client isn't ready
		if(getCurrentState() != stateConnected) {
			//Telling the response listener
			//responseListener.onFail(Constants.messageErrorCodeLocalNetwork, null);
			responseListener.onFail();
			
			//Returning false
			return false;
		}
		
		//Getting the request ID
		short requestID = getNextRequestID();
		
		//Sending the request
		boolean result = currentCommunicationsManager.requestChatCreation(requestID, members, service);
		
		//Validating the result
		if(!result) {
			//Telling the response listener
			//responseListener.onFail(Constants.messageErrorCodeLocalIO, null);
			responseListener.onFail();
			
			//Returning false
			return false;
		}
		
		//Adding the request
		chatCreationRequests.put(requestID, responseListener);
		
		//Starting the timer
		responseListener.startTimer();
		
		//Returning true
		return true;
	}
	
	private ArrayList<String> structConversationItemsToUsers(List<SharedValues.ConversationItem> structConversationItems) {
		//Creating the users list
		ArrayList<String> users = new ArrayList<>();
		
		//Iterating over the struct conversation items
		for(SharedValues.ConversationItem structConversationItem : structConversationItems) {
			//Getting the users
			String[] usersInStruct;
			if(structConversationItem instanceof SharedValues.MessageInfo)
				usersInStruct = new String[]{((SharedValues.MessageInfo) structConversationItem).sender};
			else if(structConversationItem instanceof SharedValues.GroupActionInfo)
				usersInStruct = new String[]{((SharedValues.GroupActionInfo) structConversationItem).agent, ((SharedValues.GroupActionInfo) structConversationItem).other};
			else if(structConversationItem instanceof SharedValues.ChatRenameActionInfo)
				usersInStruct = new String[]{((SharedValues.ChatRenameActionInfo) structConversationItem).agent};
			else continue;
			
			//Adding the user to the list if they're valid
			for(String user : usersInStruct) if(user != null) users.add(user);
		}
		
		//Returning the users list
		return users;
	}
	
	public static class TransferConversationStruct {
		private final String guid;
		private final ConversationInfo.ConversationState state;
		private final String name;
		private final List<ConversationItem> conversationItems;
		
		public TransferConversationStruct(String guid, ConversationInfo.ConversationState state, String name, List<ConversationItem> conversationItems) {
			this.guid = guid;
			this.state = state;
			this.name = name;
			this.conversationItems = conversationItems;
		}
		
		public String getGuid() {
			return guid;
		}
		
		public ConversationInfo.ConversationState getState() {
			return state;
		}
		
		public String getName() {
			return name;
		}
		
		public List<ConversationItem> getConversationItems() {
			return conversationItems;
		}
	}
	
	public static void cleanConversationItem(Blocks.ConversationItem conversationItem) {
		///Checking if the item is a message
		if(conversationItem instanceof Blocks.MessageInfo) {
			Blocks.MessageInfo messageInfo = (Blocks.MessageInfo) conversationItem;
			
			//Creating empty lists
			if(messageInfo.attachments == null) messageInfo.attachments = new ArrayList<>();
			if(messageInfo.stickers == null) messageInfo.stickers = new ArrayList<>();
			if(messageInfo.tapbacks == null) messageInfo.tapbacks = new ArrayList<>();
			
			//Invalidating empty strings
			if(messageInfo.text != null && messageInfo.text.isEmpty()) messageInfo.text = null;
			if(messageInfo.sendEffect != null && messageInfo.sendEffect.isEmpty()) messageInfo.sendEffect = null;
			
			for(Blocks.AttachmentInfo attachmentInfo : messageInfo.attachments) {
				if(attachmentInfo.type == null) attachmentInfo.type = Constants.defaultMIMEType;
			}
		} else if(conversationItem instanceof Blocks.ChatRenameActionInfo) {
			Blocks.ChatRenameActionInfo action = (Blocks.ChatRenameActionInfo) conversationItem;
			
			//Invalidating empty strings
			if(action.newChatName != null && action.newChatName.isEmpty()) action.newChatName = null;
		}/* else if(conversationItem instanceof Blocks.GroupActionInfo) {
			Blocks.GroupActionInfo action = (Blocks.GroupActionInfo) conversationItem;
		}*/
	}
	
	public static int countUnreadMessages(List<ConversationItem> items) {
		int count = 0;
		for(ConversationItem conversationItem : items) {
			if(conversationItem instanceof MessageInfo && !((MessageInfo) conversationItem).isOutgoing()) count++;
		}
		return count;
	}
	
	public static class FileDownloadRequestDeregistrationListener extends StaticInterfaceListener implements Consumer<FileDownloadRequest> {
		public FileDownloadRequestDeregistrationListener(ConnectionManager manager) {
			super(manager);
		}
		
		@Override
		public void accept(FileDownloadRequest request) {
			ConnectionManager manager = getManager();
			if(manager != null) manager.removeDownloadRequest(request);
		}
	}
	
	public static class MessageResponseManagerDeregistrationListener extends StaticInterfaceListener implements Consumer<MessageResponseManager> {
		public MessageResponseManagerDeregistrationListener(ConnectionManager manager) {
			super(manager);
		}
		
		@Override
		public void accept(MessageResponseManager item) {
			ConnectionManager manager = getManager();
			int index = manager.messageSendRequests.indexOfValue(item);
			if(index != -1) manager.messageSendRequests.removeAt(index);
		}
	}
	
	public static class ChatCreationDeregistrationListener extends StaticInterfaceListener implements Consumer<ChatCreationResponseManager> {
		public ChatCreationDeregistrationListener(ConnectionManager manager) {
			super(manager);
		}
		
		@Override
		public void accept(ChatCreationResponseManager item) {
			ConnectionManager manager = getManager();
			int index = manager.chatCreationRequests.indexOfValue(item);
			if(index != -1) manager.messageSendRequests.removeAt(index);
		}
	}
	
	private static class AtomicBooleanFinishTrigger implements Runnable {
		private final WeakReference<AtomicBoolean> reference;
		
		AtomicBooleanFinishTrigger(AtomicBoolean atomicBoolean) {
			reference = new WeakReference<>(atomicBoolean);
		}
		
		@Override
		public void run() {
			AtomicBoolean atomicBoolean = reference.get();
			if(atomicBoolean != null) atomicBoolean.set(false);
		}
	}
	
	private static class FileProcessingRequestUpdateConsumer extends StaticInterfaceListener implements Consumer<FileProcessingRequest> {
		FileProcessingRequestUpdateConsumer(ConnectionManager manager) {
			super(manager);
		}
		
		@Override
		public void accept(FileProcessingRequest request) {
			ConnectionManager connectionManager = getManager();
			if(connectionManager == null) return;
			connectionManager.fileProcessingRequestCurrent.set(request);
		}
	}
	
	public static abstract class Packager {
		/**
		 * Prepares data before being sent, usually by compressing it
		 *
		 * @param data the unpackaged data to be sent
		 * @param length the length of the data in the array
		 * @return the packaged data, or null if the process was unsuccessful
		 */
		public abstract byte[] packageData(byte[] data, int length);
		
		/**
		 * Reverts received data transmissions, usually be decompressing it
		 *
		 * @param data the packaged data
		 * @return the unpackaged data, or null if the process was unsuccessful
		 */
		public abstract byte[] unpackageData(byte[] data);
	}
	
	public static class PackagerGZIP extends Packager {
		@Override
		public byte[] packageData(byte[] data, int length) {
			try {
				return Constants.compressGZIP(data, length);
			} catch(IOException exception) {
				exception.printStackTrace();
				Crashlytics.logException(exception);
				
				return null;
			}
		}
		
		@Override
		public byte[] unpackageData(byte[] data) {
			try {
				return Constants.decompressGZIP(data);
			} catch(IOException exception) {
				exception.printStackTrace();
				
				return null;
			}
		}
	}
	
	interface ConnectionServiceSource {
		CommunicationsManager get(ConnectionManager connectionManager, Context context);
	}
	
	public static class PacketStruct {
		public final int type;
		public final byte[] content;
		public Runnable sentRunnable;
		
		public PacketStruct(int type, byte[] content) {
			this.type = type;
			this.content = content;
		}
		
		public PacketStruct(int type, byte[] content, Runnable sentRunnable) {
			this(type, content);
			this.sentRunnable = sentRunnable;
		}
	}
	
	private static class StaticInterfaceListener {
		private final WeakReference<ConnectionManager> managerReference;
		
		StaticInterfaceListener(ConnectionManager manager) {
			managerReference = new WeakReference<>(manager);
		}
		
		ConnectionManager getManager() {
			return managerReference.get();
		}
	}
	
	public interface ServiceCallbacks {
		void schedulePing();
		void cancelSchedulePing();
		
		void schedulePassiveReconnection();
		void cancelSchedulePassiveReconnection();
	}
}