package me.tagavari.airmessage.connection;

import android.content.Context;

import java.util.List;

import me.tagavari.airmessage.connection.request.ConversationInfoRequest;

public abstract class CommunicationsManager<D, P> implements DataProxyListener<D> {
	protected final ConnectionManager connectionManager;
	protected DataProxy<D, P> dataProxy;
	protected final Context context;
	
	public CommunicationsManager(ConnectionManager connectionManager, Context context) {
		this.connectionManager = connectionManager;
		this.context = context;
	}
	
	/**
	 * Assigns the data proxy for this communications manager
	 * Must be called in the constructor extending this class
	 * @param dataProxy The data proxy to use
	 */
	protected void setProxy(DataProxy<D, P> dataProxy) {
		this.dataProxy = dataProxy;
	}
	
	/**
	 * Cleanly closes the connection between the server and this client
	 */
	public abstract void initiateClose();
	
	/**
	 * Connects to the server
	 */
	public final void connect() {
		//Connecting the proxy
		dataProxy.start();
	}
	
	/**
	 * Disconnects the connection manager from the server
	 */
	public final void disconnect() {
		//Disconnecting the proxy
		dataProxy.stop(ConnectionManager.intentResultCodeConnection);
	}
	
	@Override
	public void onOpen() {
		connectionManager.onConnect(this);
	}
	
	@Override
	public void onClose(int reason) {
		connectionManager.onDisconnect(this, reason);
	}
	
	@Override
	public void onMessage(D data) {
		connectionManager.onPacket(this);
	}
	
	public void onHandshakeCompleted(String installationID, String deviceName, String systemVersion, String softwareVersion) {
		connectionManager.onHandshakeCompleted(this, installationID, deviceName, systemVersion, softwareVersion);
	}
	
	/**
	 * Get whether or not the server is connected via the fallback address
	 * @return fallback
	 */
	public abstract boolean isConnectedFallback();
	
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
	 * Sends a ping packet to the server
	 *
	 * @return whether or not the message was successfully sent
	 */
	public abstract boolean sendPing();
	
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
	 * Checks if the specified feature is supported by the current protocol
	 * @param feature the feature to check
	 * @return whether or not this protocol manager can handle the specified feature
	 */
	public abstract boolean checkSupportsFeature(String feature);
}
