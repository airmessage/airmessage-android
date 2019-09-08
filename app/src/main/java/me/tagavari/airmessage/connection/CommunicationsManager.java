package me.tagavari.airmessage.connection;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import java.util.List;

import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.connection.request.ConversationInfoRequest;

public abstract class CommunicationsManager {
	//Creating the reference values
	private static final long pingExpiryTime = 20 * 1000; //20 seconds
	
	protected ConnectionManager connectionManager;
	protected final Context context;
	
	//Creating the request values
	public byte launchID;
	
	//Creating the connection values
	private final Handler handler = new Handler();
	private final Runnable pingExpiryRunnable = this::disconnectReconnect;
	
	public CommunicationsManager(ConnectionManager connectionManager, Context context) {
		this.connectionManager = connectionManager;
		this.context = context;
	}
	
	/**
	 * Connects to the server
	 *
	 * @param launchID an ID to represent and track this connection
	 * @return whether or not the request was successful
	 */
	public boolean connect(byte launchID) {
		connectionManager.updateCommunicationsManager(this);
		this.launchID = launchID;
		
		//Stopping the passive reconnection timer
		connectionManager.getServiceCallbacks().cancelSchedulePassiveReconnection();
		
		return false;
	}
	
	/**
	 * Connects to the server with multiple attempts, (eg. after a network switch)
	 */
	public abstract void disconnectReconnect();
	
	/**
	 * Disconnects the connection manager from the server
	 */
	public void disconnect() {
		//Clearing the reconnect flag
		connectionManager.setFlagDropReconnect(false);
	}
	
	/**
	 * Cleans up timers after a disconnection occurs
	 */
	public void handleDisconnection() {
		//Cancelling the keepalive timer
		connectionManager.getServiceCallbacks().cancelSchedulePing();
		
		//Cancelling the expiry timer
		handler.removeCallbacks(pingExpiryRunnable);
		
		//Starting the passive reconnection timer
		connectionManager.getServiceCallbacks().schedulePassiveReconnection();
	}
	
	/**
	 * Get the current state of the connection manager
	 *
	 * @return an integer representing the state
	 */
	public abstract int getState();
	
	/**
	 * Get whether or not the server is connected via the fallback address
	 * @return fallback
	 */
	public abstract boolean isConnectedFallback();
	
	/**
	 * Sends a ping packet to the server
	 *
	 * @return whether or not the message was successfully sent
	 */
	public boolean sendPing() {
		//Starting the ping timer
		handler.postDelayed(pingExpiryRunnable, pingExpiryTime);
		
		return false;
	}
	
	/**
	 * Notifies the connection manager of a message, cancelling the ping expiry timer
	 */
	public void onMessage() {
		//Cancelling the ping timer
		handler.removeCallbacks(pingExpiryRunnable);
		
		//Updating the scheduled ping
		connectionManager.getServiceCallbacks().schedulePing();
		
		//Updating the last connection time
		if(connectionManager.getFlagMarkEndTime()) {
			SharedPreferences.Editor editor = MainApplication.getInstance().getConnectivitySharedPrefs().edit();
			editor.putLong(MainApplication.sharedPreferencesConnectivityKeyLastConnectionTime, System.currentTimeMillis());
			editor.apply();
		}
	}
	
	/**
	 * Gets a packager for processing transferable data via this protocol version
	 *
	 * @return the packager
	 */
	public abstract ConnectionManager.Packager getPackager();
	
	/**
	 * Returns the hash algorithm to use with this protocol
	 *
	 * @return the hash algorithm
	 */
	public abstract String getHashAlgorithm();
	
	/**
	 * Requests a message to be sent to the specified conversation
	 *
	 * @param requestID the ID of the request
	 * @param chatGUID the GUID of the target conversation
	 * @param message the message to send
	 * @return whether or not the request was successfully sent
	 */
	public abstract boolean sendMessage(short requestID, String chatGUID, String message);
	
	/**
	 * Requests a message to be send to the specified conversation members via the service
	 *
	 * @param requestID the ID of the request
	 * @param chatMembers the members to send the message to
	 * @param message the message to send
	 * @param service the service to send the message across
	 * @return whether or not the request was successfully sent
	 */
	public abstract boolean sendMessage(short requestID, String[] chatMembers, String message, String service);
	
