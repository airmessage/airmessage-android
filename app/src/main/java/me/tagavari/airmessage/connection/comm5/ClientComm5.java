package me.tagavari.airmessage.connection.comm5;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.security.GeneralSecurityException;
import java.util.Collection;

import io.reactivex.rxjava3.core.Observable;
import me.tagavari.airmessage.connection.CommunicationsManager;
import me.tagavari.airmessage.connection.DataProxy;
import me.tagavari.airmessage.connection.MassRetrievalParams;
import me.tagavari.airmessage.connection.exception.AMRequestException;
import me.tagavari.airmessage.connection.listener.CommunicationsManagerListener;
import me.tagavari.airmessage.data.SharedPreferencesManager;
import me.tagavari.airmessage.enums.ConnectionErrorCode;
import me.tagavari.airmessage.enums.ConnectionFeature;
import me.tagavari.airmessage.enums.MessageSendErrorCode;
import me.tagavari.airmessage.enums.ProxyType;
import me.tagavari.airmessage.redux.ReduxEventAttachmentUpload;
import me.tagavari.airmessage.util.ConnectionParams;
import me.tagavari.airmessage.util.ConversationTarget;

public class ClientComm5 extends CommunicationsManager<EncryptedPacket> {
	private static final String TAG = ClientComm5.class.getSimpleName();
	
	private static final int communicationsVersion = 5;
	
	//Creating the connection values
	private ProtocolManager<EncryptedPacket> protocolManager = null;
	private int protocolManagerVer = -1;
	
	//Creating the handshake values
	private final Runnable handshakeExpiryRunnable = () -> {
		Log.i(TAG, "Disconnecting from server due to handshake expiry");
		disconnect(ConnectionErrorCode.connection);
	};
	private static final long handshakeExpiryTime = 1000 * 10; //10 seconds
	
	//Creating the transmission values
	private static final int nhtInformation = 100;
	
	//Creating the state values
	private boolean connectionOpened = false;
	
	//Creating the parameter values
	private String password;
	
	public ClientComm5(CommunicationsManagerListener listener, int proxyType) {
		super(listener, proxyType);
	}
	
	@Override
	public void connect(Context context, @Nullable ConnectionParams override) {
		//Saving the password for protocol-level encryption
		if(override == null) {
			try {
				password = SharedPreferencesManager.getDirectConnectionPassword(context);
			} catch(GeneralSecurityException | IOException exception) {
				exception.printStackTrace();
			}
		} else {
			password = ((ConnectionParams.Security) override).getPassword();
		}
		
		super.connect(context, override);
	}
	
	@Override
	protected DataProxy<EncryptedPacket> getDataProxy(int proxyType) {
		//Assigning the proxy
		if(proxyType == ProxyType.direct) return new ProxyDirectTCP();
		else if(proxyType == ProxyType.connect) return new ProxyConnect();
		else throw new IllegalArgumentException("Unknown proxy type " + proxyType);
	}
	
	protected void handleOpen() {
		//Starting the handshake timeout
		startTimeoutTimer();
		
		//Setting the state
		connectionOpened = true;
	}
	
	@Override
	protected void handleClose(@ConnectionErrorCode int reason) {
		//Cancelling the handshake timeout
		stopTimeoutTimer();
		
		//Invalidating the connection managers
		protocolManager = null;
		protocolManagerVer = -1;
		
		//Setting the state
		connectionOpened = false;
	}
	
	void startTimeoutTimer() {
		getHandler().postDelayed(handshakeExpiryRunnable, handshakeExpiryTime);
	}
	
	void stopTimeoutTimer() {
		getHandler().removeCallbacks(handshakeExpiryRunnable);
	}
	
	@Override
	protected void handleMessage(EncryptedPacket packet) {
		//Sending an update for the received packet
		runListener(CommunicationsManagerListener::onPacket);
		
		//Processing the data
		if(protocolManager != null) protocolManager.processData(packet.getData(), packet.getEncrypt());
		else processFloatingData(packet.getData());
	}
	
	/**
	 * Processes any data before a protocol manager is selected, usually to handle version processing
	 */
	private void processFloatingData(byte[] data) {
		//Unpacking the data
		AirUnpacker unpacker = new AirUnpacker(data);
		
		try {
			//Reading the message type
			int messageType = unpacker.unpackInt();
			
			if(messageType == nhtInformation) {
				processServerInformation(unpacker);
			}
		} catch(IOException | BufferUnderflowException exception) {
			//Logging the exception
			exception.printStackTrace();
			
			//Disconnecting
			getHandler().post(() -> disconnect(ConnectionErrorCode.connection));
		}
	}
	
