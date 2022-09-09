package me.tagavari.airmessage.connection.comm5;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.messaging.FirebaseMessaging;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import me.tagavari.airmessage.BuildConfig;
import me.tagavari.airmessage.connection.DataProxy;
import me.tagavari.airmessage.connection.encryption.EncryptionAES;
import me.tagavari.airmessage.connection.encryption.EncryptionManager;
import me.tagavari.airmessage.data.SharedPreferencesManager;
import me.tagavari.airmessage.enums.ConnectionErrorCode;
import me.tagavari.airmessage.util.ConnectionParams;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import me.tagavari.airmessage.BuildConfig;
import me.tagavari.airmessage.connection.DataProxy;
import me.tagavari.airmessage.connection.encryption.EncryptionAES;
import me.tagavari.airmessage.connection.encryption.EncryptionManager;
import me.tagavari.airmessage.data.SharedPreferencesManager;
import me.tagavari.airmessage.enums.ConnectionErrorCode;
import me.tagavari.airmessage.flavor.CrashlyticsBridge;
import me.tagavari.airmessage.util.ConnectionParams;

/**
 * Handles connecting via WebSocket to AirMessage's Connect servers
 */
class ProxyConnect extends DataProxy<EncryptedPacket> {
	//Creating the constants
	private static final String TAG = ProxyConnect.class.getSimpleName();
	
	private static final URI connectHostname = URI.create(BuildConfig.CONNECT_ENDPOINT);
	private static final long handshakeTimeout = 8 * 1000;
	
	//Creating the state values
	private boolean isRunning = false;
	private final Handler handler = new Handler(Looper.getMainLooper());
	private final Runnable handshakeExpiryRunnable = () -> stop(ConnectionErrorCode.internet);
	private WSClient client;
	private Scheduler encryptionScheduler;
	@Nullable private EncryptionManager encryptionManager;
	
	@Override
	public void start(Context context, @Nullable ConnectionParams override) {
		if(isRunning) {
			CrashlyticsBridge.recordException(new IllegalStateException("Tried to start proxy, but it is already running!"));
			return;
		}
		
		String password;
		if(override == null) {
			try {
				password = SharedPreferencesManager.getDirectConnectionPassword(context);
			} catch(IOException | GeneralSecurityException exception) {
				exception.printStackTrace();
				notifyClose(ConnectionErrorCode.internalError);
				return;
			}
		} else {
			if(override instanceof ConnectionParams.Security) {
				password = ((ConnectionParams.Security) override).getPassword();
			} else {
				password = null;
			}
		}
		
		if(password != null) {
			encryptionManager = new EncryptionAES(password);
		}
		
		//Checking if the user is logged in
		FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();
		if(mUser == null) {
			//User isn't logged in
			notifyClose(ConnectionErrorCode.connectAccountValidation);
			return;
		}
		
		//Fetching the user's ID token and FCM token
		Tasks.whenAllComplete(mUser.getIdToken(false), FirebaseMessaging.getInstance().getToken()).addOnCompleteListener(task -> {
			List<Task<?>> tasks = task.getResult();
			Task<GetTokenResult> accountIDTask = (Task<GetTokenResult>) tasks.get(0);
			Task<String> fcmTokenTask = (Task<String>) tasks.get(1);
			
			//Getting the results
			String idToken;
			String fcmToken;
			if(accountIDTask.isSuccessful()) {
				idToken = accountIDTask.getResult().getToken();
			} else {
				//Error
				Exception exception = accountIDTask.getException();
				exception.printStackTrace();
				
				if(exception instanceof FirebaseNetworkException) {
					//Surface as network exception
					notifyClose(ConnectionErrorCode.internet);
				} else {
					//Some other error
					CrashlyticsBridge.recordException(exception);
					notifyClose(ConnectionErrorCode.internalError);
				}
				
				return;
			}
			if(fcmTokenTask.isSuccessful()) {
				fcmToken = fcmTokenTask.getResult();
			} else {
				//Error
				Exception exception = fcmTokenTask.getException();
				exception.printStackTrace();
				
				if(exception instanceof FirebaseNetworkException) {
					//Surface as network exception
					notifyClose(ConnectionErrorCode.internet);
				} else {
					//Some other error
					CrashlyticsBridge.recordException(exception);
					notifyClose(ConnectionErrorCode.internalError);
				}
				return;
			}
			
			//Constructing the headers
			Map<String, String> headers = new HashMap<>();
			headers.put("Origin", "app");
			
			//Building the URL
			Uri uri = new Uri.Builder()
				.scheme(connectHostname.getScheme())
				.encodedAuthority(connectHostname.getAuthority())
				.path(connectHostname.getPath())
				.appendQueryParameter("communications", Integer.toString(NHT.commVer))
				.appendQueryParameter("is_server", Boolean.toString(false))
				.appendQueryParameter("installation_id", SharedPreferencesManager.getInstallationID(context))
				.appendQueryParameter("id_token", idToken)
				.appendQueryParameter("fcm_token", fcmToken)
				.build();
			
			//Starting the connection
			try {
				client = new WSClient(new URI(uri.toString()), headers);
				client.connect();
			} catch(URISyntaxException exception) {
				exception.printStackTrace();
				CrashlyticsBridge.recordException(exception);
				stop(ConnectionErrorCode.internalError);
			}
		});
		
		//Initializing the scheduler
		encryptionScheduler = Schedulers.from(Executors.newSingleThreadExecutor(), true);
		
		//Updating the running state
		isRunning = true;
	}
	
