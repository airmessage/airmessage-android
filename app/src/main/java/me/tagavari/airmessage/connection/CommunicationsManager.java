package me.tagavari.airmessage.connection;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import java.io.File;
import java.util.Collection;

import io.reactivex.rxjava3.core.Observable;
import me.tagavari.airmessage.connection.listener.CommunicationsManagerListener;
import me.tagavari.airmessage.enums.ConnectionErrorCode;
import me.tagavari.airmessage.enums.ConnectionFeature;
import me.tagavari.airmessage.enums.ProxyType;
import me.tagavari.airmessage.redux.ReduxEventAttachmentUpload;
import me.tagavari.airmessage.util.ConversationTarget;
import me.tagavari.airmessage.util.DirectConnectionParams;

public abstract class CommunicationsManager<Packet> {
	//Creating the handler
	private final Handler handler = new Handler(Looper.getMainLooper());
	
	//Creating the connection values
	private final CommunicationsManagerListener listener;
	@ProxyType private final int dataProxyType;
	private final DataProxy<Packet> dataProxy;
	
	public CommunicationsManager(CommunicationsManagerListener listener, @ProxyType int proxyType) {
		//Setting the values
		this.listener = listener;
		this.dataProxyType = proxyType;
		dataProxy = getDataProxy(proxyType);
		
		//Setting the data proxy listener
		dataProxy.setListener(new DataProxyListener<Packet>() {
			@Override
			public void handleOpen() {
				//Handling the event
				CommunicationsManager.this.handleOpen();
			}
			
			@Override
			public void handleClose(int reason) {
				//Forwarding the event to the listener
				if(listener != null) listener.onClose(reason);
				
				//Handling the event
				CommunicationsManager.this.handleClose(reason);
			}
			
			@Override
			public void handleMessage(Packet packet) {
				//Handling the event
				CommunicationsManager.this.handleMessage(packet);
			}
		});
	}
	
	/**
	 * Gets a data proxy from a data proxy type
	 * @param proxyType The data proxy type
	 * @return The data proxy
	 * @throws IllegalArgumentException If the proxy type is invalid or unsupported
	 */
	protected abstract DataProxy<Packet> getDataProxy(@ProxyType int proxyType);
	
	/**
	 * Connects to the server
	 */
	public void connect(Context context, @Nullable Object override) {
		//Connecting the proxy
		dataProxy.start(context, override);
	}
	
	/**
	 * Disconnects from the server
	 * @param code The error code if this disconnection, or -1 for default
	 */
	public final void disconnect(@ConnectionErrorCode int code) {
		dataProxy.stop(code);
	}
	
	//Used in implementations
	protected abstract void handleOpen();
	protected abstract void handleClose(@ConnectionErrorCode int reason);
	protected abstract void handleMessage(Packet packet);
	
	/**
	 * Handles a handshake message
	 * @param installationID The installation ID of the server
	 * @param deviceName The device name of the server
	 * @param systemVersion The system version of the server
	 * @param softwareVersion The AirMessage version of the server
	 */
	public void onHandshake(String installationID, String deviceName, String systemVersion, String softwareVersion) {
		//Forwarding the event to the listener
		if(listener != null) listener.onOpen(installationID, deviceName, systemVersion, softwareVersion);
	}
	
	/**
	 * Get whether the server is connected via a fallback method
	 * @return Whether the server is connected via a fallback method
	 */
	public abstract boolean isConnectedFallback();
	
	/**
	 * Sends a ping packet to the server
	 *
	 * @return Whether the message was successfully sent
	 */
	public abstract boolean sendPing();
	
	/**
	 * Requests a message to be sent to the specified conversation
	 *
	 * @param requestID The ID of the request
	 * @param conversation The conversation to send to
	 * @param message The message to send
	 * @return Whether the request was successfully sent
	 */
	public abstract boolean sendMessage(short requestID, ConversationTarget conversation, String message);
	
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
	public abstract boolean requestRetrievalTime(long timeLower, long timeUpper);
	
	/**
	 * Requests an ID range-based message retrieval
	 * @param idLower The ID to retrieve messages beyond (exclusive)
	 * @return Whether the request was successfully sent
	 */
	public abstract boolean requestRetrievalID(long idLower);
	
	/**
	 * Requests a mass message retrieval
	 *
	 * @param requestID The ID used to validate conflicting requests
	 * @param params The mass retrieval parameters to use
	 * @return Whether the request was successfully sent
	 */
	public abstract boolean requestRetrievalAll(short requestID, MassRetrievalParams params);
	
	/**
	 * Requests the creation of a new conversation on the server
	 * @param requestID The ID used to validate conflicting requests
	 * @param members The participating members' contact addresses for this conversation
	 * @param service The service that this conversation will use
	 * @return Whether the request was successfully sent
	 */
	public abstract boolean requestChatCreation(short requestID, String[] members, String service);
	
	/**
	 * Updates the server with this installation's push token
	 * @param token The token to send
	 * @return Whether the request was successfully sent
	 */
	public abstract boolean sendPushToken(String token);
	
	/**
	 * Checks if the specified communications version is applicable
	 *
	 * @param version The major communications version to check
	 * @return 0 if the version is applicable, -1 if the version is too old, 1 if the version is too new
	 */
	public abstract int checkCommVerApplicability(int version);
	
	/**
	 * Checks if the specified feature is supported by the current protocol
	 * @param featureID The feature to check
	 * @return Whether or not this protocol manager can handle the specified feature
	 */
	public abstract boolean isFeatureSupported(@ConnectionFeature int featureID);
	
	/**
	 * Checks if this current communications setup should be kept alive
	 * This usually entails:
	 * - Keepalive pings
	 * - Foreground service
	 * If persistence is not requested, the app will NOT try to keep the connection alive,
	 * and it is expected that the connection is re-established when necessary.
	 * @return Whether persistence should be enabled
	 */
	public abstract boolean requiresPersistence();
	
	/**
	 * Gets the active communications version
	 */
	public abstract String getCommunicationsVersion();
	
	/**
	 * Gets the handler for this communications manager
	 */
	public Handler getHandler() {
		return handler;
	}
	
	/**
	 * Gets the listener for interfacing with a communications manager
	 */
	private CommunicationsManagerListener getListener() {
		return listener;
	}
	
	/**
	 * Calls the provided callback function with the listener on the main thread
	 */
	public void runListener(Consumer<CommunicationsManagerListener> callback) {
		handler.post(() -> callback.accept(getListener()));
	}
	
	/**
	 * Gets the data proxy type
	 */
	protected int getDataProxyType() {
		return dataProxyType;
	}
	
	/**
	 * Gets the data proxy
	 */
	protected DataProxy<Packet> getDataProxy() {
		return dataProxy;
	}
}
