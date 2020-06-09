package me.tagavari.airmessage.connection.comm5;

import android.content.Context;

import java.io.IOException;
import java.util.List;

import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.connection.MassRetrievalParams;
import me.tagavari.airmessage.connection.request.ConversationInfoRequest;

abstract class ProtocolManager {
	final Context context;
	final ConnectionManager connectionManager;
	final ClientComm5 communicationsManager;
	
	/**
	 * Constructor
	 * @param connectionManager The connections manager instance
	 * @param communicationsManager The communications manager instance
	 */
	ProtocolManager(Context context, ConnectionManager connectionManager, ClientComm5 communicationsManager) {
		this.context = context;
		this.connectionManager = connectionManager;
		this.communicationsManager = communicationsManager;
	}
	
	/**
	 * Sends a ping packet to the server
	 *
	 * @param sentRunnable execute this runnable after the message is sent
	 * @return whether or not the message was successfully sent
	 */
	abstract boolean sendConnectionClose(Runnable sentRunnable);
	
	/**
	 * Sends a ping packet to the server
	 *
	 * @return whether or not the message was successfully sent
	 */
	abstract boolean sendPing();
	
	/**
	 * Handles incoming data received from the server
	 *
	 * @param data the data received from the network
	 * @param wasEncrypted true if this data was encrypted
	 */
	abstract void processData(byte[] data, boolean wasEncrypted);
	
	/**
	 * Sends an authentication request to the server
	 *
	 * @param unpacker The unpacker of the server's info data, after reading the communications versions
	 * @return whether or not the message was successfully sent
	 */
	abstract boolean sendAuthenticationRequest(AirUnpacker unpacker) throws IOException;
	
	/**
	 * Requests a message to be sent to the specified conversation
	 *
	 * @param requestID the ID of the request
	 * @param chatGUID the GUID of the target conversation
	 * @param message the message to send
	 * @return whether or not the request was successfully sent
	 */
	abstract boolean sendMessage(short requestID, String chatGUID, String message);
	
	/**
	 * Requests a message to be send to the specified conversation members via the service
	 *
	 * @param requestID the ID of the request
	 * @param chatMembers the members to send the message to
	 * @param message the message to send
	 * @param service the service to send the message across
	 * @return whether or not the request was successfully sent
	 */
	abstract boolean sendMessage(short requestID, String[] chatMembers, String message, String service);
	
	/**
	 * Requests the download of a remote attachment
	 *
	 * @param requestID the ID of the request
	 * @return whether or not the request was successful
	 */
	abstract boolean addDownloadRequest(short requestID, String attachmentGUID, Runnable sentRunnable);
	
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
	abstract boolean uploadFilePacket(short requestID, int requestIndex, String conversationGUID, byte[] data, String fileName, boolean isLast);
	
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
	abstract boolean uploadFilePacket(short requestID, int requestIndex, String[] conversationMembers, byte[] data, String fileName, String service, boolean isLast);
	
	/**
	 * Sends a request to fetch conversation information
	 *
	 * @param list the list of conversation requests
	 * @return whether or not the request was successfully sent
	 */
	abstract boolean sendConversationInfoRequest(List<ConversationInfoRequest> list);
	
	/**
	 * Requests a time range-based message retrieval
	 *
	 * @param timeLower the lower time range limit
	 * @param timeUpper the upper time range limit
	 * @return whether or not the request was successfully sent
	 */
	abstract boolean requestRetrievalTime(long timeLower, long timeUpper);
	
	/**
	 * Requests a mass message retrieval
	 *
	 * @param requestID the ID used to validate conflicting requests
	 * @param params the mass retrieval parameters to use
	 * @return whether or not the request was successfully sent
	 */
	abstract boolean requestRetrievalAll(short requestID, MassRetrievalParams params);
	
	/**
	 * Requests the creation of a new conversation on the server
	 * @param requestID the ID used to validate conflicting requests
	 * @param members the participating members' contact addresses for this conversation
	 * @param service the service that this conversation will use
	 * @return whether or not the request was successfully sent
	 */
	abstract boolean requestChatCreation(short requestID, String[] members, String service);
	
	/**
	 * Gets a packager for processing transferable data via this protocol version
	 *
	 * @return the packager
	 */
	abstract ConnectionManager.Packager getPackager();
	
	/**
	 * Returns the hash algorithm to use with this protocol
	 *
	 * @return the hash algorithm
	 */
	abstract String getHashAlgorithm();
	
	/**
	 * Checks if the specified feature is supported by the current protocol
	 * @param feature the feature to check
	 * @return whether or not this protocol manager can handle the specified feature
	 */
	abstract boolean checkSupportsFeature(String feature);
}