	private void stopAsync(@ConnectionErrorCode int code) {
		handler.post(() -> stop(code));
	}
	
	@Override
	public void stop(@ConnectionErrorCode int code) {
		//Returning if this proxy is not running
		if(!isRunning) return;
		
		//Stopping the client
		if(client != null) client.closeSilently();
		
		//Cancelling the handshake expiry timer
		handler.removeCallbacks(handshakeExpiryRunnable);
		
		//Calling the listener
		notifyClose(code);
		
		//Cleaning up the scheduler
		encryptionScheduler.shutdown();
		encryptionScheduler = null;
		
		//Updating the running state
		isRunning = false;
	}
	
	@Override
	public boolean send(EncryptedPacket packet) {
		if(!client.isOpen()) return false;
		
		//Check for encryption support
		boolean serverSupportsEncryption = isServerRequestsEncryption();
		boolean clientSupportsEncryption = encryptionManager != null;
		if(serverSupportsEncryption && !clientSupportsEncryption) {
			Log.e(TAG, "The server requests encryption, but no password is set");
			return false;
		}
		
		//Encrypting the content if requested and a password is set
		byte[] packetData = packet.getData();
		boolean packetWantsEncryption = packet.getEncrypt();
		boolean isEncrypted = packetWantsEncryption && serverSupportsEncryption;
		
		Single.fromCallable(() -> {
			if(isEncrypted) {
				return encryptionManager.encrypt(packetData);
			} else {
				return packetData;
			}
		})
			.subscribeOn(encryptionScheduler)
			.doOnSuccess((content) -> {
				//Constructing and sending the message
				ByteBuffer byteBuffer = ByteBuffer.allocate(1 + (Integer.SIZE / Byte.SIZE) + content.length);
				byteBuffer.putInt(NHT.nhtClientProxy);
				
				if(isEncrypted) byteBuffer.put((byte) -100); //The content is encrypted
				else if(serverSupportsEncryption) byteBuffer.put((byte) -101); //We support encryption, but this packet should not be encrypted
				else byteBuffer.put((byte) -102); //We don't support encryption
				
				byteBuffer.put(content);
				
				//Sending the data
				client.send(byteBuffer.array());
			})
			.doOnError(Throwable::printStackTrace)
			.onErrorComplete()
			.subscribe();
		
		return true;
	}
	
	@Override
	public boolean isUsingFallback() {
		//AirMessage Connect doesn't use fallback methods
		return false;
	}
	
	public void sendTokenAdd(String token) {
		//Converting the token to bytes
		byte[] tokenBytes = token.getBytes(StandardCharsets.UTF_8);
		
		//Constructing and sending the message
		ByteBuffer byteBuffer = ByteBuffer.allocate((Integer.SIZE / Byte.SIZE) + tokenBytes.length);
		byteBuffer.putInt(NHT.nhtClientAddFCMToken);
		byteBuffer.put(tokenBytes);
		
		//Sending the data
		client.send(byteBuffer.array());
	}
	
