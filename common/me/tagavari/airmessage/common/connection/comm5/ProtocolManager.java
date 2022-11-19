package me.tagavari.airmessage.common.connection.comm5;

import androidx.annotation.NonNull;

import io.reactivex.rxjava3.core.Observable;
import me.tagavari.airmessage.common.connection.DataProxy;
import me.tagavari.airmessage.common.connection.MassRetrievalParams;
import me.tagavari.airmessage.common.enums.ConnectionFeature;
import me.tagavari.airmessage.common.redux.ReduxEventAttachmentUpload;
import me.tagavari.airmessage.common.util.ConversationTarget;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

abstract class ProtocolManager<Packet> {
	protected final ClientComm5 communicationsManager;
	protected final DataProxy<Packet> dataProxy;
	
	/**
	 * Constructs a new protocol manager
	 * @param communicationsManager The communications manager to use
	 * @param dataProxy The data proxy to use
	 */
	ProtocolManager(ClientComm5 communicationsManager, DataProxy<Packet> dataProxy) {
		this.communicationsManager = communicationsManager;
		this.dataProxy = dataProxy;
	}
	
	/**
	 * Handles incoming data received from the server
	 *
	 * @param data The data received from the network
	 * @param wasEncrypted True if this data was encrypted
	 */
	abstract void processData(byte[] data, boolean wasEncrypted);
	
	/**
	 * Sends a ping packet to the server
	 *
	 * @return Whether the message was successfully sent
	 */
	abstract boolean sendPing();
	
	/**
	 * Sends an authentication request to the server
	 *
	 * @param unpacker The unpacker of the server's info data, after reading the communications versions
	 * @return Whether the message was successfully sent
	 */
	abstract boolean sendAuthenticationRequest(AirUnpacker unpacker) throws IOException;
	
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
	 * @param conversations The list of conversation GUIDs to request
	 * @return Whether the request was successfully sent
	 */
	public abstract boolean requestConversationInfo(Collection<String> conversations);
	
	/**
	 * Requests a time range-based message retrieval
	 *
	 * @param timeLower The lower time range limit
	 * @param timeUpper The upper time range limit
	 * @return Whether the request was successfully sent
	 */
	abstract boolean requestRetrievalTime(long timeLower, long timeUpper);
	
	/**
	 * Requests an ID range-based message retrieval
	 *
	 * @param idLower The ID to retrieve messages beyond (exclusive)
	 * @param timeLower The lower time range limit
	 * @param timeUpper The upper time range limit
	 * @return Whether the request was successfully sent
	 */
	abstract boolean requestRetrievalID(long idLower, long timeLower, long timeUpper);
	
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

	/**
	 * Installs the server update with the specified ID
	 * @param updateID The ID of the update to install
	 * @return Whether the request was successfully sent
	 */
	abstract boolean installSoftwareUpdate(int updateID);
	
	/**
	 * Requests a new FaceTime link
	 * @return Whether the request was successfully sent
	 */
	abstract boolean requestFaceTimeLink();
	
	/**
	 * Initiates a new outgoing FaceTime call with the specified addresses
	 * @param addresses The list of addresses to initiate the call with
	 * @return Whether the request was successfully sent
	 */
	abstract boolean initiateFaceTimeCall(List<String> addresses);
	
	/**
	 * Accepts or rejects a pending incoming FaceTime call
	 * @param caller The name of the caller to accept the call of
	 * @param accept True to accept the call, or false to reject
	 * @return Whether the request was successfully sent
	 */
	abstract boolean handleIncomingFaceTimeCall(@NonNull String caller, boolean accept);
	
	/**
	 * Tells the server to leave the FaceTime call.
	 * This should be called after the client has connected to the call with the
	 * FaceTime link to avoid two of the same user connected.
	 * @return Whether the request was successfully sent
	 */
	abstract boolean dropFaceTimeCallServer();

	/**
	 * Checks if the specified feature is supported by the current protocol
	 * @param featureID The feature to check
	 * @return Whether this protocol manager can handle the specified feature
	 */
	abstract boolean isFeatureSupported(@ConnectionFeature int featureID);
}
