package me.tagavari.airmessage.connection.comm5;

import android.os.Handler;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.connection.CookieBuilder;
import me.tagavari.airmessage.connection.DataProxy;

class ProxyConnect extends DataProxy5 {
	//Creating the constants
	//private static final URI connectHostname = URI.create("wss://connect.airmessage.org");
	private static final URI connectHostname = URI.create("ws://10.0.0.52:1259");
	private static final long handshakeTimeout = 8 * 1000;
	
	//Creating the state values
	private final Handler handler = new Handler();
	private final Runnable handshakeExpiryRunnable = () -> stop(ConnectionManager.connResultInternet);
	private WSClient client;
	
	@Override
	public void start() {
		//Fetching the user's ID token
		FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();
		if(mUser == null) {
			//User isn't logged in
			onClose(ConnectionManager.connResultDirectUnauthorized);
			return;
		}
		mUser.getIdToken(false).addOnCompleteListener(task -> {
			if(task.isSuccessful()) {
				String idToken = task.getResult().getToken();
				
				//Constructing the headers
				Map<String, String> headers = new HashMap<>();
				headers.put("Cookie", new CookieBuilder()
						.with("communications", NHT.commVer)
						.with("isServer", false)
						.with("installationID", MainApplication.getInstance().getInstallationID())
						.with("idToken", idToken)
						.toString()
				);
				
				//Starting the server
				client = new WSClient(connectHostname, headers);
				client.connect();
			} else {
				//Error
				task.getException().printStackTrace();
				onClose(ConnectionManager.connResultInternalError);
			}
		});
	}
	
	@Override
	public void stop(int code) {
		//Stopping the client
		if(client != null) client.close();
		
		handleWSClose(code);
	}
	
	private void handleWSClose(int code) {
		//Cancelling the handshake expiry timer
		handler.removeCallbacks(handshakeExpiryRunnable);
		
		//Calling the listener
		onClose(code);
	}
	
	private void handleWSOpen() {
		//Starting the handshake expiry timer
		handler.postDelayed(handshakeExpiryRunnable, handshakeTimeout);
	}
	
	private void handleWSMessage(ByteBuffer bytes) {
		try {
			//Unpacking the message
			int type = bytes.getInt();
			
			switch(type) {
				case NHT.nhtConnectionOK: {
					//Cancelling the handshake expiry timer
					handler.removeCallbacks(handshakeExpiryRunnable);
					
					//Calling the listener
					ProxyConnect.this.onOpen();
					
					break;
				}
				case NHT.nhtClientProxy: {
					//Reading the data
					byte[] data = new byte[bytes.remaining()];
					bytes.get(data);
					
					//Handling the message
					ProxyConnect.this.onMessage(new PacketStructIn(data, false));
					
					break;
				}
			}
		} catch(BufferUnderflowException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean send(PacketStructOut packet) {
		//Constructing and sending the message
		ByteBuffer byteBuffer = ByteBuffer.allocate((Integer.SIZE / Byte.SIZE) + packet.getData().length);
		byteBuffer.putInt(NHT.nhtClientProxy);
		byteBuffer.put(packet.getData());
		
		//Sending the data
		client.send(byteBuffer.array());
		
		//Running the sent runnable immediately
		if(packet.getSentRunnable() != null) packet.getSentRunnable().run();
		
		return false;
	}
	
	@Override
	public boolean requiresAuthentication() {
		return false;
	}
	
	protected class WSClient extends WebSocketClient {
		WSClient(URI serverUri, Map<String, String> httpHeaders) {
			super(serverUri, httpHeaders);
		}
		
		@Override
		public void onOpen(ServerHandshake handshake) {
			handleWSOpen();
		}
		
		@Override
		public void onMessage(String message) {
		
		}
		
		@Override
		public void onMessage(ByteBuffer bytes) {
			handleWSMessage(bytes);
		}
		
		@Override
		public void onClose(int code, String reason, boolean remote) {
			//Calling the listener
			handleWSClose(webSocketToLocalCode(code));
		}
		
		@Override
		public void onError(Exception exception) {
			exception.printStackTrace();
		}
	}
	
	private static int webSocketToLocalCode(int code) {
		switch(code) {
			case CloseFrame.NEVER_CONNECTED:
			case CloseFrame.BUGGYCLOSE:
			case CloseFrame.FLASHPOLICY:
			case CloseFrame.ABNORMAL_CLOSE:
			case CloseFrame.NORMAL:
				return ConnectionManager.connResultInternet;
			case CloseFrame.PROTOCOL_ERROR:
				return ConnectionManager.connResultBadRequest;
			case CloseFrame.POLICY_VALIDATION:
				return ConnectionManager.connResultClientOutdated;
			case NHT.closeCodeNoGroup:
				return ConnectionManager.connResultConnectNoGroup;
			case NHT.closeCodeNoCapacity:
				return ConnectionManager.connResultConnectNoCapacity;
			case NHT.closeCodeAccountValidation:
				return ConnectionManager.connResultConnectAccountValidation;
			case NHT.closeCodeNoSubscription:
				return ConnectionManager.connResultConnectNoSubscription;
			case NHT.closeCodeOtherLocation:
				return ConnectionManager.connResultConnectOtherLocation;
			default:
				return ConnectionManager.connResultExternalError;
		}
	}
	
	static class NHT {
		//AirMessage Connect communications version
		static final int commVer = 1;
		
		//Shared het header types
		/*
		The connected device has been connected successfully
		 */
		static final int nhtConnectionOK = 0;
		
		//Client-only net header types
		
		/*
		Proxy the message to the server (client -> connect)
		
		payload - data
		 */
		static final int nhtClientProxy = 100;
		
		//Server-only net header types
		
		/*
		Notify a new client connection (connect -> server)
		
		int - connection ID
		 */
		static final int nhtServerOpen = 200;
		
		/*
		Close a connected client (server -> connect)
		Notify a closed connection (connect -> server)
		
		int - connection ID
		 */
		static final int nhtServerClose = 201;
		
		/*
		Proxy the message to the client (server -> connect)
		Receive data from a connected client (connect -> server)
		
		int - connection ID
		payload - data
		 */
		static final int nhtServerProxy = 210;
		
		/*
		Proxy the message to all connected clients (server -> connect)
		
		payload - data
		 */
		static final int nhtServerProxyBroadcast = 211;
		
		/*
		Notify a client over FCM (server -> connect)
		
		int - connection ID
		 */
		static final int nhtServerNotify = 211;
		
		//Disconnection codes
		static final int closeCodeNoGroup = 4000; //There is no active group with a matching ID
		static final int closeCodeNoCapacity = 4001; //The client's group is at capacity
		static final int closeCodeAccountValidation = 4002; //This account couldn't be validated
		static final int closeCodeServerTokenRefresh = 4003; //The server's provided installation ID is out of date; log in again to re-link this device
		static final int closeCodeNoSubscription = 4004; //This user does not have an active subscription
		static final int closeCodeOtherLocation = 4005; //Logged in from another location
	}
}