	public void sendTokenRemove(String token) {
		//Converting the token to bytes
		byte[] tokenBytes = token.getBytes(StandardCharsets.UTF_8);
		
		//Constructing and sending the message
		ByteBuffer byteBuffer = ByteBuffer.allocate((Integer.SIZE / Byte.SIZE) + tokenBytes.length);
		byteBuffer.putInt(NHT.nhtClientRemoveFCMToken);
		byteBuffer.put(tokenBytes);
		
		//Sending the data
		client.send(byteBuffer.array());
	}
	
	protected class WSClient extends WebSocketClient {
		private boolean silentClose = false;
		
		WSClient(URI serverUri, Map<String, String> httpHeaders) {
			super(serverUri, httpHeaders);
			
			setConnectionLostTimeout(0);
		}
		
		@Override
		public void onOpen(ServerHandshake handshake) {
			//Starting the handshake expiry timer
			handler.postDelayed(handshakeExpiryRunnable, handshakeTimeout);
		}
		
		@Override
		public void onMessage(String message) {
		
		}
		
		@Override
		public void onMessage(ByteBuffer bytes) {
			try {
				//Unpacking the message
				int type = bytes.getInt();
				
				switch(type) {
					case NHT.nhtConnectionOK: {
						//Cancelling the handshake expiry timer
						handler.removeCallbacks(handshakeExpiryRunnable);
						
						//Calling the listener
						ProxyConnect.this.notifyOpen();
						
						break;
					}
					case NHT.nhtClientProxy: {
						/*
						 * App-level encryption was added at a later date,
						 * so we use a hack by checking the first byte of the message.
						 *
						 * All message types will have the first byte as 0 or -1,
						 * so we can check for other values here.
						 *
						 * If we find a match, assume that this was intentional from the server.
						 * Otherwise, backtrack and assume the server doesn't support encryption.
						 *
						 * -100 -> The content is encrypted
						 * -101 -> The content is not encrypted, but the server has encryption enabled
						 * -102 -> The server has encryption disabled
						 * Anything else -> The server does not support encryption
						 */
						boolean isSecure, isEncrypted;
						byte encryptionValue = bytes.get();
						if(encryptionValue == -100) isSecure = isEncrypted = true;
						else if(encryptionValue == -101) isSecure = isEncrypted = false;
						else if(encryptionValue == -102) {
							isSecure = true;
							isEncrypted = false;
						} else {
							Log.w(TAG, "Received unknown encryption value:" + encryptionValue);
							return;
						}
						byte[] data = new byte[bytes.remaining()];
						bytes.get(data);
						
						//Decrypting the data
						if(isEncrypted && encryptionManager != null) {
							data = encryptionManager.decrypt(data);
						}
						
						//Handling the message
						ProxyConnect.this.notifyMessage(new EncryptedPacket(data, isSecure));
						
						break;
					}
				}
			} catch(BufferUnderflowException | GeneralSecurityException exception) {
				exception.printStackTrace();
				CrashlyticsBridge.recordException(exception);
			}
		}
		
		@Override
		public void onClose(int code, String reason, boolean remote) {
			//Ignoring if we've been told to be silent
			if(silentClose) return;
			
			//Calling the listener
			stopAsync(webSocketToLocalCode(code));
		}
		
		@Override
		public void onError(Exception exception) {
			exception.printStackTrace();
		}
		
		@Override
		protected void onSetSSLParameters(SSLParameters sslParameters) {
			//Don't perform hostname validation
		}
		
		@Override
		public void onWebsocketHandshakeReceivedAsClient(WebSocket conn, ClientHandshake request, ServerHandshake response) throws InvalidDataException {
			super.onWebsocketHandshakeReceivedAsClient(conn, request, response);
			
			//Perform hostname validation here instead
			HostnameVerifier hostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
			Socket socket = getSocket();
			if(socket instanceof SSLSocket) {
				SSLSession session = ((SSLSocket) socket).getSession();
				if(!hostnameVerifier.verify(conn.getRemoteSocketAddress().getHostName(), session)) {
					try {
						Log.e(ProxyConnect.class.getName(), "Expected " + conn.getRemoteSocketAddress().getHostName() + ", found " + session.getPeerPrincipal());
						throw new InvalidDataException(CloseFrame.POLICY_VALIDATION, "Expected " + conn.getRemoteSocketAddress().getHostName() + ", found " + session.getPeerPrincipal());
					} catch(SSLPeerUnverifiedException exception) {
						exception.printStackTrace();
						throw new InvalidDataException(CloseFrame.POLICY_VALIDATION);
					}
				}
			}
		}
		
