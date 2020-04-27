package me.tagavari.airmessage.connection;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.util.SparseArray;

import androidx.core.util.Consumer;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.crashlytics.android.Crashlytics;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
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
import me.tagavari.airmessage.common.Blocks;
import me.tagavari.airmessage.connection.comm4.ClientComm4;
import me.tagavari.airmessage.connection.comm5.ClientComm5;
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
import me.tagavari.airmessage.util.Constants;

public class ConnectionManager {
	/* COMMUNICATIONS VERSION CHANGES
	 *  1 - Original release
	 *  2 - Serialization changes
	 *  3 - Original rework without WS layer
	 *  4 - Better stability and security, with sub-version support
	 *  5 - MessagePack serialization, encrypt everything
	 */
	public static final int mmCommunicationsVersion = 5;
	public static final int mmCommunicationsSubVersion = 1;
	
	public static final int maxPacketAllocation = 50 * 1024 * 1024; //50 MB
	
	public static final String localBCStateUpdate = "LocalMSG-ConnectionService-State";
	public static final String localBCMassRetrieval = "LocalMSG-ConnectionService-MassRetrievalProgress";
	public static final String localBCSyncNeeded = "LocalMSG-ConnectionService-SyncNeeded";
	
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
	
	public static final int proxyTypeDirect = 0;
	public static final int proxyTypeConnect = 1;
	
	public static final int largestFileSize = 1024 * 1024 * 100; //100 MB
	public static final int attachmentChunkSize = 1024 * 1024; //1 MiB
	
	private static final long pingExpiryTime = 40 * 1000; //40 seconds
	
	public static final Pattern regExValidPort = Pattern.compile("(:([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5]?))$");
	
	private static final SecureRandom secureRandom = new SecureRandom();
	
	//Creating the service values
	private final ServiceCallbacks serviceCallbacks;
	
	//Creating the communications values
	private static final List<Class> communicationsClassPriorityList = Arrays.asList(ClientComm5.class);
	private static final List<CommunicationsManagerSource> communicationsInstancePriorityList = Arrays.asList(ClientComm5::new);
	
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
	public static int proxyType = proxyTypeDirect;
	
	public static String hostname = null;
	public static String hostnameFallback = null;
	public static String password = null;
	
	private int currentState = stateDisconnected;
	private CommunicationsManager currentCommunicationsManager = null;
	private static byte currentLaunchID = 0;
	private static byte nextLaunchID = 0;
	private int lastConnectionResult = -1;
	private boolean connectionEstablished = false;
	
	private boolean flagShutdownRequested = false; //Disables forwarding when disconnecting
	
	private int activeCommunicationsVersion = -1;
	private int activeCommunicationsSubVersion = -1;
	private boolean serverSyncNeeded;
	private String serverInstallationID;
	private String serverDeviceName;
	private String serverSystemVersion;
	private String serverSoftwareVersion;
	
	//Creating the quick request variables
	private final SparseArray<MessageResponseManager> messageSendRequests = new SparseArray<>();
	private final SparseArray<ChatCreationResponseManager> chatCreationRequests = new SparseArray<>();
	private short currentRequestID = 0;
	
	//Creating the handler values
	private final Handler handler = new Handler();
	private final Runnable pingExpiryRunnable = this::disconnect;
	
	private final ArrayList<ConversationInfoRequest> pendingConversations = new ArrayList<>();
	
