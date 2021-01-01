package me.tagavari.airmessage.connection.comm4;

import java.io.File;
import java.util.Collection;

import io.reactivex.rxjava3.core.Observable;
import me.tagavari.airmessage.connection.DataProxy;
import me.tagavari.airmessage.connection.MassRetrievalParams;
import me.tagavari.airmessage.redux.ReduxEventAttachmentUpload;
import me.tagavari.airmessage.util.ConversationTarget;

abstract class ProtocolManager<Packet> {
	protected final ClientComm4 communicationsManager;
	protected final DataProxy<Packet> dataProxy;
	
	/**
	 * Constructs a new protocol manager
	 * @param communicationsManager The communications manager to use
	 * @param dataProxy The data proxy to use
	 */
	ProtocolManager(ClientComm4 communicationsManager, DataProxy<Packet> dataProxy) {
		this.communicationsManager = communicationsManager;
		this.dataProxy = dataProxy;
	}
	
	/**
	 * Handles incoming data received from the server
	 *
	 * @param data The data received from the network
	 * @param type The header type of this data
	 */
	abstract void processData(byte[] data, int type);
	
	/**
	 * Sends a ping packet to the server
	 *
	 * @return Whether the message was successfully sent
	 */
	abstract boolean sendPing();
	
	/**
	 * Sends an authentication request to the server
	 *
	 * @return Whether the message was successfully sent
	 */
	abstract boolean sendAuthenticationRequest();
	
	/**
	 * Requests a message to be sent to the specified conversation
	 *
	 * @param requestID The ID of the request
	 * @param conversation The conversation to send to
	 * @param message The message to send
	 * @return Whether the request was successfully sent
	 */
	abstract boolean sendMessage(short requestID, ConversationTarget conversation, String message);
	
	/**
	 * Uploads a file chunk to be sent to the specified conversation
	 *
	 * @param requestID The ID of the request
	 * @param conversation The conversation to send to
	 * @param file The file to send
	 * @return A {@link ReduxEventAttachmentUpload} for the progress of this upload
	 */
	public abstract Observable<ReduxEventAttachmentUpload> sendFile(short requestID, ConversationTarget conversation, File file);
	
	/**
	 * Requests the download of a remote attachment
	 *
	 * @param requestID The ID of the request
	 * @param attachmentGUID The GUID of the attachment to fetch
	 * @return Whether the request was successful
	 */
	public abstract boolean requestAttachmentDownload(short requestID, String attachmentGUID);
	
	/**
	 * Sends a request to fetch conversation information
	 *
	 * @param list The list of conversation requests
	 * @return Whether the request was successfully sent
	 */
	public abstract boolean requestConversationInfo(Collection<String> list);
	
	/**
	 * Requests a time range-based message retrieval
	 *
	 * @param timeLower The lower time range limit
	 * @param timeUpper The upper time range limit
	 * @return Whether the request was successfully sent
	 */
	abstract boolean requestRetrievalTime(long timeLower, long timeUpper);
	
	/**
	 * Requests a mass message retrieval
	 *
	 * @param requestID The ID used to validate conflicting requests
	 * @param params The mass retrieval parameters to use
	 * @return Whether the request was successfully sent
	 */
	abstract boolean requestRetrievalAll(short requestID, MassRetrievalParams params);
	
	/**
	 * Requests the creation of a new conversation on the server
	 * @param requestID The ID used to validate conflicting requests
	 * @param members The participating members' contact addresses for this conversation
	 * @param service The service that this conversation will use
	 * @return Whether the request was successfully sent
	 */
	abstract boolean requestChatCreation(short requestID, String[] members, String service);
}
