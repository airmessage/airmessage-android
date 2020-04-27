package me.tagavari.airmessage.connection.comm5;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePackException;
import org.msgpack.core.MessageUnpacker;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.logging.Level;

import me.tagavari.airmessage.connection.CommunicationsManager;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.connection.MassRetrievalParams;
import me.tagavari.airmessage.connection.request.ConversationInfoRequest;

public class ClientComm5 extends CommunicationsManager<PacketStructIn, PacketStructOut> {
	//Creating the connection values
	private ProtocolManager protocolManager = null;
	private EncryptionManager encryptionManager = null;
	
	private static final long handshakeExpiryTime = 1000 * 10; //10 seconds
	final Handler handler = new Handler();
	
	//Creating the transmission values
	private static final int nhtInformation = 0;

	final Runnable handshakeExpiryRunnable = () -> dataProxy.stop(ConnectionManager.intentResultCodeConnection);
	
	//Creating the state values
	private boolean connectionOpened = false;
	
	public ClientComm5(ConnectionManager connectionManager, int proxyType, Context context) {
		super(connectionManager, context);
		
		//Assigning the proxy
		if(proxyType == ConnectionManager.proxyTypeDirect) setProxy(new ProxyDirectTCP());
		else if(proxyType == ConnectionManager.proxyTypeConnect) setProxy(new ProxyConnect());
		
		//Hooking into the data proxy
		dataProxy.addDataListener(this);
	}
	
	@Override
	public void initiateClose() {
		if(connectionOpened && protocolManager != null) {
			//Notifying the server before disconnecting
			protocolManager.sendConnectionClose(() -> dataProxy.stop(ConnectionManager.intentResultCodeConnection));
		} else {
			//The server isn't connected, disconnect right away
			dataProxy.stop(ConnectionManager.intentResultCodeConnection);
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
		
		//Invalidating the connection managers
		protocolManager = null;
		encryptionManager = null;
	}
	
	@Override
	public void onMessage(PacketStructIn message) {
		//Calling the super method
		super.onMessage(message);
		
		//Decrypting the message
		if(message.isEncrypted()) {
			if(encryptionManager == null) return;
			
			try {
				message.decrypt(encryptionManager);
			} catch(GeneralSecurityException exception) {
				exception.printStackTrace();
				return;
			}
		}
		
		//Processing the data
		if(protocolManager == null) processFloatingData(message.getData());
		else protocolManager.processData(message.getData(), message.isEncrypted());
	}
	
	@Override
	public boolean isConnectedFallback() {
		//TODO re-implement fallback connections
		//if(connectionThread == null) return false;
		//return connectionThread.isUsingFallback();
		return false;
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
	
	EncryptionManager getEncryptionManager() {
		return encryptionManager;
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
	public int checkCommVerApplicability(int version) {
		return Integer.compare(version, 5);
	}
	
	public boolean isConnectionOpened() {
		return connectionOpened;
	}
	
	boolean queuePacket(byte[] data, boolean encrypt) {
		return queuePacket(data, encrypt, null);
	}
	
	boolean queuePacket(byte[] data, boolean encrypt, Runnable sentRunnable) {
		//Encrypting the content if requested
		if(encrypt && encryptionManager != null) {
			try {
				data = encryptionManager.encrypt(data);
			} catch(GeneralSecurityException exception) {
				exception.printStackTrace();
				return false;
			}
		}
		
		//Queuing the packet
		return dataProxy.send(new PacketStructOut(data, encrypt, sentRunnable));
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
	private void processFloatingData(byte[] data) {
		//Wrapping the data in a MessagePack unpacker
		MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data);
		
		try {
			//Reading the message type
			int messageType = unpacker.unpackInt();
			
			if(messageType == nhtInformation) {
				processServerInformation(unpacker);
			}
		} catch(IOException | MessagePackException exception) {
			//Logging the exception
			exception.printStackTrace();
			
			//Disconnecting
			disconnect();
		} finally {
			try {
				unpacker.close();
			} catch(IOException exception) {
				exception.printStackTrace();
			}
		}
	}
	
	private void processServerInformation(MessageUnpacker unpacker) throws IOException {
		//Restarting the authentication timer
		handler.removeCallbacks(handshakeExpiryRunnable);
		handler.postDelayed(handshakeExpiryRunnable, handshakeExpiryTime);
		
		//Reading the communications version information
		int communicationsVersion = unpacker.unpackInt();
		int communicationsSubVersion = unpacker.unpackInt();
		
		//Checking if the client can't handle this communications version
		int verApplicability = checkCommVerApplicability(communicationsVersion);
		if(verApplicability != 0) {
			//Terminating the connection
			dataProxy.stop(verApplicability < 0 ? ConnectionManager.intentResultCodeServerOutdated : ConnectionManager.intentResultCodeClientOutdated);
			return;
		}
		
		protocolManager = findProtocolManager(communicationsSubVersion);
		if(protocolManager == null) {
			dataProxy.stop(ConnectionManager.intentResultCodeClientOutdated);
			return;
		}
		
		encryptionManager = findEncryptionManager(communicationsSubVersion);
		
		//Updating the protocol version
		new Handler(Looper.getMainLooper()).post(() -> connectionManager.setActiveCommunicationsInfo(communicationsVersion, communicationsSubVersion));
		
		//Sending the handshake data
		protocolManager.sendAuthenticationRequest(unpacker);
	}
	
	private ProtocolManager findProtocolManager(int subVersion) {
		switch(subVersion) {
			default:
				return null;
			case 1:
				return new ClientProtocol1(context, connectionManager, this);
		}
	}
	
	private EncryptionManager findEncryptionManager(int subVersion) {
		if(dataProxy instanceof ProxyConnect) return null;
		return new EncryptionAES();
	}
	
	@Override
	public boolean checkSupportsFeature(String feature) {
		//Forwarding the request to the protocol manager
		if(protocolManager == null) return false;
		return protocolManager.checkSupportsFeature(feature);
	}
}