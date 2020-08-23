package me.tagavari.airmessage.connection.comm4;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.nio.ByteBuffer;
import java.util.List;

import me.tagavari.airmessage.connection.CommunicationsManager;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.connection.MassRetrievalParams;
import me.tagavari.airmessage.connection.ProxyNoOp;
import me.tagavari.airmessage.connection.request.ConversationInfoRequest;

public class ClientComm4 extends CommunicationsManager<PacketStructIn, PacketStructOut> {
	//Creating the connection values
	private ProtocolManager protocolManager = null;
	
	private static final long handshakeExpiryTime = 1000 * 10; //10 seconds
	final Handler handler = new Handler();
	
	//Creating the transmission values
	static final int nhtClose = -1;
	static final int nhtPing = -2;
	static final int nhtPong = -3;
	static final int nhtInformation = 0;

	final Runnable handshakeExpiryRunnable = () -> dataProxy.stop(ConnectionManager.connResultConnection);
	
	//Creating the state values
	private boolean connectionOpened = false;
	
	public ClientComm4(ConnectionManager connectionManager, int proxyType, Context context) {
		super(connectionManager, context);
		
		//Assigning the proxy
		if(proxyType == ConnectionManager.proxyTypeDirect) setProxy(new ProxyDirectTCP());
		else setProxy(new ProxyNoOp<>());
		
		//Hooking into the data proxy
		dataProxy.addDataListener(this);
	}
	
	@Override
	public void initiateClose() {
		if(connectionOpened) {
			//Notifying the server before disconnecting
			queuePacket(new PacketStructOut(nhtClose, new byte[0], () -> dataProxy.stop(ConnectionManager.connResultConnection)));
		} else {
			//The server isn't connected, disconnect right away
			dataProxy.stop(ConnectionManager.connResultConnection);
		}
	}
	
	@Override
	public void onOpen() {
		//Calling the super method
		super.onOpen();
		
		//Starting the handshake expiry timer
		handler.postDelayed(handshakeExpiryRunnable, handshakeExpiryTime);
		
		//Setting the state
		connectionOpened = true;
	}
	
	@Override
	public void onClose(int reason) {
		//Calling the super method
		super.onClose(reason);
		
		//Cancelling the handshake expiry timer
		handler.removeCallbacks(handshakeExpiryRunnable);
	}
	
	@Override
	public void onMessage(PacketStructIn message) {
		//Calling the super method
		super.onMessage(message);
		
		//Processing the data
		if(protocolManager == null) processFloatingData(message.getHeader(), message.getData());
		else protocolManager.processData(message.getHeader(), message.getData());
	}
	
	@Override
	public boolean isConnectedFallback() {
		if(dataProxy instanceof ProxyDirectTCP) return ((ProxyDirectTCP) dataProxy).isUsingFallback();
		else return false;
	}
	
	@Override
	public boolean sendPing() {
		if(protocolManager == null) return false;
		else return protocolManager.sendPing();
	}
	
	@Override
	public ConnectionManager.Packager getPackager() {
		if(protocolManager == null) return null;
		return protocolManager.getPackager();
	}
	
	@Override
	public String getHashAlgorithm() {
		if(protocolManager == null) return null;
		return protocolManager.getHashAlgorithm();
	}
	
	@Override
	public boolean sendMessage(short requestID, String chatGUID, String message) {
		if(protocolManager == null) return false;
		return protocolManager.sendMessage(requestID, chatGUID, message);
	}
	
	@Override
	public boolean sendMessage(short requestID, String[] chatMembers, String message, String service) {
		if(protocolManager == null) return false;
		return protocolManager.sendMessage(requestID, chatMembers, message, service);
	}
	
	@Override
	public boolean addDownloadRequest(short requestID, String attachmentGUID, Runnable sentRunnable) {
		if(protocolManager == null) return false;
		return protocolManager.addDownloadRequest(requestID, attachmentGUID, sentRunnable);
	}
	
	@Override
	public boolean sendConversationInfoRequest(List<ConversationInfoRequest> list) {
		if(protocolManager == null) return false;
		return protocolManager.sendConversationInfoRequest(list);
	}
	
	@Override
	public boolean uploadFilePacket(short requestID, int requestIndex, String conversationGUID, byte[] data, String fileName, boolean isLast) {
		if(protocolManager == null) return false;
		return protocolManager.uploadFilePacket(requestID, requestIndex, conversationGUID, data, fileName, isLast);
	}
	
	@Override
	public boolean uploadFilePacket(short requestID, int requestIndex, String[] conversationMembers, byte[] data, String fileName, String service, boolean isLast) {
		if(protocolManager == null) return false;
		return protocolManager.uploadFilePacket(requestID, requestIndex, conversationMembers, data, fileName, service, isLast);
	}
	
	@Override
	public boolean requestRetrievalTime(long timeLower, long timeUpper) {
		if(protocolManager == null) return false;
		return protocolManager.requestRetrievalTime(timeLower, timeUpper);
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
	public int checkCommVerApplicability(int version) {
		return Integer.compare(version, 4);
	}
	
	public boolean isConnectionOpened() {
		return connectionOpened;
	}
	
	boolean queuePacket(PacketStructOut packet) {
		return dataProxy.send(packet);
	}
	
	/**
	 * Disconnects the connection manager from the server with a custom error code
	 *
	 * @param code The error code to disconnect with
	 */
	void disconnect(int code) {
		//Disconnecting the proxy
		dataProxy.stop(code);
	}
	
	/**
	 * Processes any data before a protocol manager is selected, usually to handle version processing
	 */
	private void processFloatingData(int messageType, byte[] data) {
		if(messageType == nhtInformation) {
			//Restarting the authentication timer
			handler.removeCallbacks(handshakeExpiryRunnable);
			handler.postDelayed(handshakeExpiryRunnable, handshakeExpiryTime);
			
			//Reading the communications version information
			ByteBuffer dataBuffer = ByteBuffer.wrap(data);
			int communicationsVersion = dataBuffer.getInt();
			int communicationsSubVersion = dataBuffer.getInt();
			
			//Checking if the client can't handle this communications version
			int verApplicability = checkCommVerApplicability(communicationsVersion);
			if(verApplicability != 0) {
				//Terminating the connection
				dataProxy.stop(verApplicability < 0 ? ConnectionManager.connResultServerOutdated : ConnectionManager.connResultClientOutdated);
				return;
			}
			
			//Communications subversions 1-5 are no longer supported
			if(communicationsSubVersion <= 5) {
				dataProxy.stop(ConnectionManager.connResultServerOutdated);
				return;
			}
			
			protocolManager = findProtocolManager(communicationsSubVersion);
			if(protocolManager == null) {
				dataProxy.stop(ConnectionManager.connResultClientOutdated);
				return;
			}
			
			//Updating the protocol version
			new Handler(Looper.getMainLooper()).post(() -> connectionManager.setActiveCommunicationsInfo(communicationsVersion, communicationsSubVersion));
			
			//Sending the handshake data
			protocolManager.sendAuthenticationRequest();
		}
	}
	
	private ProtocolManager findProtocolManager(int subVersion) {
		switch(subVersion) {
			default:
				return null;
			case 6:
				return new ClientProtocol6(context, connectionManager, this);
		}
	}
	
	@Override
	public boolean checkSupportsFeature(String feature) {
		//Forwarding the request to the protocol manager
		if(protocolManager == null) return false;
		return protocolManager.checkSupportsFeature(feature);
	}
	
	@Override
	public boolean requiresPersistence() {
		return true;
	}
}