	public ConnectionManager(ServiceCallbacks serviceCallbacks) {
		//Setting the service information
		this.serviceCallbacks = serviceCallbacks;
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
	
	public void onConnect(CommunicationsManager communicationsManager) {
		//Setting the current connections manager
		currentCommunicationsManager = communicationsManager;
	}
	
	public void onHandshakeCompleted(CommunicationsManager communicationsManager, String installationID, String deviceName, String systemVersion, String softwareVersion) {
		//Setting the connection as established
		connectionEstablished = true;
		
		//Updating the state
		updateState(getContext(), stateConnected, 0, getCurrentLaunchID());
		
		//Scheduling the ping
		serviceCallbacks.schedulePing();
		
		//Stopping the passive reconnection timer
		serviceCallbacks.cancelSchedulePassiveReconnection();
		
		//Updating shared preferences
		SharedPreferences sharedPrefs = MainApplication.getInstance().getConnectivitySharedPrefs();
		
		long lastConnectionTime = sharedPrefs.getLong(MainApplication.sharedPreferencesConnectivityKeyLastConnectionTime, System.currentTimeMillis());
		
		SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();
		sharedPrefsEditor.putLong(MainApplication.sharedPreferencesConnectivityKeyLastConnectionTime, System.currentTimeMillis());
		
		//Checking if an installation ID was provided
		boolean isNewServer; //Is this server different from the one we connected to last time?
		boolean isNewServerSinceSync; //Is this server different from the one we connected to last time, since we last synced our messages?
		if(installationID != null) {
			//Getting the last installation ID
			String lastInstallationID = sharedPrefs.getString(MainApplication.sharedPreferencesConnectivityKeyLastConnectionInstallationID, null);
			String lastInstallationIDSinceSync = sharedPrefs.getString(MainApplication.sharedPreferencesConnectivityKeyLastSyncInstallationID, null);
			
			//If the installation ID changed, we are connected to a new server
			isNewServer = !installationID.equals(lastInstallationID);
			
			//Updating the saved value
			if(isNewServer) sharedPrefsEditor.putString(MainApplication.sharedPreferencesConnectivityKeyLastConnectionInstallationID, installationID);
			
			//"notrigger" is assigned to this value when upgrading from 0.5.X to prevent sync prompts after upgrading
			if("notrigger".equals(lastInstallationIDSinceSync)) {
				//Don't sync messages
				isNewServerSinceSync = false;
				
				//Update the saved value for next time
				sharedPrefsEditor.putString(MainApplication.sharedPreferencesConnectivityKeyLastSyncInstallationID, installationID);
			} else {
				isNewServerSinceSync = !installationID.equals(lastInstallationIDSinceSync);
			}
		} else {
			//Getting the last hostname
			String lastConnectionHostname = sharedPrefs.getString(MainApplication.sharedPreferencesConnectivityKeyLastConnectionHostname, null);
			
			//If the hostname changed, assume we are connected to a new server
			isNewServer = !hostname.equals(lastConnectionHostname);
			
			//Updating the saved value
			if(isNewServer) sharedPrefsEditor.putString(MainApplication.sharedPreferencesConnectivityKeyLastConnectionHostname, ConnectionManager.hostname);
			
			//No way to tell
			isNewServerSinceSync = false;
		}
		
		//Applying shared preferences changes
		sharedPrefsEditor.apply();
		
		//Setting the last connection result
		setLastConnectionResult(ConnectionManager.intentResultCodeSuccess);
		
		//Retrieving the pending conversation info
		communicationsManager.sendConversationInfoRequest(pendingConversations);
		
		//Checking if we are still connected to the same server
		if(!isNewServer) {
			//Fetching the messages since the last connection time
			retrieveMessagesSince(lastConnectionTime, System.currentTimeMillis());
		}
		
		//Checking if we are connected to a new server since syncing (and thus should prompt the user to sync)
		serverSyncNeeded = isNewServerSinceSync;
		if(isNewServerSinceSync) {
			//Sending a broadcast
			LocalBroadcastManager.getInstance(getContext()).sendBroadcast(
					new Intent(localBCSyncNeeded)
							.putExtra(Constants.intentParamInstallationID, installationID)
							.putExtra(Constants.intentParamName, deviceName)
					);
		}
		
		//Recording the server information
		serverInstallationID = installationID;
		serverDeviceName = deviceName;
		serverSystemVersion = systemVersion;
		serverSoftwareVersion = softwareVersion;
	}
	
	public void onDisconnect(CommunicationsManager communicationsManager, int code) {
		//Checking if there was an established connection
		if(connectionEstablished) {
			//Recording the end time
			SharedPreferences.Editor editor = MainApplication.getInstance().getConnectivitySharedPrefs().edit();
			editor.putLong(MainApplication.sharedPreferencesConnectivityKeyLastConnectionTime, System.currentTimeMillis());
			editor.apply();
		}
		//Checking if no shutdown was requested and the code is an error
		else if(!flagShutdownRequested && code != intentResultCodeUnauthorized) {
			//Connecting via the next communications manager
			int targetIndex = communicationsClassPriorityList.indexOf(communicationsManager.getClass()) + 1;
			if(targetIndex < communicationsInstancePriorityList.size()) {
				communicationsInstancePriorityList.get(targetIndex).get(this, proxyType, getContext()).connect();
				return;
			}
		}
		
		//Updating the state
		updateState(getContext(), stateDisconnected, code, getCurrentLaunchID());
		
		//Cancelling the keepalive timer
		serviceCallbacks.cancelSchedulePing();
		
		//Starting the passive reconnection timer
		serviceCallbacks.schedulePassiveReconnection();
		
		//Resetting the flags
		connectionEstablished = false;
		flagShutdownRequested = false;
	}
	
	public void onPacket(CommunicationsManager communicationsManager) {
		//Cancelling the ping timer
		handler.removeCallbacks(pingExpiryRunnable);
		
		//Scheduling a new ping
		serviceCallbacks.schedulePing();
		
		//Updating the last connection time
		if(connectionEstablished) {
			SharedPreferences.Editor editor = MainApplication.getInstance().getConnectivitySharedPrefs().edit();
			editor.putLong(MainApplication.sharedPreferencesConnectivityKeyLastConnectionTime, System.currentTimeMillis());
			editor.apply();
		}
	}
	
	private Context getContext() {
		return MainApplication.getInstance();
	}
	
	public boolean connect(Context context) {
		return connect(context, getNextLaunchID());
	}
	
	public boolean connect(Context context, byte launchID) {
		//Closing the current connection if it exists
		if(getCurrentState() != stateDisconnected) disconnect();
		
		//Setting the current launch ID
		currentLaunchID = launchID;
		
		//Returning if there is no connection
		{
			NetworkInfo activeNetwork = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
			boolean isConnected = activeNetwork != null && activeNetwork.isConnected();
			if(!isConnected) {
				//Notifying the connection listeners
				broadcastState(context, stateDisconnected, intentResultCodeConnection, launchID);
				
				return false;
			}
		}
		
		/* //Getting the proxy
		DataProxy proxy = getDataProxy(context);
		
		//Checking if the proxy is invalid
		if(proxy == null) {
			//Notifying the connection listeners
			broadcastState(context, stateDisconnected, intentResultCodeConnection, launchID);
			
			return false;
		} */
		
		//Notifying the state
		updateState(context, stateConnecting, 0, launchID);
		
		//Connecting through the top of the priority queue
		communicationsInstancePriorityList.get(0).get(this, proxyType, context).connect();
		
		//Return true
		return true;
	}
	
	public void disconnect() {
		if(currentCommunicationsManager != null) currentCommunicationsManager.initiateClose();
	}
	
	public void ping() {
		//Returning if there is no connection
		if(currentState != stateConnected) return;
		
		//Sending a ping
		currentCommunicationsManager.sendPing();
		
		//Starting the ping timer
		handler.postDelayed(pingExpiryRunnable, pingExpiryTime);
	}
	
	public void setFlagShutdownRequested() {
		flagShutdownRequested = true;
	}
	
	public boolean isConnected() {
		return currentState == stateConnected;
	}
	
	public int getCurrentState() {
		return currentState;
	}
	
	public boolean isConnectedFallback() {
		if(currentCommunicationsManager == null) return false;
		return currentCommunicationsManager.isConnectedFallback();
	}
	
	public CommunicationsManager getCurrentCommunicationsManager() {
		return currentCommunicationsManager;
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
	
	public boolean isServerSyncNeeded() {
		return serverSyncNeeded;
	}
	
	public void clearServerSyncNeeded() {
		serverSyncNeeded = false;
	}
	
	public String getServerInstallationID() {
		return serverInstallationID;
	}
	
	public String getServerDeviceName() {
		return serverDeviceName;
	}
	
	public String getServerSystemVersion() {
		return serverSystemVersion;
	}
	
	public String getServerSoftwareVersion() {
		return serverSoftwareVersion;
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
	
	public static byte getNextLaunchID() {
		return (byte) (currentLaunchID + 1);
	}
	
	public int getLastConnectionResult() {
		return lastConnectionResult;
	}
	
	public void setLastConnectionResult(int lastConnectionResult) {
		this.lastConnectionResult = lastConnectionResult;
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
	
	private void updateState(Context context, int state, int code, byte launchID) {
		//Updating the current state
		currentState = state;
		
		//Broadcasting the state
		broadcastState(context, state, code, launchID);
	}
	
	/**
	 * Sends a broadcast to the listeners
	 *
	 * @param state the state of the connection
	 * @param code the error code, if the state is disconnected
	 * @param launchID the launch ID of the connection
	 */
	private void broadcastState(Context context, int state, int code, byte launchID) {
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
		if(currentState != stateConnected) return false;
		
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
		if((massRetrievalThread != null && massRetrievalThread.isInProgress()) || currentState != stateConnected || currentMassRetrievalParams == null) return false;
		
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
		if(currentCommunicationsManager == null || currentState != stateConnected) return false;
		
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
		if(currentState != stateConnected) {
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
		if(currentState != stateConnected) {
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
		if(currentState != stateConnected) return false;
		
		//Sending the request
		return currentCommunicationsManager.requestRetrievalTime(timeLower, timeUpper);
	}
	
	public boolean createChat(String[] members, String service, ChatCreationResponseManager responseListener) {
		//Checking if the client isn't ready
		if(currentState != stateConnected) {
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
	
	public static SecureRandom getSecureRandom() {
		return secureRandom;
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
			if(manager != null) {
				int index = manager.messageSendRequests.indexOfValue(item);
				if(index != -1) manager.messageSendRequests.removeAt(index);
			}
		}
	}
	
	public static class ChatCreationDeregistrationListener extends StaticInterfaceListener implements Consumer<ChatCreationResponseManager> {
		public ChatCreationDeregistrationListener(ConnectionManager manager) {
			super(manager);
		}
		
		@Override
		public void accept(ChatCreationResponseManager item) {
			ConnectionManager manager = getManager();
			if(manager != null) {
				int index = manager.chatCreationRequests.indexOfValue(item);
				if(index != -1) manager.chatCreationRequests.removeAt(index);
			}
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
			ConnectionManager manager = getManager();
			if(manager != null) manager.fileProcessingRequestCurrent.set(request);
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
	
	interface CommunicationsManagerSource {
		CommunicationsManager get(ConnectionManager connectionManager, int proxyType, Context context);
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