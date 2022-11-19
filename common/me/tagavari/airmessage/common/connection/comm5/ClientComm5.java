package me.tagavari.airmessage.common.connection.comm5;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.rxjava3.core.Observable;
import me.tagavari.airmessage.common.connection.CommunicationsManager;
import me.tagavari.airmessage.common.connection.DataProxy;
import me.tagavari.airmessage.common.connection.MassRetrievalParams;
import me.tagavari.airmessage.common.connection.exception.AMRequestException;
import me.tagavari.airmessage.common.connection.listener.CommunicationsManagerListener;
import me.tagavari.airmessage.common.data.SharedPreferencesManager;
import me.tagavari.airmessage.common.enums.ConnectionErrorCode;
import me.tagavari.airmessage.common.enums.ConnectionFeature;
import me.tagavari.airmessage.common.enums.MessageSendErrorCode;
import me.tagavari.airmessage.common.enums.ProxyType;
import me.tagavari.airmessage.common.redux.ReduxEventAttachmentUpload;
import me.tagavari.airmessage.common.util.ConnectionParams;
import me.tagavari.airmessage.common.util.ConversationTarget;
import me.tagavari.airmessage.connection.comm5.ProxyConnect;

import java.io.File;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Collections;

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
			if(override instanceof ConnectionParams.Security) {
				password = ((ConnectionParams.Security) override).getPassword();
			} else {
				password = null;
			}
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
		int majVerApplicability = checkCommVerApplicability(communicationsVersion);
		if(majVerApplicability != 0) {
			//Terminating the connection
			getHandler().post(() -> disconnect(majVerApplicability < 0 ? ConnectionErrorCode.serverOutdated : ConnectionErrorCode.clientOutdated));
			return;
		}
		
		int minVerApplicability = checkProtocolManagerApplicability(communicationsSubVersion);
		if(minVerApplicability < 0) {
			getHandler().post(() -> disconnect(ConnectionErrorCode.serverOutdated));
			return;
		} else if(minVerApplicability > 0) {
			getHandler().post(() -> disconnect(ConnectionErrorCode.clientOutdated));
			return;
		}
		
		//Finding the matching protocol manager
		protocolManager = findProtocolManager(communicationsSubVersion);
		
		//Updating the protocol version
		protocolManagerVer = communicationsSubVersion;
		
		//Sending the handshake data
		protocolManager.sendAuthenticationRequest(unpacker);
	}
	
	private int checkProtocolManagerApplicability(int subVersion) {
		if(subVersion < 5) return -1;
		else if(subVersion > 5) return 1;
		else return 0;
	}
	
	@NonNull
	private ProtocolManager<EncryptedPacket> findProtocolManager(int subVersion) {
		switch(subVersion) {
			default:
				throw new IllegalArgumentException("Invalid communications sub version " + subVersion);
			case 5:
				return new ClientProtocol5(this, getDataProxy());
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
	public boolean installSoftwareUpdate(int updateID) {
		if(protocolManager == null) return false;
		return protocolManager.installSoftwareUpdate(updateID);
	}

	@Override
	public boolean requestFaceTimeLink() {
		if(protocolManager == null) return false;
		return protocolManager.requestFaceTimeLink();
	}
	
	@Override
	public boolean initiateFaceTimeCall(List<String> addresses) {
		if(protocolManager == null) return false;
		return protocolManager.initiateFaceTimeCall(addresses);
	}
	
	@Override
	public boolean handleIncomingFaceTimeCall(@NonNull String caller, boolean accept) {
		if(protocolManager == null) return false;
		return protocolManager.handleIncomingFaceTimeCall(caller, accept);
	}
	
	@Override
	public boolean dropFaceTimeCallServer() {
		if(protocolManager == null) return false;
		return protocolManager.dropFaceTimeCallServer();
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
	public List<Integer> getCommunicationsVersion() {
		if(protocolManagerVer == -1) {
			return Collections.singletonList(communicationsVersion);
		} else {
			return Arrays.asList(communicationsVersion, protocolManagerVer);
		}
	}
	
	String getPassword() {
		return password;
	}
}