		public void closeSilently() {
			silentClose = true;
			close();
		}
	}
	
	@ConnectionErrorCode
	private static int webSocketToLocalCode(int code) {
		switch(code) {
			case CloseFrame.NEVER_CONNECTED:
			case CloseFrame.BUGGYCLOSE:
			case CloseFrame.FLASHPOLICY:
			case CloseFrame.ABNORMAL_CLOSE:
			case CloseFrame.NORMAL:
				return ConnectionErrorCode.internet;
			case CloseFrame.PROTOCOL_ERROR:
			case CloseFrame.POLICY_VALIDATION:
				return ConnectionErrorCode.badRequest;
			case NHT.closeCodeIncompatibleProtocol:
				return ConnectionErrorCode.clientOutdated;
			case NHT.closeCodeNoGroup:
				return ConnectionErrorCode.connectNoGroup;
			case NHT.closeCodeNoCapacity:
				return ConnectionErrorCode.connectNoCapacity;
			case NHT.closeCodeAccountValidation:
				return ConnectionErrorCode.connectAccountValidation;
			case NHT.closeCodeNoActivation:
				return ConnectionErrorCode.connectNoActivation;
			case NHT.closeCodeOtherLocation:
				return ConnectionErrorCode.connectOtherLocation;
			default:
				return ConnectionErrorCode.externalError;
		}
	}
	
	static class NHT {
		//AirMessage Connect communications version
		static final int commVer = 1;
		
		//Shared het header types
		/*
		 * The connected device has been connected successfully
		 */
		static final int nhtConnectionOK = 0;
		
		//Client-only net header types
		
		/*
		 * Proxy the message to the server (client -> connect)
		 * Receive data from the server (connect -> client)
		 *
		 * payload - data
		 */
		static final int nhtClientProxy = 100;
		
		/*
		 * Add an item to the list of FCM tokens (client -> connect)
		 *
		 * string - registration token
		 */
		static final int nhtClientAddFCMToken = 110;
		
		/*
		 * Remove an item from the list of FCM tokens (client -> connect)
		 *
		 * string - registration token
		 */
		static final int nhtClientRemoveFCMToken = 111;
		
		//Server-only net header types
		
		/*
		 * Notify a new client connection (connect -> server)
		 *
		 * int - connection ID
		 */
		static final int nhtServerOpen = 200;
		
		/*
		 * Close a connected client (server -> connect)
		 * Notify a closed connection (connect -> server)
		 *
		 * int - connection ID
		 */
		static final int nhtServerClose = 201;
		
		/*
		 * Proxy the message to the client (server -> connect)
		 * Receive data from a connected client (connect -> server)
		 *
		 * int - connection ID
		 * payload - data
		 */
		static final int nhtServerProxy = 210;
		
		/*
		 * Proxy the message to all connected clients (server -> connect)
		 *
		 * payload - data
		 */
		static final int nhtServerProxyBroadcast = 211;
		
		/**
		 * Notify offline clients of a new message
		 */
		static final int nhtServerNotifyPush = 212;
		
		//Disconnection codes
		static final int closeCodeIncompatibleProtocol = 4000; //No protocol version matching the one requested
		static final int closeCodeNoGroup = 4001; //There is no active group with a matching ID
		static final int closeCodeNoCapacity = 4002; //The client's group is at capacity
		static final int closeCodeAccountValidation = 4003; //This account couldn't be validated
		static final int closeCodeServerTokenRefresh = 4004; //The server's provided installation ID is out of date; log in again to re-link this device
		static final int closeCodeNoActivation = 4005; //This user's account is not activated
		static final int closeCodeOtherLocation = 4006; //Logged in from another location
	}
}