	private void processServerInformation(AirUnpacker unpacker) throws IOException {
		//Restarting the authentication timer
		getHandler().removeCallbacks(handshakeExpiryRunnable);
		getHandler().postDelayed(handshakeExpiryRunnable, handshakeExpiryTime);
		
		//Reading the communications version information
		int communicationsVersion = unpacker.unpackInt();
		int communicationsSubVersion = unpacker.unpackInt();
		
		//Checking if the client can't handle this communications version
		int verApplicability = checkCommVerApplicability(communicationsVersion);
		if(verApplicability != 0) {
			//Terminating the connection
			getHandler().post(() -> disconnect(verApplicability < 0 ? ConnectionErrorCode.serverOutdated : ConnectionErrorCode.clientOutdated));
			return;
		}
		
		//Finding a matching protocol manager
		protocolManager = findProtocolManager(communicationsSubVersion);
		if(protocolManager == null) {
			getHandler().post(() -> disconnect(ConnectionErrorCode.clientOutdated));
			return;
		}
		
		//Updating the protocol version
		protocolManagerVer = communicationsSubVersion;
		
		//Sending the handshake data
		protocolManager.sendAuthenticationRequest(unpacker);
	}
	
	private ProtocolManager<EncryptedPacket> findProtocolManager(int subVersion) {
		switch(subVersion) {
			default:
				return null;
			case 1:
				return new ClientProtocol1(this, getDataProxy());
			case 2:
				return new ClientProtocol2(this, getDataProxy());
			case 3:
				return new ClientProtocol3(this, getDataProxy());
		}
	}
	
	@Override
	public boolean sendPing() {
		if(protocolManager == null) return false;
		else return protocolManager.sendPing();
	}
	
	@Override
	public boolean sendMessage(short requestID, ConversationTarget conversation, String message) {
		if(protocolManager == null) return false;
		return protocolManager.sendMessage(requestID, conversation, message);
	}
	
	@Override
	public Observable<ReduxEventAttachmentUpload> sendFile(short requestID, ConversationTarget conversation, File file) {
		if(protocolManager == null) return Observable.error(new AMRequestException(MessageSendErrorCode.localNetwork));
		return protocolManager.sendFile(requestID, conversation, file);
	}
	
	@Override
	public boolean requestAttachmentDownload(short requestID, String attachmentGUID) {
		if(protocolManager == null) return false;
		return protocolManager.requestAttachmentDownload(requestID, attachmentGUID);
	}
	
	@Override
	public boolean requestConversationInfo(Collection<String> conversations) {
		if(protocolManager == null) return false;
		return protocolManager.requestConversationInfo(conversations);
	}
	
	@Override
	public boolean requestRetrievalTime(long timeLower, long timeUpper) {
		if(protocolManager == null) return false;
		return protocolManager.requestRetrievalTime(timeLower, timeUpper);
	}
	
	@Override
	public boolean requestRetrievalID(long idLower, long timeLower, long timeUpper) {
		if(protocolManager == null) return false;
		return protocolManager.requestRetrievalID(idLower, timeLower, timeUpper);
	}
	
	@Override
	public boolean requestRetrievalAll(short requestID, MassRetrievalParams params) {
		if(protocolManager == null) return false;
		return protocolManager.requestRetrievalAll(requestID, params);
	}
	
	@Override
	public boolean requestChatCreation(short requestID, String[] members, String service) {
		if(protocolManager == null) return false;
		return protocolManager.requestChatCreation(requestID, members, service);
	}
	
	@Override
	public boolean sendPushToken(String token) {
		//Returning if there is no connection
		if(!isConnectionOpened()) return false;
		
		//Handling the message through the Connect proxy
		if(getDataProxyType() == ProxyType.connect) {
			((ProxyConnect) getDataProxy()).sendTokenAdd(token);
			return true;
		}
		
		return false;
	}
	
	@Override
	public int checkCommVerApplicability(int version) {
		return Integer.compare(version, communicationsVersion);
	}
	
	public boolean isConnectionOpened() {
		return connectionOpened;
	}
	
	@Override
	public boolean isProxySupported(@ProxyType int proxyType) {
		return proxyType == ProxyType.direct || proxyType == ProxyType.connect;
	}
	
	@Override
	public boolean isFeatureSupported(@ConnectionFeature int featureID) {
		//Forwarding the request to the protocol manager
		if(protocolManager == null) return false;
		return protocolManager.isFeatureSupported(featureID);
	}
	
	@Override
	public boolean requiresPersistence() {
		return getDataProxyType() == ProxyType.direct;
	}
	
	@Override
	public String getCommunicationsVersion() {
		return communicationsVersion + "." + (protocolManagerVer != -1 ? protocolManagerVer : "X");
	}
	
	String getPassword() {
		return password;
	}
}