	/**
	 * Requests the download of a remote attachment
	 *
	 * @param requestID the ID of the request
	 * @param attachmentGUID the GUID of the attachment to fetch
	 * @param sentRunnable the runnable to call once the request has been sent (usually used to initialize the timeout)
	 * @return whether or not the request was successful
	 */
	public abstract boolean addDownloadRequest(short requestID, String attachmentGUID, Runnable sentRunnable);
	
	/**
	 * Uploads a file chunk to be sent to the specified conversation
	 *
	 * @param requestID the ID of the request
	 * @param requestIndex the index of the request
	 * @param conversationGUID the conversation to send the file to
	 * @param data the transmission-ready bytes of the file chunk
	 * @param fileName the name of the file to send
	 * @param isLast whether or not this is the last file packet
	 * @return whether or not the action was successful
	 */
	public abstract boolean uploadFilePacket(short requestID, int requestIndex, String conversationGUID, byte[] data, String fileName, boolean isLast);
	
	/**
	 * Uploads a file chunk to be sent to the specified conversation members
	 *
	 * @param requestID the ID of the request
	 * @param requestIndex the index of the request
	 * @param conversationMembers the members of the conversation to send the file to
	 * @param data the transmission-ready bytes of the file chunk
	 * @param fileName the name of the file to send
	 * @param service the service to send the file across
	 * @param isLast whether or not this is the last file packet
	 * @return whether or not the action was successful
	 */
	public abstract boolean uploadFilePacket(short requestID, int requestIndex, String[] conversationMembers, byte[] data, String fileName, String service, boolean isLast);
	
	/**
	 * Sends a request to fetch conversation information
	 *
	 * @param list the list of conversation requests
	 * @return whether or not the request was successfully sent
	 */
	public abstract boolean sendConversationInfoRequest(List<ConversationInfoRequest> list);
	
	/**
	 * Requests a time range-based message retrieval
	 *
	 * @param timeLower the lower time range limit
	 * @param timeUpper the upper time range limit
	 * @return whether or not the request was successfully sent
	 */
	public abstract boolean requestRetrievalTime(long timeLower, long timeUpper);
	
	/**
	 * Requests a mass message retrieval
	 *
	 * @param requestID the ID used to validate conflicting requests
	 * @param params the mass retrieval parameters to use
	 * @return whether or not the request was successfully sent
	 */
	public abstract boolean requestRetrievalAll(short requestID, MassRetrievalParams params);
	
	/**
	 * Requests the creation of a new conversation on the server
	 * @param requestID the ID used to validate conflicting requests
	 * @param members the participating members' contact addresses for this conversation
	 * @param service the service that this conversation will use
	 * @return whether or not the request was successfully sent
	 */
	public abstract boolean requestChatCreation(short requestID, String[] members, String service);
	
	/**
	 * Checks if the specified communications version is applicable
	 *
	 * @param version the major communications version to check
	 * @return 0 if the version is applicable, -1 if the version is too old, 1 if the version is too new
	 */
	public abstract int checkCommVerApplicability(int version);
	
	/**
	 * Forwards a request to the next connection manager
	 *
	 * @param launchID an ID used to identify connection attempts
	 * @param thread whether or not to use a new thread
	 * @return if the request was forwarded
	 */
	public boolean forwardRequest(byte launchID, boolean thread) {
		int targetIndex = connectionManager.communicationsClassPriorityList.indexOf(getClass()) + 1;
		if(targetIndex == connectionManager.communicationsInstancePriorityList.size()) return false;
		if(thread) {
			new Handler(Looper.getMainLooper()).post(() -> {
				if(ConnectionManager.getCurrentLaunchID() == launchID) connectionManager.communicationsInstancePriorityList.get(targetIndex).get(connectionManager, context).connect(launchID);
			});
		} else connectionManager.communicationsInstancePriorityList.get(targetIndex).get(connectionManager, context).connect(launchID);
		return true;
	}
	
	/**
	 * Checks if the specified feature is supported by the current protocol
	 * @param feature the feature to check
	 * @return whether or not this protocol manager can handle the specified feature
	 */
	public abstract boolean checkSupportsFeature(String feature);
}
