package me.tagavari.airmessage.connection.comm4;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.rxjava3.core.Observable;
import me.tagavari.airmessage.connection.CommunicationsManager;
import me.tagavari.airmessage.connection.DataProxy;
import me.tagavari.airmessage.connection.MassRetrievalParams;
import me.tagavari.airmessage.connection.ProxyNoOp;
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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.List;

public class ClientComm4 extends CommunicationsManager<HeaderPacket> {
	private static final int communicationsVersion = 4;
	
	//Creating the connection values
	ProtocolManager<HeaderPacket> protocolManager = null;
	private int protocolManagerVer = -1;
	
	//Creating the handshake values
	final Runnable handshakeExpiryRunnable = () -> disconnect(ConnectionErrorCode.connection);
	private static final long handshakeExpiryTime = 1000 * 10; //10 seconds
	
	//Creating the transmission values
	static final int nhtClose = -1;
	static final int nhtPing = -2;
	static final int nhtPong = -3;
	static final int nhtInformation = 0;
	
	//Creating the state values
	private boolean connectionOpened = false;
	
	//Creating the parameter values
	private String password;
	
	public ClientComm4(CommunicationsManagerListener listener, int proxyType) {
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
	protected DataProxy<HeaderPacket> getDataProxy(int proxyType) {
		//Assigning the proxy
		if(proxyType == ProxyType.direct) return new ProxyDirectTCP();
		else if(proxyType == ProxyType.connect) return new ProxyNoOp<>();
		else throw new IllegalArgumentException("Unknown proxy type " + proxyType);
	}
	
	@Override
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
	protected void handleMessage(HeaderPacket packet) {
		//Sending an update for the received packet
		runListener(CommunicationsManagerListener::onPacket);
		
		//Processing the data
		if(protocolManager != null) protocolManager.processData(packet.getData(), packet.getType());
		else processFloatingData(packet.getData(), packet.getType());
	}
	
	/**
	 * Processes any data before a protocol manager is selected, usually to handle version processing
	 */
	private void processFloatingData(byte[] data, int type) {
		//Only accepting information messages
		if(type != nhtInformation) return;
		
		//Restarting the authentication timer
		getHandler().removeCallbacks(handshakeExpiryRunnable);
		getHandler().postDelayed(handshakeExpiryRunnable, handshakeExpiryTime);
		
		//Reading the communications version information
		ByteBuffer dataBuffer = ByteBuffer.wrap(data);
		int communicationsVersion = dataBuffer.getInt();
		int communicationsSubVersion = dataBuffer.getInt();
		
		//Checking if the client can't handle this communications version
		int verApplicability = checkCommVerApplicability(communicationsVersion);
		if(verApplicability != 0) {
			//Terminating the connection
			getHandler().post(() -> disconnect(verApplicability < 0 ? ConnectionErrorCode.serverOutdated : ConnectionErrorCode.clientOutdated));
			return;
		}
		
		//Communications subversions 1-5 are no longer supported
		if(communicationsSubVersion <= 5) {
			getHandler().post(() -> disconnect(ConnectionErrorCode.serverOutdated));
			return;
		}
		
		protocolManager = findProtocolManager(communicationsSubVersion);
		if(protocolManager == null) {
			getHandler().post(() -> disconnect(ConnectionErrorCode.clientOutdated));
			return;
		}
		
		//Updating the protocol version
		protocolManagerVer = communicationsSubVersion;
		
		//Sending the handshake data
		protocolManager.sendAuthenticationRequest();
	}
	
	private ProtocolManager<HeaderPacket> findProtocolManager(int subVersion) {
		switch(subVersion) {
			default:
				return null;
			case 6:
				return new ClientProtocol6(this, getDataProxy());
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
		throw new UnsupportedOperationException("Not supported");
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
		//No support
		return false;
	}

	@Override
	public boolean installSoftwareUpdate(int updateID) {
		return false;
	}

	@Override
	public boolean requestFaceTimeLink() {
		return false;
	}
	
	@Override
	public boolean initiateFaceTimeCall(List<String> addresses) {
		return false;
	}
	
	@Override
	public boolean handleIncomingFaceTimeCall(@NonNull String caller, boolean accept) {
		return false;
	}
	
	@Override
	public boolean dropFaceTimeCallServer() {
		return false;
	}
	
	@Override
	public int checkCommVerApplicability(int version) {
		return Integer.compare(version, 4);
	}
	
	public boolean isConnectionOpened() {
		return connectionOpened;
	}
	
	@Override
	public boolean isProxySupported(@ProxyType int proxyType) {
		return proxyType == ProxyType.direct;
	}
	
	@Override
	public boolean isFeatureSupported(@ConnectionFeature int featureID) {
		return false;
	}
	
	@Override
	public boolean requiresPersistence() {
		return true;
	}
	
	@Override
	public String getCommunicationsVersion() {
		return communicationsVersion + "." + (protocolManagerVer != -1 ? protocolManagerVer : "X");
	}
	
	String getPassword() {
		return password;
	}
}