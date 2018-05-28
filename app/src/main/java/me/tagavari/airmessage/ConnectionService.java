package me.tagavari.airmessage;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.OpenableColumns;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.LongSparseArray;
import android.util.SparseArray;

import com.crashlytics.android.Crashlytics;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ClientHandshakeBuilder;
import org.java_websocket.handshake.ServerHandshake;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.NotYetConnectedException;
import java.security.DigestInputStream;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import me.tagavari.airmessage.common.Blocks;
import me.tagavari.airmessage.common.SharedValues;

public class ConnectionService extends Service {
	/* COMMUNICATIONS VERSION CHANGES
	 *  1 - Original release
	 *  2 - Serialization changes
	 *  3 - Reworked without WS layer
	 *  4 - Better stability and security, with sub-version support
	 */
	public static final int mmCommunicationsVersion = 4;
	public static final int mmCommunicationsSubVersion = 2;
	
	private static final int[] applicableCommunicationsVersions = {4, 3, 2};
	private static final int notificationID = -1;
	static final int maxPacketAllocation = 50 * 1024 * 1024; //50 MB
	
	static final String localBCStateUpdate = "LocalMSG-ConnectionService-State";
	static final String localBCMassRetrieval = "LocalMSG-ConnectionService-MassRetrievalProgress";
	static final String BCPingTimer = "me.tagavari.airmessage.ConnectionService-StartPing";
	
	static final int intentResultCodeSuccess = 0;
	static final int intentResultCodeInternalException = 1;
	static final int intentResultCodeBadRequest = 2;
	static final int intentResultCodeClientOutdated = 3;
	static final int intentResultCodeServerOutdated = 4;
	static final int intentResultCodeUnauthorized = 5;
	static final int intentResultCodeConnection = 6;
	
	static final int intentExtraStateMassRetrievalStarted = 0;
	static final int intentExtraStateMassRetrievalProgress = 1;
	static final int intentExtraStateMassRetrievalFinished = 2;
	static final int intentExtraStateMassRetrievalFailed = 3;
	
	static final String selfIntentActionConnect = "connect";
	static final String selfIntentActionDisconnect = "disconnect";
	static final String selfIntentActionStop = "stop";
	
	static final int stateDisconnected = 0;
	static final int stateConnecting = 1;
	static final int stateConnected = 2;
	
	static final int attachmentChunkSize = 1024 * 1024; //1 MiB
	static final long keepAliveMillis = 30 * 60 * 1000; //30 minutes
	static final long keepAliveWindowMillis = 5 * 60 * 1000; //5 minutes
	static final long dropReconnectDelayMillis = 1000; //1 second
	
	private static final Pattern regExValidPort = Pattern.compile("(:([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5]?))$");
	private static final Pattern regExValidProtocol = Pattern.compile("^ws(s?)://");
	
	private PendingIntent pingPendingIntent;
	
	private final List<Class> connectionManagerClassPriorityList = Arrays.asList(ClientCommCaladium.class, ClientComm3.class, ClientComm2.class);
	private final List<ConnectionManagerSource> connectionManagerPriorityList = Arrays.asList(ClientCommCaladium::new, ClientComm3::new, ClientComm2::new);
	
	//Creating the access values
	private static WeakReference<ConnectionService> serviceReference = null;
	
	//Creating the connection values
	static String hostname = null;
	static String password = null;
	private ConnectionManager currentConnectionManager = null;
	static int lastConnectionResult = -1;
	private boolean flagMarkEndTime = false; //Marks the time that the connection is closed, so that missed messages can be fetched since that time when reconnecting
	private boolean flagDropReconnect = false; //Automatically starts a new connection when the connection is closed
	private int activeCommunicationsVersion = -1;
	
	//private final List<FileUploadRequest> fileUploadRequestQueue = new ArrayList<>();
	//private Thread fileUploadRequestThread = null;
	
	private final BlockingQueue<FileUploadRequest> fileUploadRequestQueue = new LinkedBlockingQueue<>();
	private AtomicBoolean fileUploadRequestThreadRunning = new AtomicBoolean(false);
	
	private final BlockingQueue<QueueTask<?, ?>> messageProcessingQueue = new LinkedBlockingQueue<>();
	private AtomicBoolean messageProcessingQueueThreadRunning = new AtomicBoolean(false);
	
	private MassRetrievalThread massRetrievalThread = null;
	
	private final List<FileDownloadRequest> fileDownloadRequests = new ArrayList<>();
	
	private static byte currentLaunchID = 0;
	
	//Creating the broadcast receivers
	private final BroadcastReceiver pingBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(currentConnectionManager == null || currentConnectionManager.getState() != stateConnected) return;
			
			//Pinging the server
			currentConnectionManager.sendPing();
			
			//Rescheduling the ping
			schedulePing();
		}
	};
	private final BroadcastReceiver networkStateChangeBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			//Returning if automatic reconnects are disabled
			if(!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preference_server_networkreconnect_key), false)) return;
			
			//Reconnecting if there is a connection available
			if(!intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)) reconnect();
		}
	};
	
	//Creating the other values
	private final SparseArray<MessageResponseManager> messageSendRequests = new SparseArray<>();
	//private final LongSparseArray<MessageResponseManager> fileSendRequests = new LongSparseArray<>();
	//private short currentRequestID = (short) (new Random(System.currentTimeMillis()).nextInt((int) Short.MAX_VALUE - (int) Short.MIN_VALUE + 1) + Short.MIN_VALUE);
	//private short currentRequestID = (short) new Random(System.currentTimeMillis()).nextInt(1 << 16);
	private short currentRequestID = 0;
	private boolean shutdownRequested = false;
	
	private final ArrayList<ConversationInfoRequest> pendingConversations = new ArrayList<>();
	private final Handler mainHandler = new Handler(Looper.getMainLooper());
	
	static ConnectionService getInstance() {
		return serviceReference == null ? null : serviceReference.get();
	}
	
	public static int getStaticActiveCommunicationsVersion() {
		//Getting the instance
		ConnectionService connectionService = getInstance();
		if(connectionService == null) return -1;
		
		//Returning the active communications version
		return connectionService.activeCommunicationsVersion;
	}
	
	int getActiveCommunicationsVersion() {
		return activeCommunicationsVersion;
	}
	
	int getCurrentState() {
		if(currentConnectionManager == null) return stateDisconnected;
		else return currentConnectionManager.getState();
	}
	
	static int getLastConnectionResult() {
		return lastConnectionResult;
	}
	
	static byte getNextLaunchID() {
		return ++currentLaunchID;
	}
	
	static ConnectionManager getStaticConnectionManager() {
		ConnectionService service = getInstance();
		if(service == null) return null;
		return service.currentConnectionManager;
	}
	
	@Override
	public void onCreate() {
		//Loading data from the database
		new FetchConversationRequests(this).execute();
		
		//Setting the instance
		serviceReference = new WeakReference<>(this);
		
		//Registering the broadcast receivers
		registerReceiver(networkStateChangeBroadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		registerReceiver(pingBroadcastReceiver, new IntentFilter(BCPingTimer));
		
		//Setting the reference values
		pingPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(BCPingTimer), PendingIntent.FLAG_UPDATE_CURRENT);
		
		//Starting the service as a foreground service (if enabled in the preferences)
		if(foregroundServiceRequested()) startForeground(-1, getBackgroundNotification(false));
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//Getting the intent action
		String intentAction = intent == null ? null : intent.getAction();
		
		//Checking if a stop has been requested
		if(selfIntentActionStop.equals(intentAction)) {
			//Setting the service as shutting down
			shutdownRequested = true;
			
			//Disconnecting
			disconnect();
			
			//Stopping the service
			stopSelf();
			
			//Returning not sticky
			return START_NOT_STICKY;
		}
		//Checking if a disconnection has been requested
		else if(selfIntentActionDisconnect.equals(intentAction)) {
			//Removing the reconnection flag
			flagDropReconnect = false;
			
			//Disconnecting
			disconnect();
			
			//Updating the notification
			postDisconnectedNotification(true);
		}
		//Reconnecting the client if requested
		else if(getCurrentState() == stateDisconnected || selfIntentActionConnect.equals(intentAction)) connect(intent != null && intent.hasExtra(Constants.intentParamLaunchID) ? intent.getByteExtra(Constants.intentParamLaunchID, (byte) 0) : getNextLaunchID());
		
		//Setting the service as not shutting down
		shutdownRequested = false;
		
		//Calling the listeners
		//for(ServiceStartCallback callback : startCallbacks) callback.onServiceStarted(this);
		//startCallbacks.clear();
		
		//Returning sticky service
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		//Disconnecting
		disconnect();
		
		//Unregistering the broadcast receivers
		unregisterReceiver(networkStateChangeBroadcastReceiver);
		unregisterReceiver(pingBroadcastReceiver);
	}
	
	/* void setForegroundState(boolean foregroundState) {
		int currentState = getCurrentState();
		
		if(foregroundState) {
			Notification notification;
			if(currentState == stateConnected) notification = getBackgroundNotification(true);
			else if(currentState == stateConnecting) notification = getBackgroundNotification(false);
			else notification = getOfflineNotification(true);
			
			startForeground(-1, notification);
		} else {
			if(disconnectedNotificationRequested() && currentState == stateDisconnected) {
				stopForeground(false);
				postDisconnectedNotification(true);
			} else {
				stopForeground(true);
			}
		}
	} */
	
	private boolean foregroundServiceRequested() {
		return true;
		//return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getResources().getString(R.string.preference_server_foregroundservice_key), false);
	}
	
	private boolean disconnectedNotificationRequested() {
		return true;
		//return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getResources().getString(R.string.preference_server_disconnectionnotification_key), true);
	}
	
	private boolean connect(byte launchID) {
		//Closing the current connection if it exists
		//if(getCurrentState() != stateDisconnected) disconnect();
		
		//Returning if there is no connection
		{
			NetworkInfo activeNetwork = ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
			boolean isConnected = activeNetwork != null && activeNetwork.isConnected();
			if(!isConnected) {
				//Updating the notification
				postDisconnectedNotification(true);
				
				//Notifying the connection listeners
				broadcastState(stateDisconnected, intentResultCodeConnection, launchID);
				
				return false;
			}
		}
		
		//Checking if there is no hostname
		if(hostname == null || hostname.isEmpty()) {
			//Retrieving the data from the shared preferences
			SharedPreferences sharedPrefs = ((MainApplication) getApplication()).getConnectivitySharedPrefs();
			hostname = sharedPrefs.getString(MainApplication.sharedPreferencesConnectivityKeyHostname, "");
			password = sharedPrefs.getString(MainApplication.sharedPreferencesConnectivityKeyPassword, "");
		}
		
		//Checking if the hostname is invalid (nothing was found in memory or on disk)
		if(hostname.isEmpty()) {
			//Updating the notification
			postDisconnectedNotification(true);
			
			//Notifying the connection listeners
			broadcastState(stateDisconnected, intentResultCodeConnection, launchID);
			
			return false;
		}
		
		//Connecting through the top of the priority queue
		boolean result = connectionManagerPriorityList.get(0).get().connect(launchID);
		
		if(result) {
			//Updating the notification
			postConnectedNotification(false);
			
			//Notifying the connection listeners
			broadcastState(stateConnecting, 0, launchID);
		} else {
			//Updating the notification
			postDisconnectedNotification(false);
			
			//Notifying the connection listeners
			broadcastState(stateDisconnected, intentResultCodeInternalException, launchID);
		}
		
		//Returning the result
		return result;
	}
	
	void disconnect() {
		if(currentConnectionManager != null) currentConnectionManager.disconnect();
	}
	
	public void reconnect() {
		connect(getNextLaunchID());
	}
	
	private void postConnectedNotification(boolean isConnected) {
		if(isConnected && !foregroundServiceRequested()) return;
		
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(notificationID, getBackgroundNotification(isConnected));
	}
	
	private void postDisconnectedNotification(boolean silent) {
		if(!foregroundServiceRequested() && !disconnectedNotificationRequested()) return;
		
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(notificationID, getOfflineNotification(silent));
	}
	
	private void clearNotification() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(notificationID);
	}
	
	/* private void finishService() {
		//Returning to a background service
		stopForeground(true);
		
		//Scheduling the task
		new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
			@Override
			public void run() {
				//Showing the offline notification
				if(PreferenceManager.getDefaultSharedPreferences(ConnectionService.this).getBoolean(getResources().getString(R.string.preference_server_disconnectionnotification_key), true)) {
					NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					notificationManager.notify(-1, getOfflineNotification());
				}
				
				//Stopping the service
				stopSelf();
			}
		}, 1);
	} */
	
	private Notification getBackgroundNotification(boolean isConnected) {
		//Building the notification
		Notification notification = new NotificationCompat.Builder(this, MainApplication.notificationChannelStatus)
				.setSmallIcon(R.drawable.push)
				.setContentTitle(isConnected ? getResources().getString(R.string.message_connection_connected) : getResources().getString(R.string.progress_connectingtoserver))
				.setContentText(getResources().getString(R.string.imperative_tapopenapp))
				.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, Conversations.class), PendingIntent.FLAG_UPDATE_CURRENT))
				.addAction(-1, getResources().getString(R.string.action_disconnect), PendingIntent.getService(this, 0, new Intent(this, ConnectionService.class).setAction(selfIntentActionDisconnect), PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_CANCEL_CURRENT))
				.addAction(-1, getResources().getString(R.string.action_quit), PendingIntent.getService(this, 0, new Intent(this, ConnectionService.class).setAction(selfIntentActionStop), PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_CANCEL_CURRENT))
				.setShowWhen(false)
				.setPriority(Notification.PRIORITY_MIN)
				.setOnlyAlertOnce(true)
				.build();
		
		//Setting the notification as ongoing
		notification.flags = Notification.FLAG_ONGOING_EVENT;
		
		//Returning the notification
		return notification;
	}
	
	private Notification getOfflineNotification(boolean silent) {
		//Building and returning the notification
		return new NotificationCompat.Builder(this, MainApplication.notificationChannelStatus)
				.setSmallIcon(R.drawable.warning)
				.setContentTitle(getResources().getString(R.string.message_connection_disconnected))
				.setContentText(getResources().getString(R.string.imperative_tapopenapp))
				.setColor(getResources().getColor(R.color.colorServerDisconnected))
				.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, Conversations.class), PendingIntent.FLAG_UPDATE_CURRENT))
				.addAction(-1, getResources().getString(R.string.action_reconnect), PendingIntent.getService(this, 0, new Intent(this, ConnectionService.class), PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_CANCEL_CURRENT))
				.addAction(-1, getResources().getString(R.string.action_quit), PendingIntent.getService(this, 0, new Intent(this, ConnectionService.class).setAction(selfIntentActionStop), PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_CANCEL_CURRENT))
				.setShowWhen(true)
				.setOnlyAlertOnce(silent)
				.build();
	}
	
	//ConnectionBinder binder = new ConnectionBinder();
	
	@Override
	public IBinder onBind(Intent intent) {
		//Returning
		return null;
		//Starting the service
		//startService(intent);
		
		//Returning the binder
		//return binder;
	}
	
	/* class ConnectionBinder extends Binder {
		ConnectionService getService() {
			return ConnectionService.this;
		}
	} */
	
	/**
	 * Sends a broadcast to the listeners
	 *
	 * @param state the state of the connection
	 * @param code the error code, if the state is disconnected
	 * @param launchID the launch ID of the connection
	 */
	void broadcastState(int state, int code, byte launchID) {
		//Notifying the connection listeners
		LocalBroadcastManager.getInstance(ConnectionService.this).sendBroadcast(new Intent(localBCStateUpdate)
				.putExtra(Constants.intentParamState, state)
				.putExtra(Constants.intentParamCode, code)
				.putExtra(Constants.intentParamLaunchID, launchID));
	}
	
	private static abstract class Packager {
		/**
		 * Prepares data before being sent, usually by compressing it
		 *
		 * @param data the unpackaged data to be sent
		 * @param length the length of the data in the array
		 * @return the packaged data, or null if the process was unsuccessful
		 */
		abstract byte[] packageData(byte[] data, int length);
		
		/**
		 * Reverts received data transmissions, usually be decompressing it
		 *
		 * @param data the packaged data
		 * @return the unpackaged data, or null if the process was unsuccessful
		 */
		abstract byte[] unpackageData(byte[] data);
	}
	
	private interface ConnectionManagerSource {
		ConnectionManager get();
	}
	
	private abstract class ConnectionManager {
		//Creating the reference values
		private static final long pingExpiryTime = 20 * 1000; //20 seconds
		//Creating the request values
		byte launchID;
		
		//Creating the connection values
		private final Handler handler = new Handler();
		private final Runnable pingExpiryRunnable = this::disconnect;
		
		/**
		 * Connects to the server
		 *
		 * @param launchID an ID to represent and track this connection
		 * @return whether or not the request was successful
		 */
		boolean connect(byte launchID) {
			if(currentConnectionManager != null && currentConnectionManager.getState() != stateDisconnected) {
				currentConnectionManager.disconnect();
			}
			currentConnectionManager = this;
			this.launchID = launchID;
			return false;
		}
		
		/**
		 * Disconnects the connection manager from the server
		 */
		void disconnect() {
			flagDropReconnect = false;
		}
		
		/**
		 * Get the current state of the connection manager
		 *
		 * @return an integer representing the state
		 */
		abstract int getState();
		
		/**
		 * Sends a ping packet to the server
		 *
		 * @return whether or not the message was successful;y sent
		 */
		boolean sendPing() {
			//Starting the ping timer
			handler.postDelayed(pingExpiryRunnable, pingExpiryTime);
			
			return false;
		}
		
		/**
		 * Notifies the connection manager of a message, cancelling the ping expiry timer
		 */
		void onMessage() {
			//Cancelling the ping timer
			handler.removeCallbacks(pingExpiryRunnable);
			
			//Updating the scheduled ping
			schedulePing();
			
			//Updating the last connection time
			if(flagMarkEndTime) {
				SharedPreferences.Editor editor = ((MainApplication) getApplication()).getConnectivitySharedPrefs().edit();
				editor.putLong(MainApplication.sharedPreferencesConnectivityKeyLastConnectionTime, System.currentTimeMillis());
				editor.apply();
			}
		}
		
		/**
		 * Gets a packager for processing transferable data via this protocol version
		 *
		 * @return the packager
		 */
		abstract Packager getPackager();
		
		/**
		 * Returns the hash algorithm to use with this protocol
		 *
		 * @return the hash algorithm
		 */
		abstract String getHashAlgorithm();
		
		/**
		 * Requests a message to be sent to the specified conversation
		 *
		 * @param requestID the ID of the request
		 * @param chatGUID the GUID of the target conversation
		 * @param message the message to send
		 * @return whether or not the request was successfully sent
		 */
		abstract boolean sendMessage(short requestID, String chatGUID, String message);
		
		/**
		 * Requests a message to be send to the specified conversation members via the service
		 *
		 * @param requestID the ID of the request
		 * @param chatMembers the members to send the message to
		 * @param message the message to send
		 * @param service the service to send the message across
		 * @return whether or not the request was successfully sent
		 */
		abstract boolean sendMessage(short requestID, String[] chatMembers, String message, String service);
		
		/**
		 * Requests the download of a remote attachment
		 *
		 * @param requestID the ID of the request
		 * @return whether or not the request was successful
		 */
		abstract boolean addDownloadRequest(short requestID, String attachmentGUID);
		
		/**
		 * Uploads a file chunk to be sent to the specified conversation
		 *
		 * @param requestID the ID of the request
		 * @param requestIndex the index of the request
		 * @param conversationGUID the conversation to send the file to
		 * @param data the transmission-ready bytes of the file chunk
		 * @param fileName the name of the file to send
		 * @param isLast whether or not this is the last file packet
		 * @return whether or not the action was successful
		 */
		abstract boolean uploadFilePacket(short requestID, int requestIndex, String conversationGUID, byte[] data, String fileName, boolean isLast);
		
		/**
		 * Uploads a file chunk to be sent to the specified conversation members
		 *
		 * @param requestID the ID of the request
		 * @param requestIndex the index of the request
		 * @param conversationMembers the members of the conversation to send the file to
		 * @param data the transmission-ready bytes of the file chunk
		 * @param fileName the name of the file to send
		 * @param service the service to send the file across
		 * @param isLast whether or not this is the last file packet
		 * @return whether or not the action was successful
		 */
		abstract boolean uploadFilePacket(short requestID, int requestIndex, String[] conversationMembers, byte[] data, String fileName, String service, boolean isLast);
		
		/**
		 * Sends a request to fetch conversation information
		 *
		 * @param list the list of conversation requests
		 * @return whether or not the request was successfully sent
		 */
		abstract boolean sendConversationInfoRequest(List<ConversationInfoRequest> list);
		
		/**
		 * Requests a time range-based message retrieval
		 *
		 * @param timeLower the lower time range limit
		 * @param timeUpper the upper time range limit
		 * @return whether or not the request was successfully sent
		 */
		abstract boolean requestRetrievalTime(long timeLower, long timeUpper);
		
		/**
		 * Requests a mass message retrieval
		 *
		 * @return whether or not the request was successfully sent
		 */
		abstract boolean requestRetrievalAll();
		
		/**
		 * Checks if the specified communications version is applicable
		 *
		 * @param version the major communications version to check
		 * @return 0 if the version is applicable, -1 if the version is too old, 1 if the version is too new
		 */
		abstract int checkCommVerApplicability(int version);
		
		/**
		 * Forwards a request to the next connection manager
		 *
		 * @param launchID an ID used to identify connection attempts
		 * @param thread whether or not to use a new thread
		 * @return if the request was forwarded
		 */
		boolean forwardRequest(byte launchID, boolean thread) {
			int targetIndex = connectionManagerClassPriorityList.indexOf(getClass()) + 1;
			if(targetIndex == connectionManagerPriorityList.size()) return false;
			if(thread) {
				new Handler(Looper.getMainLooper()).post(() -> {
					if(currentLaunchID == launchID) connectionManagerPriorityList.get(targetIndex).get().connect(launchID);
				});
			} else connectionManagerPriorityList.get(targetIndex).get().connect(launchID);
			return true;
		}
		
		abstract class ProtocolManager {
			/**
			 * Sends a ping packet to the server
			 * @return whether or not the message was successfully sent
			 */
			abstract boolean sendPing();
			
			/**
			 * Handles incoming data received from the server
			 *
			 * @param messageType header data representing the message type
			 * @param data the raw data received from the network
			 */
			abstract void processData(int messageType, byte[] data);
			
			/**
			 * Sends an authentication request to the server
			 * @return whether or not the message was successfully sent
			 */
			abstract boolean sendAuthenticationRequest();
			
			/**
			 * Requests a message to be sent to the specified conversation
			 *
			 * @param requestID the ID of the request
			 * @param chatGUID the GUID of the target conversation
			 * @param message the message to send
			 * @return whether or not the request was successfully sent
			 */
			abstract boolean sendMessage(short requestID, String chatGUID, String message);
			
			/**
			 * Requests a message to be send to the specified conversation members via the service
			 *
			 * @param requestID the ID of the request
			 * @param chatMembers the members to send the message to
			 * @param message the message to send
			 * @param service the service to send the message across
			 * @return whether or not the request was successfully sent
			 */
			abstract boolean sendMessage(short requestID, String[] chatMembers, String message, String service);
			
			/**
			 * Requests the download of a remote attachment
			 *
			 * @param requestID the ID of the request
			 * @return whether or not the request was successful
			 */
			abstract boolean addDownloadRequest(short requestID, String attachmentGUID);
			
			/**
			 * Uploads a file chunk to be sent to the specified conversation
			 *
			 * @param requestID the ID of the request
			 * @param requestIndex the index of the request
			 * @param conversationGUID the conversation to send the file to
			 * @param data the transmission-ready bytes of the file chunk
			 * @param fileName the name of the file to send
			 * @param isLast whether or not this is the last file packet
			 * @return whether or not the action was successful
			 */
			abstract boolean uploadFilePacket(short requestID, int requestIndex, String conversationGUID, byte[] data, String fileName, boolean isLast);
			
			/**
			 * Uploads a file chunk to be sent to the specified conversation members
			 *
			 * @param requestID the ID of the request
			 * @param requestIndex the index of the request
			 * @param conversationMembers the members of the conversation to send the file to
			 * @param data the transmission-ready bytes of the file chunk
			 * @param fileName the name of the file to send
			 * @param service the service to send the file across
			 * @param isLast whether or not this is the last file packet
			 * @return whether or not the action was successful
			 */
			abstract boolean uploadFilePacket(short requestID, int requestIndex, String[] conversationMembers, byte[] data, String fileName, String service, boolean isLast);
			
			/**
			 * Sends a request to fetch conversation information
			 *
			 * @param list the list of conversation requests
			 * @return whether or not the request was successfully sent
			 */
			abstract boolean sendConversationInfoRequest(List<ConversationInfoRequest> list);
			
			/**
			 * Requests a time range-based message retrieval
			 *
			 * @param timeLower the lower time range limit
			 * @param timeUpper the upper time range limit
			 * @return whether or not the request was successfully sent
			 */
			abstract boolean requestRetrievalTime(long timeLower, long timeUpper);
			
			/**
			 * Requests a mass message retrieval
			 *
			 * @return whether or not the request was successfully sent
			 */
			abstract boolean requestRetrievalAll();
			
			/**
			 * Gets a packager for processing transferable data via this protocol version
			 *
			 * @return the packager
			 */
			abstract Packager getPackager();
			
			/**
			 * Returns the hash algorithm to use with this protocol
			 *
			 * @return the hash algorithm
			 */
			abstract String getHashAlgorithm();
			
			/**
			 * Returns the charset used when serializing strings
			 *
			 * @return the charset
			 */
			abstract String getCharset();
			
			/**
			 * Checks if the specified sub-communications version is applicable
			 *
			 * @param version the minor communications version to check
			 * @return whether or not this protocol manager can handle the specified version
			 */
			abstract boolean checkSubVerApplicability(int version);
		}
	}
	
	private class ClientCommCaladium extends ConnectionManager {
		//Creating the connection values
		private ProtocolManager protocolManager = null;
		private ConnectionThread connectionThread = null;
		private int currentState = stateDisconnected;
		
		private static final long handshakeExpiryTime = 1000 * 10; //10 seconds
		private final Handler handler = new Handler();
		private final Runnable handshakeExpiryRunnable = () -> {
			if(connectionThread != null) connectionThread.closeConnection(intentResultCodeConnection, true);
		};
		
		//Creating the transmission values
		private static final String stringCharset = "UTF-8";
		
		private static final int nhtClose = -1;
		private static final int nhtPing = -2;
		private static final int nhtPong = -3;
		private static final int nhtInformation = 0;
		private static final int nhtAuthentication = 1;
		private static final int nhtMessageUpdate = 2;
		private static final int nhtTimeRetrieval = 3;
		private static final int nhtMassRetrieval = 4;
		private static final int nhtMassRetrievalFinish = 10;
		private static final int nhtConversationUpdate = 5;
		private static final int nhtModifierUpdate = 6;
		private static final int nhtAttachmentReq = 7;
		private static final int nhtAttachmentReqConfirm = 8;
		private static final int nhtAttachmentReqFail = 9;
		
		private static final int nhtSendResult = 100;
		private static final int nhtSendTextExisting = 101;
		private static final int nhtSendTextNew = 102;
		private static final int nhtSendFileExisting = 103;
		private static final int nhtSendFileNew = 104;
		
		private static final int nhtAuthenticationOK = 0;
		private static final int nhtAuthenticationUnauthorized = 1;
		private static final int nhtAuthenticationBadRequest = 2;
		
		private static final String transmissionCheck = "4yAIlVK0Ce_Y7nv6at_hvgsFtaMq!lZYKipV40Fp5E%VSsLSML";
		
		//Creating the other values
		private boolean connectionEstablished = false;
		
		@Override
		boolean connect(byte launchID) {
			//Calling the super method
			super.connect(launchID);
			
			//Parsing the hostname
			String cleanHostname = hostname;
			int port = Constants.defaultPort;
			if(regExValidPort.matcher(cleanHostname).find()) {
				String[] targetDetails = hostname.split(":");
				cleanHostname = targetDetails[0];
				port = Integer.parseInt(targetDetails[1]);
			}
			
			//Setting the state as connecting
			currentState = stateConnecting;
			
			//Starting the connection
			connectionThread = new ConnectionThread(cleanHostname, port);
			connectionThread.start();
			
			//Returning true
			return true;
		}
		
		@Override
		void disconnect() {
			super.disconnect();
			connectionThread.initiateClose(intentResultCodeConnection, false);
		}
		
		@Override
		int getState() {
			return currentState;
		}
		
		private boolean queuePacket(PacketStruct packet) {
			return connectionThread != null && connectionThread.queuePacket(packet);
		}
		
		@Override
		boolean sendPing() {
			super.sendPing();
			
			if(protocolManager == null) return false;
			else return protocolManager.sendPing();
		}
		
		@Override
		Packager getPackager() {
			if(protocolManager == null) return null;
			return protocolManager.getPackager();
		}
		
		@Override
		String getHashAlgorithm() {
			if(protocolManager == null) return null;
			return protocolManager.getHashAlgorithm();
		}
		
		@Override
		boolean sendMessage(short requestID, String chatGUID, String message) {
			if(protocolManager == null) return false;
			return protocolManager.sendMessage(requestID, chatGUID, message);
		}
		
		@Override
		boolean sendMessage(short requestID, String[] chatMembers, String message, String service) {
			if(protocolManager == null) return false;
			return protocolManager.sendMessage(requestID, chatMembers, message, service);
		}
		
		@Override
		boolean addDownloadRequest(short requestID, String attachmentGUID) {
			if(protocolManager == null) return false;
			return protocolManager.addDownloadRequest(requestID, attachmentGUID);
		}
		
		@Override
		boolean sendConversationInfoRequest(List<ConversationInfoRequest> list) {
			if(protocolManager == null) return false;
			return protocolManager.sendConversationInfoRequest(list);
		}
		
		@Override
		boolean uploadFilePacket(short requestID, int requestIndex, String conversationGUID, byte[] data, String fileName, boolean isLast) {
			if(protocolManager == null) return false;
			return protocolManager.uploadFilePacket(requestID, requestIndex, conversationGUID, data, fileName, isLast);
		}
		
		@Override
		boolean uploadFilePacket(short requestID, int requestIndex, String[] conversationMembers, byte[] data, String fileName, String service, boolean isLast) {
			if(protocolManager == null) return false;
			return protocolManager.uploadFilePacket(requestID, requestIndex, conversationMembers, data, fileName, service, isLast);
		}
		
		@Override
		boolean requestRetrievalTime(long timeLower, long timeUpper) {
			if(protocolManager == null) return false;
			return protocolManager.requestRetrievalTime(timeLower, timeUpper);
		}
		
		@Override
		boolean requestRetrievalAll() {
			if(protocolManager == null) return false;
			return protocolManager.requestRetrievalAll();
		}
		
		@Override
		int checkCommVerApplicability(int version) {
			return Integer.compare(version, 4);
		}
		
		private void updateStateDisconnected(int reason, boolean forwardRequest) {
			//Setting the state
			currentState = stateDisconnected;
			
			//Setting the connection established flag
			connectionEstablished = false;
			
			//Stopping the timers
			handler.removeCallbacks(handshakeExpiryRunnable);
			
			//Invalidating the protocol manager
			protocolManager = null;
			
			new Handler(Looper.getMainLooper()).post(ConnectionService.this::cancelMassRetrieval);
			
			//Attempting to connect via the legacy method
			if(!forwardRequest || !forwardRequest(launchID, true)) {
				new Handler(Looper.getMainLooper()).post(() -> {
					//Checking if this is the most recent launch
					if(currentLaunchID == launchID) {
						//Setting the last connection result
						lastConnectionResult = reason;
						
						//Notifying the connection listeners
						broadcastState(stateDisconnected, reason, launchID);
						
						//Updating the notification state
						if(!shutdownRequested) postDisconnectedNotification(false);
						
						//Checking if a connection existed for reconnection and the preference is enabled
						if(flagDropReconnect && PreferenceManager.getDefaultSharedPreferences(MainApplication.getInstance()).getBoolean(MainApplication.getInstance().getResources().getString(R.string.preference_server_dropreconnect_key), false)) {
							//Reconnecting
							new Handler().postDelayed(() -> {
								if(currentState == stateDisconnected && currentLaunchID == launchID) connect(getNextLaunchID());
							}, dropReconnectDelayMillis);
						}
						
						//Clearing the flags
						flagMarkEndTime = flagDropReconnect = false;
					}
				});
			}
		}
		
		private void updateStateConnected() {
			//Setting the connection established flag
			connectionEstablished = true;
			
			//Reading the shared preferences connectivity information
			SharedPreferences sharedPrefs = ((MainApplication) getApplication()).getConnectivitySharedPrefs();
			String lastConnectionHostname = sharedPrefs.getString(MainApplication.sharedPreferencesConnectivityKeyLastConnectionHostname, null);
			long lastConnectionTime = sharedPrefs.getLong(MainApplication.sharedPreferencesConnectivityKeyLastConnectionTime, -1);
			
			//Updating the shared preferences connectivity information
			SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();
			if(!hostname.equals(lastConnectionHostname)) sharedPrefsEditor.putString(MainApplication.sharedPreferencesConnectivityKeyLastConnectionHostname, hostname);
			sharedPrefsEditor.putLong(MainApplication.sharedPreferencesConnectivityKeyLastConnectionTime, System.currentTimeMillis());
			sharedPrefsEditor.apply();
			
			//Running on the main thread
			new Handler(Looper.getMainLooper()).post(() -> {
				//Checking if this is the most recent launch
				if(currentLaunchID == launchID) {
					//Setting the last connection result
					lastConnectionResult = intentResultCodeSuccess;
					
					//Setting the state
					currentState = stateConnected;
					
					//Retrieving the pending conversation info
					sendConversationInfoRequest(pendingConversations);
					
					//Setting the flags
					flagMarkEndTime = flagDropReconnect = true;
					
					//Checking if the last connection is the same as the current one
					if(hostname.equals(lastConnectionHostname)) {
						//Fetching the messages since the last connection time
						retrieveMessagesSince(lastConnectionTime, System.currentTimeMillis());
					}
				}
			});
			
			//Notifying the connection listeners
			broadcastState(stateConnected, -1, launchID);
			
			//Updating the notification
			if(foregroundServiceRequested()) postConnectedNotification(true);
			else clearNotification();
			
			//Scheduling the ping
			schedulePing();
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
				
				//Checking if the client can't handle the communications version
				int verApplicability = checkCommVerApplicability(communicationsVersion);
				if(verApplicability != 0) {
					//Terminating the connection
					connectionThread.closeConnection(verApplicability == -1 ? intentResultCodeServerOutdated : intentResultCodeClientOutdated, verApplicability == -1);
					return;
				}
				protocolManager = findProtocolManager(communicationsSubVersion);
				if(protocolManager == null) {
					connectionThread.closeConnection(intentResultCodeClientOutdated, false);
					return;
				}
				
				//Recording the protocol version
				new Handler(Looper.getMainLooper()).post(() -> activeCommunicationsVersion = communicationsVersion);
				
				//Sending the handshake data
				protocolManager.sendAuthenticationRequest();
			}
		}
		
		private ProtocolManager findProtocolManager(int subVersion) {
			switch(subVersion) {
				default:
					return null;
				case 1:
					return new ClientProtocol1();
				case 2:
					return new ClientProtocol2();
			}
		}
		
		private class ConnectionThread extends Thread {
			//Creating the reference connection values
			private final String hostname;
			private final int port;
			
			private Socket socket;
			private DataInputStream inputStream;
			private DataOutputStream outputStream;
			private WriterThread writerThread = null;
			
			ConnectionThread(String hostname, int port) {
				this.hostname = hostname;
				this.port = port;
			}
			
			@Override
			public void run() {
				try {
					//Returning if the thread is interrupted
					if(isInterrupted()) return;
					
					//Connecting to the server
					socket = new Socket();
					//socket.setKeepAlive(true);
					socket.connect(new InetSocketAddress(hostname, port), 10 * 1000);
					
					//Returning if the thread is interrupted
					if(isInterrupted()) {
						try {
							socket.close();
						} catch(IOException exception) {
							exception.printStackTrace();
						}
						return;
					}
					
					//Getting the streams
					inputStream = new DataInputStream(socket.getInputStream());
					outputStream = new DataOutputStream(socket.getOutputStream());
					
					//Starting the writer thread
					writerThread = new WriterThread();
					writerThread.start();
				} catch(IOException exception) {
					//Printing the stack trace
					exception.printStackTrace();
					
					//Updating the state
					updateStateDisconnected(intentResultCodeConnection, !(exception instanceof ConnectException));
					
					//Returning
					return;
				}
				
				//Starting the handshake timer
				handler.postDelayed(handshakeExpiryRunnable, handshakeExpiryTime);
				
				//Reading from the input stream
				while(!isInterrupted()) {
					try {
						//Reading the header data
						int messageType = inputStream.readInt();
						int contentLen = inputStream.readInt();
						
						//Checking if the content length is greater than the maximum packet allocation
						if(contentLen > maxPacketAllocation) {
							//Logging the error
							Logger.getGlobal().log(Level.WARNING, "Rejecting large packet (type: " + messageType + " - size: " + contentLen + ")");
							
							//Closing the connection
							closeConnection(intentResultCodeConnection, !connectionEstablished);
							break;
						}
						
						//Reading the content
						byte[] content = new byte[contentLen];
						if(contentLen > 0) {
							int bytesRemaining = contentLen;
							int offset = 0;
							int readCount;
							while(bytesRemaining > 0) {
								readCount = inputStream.read(content, offset, bytesRemaining);
								if(readCount == -1) { //No data read, stream is closed
									closeConnection(intentResultCodeConnection, !connectionEstablished);
									return;
								}
								
								offset += readCount;
								bytesRemaining -= readCount;
							}
						}
						
						//Processing the data
						if(protocolManager == null) processFloatingData(messageType, content);
						else protocolManager.processData(messageType, content);
					} catch(SSLHandshakeException exception) {
						//Closing the connection
						exception.printStackTrace();
						closeConnection(intentResultCodeConnection, true);
						
						//Breaking
						break;
					} catch(IOException exception) {
						//Closing the connection
						exception.printStackTrace();
						closeConnection(intentResultCodeConnection, !connectionEstablished);
						
						//Breaking
						break;
					}
				}
				
				//Closing the socket
				try {
					socket.close();
				} catch(IOException exception) {
					exception.printStackTrace();
				}
			}
			
			boolean queuePacket(PacketStruct packet) {
				if(writerThread == null) return false;
				writerThread.uploadQueue.add(packet);
				return true;
			}
			
			void initiateClose(int resultCode, boolean forwardRequest) {
				//Sending a message and finishing the threads
				if(writerThread == null) {
					interrupt();
				} else {
					queuePacket(new PacketStruct(nhtClose, new byte[0], () -> {
						interrupt();
						writerThread.interrupt();
					}));
				}
				
				//Updating the state
				updateStateDisconnected(resultCode, forwardRequest);
			}
			
			private void closeConnection(int reason, boolean forwardRequest) {
				//Finishing the threads
				if(writerThread != null) writerThread.interrupt();
				interrupt();
				
				//Updating the state
				updateStateDisconnected(reason, forwardRequest);
			}
			
			synchronized boolean sendDataSync(int messageType, byte[] data, boolean flush) {
				try {
					//Writing the message
					outputStream.writeInt(messageType);
					outputStream.writeInt(data.length);
					outputStream.write(data);
					if(flush) outputStream.flush();
					
					//Returning true
					return true;
				} catch(IOException exception) {
					//Logging the exception
					exception.printStackTrace();
					
					//Closing the connection
					if(socket.isConnected()) {
						closeConnection(intentResultCodeConnection, false);
					} else {
						Crashlytics.logException(exception);
					}
					
					//Returning false
					return false;
				}
			}
			
			private class WriterThread extends Thread {
				//Creating the queue
				final BlockingQueue<PacketStruct> uploadQueue = new LinkedBlockingQueue<>();
				
				@Override
				public void run() {
					PacketStruct packet;
					
					try {
						while(!isInterrupted()) {
							try {
								packet = uploadQueue.take();
								
								try {
									//outputStream.write(ByteBuffer.allocate(Integer.SIZE / 8 * 2).putInt(packet.type).putInt(packet.content.length).array());
									//outputStream.write(packet.content);
									sendDataSync(packet.type, packet.content, false);
								} finally {
									if(packet.sentRunnable != null) packet.sentRunnable.run();
								}
								
								while((packet = uploadQueue.poll()) != null) {
									try {
										//outputStream.write(ByteBuffer.allocate(Integer.SIZE / 8 * 2).putInt(packet.type).putInt(packet.content.length).array());
										//outputStream.write(packet.content);
										sendDataSync(packet.type, packet.content, false);
									} finally {
										if(packet.sentRunnable != null) packet.sentRunnable.run();
									}
								}
								
								outputStream.flush();
							} catch(IOException exception) {
								exception.printStackTrace();
								
								if(socket.isConnected()) {
									closeConnection(intentResultCodeConnection, false);
								} else {
									Crashlytics.logException(exception);
								}
							}
						}
						//closeConnection(intentResultCodeConnection, false);
					} catch(InterruptedException exception) {
						//exception.printStackTrace();
						//closeConnection(intentResultCodeConnection, false); //Can only be interrupted from closeConnection, so this is pointless
						
						return;
					}
				}
				
				/* private void sendPacket(PacketStruct packet) throws IOException {
					outputStream.write(ByteBuffer.allocate(Integer.SIZE / 8 * 2).putInt(packet.type).putInt(packet.content.length).array());
					outputStream.write(packet.content);
					outputStream.flush();
				} */
			}
		}
		
		private class ClientProtocol1 extends ProtocolManager {
			private final Packager protocolPackager = new PackagerComm3();
			private static final String hashAlgorithm = "MD5";
			private static final String stringCharset = "UTF-8";
			
			@Override
			boolean sendPing() {
				return queuePacket(new PacketStruct(nhtPing, new byte[0]));
			}
			
			@Override
			void processData(int messageType, byte[] data) {
				//Notifying the super connection manager of a message
				onMessage();
				
				switch(messageType) {
					case nhtClose:
						connectionThread.closeConnection(intentResultCodeConnection, false);
						break;
					case nhtPing:
						queuePacket(new PacketStruct(nhtPong, new byte[0]));
						break;
					case nhtAuthentication: {
						//Stopping the authentication timer
						handler.removeCallbacks(handshakeExpiryRunnable);
						
						//Reading the result code
						ByteBuffer dataBuffer = ByteBuffer.wrap(data);
						int resultCode = dataBuffer.getInt();
						
						//Translating the result to the local value
						switch(resultCode) {
							case nhtAuthenticationOK:
								resultCode = intentResultCodeSuccess;
								break;
							case nhtAuthenticationUnauthorized:
								resultCode = intentResultCodeUnauthorized;
								break;
							case nhtAuthenticationBadRequest:
								resultCode = intentResultCodeBadRequest;
								break;
									/* case nhtAuthenticationVersionMismatch:
										if(SharedValues.mmCommunicationsVersion > communicationsVersion) result = intentResultCodeServerOutdated;
										else result = intentResultCodeClientOutdated;
										break; */
						}
						
						//Finishing the connection establishment if the handshake was successful
						if(resultCode == intentResultCodeSuccess) updateStateConnected();
							//Otherwise terminating the connection
						else connectionThread.closeConnection(resultCode, resultCode == intentResultCodeBadRequest); //Only forward the request if the request couldn't be processed (an unauthorized response means that the server could understand the request)
						
						break;
					}
					case nhtMessageUpdate:
					case nhtTimeRetrieval: {
						//Reading the list
						List<Blocks.ConversationItem> list;
						try(ByteArrayInputStream src = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(src)) {
							SharedValues.EncryptableData dataSec = (SharedValues.EncryptableData) in.readObject();
							dataSec.decrypt(password);
							
							try(ByteArrayInputStream srcSec = new ByteArrayInputStream(dataSec.data); ObjectInputStream inSec = new ObjectInputStream(srcSec)) {
								int count = inSec.readInt();
								list = new ArrayList<>(count);
								for(int i = 0; i < count; i++) list.add(((SharedValues.ConversationItem) inSec.readObject()).toBlock());
							}
						} catch(IOException | RuntimeException | ClassNotFoundException | GeneralSecurityException exception) {
							exception.printStackTrace();
							break;
						}
						
						//Processing the messages
						processMessageUpdate(list, true);
						
						break;
					}
					case nhtMassRetrieval: {
						//Reading the lists
						List<Blocks.ConversationItem> listItems;
						List<Blocks.ConversationInfo> listConversations;
						try(ByteArrayInputStream src = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(src)) {
							SharedValues.EncryptableData dataSec = (SharedValues.EncryptableData) in.readObject();
							dataSec.decrypt(password);
							
							try(ByteArrayInputStream srcSec = new ByteArrayInputStream(dataSec.data); ObjectInputStream inSec = new ObjectInputStream(srcSec)) {
								int count = inSec.readInt();
								listItems = new ArrayList<>(count);
								for(int i = 0; i < count; i++) listItems.add(((SharedValues.ConversationItem) inSec.readObject()).toBlock());
								
								count = inSec.readInt();
								listConversations = new ArrayList<>(count);
								for(int i = 0; i < count; i++) listConversations.add(((SharedValues.ConversationInfo) inSec.readObject()).toBlock());
							}
						} catch(IOException | RuntimeException | ClassNotFoundException | GeneralSecurityException exception) {
							exception.printStackTrace();
							break;
						}
						
						//Processing the messages
						processMassRetrievalResult(listItems, listConversations);
						
						break;
					}
					case nhtConversationUpdate: {
						//Reading the list
						List<Blocks.ConversationInfo> list;
						try(ByteArrayInputStream src = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(src)) {
							SharedValues.EncryptableData dataSec = (SharedValues.EncryptableData) in.readObject();
							dataSec.decrypt(password);
							
							try(ByteArrayInputStream srcSec = new ByteArrayInputStream(dataSec.data); ObjectInputStream inSec = new ObjectInputStream(srcSec)) {
								int count = inSec.readInt();
								list = new ArrayList<>(count);
								for(int c = 0; c < count; c++) {
									String guid = inSec.readUTF();
									boolean available = inSec.readBoolean();
									if(available) {
										String service = in.readUTF();
										String name = in.readBoolean() ? in.readUTF() : null;
										String[] members = new String[in.readInt()];
										for(int i = 0; i < members.length; i++) members[i] = in.readUTF();
										list.add(new Blocks.ConversationInfo(guid, service, name, members));
									} else {
										list.add(new Blocks.ConversationInfo(guid));
									}
								}
							}
						} catch(IOException | RuntimeException | ClassNotFoundException | GeneralSecurityException exception) {
							exception.printStackTrace();
							break;
						}
						
						//Processing the conversations
						processChatInfoResponse(list);
						
						break;
					}
					case nhtModifierUpdate: {
						//Reading the list
						List<Blocks.ModifierInfo> list;
						try(ByteArrayInputStream bis = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(bis)) {
							SharedValues.EncryptableData dataSec = (SharedValues.EncryptableData) in.readObject();
							dataSec.decrypt(password);
							
							try(ByteArrayInputStream srcSec = new ByteArrayInputStream(dataSec.data); ObjectInputStream inSec = new ObjectInputStream(srcSec)) {
								int count = inSec.readInt();
								list = new ArrayList<>(count);
								for(int i = 0; i < count; i++) list.add(((SharedValues.ModifierInfo) inSec.readObject()).toBlock());
							}
						} catch(IOException | RuntimeException | ClassNotFoundException | GeneralSecurityException exception) {
							exception.printStackTrace();
							break;
						}
						
						//Processing the conversations
						processModifierUpdate(list, getPackager());
						
						break;
					}
					case nhtAttachmentReq: {
						//Reading the data
						final short requestID;
						final int requestIndex;
						final long fileSize;
						final boolean isLast;
						
						final String fileGUID;
						final byte[] compressedBytes;
						
						try(ByteArrayInputStream bis = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(bis)) {
							requestID = in.readShort();
							requestIndex = in.readInt();
							if(requestIndex == 0) fileSize = in.readLong();
							else fileSize = -1;
							isLast = in.readBoolean();
							
							SharedValues.EncryptableData dataSec = (SharedValues.EncryptableData) in.readObject();
							dataSec.decrypt(password);
							
							try(ByteArrayInputStream srcSec = new ByteArrayInputStream(dataSec.data); ObjectInputStream inSec = new ObjectInputStream(srcSec)) {
								fileGUID = inSec.readUTF();
								int contentLen = inSec.readInt();
								if(contentLen > maxPacketAllocation) {
									//Logging the error
									Logger.getGlobal().log(Level.WARNING, "Rejecting large byte chunk (type: " + messageType + " - size: " + contentLen + ")");
									
									//Closing the connection
									connectionThread.closeConnection(intentResultCodeConnection, false);
									break;
								}
								compressedBytes = new byte[contentLen];
								inSec.readFully(compressedBytes);
							}
						} catch(IOException | RuntimeException | ClassNotFoundException | GeneralSecurityException exception) {
							exception.printStackTrace();
							break;
						}
						
						//Running on the UI thread
						mainHandler.post(() -> {
							//Searching for a matching request
							for(FileDownloadRequest request : fileDownloadRequests) {
								if(request.requestID != requestID || !request.attachmentGUID.equals(fileGUID)) continue;
								if(requestIndex == 0) request.setFileSize(fileSize);
								request.processFileFragment(ConnectionService.this, compressedBytes, requestIndex, isLast, getPackager());
								if(isLast) fileDownloadRequests.remove(request);
								break;
							}
						});
						
						break;
					}
					case nhtAttachmentReqConfirm: {
						//Reading the data
						final short requestID = ByteBuffer.wrap(data).getShort();
						
						//Running on the UI thread
						mainHandler.post(() -> {
							//Searching for a matching request
							for(FileDownloadRequest request : fileDownloadRequests) {
								if(request.requestID != requestID/* || !request.attachmentGUID.equals(fileGUID)*/) continue;
								request.stopTimer(true);
								request.onResponseReceived();
								break;
							}
						});
						
						break;
					}
					case nhtAttachmentReqFail: {
						//Reading the data
						final short requestID = ByteBuffer.wrap(data).getShort();
						
						//Running on the UI thread
						mainHandler.post(() -> {
							//Searching for a matching request
							for(FileDownloadRequest request : fileDownloadRequests) {
								if(request.requestID != requestID/* || !request.attachmentGUID.equals(fileGUID)*/) continue;
								request.failDownload();
								break;
							}
						});
						
						break;
					}
					case nhtSendResult: {
						//Reading the data
						final short requestID;
						final boolean result;
						
						ByteBuffer byteBuffer = ByteBuffer.wrap(data);
						requestID = byteBuffer.getShort();
						result = byteBuffer.get() == 1;
						
						//Getting the message response manager
						final MessageResponseManager messageResponseManager = messageSendRequests.get(requestID);
						if(messageResponseManager != null) {
							//Removing the request
							messageSendRequests.remove(requestID);
							messageResponseManager.stopTimer(false);
							
							//Running on the UI thread
							new Handler(Looper.getMainLooper()).post(() -> {
								//Telling the listener
								if(result) messageResponseManager.onSuccess();
								else messageResponseManager.onFail(messageSendExternalException);
							});
						}
						
						break;
					}
				}
			}
			
			@Override
			boolean sendAuthenticationRequest() {
				//Returning false if there is no connection thread
				if(connectionThread == null) return false;
				
				byte[] packetData;
				try(ByteArrayOutputStream trgt = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(trgt)) {
					out.writeObject(new SharedValues.EncryptableData(transmissionCheck.getBytes(stringCharset)).encrypt(password));
					out.flush();
					
					packetData = trgt.toByteArray();
				} catch(IOException | GeneralSecurityException exception) {
					//Logging the error
					exception.printStackTrace();
					Crashlytics.logException(exception);
					
					//Closing the connection
					connectionThread.closeConnection(intentResultCodeInternalException, false);
					
					//Returning false
					return false;
				}
				
				//Sending the message
				connectionThread.queuePacket(new PacketStruct(nhtAuthentication, packetData));
				
				//Returning true
				return true;
			}
			
			@Override
			boolean sendMessage(short requestID, String chatGUID, String message) {
				//Returning false if there is no connection thread
				if(connectionThread == null) return false;
				
				byte[] packetData;
				try(ByteArrayOutputStream trgt = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(trgt);
					ByteArrayOutputStream trgtSec = new ByteArrayOutputStream(); ObjectOutputStream outSec = new ObjectOutputStream(trgtSec)) {
					//Adding the data
					out.writeShort(requestID); //Request ID
					
					outSec.writeUTF(chatGUID); //Chat GUID
					outSec.writeUTF(message); //Message
					outSec.flush();
					
					out.writeObject(new SharedValues.EncryptableData(trgtSec.toByteArray()).encrypt(password)); //Encrypted data
					
					out.flush();
					
					packetData = trgt.toByteArray();
				} catch(IOException | GeneralSecurityException exception) {
					//Printing the stack trace
					exception.printStackTrace();
					
					//Returning false
					return false;
				}
				
				//Sending the message
				connectionThread.queuePacket(new PacketStruct(nhtSendTextExisting, packetData));
				
				//Returning true
				return true;
			}
			
			@Override
			boolean sendMessage(short requestID, String[] chatMembers, String message, String service) {
				//Returning false if there is no connection thread
				if(connectionThread == null) return false;
				
				byte[] packetData;
				try(ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos);
					ByteArrayOutputStream bosSec = new ByteArrayOutputStream(); ObjectOutputStream outSec = new ObjectOutputStream(bosSec)) {
					//Adding the data
					out.writeShort(requestID); //Request ID
					
					outSec.writeInt(chatMembers.length); //Members
					for(String item : chatMembers) outSec.writeUTF(item);
					outSec.writeUTF(message); //Message
					outSec.writeUTF(service); //Service
					outSec.flush();
					
					out.writeObject(new SharedValues.EncryptableData(bosSec.toByteArray()).encrypt(password)); //Encrypted data
					
					out.flush();
					
					packetData = bos.toByteArray();
				} catch(IOException | GeneralSecurityException exception) {
					//Printing the stack trace
					exception.printStackTrace();
					
					//Returning false
					return false;
				}
				
				//Sending the message
				connectionThread.queuePacket(new PacketStruct(nhtSendTextNew, packetData));
				
				//Returning true
				return true;
			}
			
			@Override
			boolean addDownloadRequest(short requestID, String attachmentGUID) {
				//Preparing to serialize the request
				try(ByteArrayOutputStream trgt = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(trgt);
					ByteArrayOutputStream trgtSec = new ByteArrayOutputStream(); ObjectOutputStream outSec = new ObjectOutputStream(trgtSec)) {
					//Adding the data
					out.writeShort(requestID); //Request ID
					out.writeInt(attachmentChunkSize); //Chunk size
					
					outSec.writeUTF(attachmentGUID); //File GUID
					outSec.flush();
					
					out.writeObject(new SharedValues.EncryptableData(trgtSec.toByteArray()).encrypt(password)); //Encrypted data
					out.flush();
					
					//Sending the message
					return queuePacket(new PacketStruct(nhtAttachmentReq, trgt.toByteArray()));
				} catch(IOException | GeneralSecurityException exception) {
					//Printing the stack trace
					exception.printStackTrace();
					Crashlytics.logException(exception);
					
					//Returning false
					return false;
				}
			}
			
			@Override
			boolean sendConversationInfoRequest(List<ConversationInfoRequest> list) {
				//Returning false if there is no connection thread
				if(connectionThread == null) return false;
				
				//Creating the guid list
				ArrayList<String> guidList;
				
				//Locking the pending conversations
				synchronized(list) {
					//Returning false if there are no pending conversations
					if(list.isEmpty()) return false;
					
					//Converting the conversation info list to a string list
					guidList = new ArrayList<>();
					for(ConversationInfoRequest conversationInfoRequest : list)
						guidList.add(conversationInfoRequest.conversationInfo.getGuid());
				}
				
				//Requesting information on new conversations
				try(ByteArrayOutputStream trgt = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(trgt);
					ByteArrayOutputStream trgtSec = new ByteArrayOutputStream(); ObjectOutputStream outSec = new ObjectOutputStream(trgtSec)) {
					outSec.writeInt(guidList.size());
					for(String item : guidList) outSec.writeUTF(item);
					outSec.flush();
					
					out.writeObject(new SharedValues.EncryptableData(trgtSec.toByteArray()).encrypt(password)); //Encrypted data
					
					//Sending the message
					connectionThread.queuePacket(new PacketStruct(nhtConversationUpdate, trgt.toByteArray()));
				} catch(IOException | GeneralSecurityException exception) {
					//Logging the exception
					exception.printStackTrace();
					Crashlytics.logException(exception);
					
					//Returning false
					return false;
				}
				
				//Returning true
				return true;
			}
			
			@Override
			boolean uploadFilePacket(short requestID, int requestIndex, String conversationGUID, byte[] data, String fileName, boolean isLast) {
				//Returning false if there is no connection thread
				if(connectionThread == null) return false;
				
				//Adding the data
				byte[] packetData;
				try(ByteArrayOutputStream trgt = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(trgt);
					ByteArrayOutputStream trgtSec = new ByteArrayOutputStream(); ObjectOutputStream outSec = new ObjectOutputStream(trgtSec)) {
					out.writeShort(requestID); //Request identifier
					out.writeInt(requestIndex); //Request index
					out.writeBoolean(isLast); //Is last message
					
					outSec.writeUTF(conversationGUID); //Chat GUID
					outSec.writeInt(data.length); //File bytes
					outSec.write(data);
					if(requestIndex == 0) outSec.writeUTF(fileName); //File name
					outSec.flush();
					
					out.writeObject(new SharedValues.EncryptableData(trgtSec.toByteArray()).encrypt(password)); //Encrypted data
					out.flush();
					
					packetData = trgt.toByteArray();
				} catch(IOException | GeneralSecurityException exception) {
					//Logging the exception
					exception.printStackTrace();
					Crashlytics.logException(exception);
					
					//Returning false
					return false;
				}
				
				//Sending the message
				connectionThread.sendDataSync(nhtSendFileExisting, packetData, true);
				
				//Returning true
				return true;
			}
			
			@Override
			boolean uploadFilePacket(short requestID, int requestIndex, String[] conversationMembers, byte[] data, String fileName, String service, boolean isLast) {
				//Returning false if there is no connection thread
				if(connectionThread == null) return false;
				
				//Adding the data
				byte[] packetData;
				try(ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
					out.writeShort(requestID); //Request identifier
					out.writeInt(requestIndex); //Request index
					out.writeBoolean(isLast); //Is last message
					
					out.writeInt(conversationMembers.length); //Chat members
					for(String item : conversationMembers) out.writeUTF(item);
					out.writeInt(data.length); //File bytes
					out.write(data);
					if(requestIndex == 0) {
						out.writeUTF(fileName); //File name
						out.writeUTF(service); //Service
					}
					out.flush();
					
					packetData = bos.toByteArray();
				} catch(IOException exception) {
					//Logging the exception
					exception.printStackTrace();
					Crashlytics.logException(exception);
					
					//Returning false
					return false;
				}
				
				//Sending the message
				connectionThread.sendDataSync(nhtSendFileNew, packetData, true);
				
				//Returning true
				return true;
			}
			
			@Override
			boolean requestRetrievalTime(long timeLower, long timeUpper) {
				//Returning false if there is no connection thread
				if(connectionThread == null) return false;
				
				//Sending the message
				connectionThread.queuePacket(new PacketStruct(nhtTimeRetrieval, ByteBuffer.allocate(Long.SIZE / 8 * 2).putLong(timeLower).putLong(timeUpper).array()));
				
				//Returning true
				return true;
			}
			
			@Override
			boolean requestRetrievalAll() {
				//Returning false if there is no connection thread
				if(connectionThread == null) return false;
				
				//Queuing the packet
				queuePacket(new PacketStruct(nhtMassRetrieval, new byte[0]));
				
				//Returning true
				return true;
			}
			
			@Override
			Packager getPackager() {
				return protocolPackager;
			}
			
			@Override
			String getHashAlgorithm() {
				return hashAlgorithm;
			}
			
			@Override
			String getCharset() {
				return stringCharset;
			}
			
			@Override
			boolean checkSubVerApplicability(int version) {
				return version == 1;
			}
		}
		
		private class ClientProtocol2 extends ProtocolManager {
			private final Packager protocolPackager = new PackagerComm3();
			private static final String hashAlgorithm = "MD5";
			private static final String stringCharset = "UTF-8";
			
			@Override
			boolean sendPing() {
				return queuePacket(new PacketStruct(nhtPing, new byte[0]));
			}
			
			@Override
			void processData(int messageType, byte[] data) {
				//Notifying the super connection manager of a message
				onMessage();
				
				switch(messageType) {
					case nhtClose:
						connectionThread.closeConnection(intentResultCodeConnection, false);
						break;
					case nhtPing:
						queuePacket(new PacketStruct(nhtPong, new byte[0]));
						break;
					case nhtAuthentication: {
						//Stopping the authentication timer
						handler.removeCallbacks(handshakeExpiryRunnable);
						
						//Reading the result code
						ByteBuffer dataBuffer = ByteBuffer.wrap(data);
						int resultCode = dataBuffer.getInt();
						
						//Translating the result to the local value
						switch(resultCode) {
							case nhtAuthenticationOK:
								resultCode = intentResultCodeSuccess;
								break;
							case nhtAuthenticationUnauthorized:
								resultCode = intentResultCodeUnauthorized;
								break;
							case nhtAuthenticationBadRequest:
								resultCode = intentResultCodeBadRequest;
								break;
									/* case nhtAuthenticationVersionMismatch:
										if(SharedValues.mmCommunicationsVersion > communicationsVersion) result = intentResultCodeServerOutdated;
										else result = intentResultCodeClientOutdated;
										break; */
						}
						
						//Finishing the connection establishment if the handshake was successful
						if(resultCode == intentResultCodeSuccess) updateStateConnected();
							//Otherwise terminating the connection
						else connectionThread.closeConnection(resultCode, resultCode == intentResultCodeBadRequest); //Only forward the request if the request couldn't be processed (an unauthorized response means that the server could understand the request)
						
						break;
					}
					case nhtMessageUpdate:
					case nhtTimeRetrieval: {
						//Reading the list
						List<Blocks.ConversationItem> list;
						try(ByteArrayInputStream src = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(src)) {
							//Reading the secure data
							try(ByteArrayInputStream srcSec = new ByteArrayInputStream(readEncrypted(in, password)); ObjectInputStream inSec = new ObjectInputStream(srcSec)) {
								list = deserializeConversationItems(inSec, inSec.readInt());
							}
						} catch(IOException | RuntimeException | GeneralSecurityException exception) {
							exception.printStackTrace();
							break;
						}
						
						//Processing the messages
						processMessageUpdate(list, true);
						
						break;
					}
					case nhtMassRetrieval: {
						try(ByteArrayInputStream src = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(src)) {
							//Reading the packet index
							int packetIndex = in.readInt();
							
							//Reading the secure data
							try(ByteArrayInputStream srcSec = new ByteArrayInputStream(readEncrypted(in, password)); ObjectInputStream inSec = new ObjectInputStream(srcSec)) {
								//Checking if this is the first packet
								if(packetIndex == 0) {
									//Reading the conversation list
									List<Blocks.ConversationInfo> conversationList = deserializeConversations(inSec, inSec.readInt());
									
									//Reading the message count
									int messageCount = inSec.readInt();
									
									//Registering the mass retrieval manager
									if(massRetrievalThread != null) massRetrievalThread.registerInfo(ConnectionService.this, conversationList, messageCount);
								} else {
									//Reading the item list
									List<Blocks.ConversationItem> listItems = deserializeConversationItems(inSec, inSec.readInt());
									
									//Processing the packet
									if(massRetrievalThread != null) massRetrievalThread.addPacket(ConnectionService.this, packetIndex, listItems);
								}
							}
						} catch(IOException | RuntimeException | GeneralSecurityException exception) {
							//Logging the exception
							exception.printStackTrace();
							
							//Cancelling the mass retrieval process
							massRetrievalThread.cancel(ConnectionService.this);
						}
						
						break;
					}
					case nhtMassRetrievalFinish: {
						//Finishing the mass retrieval
						if(massRetrievalThread != null) massRetrievalThread.finish();
						
						break;
					}
					case nhtConversationUpdate: {
						//Reading the list
						List<Blocks.ConversationInfo> list;
						try(ByteArrayInputStream src = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(src)) {
							//Reading the secure data
							try(ByteArrayInputStream srcSec = new ByteArrayInputStream(readEncrypted(in, password)); ObjectInputStream inSec = new ObjectInputStream(srcSec)) {
								list = deserializeConversations(inSec, inSec.readInt());
							}
						} catch(IOException | RuntimeException | GeneralSecurityException exception) {
							exception.printStackTrace();
							break;
						}
						
						//Processing the conversations
						processChatInfoResponse(list);
						
						break;
					}
					case nhtModifierUpdate: {
						//Reading the list
						List<Blocks.ModifierInfo> list;
						try(ByteArrayInputStream bis = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(bis)) {
							//Reading the secure data
							try(ByteArrayInputStream srcSec = new ByteArrayInputStream(readEncrypted(in, password)); ObjectInputStream inSec = new ObjectInputStream(srcSec)) {
								list = deserializeModifiers(inSec, inSec.readInt());
							}
						} catch(IOException | RuntimeException | GeneralSecurityException exception) {
							exception.printStackTrace();
							break;
						}
						
						//Processing the conversations
						processModifierUpdate(list, getPackager());
						
						break;
					}
					case nhtAttachmentReq: {
						//Reading the data
						final short requestID;
						final int requestIndex;
						final long fileSize;
						final boolean isLast;
						
						final String fileGUID;
						final byte[] compressedBytes;
						
						try(ByteArrayInputStream bis = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(bis)) {
							requestID = in.readShort();
							requestIndex = in.readInt();
							if(requestIndex == 0) fileSize = in.readLong();
							else fileSize = -1;
							isLast = in.readBoolean();
							
							//Reading the secure data
							try(ByteArrayInputStream srcSec = new ByteArrayInputStream(readEncrypted(in, password)); ObjectInputStream inSec = new ObjectInputStream(srcSec)) {
								fileGUID = inSec.readUTF();
								compressedBytes = new byte[inSec.readInt()];
								inSec.readFully(compressedBytes);
							}
						} catch(IOException | RuntimeException | GeneralSecurityException exception) {
							exception.printStackTrace();
							break;
						}
						
						//Running on the UI thread
						mainHandler.post(() -> {
							//Searching for a matching request
							for(FileDownloadRequest request : fileDownloadRequests) {
								if(request.requestID != requestID || !request.attachmentGUID.equals(fileGUID)) continue;
								if(requestIndex == 0) request.setFileSize(fileSize);
								request.processFileFragment(ConnectionService.this, compressedBytes, requestIndex, isLast, getPackager());
								if(isLast) fileDownloadRequests.remove(request);
								break;
							}
						});
						
						break;
					}
					case nhtAttachmentReqConfirm: {
						//Reading the data
						final short requestID = ByteBuffer.wrap(data).getShort();
						
						//Running on the UI thread
						mainHandler.post(() -> {
							//Searching for a matching request
							for(FileDownloadRequest request : fileDownloadRequests) {
								if(request.requestID != requestID/* || !request.attachmentGUID.equals(fileGUID)*/) continue;
								request.stopTimer(true);
								request.onResponseReceived();
								break;
							}
						});
						
						break;
					}
					case nhtAttachmentReqFail: {
						//Reading the data
						final short requestID = ByteBuffer.wrap(data).getShort();
						
						//Running on the UI thread
						mainHandler.post(() -> {
							//Searching for a matching request
							for(FileDownloadRequest request : fileDownloadRequests) {
								if(request.requestID != requestID/* || !request.attachmentGUID.equals(fileGUID)*/) continue;
								request.failDownload();
								break;
							}
						});
						
						break;
					}
					case nhtSendResult: {
						//Reading the data
						final short requestID;
						final boolean result;
						
						ByteBuffer byteBuffer = ByteBuffer.wrap(data);
						requestID = byteBuffer.getShort();
						result = byteBuffer.get() == 1;
						
						//Getting the message response manager
						final MessageResponseManager messageResponseManager = messageSendRequests.get(requestID);
						if(messageResponseManager != null) {
							//Removing the request
							messageSendRequests.remove(requestID);
							messageResponseManager.stopTimer(false);
							
							//Running on the UI thread
							new Handler(Looper.getMainLooper()).post(() -> {
								//Telling the listener
								if(result) messageResponseManager.onSuccess();
								else messageResponseManager.onFail(messageSendExternalException);
							});
						}
						
						break;
					}
				}
			}
			
			private List<Blocks.ConversationInfo> deserializeConversations(ObjectInputStream in, int count) throws IOException {
				//Creating the list
				List<Blocks.ConversationInfo> list = new ArrayList<>(count);
				
				//Iterating over the items
				for(int i = 0; i < count; i++) {
					String guid = in.readUTF();
					boolean available = in.readBoolean();
					if(available) {
						String service = in.readUTF();
						String name = in.readBoolean() ? in.readUTF() : null;
						String[] members = new String[in.readInt()];
						for(int m = 0; m < members.length; m++) members[m] = in.readUTF();
						list.add(new Blocks.ConversationInfo(guid, service, name, members));
					} else {
						list.add(new Blocks.ConversationInfo(guid));
					}
				}
				
				//Returning the list
				return list;
			}
			
			private static final int conversationItemTypeMessage = 0;
			private static final int conversationItemTypeGroupAction = 1;
			private static final int conversationItemTypeChatRename = 2;
			private List<Blocks.ConversationItem> deserializeConversationItems(ObjectInputStream in, int count) throws IOException {
				//Creating the list
				List<Blocks.ConversationItem> list = new ArrayList<>(count);
				
				//Iterating over the items
				for(int i = 0; i < count; i++) {
					int type = in.readInt();
					
					String guid = in.readUTF();
					String chatGuid = in.readUTF();
					long date = in.readLong();
					
					switch(type) {
						default:
							throw new IOException("Invalid conversation type: " + type);
						case conversationItemTypeMessage: {
							String text = in.readBoolean() ? in.readUTF() : null;
							String sender = in.readBoolean() ? in.readUTF() : null;
							List<Blocks.AttachmentInfo> attachments = deserializeAttachments(in, in.readInt());
							List<Blocks.StickerModifierInfo> stickers = (List<Blocks.StickerModifierInfo>) (List<?>) deserializeModifiers(in, in.readInt());
							List<Blocks.TapbackModifierInfo> tapbacks = (List<Blocks.TapbackModifierInfo>) (List<?>) deserializeModifiers(in, in.readInt());
							String sendEffect = in.readBoolean() ? in.readUTF() : null;
							int stateCode = in.readInt();
							int errorCode = in.readInt();
							long dateRead = in.readLong();
							
							list.add(new Blocks.MessageInfo(guid, chatGuid, date, text, sender, attachments, stickers, tapbacks, sendEffect, stateCode, errorCode, dateRead));
							break;
						}
						case conversationItemTypeGroupAction: {
							String agent = in.readBoolean() ? in.readUTF() : null;
							String other = in.readBoolean() ? in.readUTF() : null;
							int groupActionType = in.readInt();
							
							list.add(new Blocks.GroupActionInfo(guid, chatGuid, date, agent, other, groupActionType));
							break;
						}
						case conversationItemTypeChatRename: {
							String agent = in.readBoolean() ? in.readUTF() : null;
							String newChatName = in.readBoolean() ? in.readUTF() : null;
							
							list.add(new Blocks.ChatRenameActionInfo(guid, chatGuid, date, agent, newChatName));
							break;
						}
					}
				}
				
				//Returning the list
				return list;
			}
			
			private List<Blocks.AttachmentInfo> deserializeAttachments(ObjectInputStream in, int count) throws IOException {
				//Creating the list
				List<Blocks.AttachmentInfo> list = new ArrayList<>(count);
				
				//Iterating over the items
				for(int i = 0; i < count; i++) {
					String guid = in.readUTF();
					String name = in.readUTF();
					String type = in.readBoolean() ? in.readUTF() : null;
					byte[] checksum;
					if(in.readBoolean()) {
						checksum = new byte[in.readInt()];
						in.readFully(checksum);
					} else {
						checksum = null;
					}
					
					list.add(new Blocks.AttachmentInfo(guid, name, type, checksum));
				}
				
				//Returning the list
				return list;
			}
			
			private static final int modifierTypeActivity = 0;
			private static final int modifierTypeSticker = 1;
			private static final int modifierTypeTapback = 2;
			private List<Blocks.ModifierInfo> deserializeModifiers(ObjectInputStream in, int count) throws IOException {
				//Creating the list
				List<Blocks.ModifierInfo> list = new ArrayList<>(count);
				
				//Iterating over the items
				for(int i = 0; i < count; i++) {
					int type = in.readInt();
					
					String message = in.readUTF();
					
					switch(type) {
						default:
							throw new IOException("Invalid modifier type: " + type);
						case modifierTypeActivity: {
							int state = in.readInt();
							long dateRead = in.readLong();
							
							list.add(new Blocks.ActivityStatusModifierInfo(message, state, dateRead));
							break;
						}
						case modifierTypeSticker: {
							int messageIndex = in.readInt();
							String fileGuid = in.readUTF();
							String sender = in.readBoolean() ? in.readUTF() : null;
							long date = in.readLong();
							byte[] image = new byte[in.readInt()];
							in.readFully(image);
							
							list.add(new Blocks.StickerModifierInfo(message, messageIndex, fileGuid, sender, date, image));
							break;
						}
						case modifierTypeTapback: {
							int messageIndex = in.readInt();
							String sender = in.readBoolean() ? in.readUTF() : null;
							int code = in.readInt();
							
							list.add(new Blocks.TapbackModifierInfo(message, messageIndex, sender, code));
							break;
						}
					}
				}
				
				//Returning the list
				return list;
			}
			
			@Override
			boolean sendAuthenticationRequest() {
				//Returning false if there is no connection thread
				if(connectionThread == null) return false;
				
				byte[] packetData;
				try(ByteArrayOutputStream trgt = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(trgt)) {
					writeEncrypted(transmissionCheck.getBytes(stringCharset), out, password);
					out.flush();
					
					packetData = trgt.toByteArray();
				} catch(IOException | GeneralSecurityException exception) {
					//Logging the error
					exception.printStackTrace();
					Crashlytics.logException(exception);
					
					//Closing the connection
					connectionThread.closeConnection(intentResultCodeInternalException, false);
					
					//Returning false
					return false;
				}
				
				//Sending the message
				connectionThread.queuePacket(new PacketStruct(nhtAuthentication, packetData));
				
				//Returning true
				return true;
			}
			
			@Override
			boolean sendMessage(short requestID, String chatGUID, String message) {
				//Returning false if there is no connection thread
				if(connectionThread == null) return false;
				
				byte[] packetData;
				try(ByteArrayOutputStream trgt = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(trgt);
					ByteArrayOutputStream trgtSec = new ByteArrayOutputStream(); ObjectOutputStream outSec = new ObjectOutputStream(trgtSec)) {
					//Adding the data
					out.writeShort(requestID); //Request ID
					
					outSec.writeUTF(chatGUID); //Chat GUID
					outSec.writeUTF(message); //Message
					outSec.flush();
					
					writeEncrypted(trgtSec.toByteArray(), out, password); //Encrypted data
					
					out.flush();
					
					packetData = trgt.toByteArray();
				} catch(IOException | GeneralSecurityException exception) {
					//Printing the stack trace
					exception.printStackTrace();
					
					//Returning false
					return false;
				}
				
				//Sending the message
				connectionThread.queuePacket(new PacketStruct(nhtSendTextExisting, packetData));
				
				//Returning true
				return true;
			}
			
			@Override
			boolean sendMessage(short requestID, String[] chatMembers, String message, String service) {
				//Returning false if there is no connection thread
				if(connectionThread == null) return false;
				
				byte[] packetData;
				try(ByteArrayOutputStream trgt = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(trgt);
					ByteArrayOutputStream trgtSec = new ByteArrayOutputStream(); ObjectOutputStream outSec = new ObjectOutputStream(trgtSec)) {
					//Adding the data
					out.writeShort(requestID); //Request ID
					
					outSec.writeInt(chatMembers.length); //Members
					for(String item : chatMembers) outSec.writeUTF(item);
					outSec.writeUTF(message); //Message
					outSec.writeUTF(service); //Service
					outSec.flush();
					
					writeEncrypted(trgtSec.toByteArray(), out, password); //Encrypted data
					
					out.flush();
					
					packetData = trgt.toByteArray();
				} catch(IOException | GeneralSecurityException exception) {
					//Printing the stack trace
					exception.printStackTrace();
					
					//Returning false
					return false;
				}
				
				//Sending the message
				connectionThread.queuePacket(new PacketStruct(nhtSendTextNew, packetData));
				
				//Returning true
				return true;
			}
			
			@Override
			boolean addDownloadRequest(short requestID, String attachmentGUID) {
				//Preparing to serialize the request
				try(ByteArrayOutputStream trgt = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(trgt);
					ByteArrayOutputStream trgtSec = new ByteArrayOutputStream(); ObjectOutputStream outSec = new ObjectOutputStream(trgtSec)) {
					//Adding the data
					out.writeShort(requestID); //Request ID
					out.writeInt(attachmentChunkSize); //Chunk size
					
					outSec.writeUTF(attachmentGUID); //File GUID
					outSec.flush();
					
					writeEncrypted(trgtSec.toByteArray(), out, password); //Encrypted data
					
					out.flush();
					
					//Sending the message
					return queuePacket(new PacketStruct(nhtAttachmentReq, trgt.toByteArray()));
				} catch(IOException | GeneralSecurityException exception) {
					//Printing the stack trace
					exception.printStackTrace();
					Crashlytics.logException(exception);
					
					//Returning false
					return false;
				}
			}
			
			@Override
			boolean sendConversationInfoRequest(List<ConversationInfoRequest> list) {
				//Returning false if there is no connection thread
				if(connectionThread == null) return false;
				
				//Creating the guid list
				ArrayList<String> guidList;
				
				//Locking the pending conversations
				synchronized(list) {
					//Returning false if there are no pending conversations
					if(list.isEmpty()) return false;
					
					//Converting the conversation info list to a string list
					guidList = new ArrayList<>();
					for(ConversationInfoRequest conversationInfoRequest : list)
						guidList.add(conversationInfoRequest.conversationInfo.getGuid());
				}
				
				//Requesting information on new conversations
				try(ByteArrayOutputStream trgt = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(trgt);
					ByteArrayOutputStream trgtSec = new ByteArrayOutputStream(); ObjectOutputStream outSec = new ObjectOutputStream(trgtSec)) {
					outSec.writeInt(guidList.size());
					for(String item : guidList) outSec.writeUTF(item);
					outSec.flush();
					
					writeEncrypted(trgtSec.toByteArray(), out, password); //Encrypted data
					
					out.flush();
					
					//Sending the message
					connectionThread.queuePacket(new PacketStruct(nhtConversationUpdate, trgt.toByteArray()));
				} catch(IOException | GeneralSecurityException exception) {
					//Logging the exception
					exception.printStackTrace();
					Crashlytics.logException(exception);
					
					//Returning false
					return false;
				}
				
				//Returning true
				return true;
			}
			
			@Override
			boolean uploadFilePacket(short requestID, int requestIndex, String conversationGUID, byte[] data, String fileName, boolean isLast) {
				//Returning false if there is no connection thread
				if(connectionThread == null) return false;
				
				//Adding the data
				byte[] packetData;
				try(ByteArrayOutputStream trgt = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(trgt);
					ByteArrayOutputStream trgtSec = new ByteArrayOutputStream(); ObjectOutputStream outSec = new ObjectOutputStream(trgtSec)) {
					out.writeShort(requestID); //Request identifier
					out.writeInt(requestIndex); //Request index
					out.writeBoolean(isLast); //Is last message
					
					outSec.writeUTF(conversationGUID); //Chat GUID
					outSec.writeInt(data.length); //File bytes
					outSec.write(data);
					if(requestIndex == 0) outSec.writeUTF(fileName); //File name
					outSec.flush();
					
					writeEncrypted(trgtSec.toByteArray(), out, password); //Encrypted data
					
					out.flush();
					
					packetData = trgt.toByteArray();
				} catch(IOException | GeneralSecurityException exception) {
					//Logging the exception
					exception.printStackTrace();
					Crashlytics.logException(exception);
					
					//Returning false
					return false;
				}
				
				//Sending the message
				connectionThread.sendDataSync(nhtSendFileExisting, packetData, true);
				
				//Returning true
				return true;
			}
			
			@Override
			boolean uploadFilePacket(short requestID, int requestIndex, String[] conversationMembers, byte[] data, String fileName, String service, boolean isLast) {
				//Returning false if there is no connection thread
				if(connectionThread == null) return false;
				
				//Adding the data
				byte[] packetData;
				try(ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
					out.writeShort(requestID); //Request identifier
					out.writeInt(requestIndex); //Request index
					out.writeBoolean(isLast); //Is last message
					
					out.writeInt(conversationMembers.length); //Chat members
					for(String item : conversationMembers) out.writeUTF(item);
					out.writeInt(data.length); //File bytes
					out.write(data);
					if(requestIndex == 0) {
						out.writeUTF(fileName); //File name
						out.writeUTF(service); //Service
					}
					out.flush();
					
					packetData = bos.toByteArray();
				} catch(IOException exception) {
					//Logging the exception
					exception.printStackTrace();
					Crashlytics.logException(exception);
					
					//Returning false
					return false;
				}
				
				//Sending the message
				connectionThread.sendDataSync(nhtSendFileNew, packetData, true);
				
				//Returning true
				return true;
			}
			
			@Override
			boolean requestRetrievalTime(long timeLower, long timeUpper) {
				//Returning false if there is no connection thread
				if(connectionThread == null) return false;
				
				//Sending the message
				connectionThread.queuePacket(new PacketStruct(nhtTimeRetrieval, ByteBuffer.allocate(Long.SIZE / 8 * 2).putLong(timeLower).putLong(timeUpper).array()));
				
				//Returning true
				return true;
			}
			
			@Override
			boolean requestRetrievalAll() {
				//Returning false if there is no connection thread
				if(connectionThread == null) return false;
				
				//Queuing the packet
				queuePacket(new PacketStruct(nhtMassRetrieval, new byte[0]));
				
				//Returning true
				return true;
			}
			
			private static final int encryptionSaltLen = 8;
			private static final int encryptionIvLen = 12; //12 bytes (instead of 16 because of GCM)
			private static final String encryptionKeyFactoryAlgorithm = "PBKDF2WithHmacSHA256";
			private static final String encryptionKeyAlgorithm = "AES";
			private static final String encryptionCipherTransformation = "AES/GCM/NoPadding";
			private static final int encryptionKeyIterationCount = 10000;
			private static final int encryptionKeyLength = 128; //128 bits
			
			private byte[] readEncrypted(ObjectInputStream stream, String password) throws IOException, GeneralSecurityException {
				//Reading the data
				byte[] salt = new byte[encryptionSaltLen];
				stream.readFully(salt);
				
				byte[] iv = new byte[encryptionIvLen];
				stream.readFully(iv);
				
				byte[] data = new byte[stream.readInt()];
				stream.readFully(data);
				
				//Creating the key
				SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(encryptionKeyFactoryAlgorithm);
				KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, encryptionKeyIterationCount, encryptionKeyLength);
				SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
				SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getEncoded(), encryptionKeyAlgorithm);
				
				//Creating the IV
				GCMParameterSpec gcmSpec = new GCMParameterSpec(encryptionKeyLength, iv);
				
				//Creating the cipher
				Cipher cipher = Cipher.getInstance(encryptionCipherTransformation);
				cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmSpec);
				
				//Deciphering the data
				byte[] block = cipher.doFinal(data);
				return block;
			}
			
			private void writeEncrypted(byte[] block, ObjectOutputStream stream, String password) throws IOException, GeneralSecurityException {
				//Creating a secure random
				SecureRandom random = new SecureRandom();
				
				//Generating a salt
				byte[] salt = new byte[encryptionSaltLen];
				random.nextBytes(salt);
				
				//Creating the key
				SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(encryptionKeyFactoryAlgorithm);
				KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, encryptionKeyIterationCount, encryptionKeyLength);
				SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
				SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getEncoded(), encryptionKeyAlgorithm);
				
				//Generating the IV
				byte[] iv = new byte[encryptionIvLen];
				random.nextBytes(iv);
				GCMParameterSpec gcmSpec = new GCMParameterSpec(encryptionKeyLength, iv);
				
				Cipher cipher = Cipher.getInstance(encryptionCipherTransformation);
				cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmSpec);
				
				//Encrypting the data
				byte[] data = cipher.doFinal(block);
				
				//Writing the data
				stream.write(salt);
				stream.write(iv);
				stream.writeInt(data.length);
				stream.write(data);
			}
			
			@Override
			Packager getPackager() {
				return protocolPackager;
			}
			
			@Override
			String getHashAlgorithm() {
				return hashAlgorithm;
			}
			
			@Override
			String getCharset() {
				return stringCharset;
			}
			
			@Override
			boolean checkSubVerApplicability(int version) {
				return version == 1;
			}
		}
	}
	
	private class ClientComm3 extends ConnectionManager {
		//Creating the reference values
		private final Packager protocolPackager = new PackagerComm3();
		private static final String hashAlgorithm = "MD5";
		
		//Creating the transmission header values
		private static final int nhtClose = -1;
		private static final int nhtPing = -2;
		private static final int nhtPong = -3;
		private static final int nhtAuthentication = 0;
		private static final int nhtMessageUpdate = 1;
		private static final int nhtTimeRetrieval = 2;
		private static final int nhtMassRetrieval = 3;
		private static final int nhtConversationUpdate = 4;
		private static final int nhtModifierUpdate = 5;
		private static final int nhtAttachmentReq = 6;
		private static final int nhtAttachmentReqConfirm = 7;
		private static final int nhtAttachmentReqFail = 8;
		
		private static final int nhtSendResult = 100;
		private static final int nhtSendTextExisting = 101;
		private static final int nhtSendTextNew = 102;
		private static final int nhtSendFileExisting = 103;
		private static final int nhtSendFileNew = 104;
		
		private static final int nhtAuthenticationOK = 0;
		private static final int nhtAuthenticationUnauthorized = 1;
		private static final int nhtAuthenticationBadRequest = 2;
		private static final int nhtAuthenticationVersionMismatch = 3;
		
		//Creating the connection values
		private boolean connectionEstablished = false;
		private int currentState = stateDisconnected;
		private ConnectionThread connectionThread = null;
		
		@Override
		boolean connect(byte launchID) {
			//Calling the super method
			super.connect(launchID);
			
			//Parsing the hostname
			String cleanHostname = hostname;
			int port = Constants.defaultPort;
			if(regExValidPort.matcher(cleanHostname).find()) {
				String[] targetDetails = hostname.split(":");
				cleanHostname = targetDetails[0];
				port = Integer.parseInt(targetDetails[1]);
			}
			
			//Setting the state as connecting
			currentState = stateConnecting;
			
			//Starting the connection
			connectionThread = new ConnectionThread(launchID, cleanHostname, password, port);
			connectionThread.start();
			
			//Returning true
			return true;
		}
		
		@Override
		void disconnect() {
			super.disconnect();
			connectionThread.initiateClose(intentResultCodeConnection, false);
		}
		
		@Override
		int getState() {
			return currentState;
		}
		
		private boolean queuePacket(PacketStruct packet) {
			return connectionThread != null && connectionThread.queuePacket(packet);
		}
		
		@Override
		boolean sendPing() {
			if(connectionThread == null) return false;
			connectionThread.sendPing();
			return true;
			//queuePacket(new PacketStruct(nhtPing, new byte[0]));
		}
		
		@Override
		Packager getPackager() {
			return protocolPackager;
		}
		
		@Override
		String getHashAlgorithm() {
			return hashAlgorithm;
		}
		
		@Override
		boolean sendMessage(short requestID, String chatGUID, String message) {
			//Returning false if there is no connection thread
			if(connectionThread == null) return false;
			
			byte[] packetData;
			try(ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
				//Adding the data
				out.writeShort(requestID); //Request ID
				out.writeUTF(chatGUID); //Chat GUID
				out.writeUTF(message); //Message
				out.flush();
				
				packetData = bos.toByteArray();
			} catch(IOException exception) {
				//Printing the stack trace
				exception.printStackTrace();
				
				//Returning false
				return false;
			}
			
			//Sending the message
			connectionThread.queuePacket(new PacketStruct(nhtSendTextExisting, packetData));
			
			//Returning true
			return true;
		}
		
		@Override
		boolean sendMessage(short requestID, String[] chatMembers, String message, String service) {
			//Returning false if there is no connection thread
			if(connectionThread == null) return false;
			
			byte[] packetData;
			try(ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
				//Adding the data
				out.writeShort(requestID); //Request ID
				out.writeInt(chatMembers.length); //Members
				for(String item : chatMembers) out.writeUTF(item);
				out.writeUTF(message); //Message
				out.writeUTF(service); //Service
				out.flush();
				
				packetData = bos.toByteArray();
			} catch(IOException exception) {
				//Printing the stack trace
				exception.printStackTrace();
				
				//Returning false
				return false;
			}
			
			//Sending the message
			connectionThread.queuePacket(new PacketStruct(nhtSendTextNew, packetData));
			
			//Returning true
			return true;
		}
		
		@Override
		boolean addDownloadRequest(short requestID, String attachmentGUID) {
			//Preparing to serialize the request
			try(ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
				//Adding the data
				out.writeShort(requestID); //Request ID
				out.writeUTF(attachmentGUID); //File GUID
				out.writeInt(attachmentChunkSize); //Chunk size
				out.flush();
				
				//Sending the message
				boolean requestQueued = queuePacket(new PacketStruct(nhtAttachmentReq, bos.toByteArray()));
				if(!requestQueued) return false;
			} catch(IOException exception) {
				//Printing the stack trace
				exception.printStackTrace();
				Crashlytics.logException(exception);
				
				//Returning false
				return false;
			}
			
			//Returning true
			return true;
		}
		
		@Override
		boolean sendConversationInfoRequest(List<ConversationInfoRequest> list) {
			//Returning false if there is no connection thread
			if(connectionThread == null) return false;
			
			//Creating the guid list
			ArrayList<String> guidList;
			
			//Locking the pending conversations
			synchronized(list) {
				//Returning false if there are no pending conversations
				if(list.isEmpty()) return false;
				
				//Converting the conversation info list to a string list
				guidList = new ArrayList<>();
				for(ConversationInfoRequest conversationInfoRequest : list)
					guidList.add(conversationInfoRequest.conversationInfo.getGuid());
			}
			
			//Requesting information on new conversations
			try(ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
				out.writeInt(guidList.size());
				for(String item : guidList) out.writeUTF(item);
				out.flush();
				
				//Sending the message
				connectionThread.queuePacket(new PacketStruct(nhtConversationUpdate, bos.toByteArray()));
			} catch(IOException exception) {
				//Logging the exception
				exception.printStackTrace();
				Crashlytics.logException(exception);
				
				//Returning false
				return false;
			}
			
			//Returning true
			return true;
		}
		
		@Override
		boolean uploadFilePacket(short requestID, int requestIndex, String conversationGUID, byte[] data, String fileName, boolean isLast) {
			//Returning false if there is no connection thread
			if(connectionThread == null) return false;
			
			//Adding the data
			byte[] packetData;
			try(ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
				out.writeShort(requestID); //Request identifier
				out.writeInt(requestIndex); //Request index
				out.writeUTF(conversationGUID); //Chat GUID
				out.writeInt(data.length); //File bytes
				out.write(data);
				if(requestIndex == 0) out.writeUTF(fileName);
				out.writeBoolean(isLast); //Is last message
				out.flush();
				
				packetData = bos.toByteArray();
			} catch(IOException exception) {
				//Logging the exception
				exception.printStackTrace();
				Crashlytics.logException(exception);
				
				//Returning false
				return false;
			}
			
			//Sending the message
			connectionThread.sendDataSync(nhtSendFileExisting, packetData, true);
			
			//Returning true
			return true;
		}
		
		@Override
		boolean uploadFilePacket(short requestID, int requestIndex, String[] conversationMembers, byte[] data, String fileName, String service, boolean isLast) {
			//Returning false if there is no connection thread
			if(connectionThread == null) return false;
			
			//Adding the data
			byte[] packetData;
			try(ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
				out.writeShort(requestID); //Request identifier
				out.writeInt(requestIndex); //Request index
				out.writeInt(conversationMembers.length); //Chat members
				for(String item : conversationMembers) out.writeUTF(item);
				out.writeInt(data.length); //File bytes
				out.write(data);
				if(requestIndex == 0) {
					out.writeUTF(fileName); //File name
					out.writeUTF(service); //Service
				}
				out.writeBoolean(isLast); //Is last message
				out.flush();
				
				packetData = bos.toByteArray();
			} catch(IOException exception) {
				//Logging the exception
				exception.printStackTrace();
				Crashlytics.logException(exception);
				
				//Returning false
				return false;
			}
			
			//Sending the message
			connectionThread.sendDataSync(nhtSendFileNew, packetData, true);
			
			//Returning true
			return true;
		}
		
		@Override
		boolean requestRetrievalTime(long timeLower, long timeUpper) {
			//Returning false if there is no connection thread
			if(connectionThread == null) return false;
			
			//Building the request
			byte[] packetData;
			try(ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
				out.writeLong(timeLower);
				out.writeLong(timeUpper);
				out.flush();
				
				//Sending the message
				packetData = bos.toByteArray();
			} catch(IOException exception) {
				//Logging the exception
				exception.printStackTrace();
				Crashlytics.logException(exception);
				
				//Returning false
				return false;
			}
			
			//Sending the message
			connectionThread.queuePacket(new PacketStruct(nhtTimeRetrieval, packetData));
			
			//Returning true
			return true;
		}
		
		@Override
		boolean requestRetrievalAll() {
			//Returning false if there is no connection thread
			if(connectionThread == null) return false;
			
			//Queuing the packet
			queuePacket(new PacketStruct(nhtMassRetrieval, new byte[0]));
			
			//Returning true
			return true;
		}
		
		@Override
		int checkCommVerApplicability(int version) {
			return Integer.compare(version, 3);
		}
		
		private class ConnectionThread extends Thread {
			private static final long authenticationTime = 1000 * 10; //10 seconds
			
			private final byte launchID;
			private final String hostname;
			private final String password;
			private final int port;
			
			private Socket socket;
			private InputStream inputStream;
			private OutputStream outputStream;
			private WriterThread writerThread = null;
			private Timer authenticationExpiryTimer = null;
			
			ConnectionThread(byte launchID, String hostname, String password, int port) {
				this.launchID = launchID;
				this.hostname = hostname;
				this.password = password;
				this.port = port;
			}
			
			@Override
			public void run() {
				try {
					//Returning if the thread is interrupted
					if(isInterrupted()) return;
					
					//Creating the SSL context
					SSLContext sslContext = SSLContext.getInstance("TLS");
					TrustManager[] trustAllCerts = new TrustManager[]{
							new X509TrustManager() {
								public java.security.cert.X509Certificate[] getAcceptedIssuers() {
									return new X509Certificate[0];
								}
								
								public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
								
								public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
							}
					};
					sslContext.init(null, trustAllCerts, new SecureRandom());
					
					//Connecting to the server
					socket = sslContext.getSocketFactory().createSocket();
					//socket.setKeepAlive(true);
					socket.connect(new InetSocketAddress(hostname, port), 10 * 1000);
					
					//Returning if the thread is interrupted
					if(isInterrupted()) {
						try {
							socket.close();
						} catch(IOException exception) {
							exception.printStackTrace();
						}
						return;
					}
					
					//Getting the streams
					inputStream = socket.getInputStream();
					outputStream = socket.getOutputStream();
					
					//Starting the writer thread
					writerThread = new WriterThread();
					writerThread.start();
				} catch(IOException | NoSuchAlgorithmException | KeyManagementException exception) {
					//Printing the stack trace
					exception.printStackTrace();
					
					//Updating the state
					updateStateDisconnected(intentResultCodeConnection, !(exception instanceof ConnectException));
					
					//Returning
					return;
				}
				
				//Sending the registration data
				try(ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
					out.writeInt(applicableCommunicationsVersions.length);
					for(int version : applicableCommunicationsVersions) out.writeInt(version);
					out.writeUTF(password);
					out.flush();
					
					queuePacket(new PacketStruct(nhtAuthentication, bos.toByteArray()));
				} catch(IOException exception) {
					//Logging the error
					exception.printStackTrace();
					Crashlytics.logException(exception);
					
					//Closing the connection
					closeConnection(intentResultCodeInternalException, true);
				}
				
				//Starting the authentication timer
				authenticationExpiryTimer = new Timer();
				authenticationExpiryTimer.schedule(new TimerTask() {
					@Override
					public void run() {
						//Stopping the expiry timer
						authenticationExpiryTimer.cancel();
						authenticationExpiryTimer = null;
						
						//Closing the connection
						closeConnection(intentResultCodeConnection, true);
					}
				}, authenticationTime);
				
				//Reading from the input stream
				while(!isInterrupted()) {
					try {
						//Reading the header data
						byte[] header = new byte[Integer.SIZE / 8 * 2];
						{
							int bytesRemaining = header.length;
							int offset = 0;
							int readCount;
							
							while(bytesRemaining > 0) {
								readCount = inputStream.read(header, offset, bytesRemaining);
								if(readCount == -1) { //No data read, stream is closed
									closeConnection(intentResultCodeConnection, false);
									return;
								}
								
								offset += readCount;
								bytesRemaining -= readCount;
							}
						}
						ByteBuffer headerBuffer = ByteBuffer.wrap(header);
						int messageType = headerBuffer.getInt();
						int contentLen = headerBuffer.getInt();
						
						if(contentLen > maxPacketAllocation) {
							//Logging the error
							Logger.getGlobal().log(Level.WARNING, "Rejecting large packet (type: " + messageType + " - size: " + contentLen + ")");
							
							//Closing the connection
							closeConnection(intentResultCodeConnection, !connectionEstablished);
							break;
						}
						
						//Reading the content
						byte[] content = new byte[contentLen];
						if(contentLen > 0) {
							int bytesRemaining = contentLen;
							int offset = 0;
							int readCount;
							while(bytesRemaining > 0) {
								readCount = inputStream.read(content, offset, bytesRemaining);
								if(readCount == -1) { //No data read, stream is closed
									closeConnection(intentResultCodeConnection, !connectionEstablished);
									return;
								}
								
								offset += readCount;
								bytesRemaining -= readCount;
							}
						}
						
						//Processing the data
						processData(messageType, content);
					} catch(SSLHandshakeException exception) {
						//Closing the connection
						exception.printStackTrace();
						closeConnection(intentResultCodeConnection, true);
						
						//Breaking
						break;
					} catch(IOException exception) {
						//Closing the connection
						exception.printStackTrace();
						closeConnection(intentResultCodeConnection, false);
						
						//Breaking
						break;
					}
				}
				
				//Closing the socket
				try {
					socket.close();
				} catch(IOException exception) {
					exception.printStackTrace();
				}
			}
			
			private void updateStateDisconnected(int reason, boolean forwardRequest) {
				//Setting the state
				currentState = stateDisconnected;
				
				//Setting the connection established flag
				connectionEstablished = false;
				
				//Stopping the timers
				if(authenticationExpiryTimer != null) authenticationExpiryTimer.cancel();
				
				//Cancelling the mass retrieval
				new Handler(Looper.getMainLooper()).post(ConnectionService.this::cancelMassRetrieval);
				
				//Attempting to connect via the legacy method
				if(!forwardRequest || !forwardRequest(launchID, true)) {
					new Handler(Looper.getMainLooper()).post(() -> {
						//Checking if this is the most recent launch
						if(currentLaunchID == launchID) {
							//Setting the last connection result
							lastConnectionResult = reason;
							
							//Notifying the connection listeners
							broadcastState(stateDisconnected, reason, launchID);
							
							//Updating the notification state
							if(!shutdownRequested) postDisconnectedNotification(false);
							
							//Checking if a connection existed for reconnection and the preference is enabled
							if(flagDropReconnect && PreferenceManager.getDefaultSharedPreferences(MainApplication.getInstance()).getBoolean(MainApplication.getInstance().getResources().getString(R.string.preference_server_dropreconnect_key), false)) {
								//Reconnecting
								new Handler().postDelayed(() -> {
									if(currentState == stateDisconnected && currentLaunchID == launchID) connect(getNextLaunchID());
								}, dropReconnectDelayMillis);
							}
							
							//Clearing the flags
							flagMarkEndTime = flagDropReconnect = false;
						}
					});
				}
			}
			
			private void updateStateConnected() {
				//Setting the connection established flag
				connectionEstablished = true;
				
				//Reading the shared preferences connectivity information
				SharedPreferences sharedPrefs = ((MainApplication) getApplication()).getConnectivitySharedPrefs();
				String lastConnectionHostname = sharedPrefs.getString(MainApplication.sharedPreferencesConnectivityKeyLastConnectionHostname, null);
				long lastConnectionTime = sharedPrefs.getLong(MainApplication.sharedPreferencesConnectivityKeyLastConnectionTime, -1);
				
				//Updating the shared preferences connectivity information
				SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();
				if(!hostname.equals(lastConnectionHostname)) sharedPrefsEditor.putString(MainApplication.sharedPreferencesConnectivityKeyLastConnectionHostname, hostname);
				sharedPrefsEditor.putLong(MainApplication.sharedPreferencesConnectivityKeyLastConnectionTime, System.currentTimeMillis());
				sharedPrefsEditor.apply();
				
				//Running on the main thread
				new Handler(Looper.getMainLooper()).post(() -> {
					//Checking if this is the most recent launch
					if(currentLaunchID == launchID) {
						//Setting the last connection result
						lastConnectionResult = intentResultCodeSuccess;
						
						//Setting the state
						currentState = stateConnected;
						
						//Retrieving the pending conversation info
						sendConversationInfoRequest(pendingConversations);
						
						//Setting the flags
						flagMarkEndTime = flagDropReconnect = true;
						
						//Checking if the last connection is the same as the current one
						if(hostname.equals(lastConnectionHostname)) {
							//Fetching the messages since the last connection time
							retrieveMessagesSince(lastConnectionTime, System.currentTimeMillis());
						}
					}
				});
				
				//Notifying the connection listeners
				broadcastState(stateConnected, -1, launchID);
				
				//Updating the notification
				if(foregroundServiceRequested()) postConnectedNotification(true);
				else clearNotification();
				
				//Scheduling the ping
				schedulePing();
			}
			
			private void processData(int messageType, byte[] data) {
				//Notifying the super connection manager of a message
				onMessage();
				
				switch(messageType) {
					case nhtClose:
						closeConnection(intentResultCodeConnection, false);
						break;
					case nhtPing:
						queuePacket(new PacketStruct(nhtPong, new byte[0]));
						break;
					case nhtAuthentication: {
						//Stopping the authentication timer
						if(authenticationExpiryTimer != null) {
							authenticationExpiryTimer.cancel();
							authenticationExpiryTimer = null;
						}
						
						try(ByteArrayInputStream src = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(src)) {
							//Recording the communications version
							int communicationsVersion = in.readInt();
							new Handler(Looper.getMainLooper()).post(() -> activeCommunicationsVersion = communicationsVersion);
							
							//Attempting to find a matching protocol version
							int verApplicability = checkCommVerApplicability(communicationsVersion);
							
							int result;
							
							//Checking if there is a matching version
							if(verApplicability == 0) {
								//Checking the result
								result = in.readInt();
								
								//Translating the result to the local value
								switch(result) {
									case nhtAuthenticationOK:
										result = intentResultCodeSuccess;
										break;
									case nhtAuthenticationUnauthorized:
										result = intentResultCodeUnauthorized;
										break;
									case nhtAuthenticationBadRequest:
										result = intentResultCodeBadRequest;
										break;
									case nhtAuthenticationVersionMismatch:
										if(mmCommunicationsVersion > communicationsVersion) result = intentResultCodeServerOutdated;
										else result = intentResultCodeClientOutdated;
										break;
								}
							} else {
								if(verApplicability == -1) result = intentResultCodeServerOutdated;
								else result = intentResultCodeClientOutdated;
							}
							
							if(result == intentResultCodeSuccess) {
								//Calling the success
								updateStateConnected();
							} else {
								//Terminating the connection
								closeConnection(result, result == intentResultCodeBadRequest); //Only forward the request if the request couldn't be processed (an unauthorized response means that the server could understand the request)
							}
						} catch(IOException | RuntimeException exception) {
							exception.printStackTrace();
						}
						
						break;
					}
					case nhtMessageUpdate:
					case nhtTimeRetrieval: {
						//Reading the list
						List<Blocks.ConversationItem> list;
						try(ByteArrayInputStream src = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(src)) {
							int count = in.readInt();
							list = new ArrayList<>(count);
							for(int i = 0; i < count; i++) list.add(((SharedValues.ConversationItem) in.readObject()).toBlock());
						} catch(IOException | RuntimeException | ClassNotFoundException exception) {
							exception.printStackTrace();
							break;
						}
						
						//Processing the messages
						processMessageUpdate(list, true);
						
						break;
					}
					case nhtMassRetrieval: {
						//Reading the lists
						List<Blocks.ConversationItem> listItems;
						List<Blocks.ConversationInfo> listConversations;
						try(ByteArrayInputStream src = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(src)) {
							int count = in.readInt();
							listItems = new ArrayList<>(count);
							for(int i = 0; i < count; i++) listItems.add(((SharedValues.ConversationItem) in.readObject()).toBlock());
							
							count = in.readInt();
							listConversations = new ArrayList<>(count);
							for(int i = 0; i < count; i++) listConversations.add(((SharedValues.ConversationInfo) in.readObject()).toBlock());
						} catch(IOException | RuntimeException | ClassNotFoundException exception) {
							exception.printStackTrace();
							break;
						}
						
						//Processing the messages
						processMassRetrievalResult(listItems, listConversations);
						
						break;
					}
					case nhtConversationUpdate: {
						//Reading the list
						List<Blocks.ConversationInfo> list;
						try(ByteArrayInputStream src = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(src)) {
							int count = in.readInt();
							list = new ArrayList<>(count);
							for(int i = 0; i < count; i++) list.add(((SharedValues.ConversationInfo) in.readObject()).toBlock());
						} catch(IOException | RuntimeException | ClassNotFoundException exception) {
							exception.printStackTrace();
							break;
						}
						
						//Processing the conversations
						processChatInfoResponse(list);
						
						break;
					}
					case nhtModifierUpdate: {
						//Reading the list
						List<Blocks.ModifierInfo> list;
						try(ByteArrayInputStream src = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(src)) {
							int count = in.readInt();
							list = new ArrayList<>(count);
							for(int i = 0; i < count; i++) list.add(((SharedValues.ModifierInfo) in.readObject()).toBlock());
						} catch(IOException | RuntimeException | ClassNotFoundException exception) {
							exception.printStackTrace();
							break;
						}
						
						//Processing the conversations
						processModifierUpdate(list, getPackager());
						
						break;
					}
					case nhtAttachmentReq: {
						//Reading the data
						final short requestID;
						final String fileGUID;
						final int requestIndex;
						final byte[] compressedBytes;
						final long fileSize;
						final boolean isLast;
						
						try(ByteArrayInputStream src = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(src)) {
							requestID = in.readShort();
							fileGUID = in.readUTF();
							requestIndex = in.readInt();
							compressedBytes = new byte[in.readInt()];
							in.readFully(compressedBytes);
							if(requestIndex == 0) fileSize = in.readLong();
							else fileSize = -1;
							isLast = in.readBoolean();
						} catch(IOException | RuntimeException exception) {
							exception.printStackTrace();
							break;
						}
						
						//Running on the UI thread
						mainHandler.post(() -> {
							//Searching for a matching request
							for(FileDownloadRequest request : fileDownloadRequests) {
								if(request.requestID != requestID || !request.attachmentGUID.equals(fileGUID)) continue;
								if(requestIndex == 0) request.setFileSize(fileSize);
								request.processFileFragment(ConnectionService.this, compressedBytes, requestIndex, isLast, getPackager());
								if(isLast) fileDownloadRequests.remove(request);
								break;
							}
						});
						
						break;
					}
					case nhtAttachmentReqConfirm: {
						//Reading the data
						final short requestID;
						final String fileGUID;
						try(ByteArrayInputStream src = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(src)) {
							requestID = in.readShort();
							fileGUID = in.readUTF();
						} catch(IOException | RuntimeException exception) {
							exception.printStackTrace();
							break;
						}
						
						//Running on the UI thread
						mainHandler.post(() -> {
							//Searching for a matching request
							for(FileDownloadRequest request : fileDownloadRequests) {
								if(request.requestID != requestID || !request.attachmentGUID.equals(fileGUID)) continue;
								request.stopTimer(true);
								request.onResponseReceived();
								break;
							}
						});
						
						break;
					}
					case nhtAttachmentReqFail: {
						//Reading the data
						final short requestID;
						final String fileGUID;
						try(ByteArrayInputStream src = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(src)) {
							requestID = in.readShort();
							fileGUID = in.readUTF();
						} catch(IOException | RuntimeException exception) {
							exception.printStackTrace();
							break;
						}
						
						//Running on the UI thread
						mainHandler.post(() -> {
							//Searching for a matching request
							for(FileDownloadRequest request : fileDownloadRequests) {
								if(request.requestID != requestID || !request.attachmentGUID.equals(fileGUID)) continue;
								request.failDownload();
								break;
							}
						});
						
						break;
					}
					case nhtSendResult: {
						//Reading the data
						final short requestID;
						final boolean result;
						try(ByteArrayInputStream src = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(src)) {
							requestID = in.readShort();
							result = in.readBoolean();
						} catch(IOException | RuntimeException exception) {
							exception.printStackTrace();
							break;
						}
						
						//Getting the message response manager
						final MessageResponseManager messageResponseManager = messageSendRequests.get(requestID);
						if(messageResponseManager != null) {
							//Removing the request
							messageSendRequests.remove(requestID);
							messageResponseManager.stopTimer(false);
							
							//Running on the UI thread
							new Handler(Looper.getMainLooper()).post(() -> {
								//Telling the listener
								if(result) messageResponseManager.onSuccess();
								else messageResponseManager.onFail(messageSendExternalException);
							});
						}
						
						break;
					}
				}
			}
			
			boolean queuePacket(PacketStruct packet) {
				if(writerThread == null) return false;
				writerThread.uploadQueue.add(packet);
				return true;
			}
			
			void sendPing() {
				queuePacket(new PacketStruct(nhtPing, new byte[0]));
			}
			
			void initiateClose(int resultCode, boolean forwardRequest) {
				//Sending a message and finishing the threads
				if(writerThread == null) {
					interrupt();
				} else {
					queuePacket(new PacketStruct(nhtClose, new byte[0], () -> {
						interrupt();
						writerThread.interrupt();
					}));
				}
				
				//Updating the state
				updateStateDisconnected(resultCode, forwardRequest);
			}
			
			private void closeConnection(int reason, boolean forwardRequest) {
				//Finishing the threads
				if(writerThread != null) writerThread.interrupt();
				interrupt();
				
				//Updating the state
				updateStateDisconnected(reason, forwardRequest);
			}
			
			synchronized boolean sendDataSync(int messageType, byte[] data, boolean flush) {
				try {
					//Writing the message
					outputStream.write(ByteBuffer.allocate(Integer.SIZE / 8 * 2).putInt(messageType).putInt(data.length).array());
					outputStream.write(data);
					if(flush) outputStream.flush();
					
					//Returning true
					return true;
				} catch(IOException exception) {
					//Logging the exception
					exception.printStackTrace();
					
					//Closing the connection
					if(socket.isConnected()) {
						closeConnection(intentResultCodeConnection, false);
					} else {
						Crashlytics.logException(exception);
					}
					
					//Returning false
					return false;
				}
			}
			
			private class WriterThread extends Thread {
				//Creating the queue
				final BlockingQueue<PacketStruct> uploadQueue = new LinkedBlockingQueue<>();
				
				@Override
				public void run() {
					PacketStruct packet;
					
					try {
						while(!isInterrupted()) {
							try {
								packet = uploadQueue.take();
								
								try {
									//outputStream.write(ByteBuffer.allocate(Integer.SIZE / 8 * 2).putInt(packet.type).putInt(packet.content.length).array());
									//outputStream.write(packet.content);
									sendDataSync(packet.type, packet.content, false);
								} finally {
									if(packet.sentRunnable != null) packet.sentRunnable.run();
								}
								
								while((packet = uploadQueue.poll()) != null) {
									try {
										//outputStream.write(ByteBuffer.allocate(Integer.SIZE / 8 * 2).putInt(packet.type).putInt(packet.content.length).array());
										//outputStream.write(packet.content);
										sendDataSync(packet.type, packet.content, false);
									} finally {
										if(packet.sentRunnable != null) packet.sentRunnable.run();
									}
								}
								
								outputStream.flush();
							} catch(IOException exception) {
								exception.printStackTrace();
								
								if(socket.isConnected()) {
									closeConnection(intentResultCodeConnection, false);
								} else {
									Crashlytics.logException(exception);
								}
							}
						}
						//closeConnection(intentResultCodeConnection, false);
					} catch(InterruptedException exception) {
						//exception.printStackTrace();
						//closeConnection(intentResultCodeConnection, false); //Can only be interrupted from closeConnection, so this is pointless
						
						return;
					}
				}
				
				private void sendPacket(PacketStruct packet) throws IOException {
					outputStream.write(ByteBuffer.allocate(Integer.SIZE / 8 * 2).putInt(packet.type).putInt(packet.content.length).array());
					outputStream.write(packet.content);
					outputStream.flush();
				}
			}
		}
	}
	
	private static class PackagerComm3 extends Packager {
		@Override
		byte[] packageData(byte[] data, int length) {
			try {
				return Constants.compressGZIP(data, length);
			} catch(IOException exception) {
				exception.printStackTrace();
				Crashlytics.logException(exception);
				
				return null;
			}
		}
		
		@Override
		byte[] unpackageData(byte[] data) {
			try {
				return Constants.decompressGZIP(data);
			} catch(IOException exception) {
				exception.printStackTrace();
				
				return null;
			}
		}
	}
	
	private class ClientComm2 extends ConnectionManager {
		//Creating the transmission header values
		public static final String headerCommVer = "MMS-Comm-Version";
		public static final String headerSoftVersion = "MMS-Soft-Version";
		public static final String headerSoftVersionCode = "MMS-Soft-Version-Code";
		public static final String headerPassword = "Password";
		
		//Creating the reference values
		private final Packager protocolPackager = new PackagerComm2();
		private static final String hashAlgorithm = "MD5";
		
		private static final int resultBadRequest = 4000;
		private static final int resultClientOutdated = 4001;
		private static final int resultServerOutdated = 4002;
		private static final int resultUnauthorized = 4003;
		
		private static final byte wsFrameUpdate = 0;
		private static final byte wsFrameTimeRetrieval = 1;
		private static final byte wsFrameMassRetrieval = 2;
		private static final byte wsFrameChatInfo = 3;
		private static final byte wsFrameModifierUpdate = 4;
		private static final byte wsFrameAttachmentReq = 5;
		private static final byte wsFrameAttachmentReqConfirmed = 6;
		private static final byte wsFrameAttachmentReqFailed = 7;
		
		private static final byte wsFrameSendResult = 100;
		private static final byte wsFrameSendTextExisting = 101;
		private static final byte wsFrameSendTextNew = 102;
		private static final byte wsFrameSendFileExisting = 103;
		private static final byte wsFrameSendFileNew = 104;
		
		//Creating the connection values
		private MMWebSocketClient wsClient = null;
		
		@Override
		boolean connect(byte launchID) {
			//Calling the super method
			super.connect(launchID);
			
			//Checking if the client is valid
			if(wsClient != null) {
				//Clearing the reconnection flag
				flagDropReconnect = false;
				
				//Closing the client
				wsClient.close();
				wsClient = null;
			}
			
			//Preparing the WS client
			try {
				//Creating the WS client
				wsClient = new MMWebSocketClient(launchID, new URI(prepareHostname(hostname)), new DraftMMS());
				wsClient.setConnectionLostTimeout(0);
				
				//Creating the SSL context
				SSLContext sslContext = SSLContext.getInstance("TLS");
				
				TrustManager[] trustAllCerts = new TrustManager[]{
						new X509TrustManager() {
							public java.security.cert.X509Certificate[] getAcceptedIssuers() {
								return new X509Certificate[0];
							}
							
							public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
							
							public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
						}
				};
				
				sslContext.init(null, trustAllCerts, new SecureRandom());
				
				//Using the secure socket
				wsClient.setSocket(sslContext.getSocketFactory().createSocket());
			} catch(IOException | KeyManagementException | URISyntaxException | NoSuchAlgorithmException exception) {
				//Printing the stack trace
				exception.printStackTrace();
				
				//Finishing the service
				//finishService();
				
				//Notifying the connection listeners
				broadcastState(stateDisconnected, intentResultCodeInternalException, launchID);
				
				//Returning false
				return false;
			}
			
			//Connecting
			wsClient.connect();
			
			//Returning true
			return true;
		}
		
		@Override
		void disconnect() {
			super.disconnect();
			if(wsClient != null && !wsClient.isClosed()) wsClient.close();
		}
		
		@Override
		int getState() {
			if(wsClient.isOpen()) return stateConnected;
			else if(wsClient.isConnecting()) return stateConnecting;
			else return stateDisconnected;
		}
		
		@Override
		boolean sendPing() {
			//Returning if the connection is invalid
			if(wsClient == null || !wsClient.isOpen()) return false;
			
			//Pinging the server
			wsClient.sendPing();
			
			//Returning true
			return true;
		}
		
		@Override
		Packager getPackager() {
			return protocolPackager;
		}
		
		@Override
		String getHashAlgorithm() {
			return hashAlgorithm;
		}
		
		@Override
		boolean sendMessage(short requestID, String chatGUID, String message) {
			//Returning if the connection is invalid
			if(wsClient == null || !wsClient.isOpen()) return false;
			
			byte[] packetData;
			try(ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
				//Adding the data
				out.writeByte(wsFrameSendTextExisting); //Message type - send existing text
				out.writeShort(requestID); //Request ID
				out.writeUTF(chatGUID); //Chat GUID
				out.writeUTF(message); //Message
				out.flush();
				
				packetData = bos.toByteArray();
			} catch(IOException exception) {
				//Printing the stack trace
				exception.printStackTrace();
				Crashlytics.logException(exception);
				
				//Returning false
				return false;
			}
			
			//Sending the message
			wsClient.send(packetData);
			
			//Returning true
			return true;
		}
		
		@Override
		boolean sendMessage(short requestID, String[] chatMembers, String message, String service) {
			//Returning if the connection is invalid
			if(wsClient == null || !wsClient.isOpen()) return false;
			
			byte[] packetData;
			try(ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
				//Adding the data
				out.writeByte(wsFrameSendTextNew); //Message type - send new text
				out.writeShort(requestID); //Request ID
				out.writeObject(chatMembers); //Chat recipients
				out.writeUTF(message); //Message
				out.writeUTF(service); //Service
				out.flush();
				
				packetData = bos.toByteArray();
			} catch(IOException exception) {
				//Printing the stack trace
				exception.printStackTrace();
				
				//Returning false
				return false;
			}
			
			//Sending the message
			wsClient.send(packetData);
			
			//Returning true
			return true;
		}
		
		@Override
		boolean addDownloadRequest(short requestID, String attachmentGUID) {
			//Returning if the connection is invalid
			if(wsClient == null || !wsClient.isOpen()) return false;
			
			//Preparing to serialize the request
			try(ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
				//Adding the data
				out.writeByte(wsFrameAttachmentReq); //Message type - attachment request
				out.writeShort(requestID); //Request ID
				out.writeUTF(attachmentGUID); //File GUID
				out.writeInt(attachmentChunkSize); //Chunk size
				out.flush();
				
				//Sending the message
				wsClient.send(bos.toByteArray());
			} catch(IOException | NotYetConnectedException exception) {
				//Printing the stack trace
				exception.printStackTrace();
				Crashlytics.logException(exception);
				
				//Returning false
				return false;
			}
			
			//Returning true
			return true;
		}
		
		@Override
		boolean uploadFilePacket(short requestID, int requestIndex, String conversationGUID, byte[] data, String fileName, boolean isLast) {
			//Returning if the connection is invalid
			if(wsClient == null || !wsClient.isOpen()) return false;
			
			//Adding the data
			byte[] packetData;
			try(ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
				out.writeByte(wsFrameSendFileExisting); //Message type - send existing file
				out.writeShort(requestID); //Request identifier
				out.writeInt(requestIndex); //Request index
				out.writeUTF(conversationGUID); //Chat GUID
				out.writeObject(data); //File bytes
				out.reset();
				if(requestIndex == 0) out.writeUTF(fileName);
				out.writeBoolean(isLast); //Is last message
				out.flush();
				
				packetData = bos.toByteArray();
			} catch(IOException exception) {
				//Logging the error
				exception.printStackTrace();
				Crashlytics.logException(exception);
				
				//Returning false
				return false;
			}
			
			//Sending the message
			wsClient.send(packetData);
			
			//Returning true
			return true;
		}
		
		@Override
		boolean uploadFilePacket(short requestID, int requestIndex, String[] conversationMembers, byte[] data, String fileName, String service, boolean isLast) {
			//Returning if the connection is invalid
			if(wsClient == null || !wsClient.isOpen()) return false;
			
			//Adding the data
			byte[] packetData;
			try(ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
				out.writeByte(wsFrameSendFileNew); //Message type - send new file
				out.writeShort(requestID); //Request identifier
				out.writeInt(requestIndex); //Request index
				out.writeObject(conversationMembers); //Chat recipients
				out.writeObject(data); //File bytes
				out.reset();
				if(requestIndex == 0) {
					out.writeUTF(fileName); //File name
					out.writeUTF(service); //Service
				}
				out.writeBoolean(isLast); //Is last message
				out.flush();
				
				packetData = bos.toByteArray();
			} catch(IOException exception) {
				//Logging the exception
				exception.printStackTrace();
				Crashlytics.logException(exception);
				
				//Returning false
				return false;
			}
			
			//Sending the message
			wsClient.send(packetData);
			
			//Returning true
			return true;
		}
		
		@Override
		boolean sendConversationInfoRequest(List<ConversationInfoRequest> list) {
			//Returning if the connection is invalid
			if(wsClient == null || !wsClient.isOpen()) return false;
			
			//Creating the guid list
			ArrayList<String> guidList;
			
			//Locking the pending conversations
			synchronized(list) {
				//Returning true if there are no pending conversations
				if(list.isEmpty()) return true;
				
				//Converting the conversation info list to a string list
				guidList = new ArrayList<>();
				for(ConversationInfoRequest conversationInfoRequest : list)
					guidList.add(conversationInfoRequest.conversationInfo.getGuid());
			}
			
			//Requesting information on new conversations
			try(ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
				//Adding the data
				out.writeByte(wsFrameChatInfo); //Message type - chat info
				out.writeObject(guidList); //Conversation list
				out.flush();
				
				//Sending the message
				wsClient.send(bos.toByteArray());
			} catch(IOException exception) {
				exception.printStackTrace();
				Crashlytics.logException(exception);
				
				//Returning false
				return false;
			}
			
			//Returning true
			return true;
		}
		
		@Override
		boolean requestRetrievalTime(long timeLower, long timeUpper) {
			//Returning if the connection is invalid
			if(wsClient == null || !wsClient.isOpen()) return false;
			
			//Preparing to serialize the request
			byte[] packetData;
			try(ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
				//Adding the data
				out.writeByte(wsFrameTimeRetrieval); //Message type - time-based retrieval
				out.writeLong(timeLower); //Lower time
				out.writeLong(timeUpper); //Upper time
				out.flush();
				
				//Sending the message
				packetData = bos.toByteArray();
			} catch(IOException exception) {
				//Logging the exception
				exception.printStackTrace();
				Crashlytics.logException(exception);
				
				//Returning false
				return false;
			}
			
			//Sending the message
			wsClient.send(packetData);
			
			//Returning true
			return true;
		}
		
		@Override
		boolean requestRetrievalAll() {
			//Returning if the connection is invalid
			if(wsClient == null || !wsClient.isOpen()) return false;
			
			//Building the request
			byte[] packetData;
			try(ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
				//Adding the data
				out.writeByte(wsFrameMassRetrieval); //Message type - Mass retrieval request
				out.flush();
				
				packetData = bos.toByteArray();
			} catch(IOException exception) {
				//Logging the error
				exception.printStackTrace();
				Crashlytics.logException(exception);
				
				//Returning false
				return false;
			}
			
			//Sending the message
			wsClient.send(packetData);
			
			//Returning true
			return true;
		}
		
		@Override
		int checkCommVerApplicability(int version) {
			return Integer.compare(version, 2);
		}
		
		private class DraftMMS extends Draft_6455 {
			@Override
			public ClientHandshakeBuilder postProcessHandshakeRequestAsClient(ClientHandshakeBuilder request) {
				//Calling the super method
				super.postProcessHandshakeRequestAsClient(request);
				
				//Building the delimited applicable communications version string
				StringBuilder applicableVersionsSB = new StringBuilder(Integer.toString(applicableCommunicationsVersions[0]));
				for(int i = 1; i < applicableCommunicationsVersions.length; i++) applicableVersionsSB.append('|').append(applicableCommunicationsVersions[i]);
				
				//Adding the communications version and password to the handshake request
				request.put(headerCommVer, applicableVersionsSB.toString());
				request.put(headerPassword, password);
				
				//Returning the request
				return request;
			}
		
		/* @Override
		public HandshakeState acceptHandshakeAsClient(ClientHandshake request, ServerHandshake response) throws InvalidHandshakeException {
			//Returning not matched if the super method rejects the request
			if(super.acceptHandshakeAsClient(request, response) == HandshakeState.NOT_MATCHED)
				return HandshakeState.NOT_MATCHED;
			
			//Returning not matched if the client didn't provide a protocol or provided an incompatible one
			if(!request.hasFieldValue(headerWebSocketProtocol) ||
					!request.getFieldValue(headerWebSocketProtocol).equalsIgnoreCase(protocolVersion))
				return HandshakeState.NOT_MATCHED;
			
			//Returning matched
			return HandshakeState.MATCHED;
		} */
			
			@Override
			public boolean equals(Object o) {
				if(this == o) return true;
				if(o == null || getClass() != o.getClass()) return false;
				
				DraftMMS that = (DraftMMS) o;
				
				return getExtension() != null ? getExtension().equals(that.getExtension()) : that.getExtension() == null;
			}
			
			@Override
			public Draft copyInstance() {
				return new DraftMMS();
			}
		}
		
		private class MMWebSocketClient extends WebSocketClient {
			//Creating the values
			private final byte launchID;
			
			MMWebSocketClient(byte launchID, URI serverUri) {
				super(serverUri);
				this.launchID = launchID;
			}
			
			MMWebSocketClient(byte launchID, URI serverUri, Draft draft) {
				super(serverUri, draft);
				this.launchID = launchID;
			}
			
			@Override
			public void onOpen(ServerHandshake handshake) {
				//Checking if this is the most recent launch
				if(currentLaunchID == launchID) {
					//Setting the last connection result
					lastConnectionResult = intentResultCodeSuccess;
					
					//Notifying the connection listeners
					broadcastState(stateConnected, -1, launchID);
					
					//Recording the server version
					{
						String commVer = handshake.getFieldValue(headerCommVer);
						if(commVer.matches("^\\d+$")) activeCommunicationsVersion = Integer.parseInt(commVer);
					}
					
					//Retrieving the pending conversation info
					sendConversationInfoRequest(pendingConversations);
					
					//Updating the notification
					if(foregroundServiceRequested()) postConnectedNotification(true);
					else clearNotification();
					
					//Setting the connection as existing
					flagMarkEndTime = flagDropReconnect = true;
					
					//Getting the last connection time
					SharedPreferences sharedPrefs = ((MainApplication) getApplication()).getConnectivitySharedPrefs();
					String lastConnectionHostname = sharedPrefs.getString(MainApplication.sharedPreferencesConnectivityKeyLastConnectionHostname, null);
					
					//Checking if the last connection is the same as the current one
					if(hostname.equals(lastConnectionHostname)) {
						//Getting the last connection time
						long lastConnectionTime = sharedPrefs.getLong(MainApplication.sharedPreferencesConnectivityKeyLastConnectionTime, -1);
						
						//Fetching the messages since the last connection time
						retrieveMessagesSince(lastConnectionTime, System.currentTimeMillis());
					}
					
					//Scheduling the ping
					schedulePing();
				}
			}
			
			@Override
			public void onMessage(String message) {}
			
			@Override
			public void onMessage(ByteBuffer bytes) {
				//Notifying the super connection manager of a message
				ClientComm2.this.onMessage();
				
				//Processing the message
				byte[] array = new byte[bytes.remaining()];
				bytes.get(array);
				
				try(ByteArrayInputStream bis = new ByteArrayInputStream(array); ObjectInputStream in = new ObjectInputStream(bis)) {
					switch(in.readByte()) { //Reading the message type and making a switch statement
						case wsFrameUpdate: { //New messages received
							List<SharedValues.ConversationItem> receivedItems = (ArrayList<SharedValues.ConversationItem>) in.readObject();
							List<Blocks.ConversationItem> blockItems = new ArrayList<>();
							for(SharedValues.ConversationItem item : receivedItems) blockItems.add(item.toBlock());
							
							//Processing the messages
							processMessageUpdate(blockItems, true);
							
							break;
						}
						case wsFrameTimeRetrieval: { //Time retrieval
							List<SharedValues.ConversationItem> receivedItems = (ArrayList<SharedValues.ConversationItem>) in.readObject();
							List<Blocks.ConversationItem> blockItems = new ArrayList<>();
							for(SharedValues.ConversationItem item : receivedItems) blockItems.add(item.toBlock());
							
							//Processing the messages
							processMessageUpdate(blockItems, true);
							
							break;
						}
						case wsFrameMassRetrieval: { //Mass retrieval
							//Breaking if the client isn't looking for a mass retrieval
							//if(!massRetrievalInProgress) break;
							
							//Reading the data
							final ArrayList<SharedValues.ConversationItem> receivedItems = (ArrayList<SharedValues.ConversationItem>) in.readObject();
							final ArrayList<SharedValues.ConversationInfo> receivedConversations = (ArrayList<SharedValues.ConversationInfo>) in.readObject();
							
							List<Blocks.ConversationItem> blockItems = new ArrayList<>();
							for(SharedValues.ConversationItem item : receivedItems) blockItems.add(item.toBlock());
							
							List<Blocks.ConversationInfo> blockConversations = new ArrayList<>();
							for(SharedValues.ConversationInfo item : receivedConversations) blockConversations.add(item.toBlock());
							
							//Processing the messages
							processMassRetrievalResult(blockItems, blockConversations);
							
							break;
						}
						case wsFrameChatInfo: { //Chat information
							final ArrayList<SharedValues.ConversationInfo> receivedItems = (ArrayList<SharedValues.ConversationInfo>) in.readObject();
							
							List<Blocks.ConversationInfo> blockItems = new ArrayList<>();
							for(SharedValues.ConversationInfo item : receivedItems) blockItems.add(item.toBlock());
							
							//Processing the conversations
							processChatInfoResponse(blockItems);
							
							break;
						}
						case wsFrameModifierUpdate: { //Message modifier update
							final ArrayList<SharedValues.ModifierInfo> receivedItems = (ArrayList<SharedValues.ModifierInfo>) in.readObject();
							
							List<Blocks.ModifierInfo> blockItems = new ArrayList<>();
							for(SharedValues.ModifierInfo item : receivedItems) blockItems.add(item.toBlock());
							
							//Processing the conversations
							processModifierUpdate(blockItems, getPackager());
							
							break;
						}
						case wsFrameAttachmentReq: { //Attachment data received
							final String guid = in.readUTF();
							final short requestID = in.readShort();
							final int requestIndex = in.readInt();
							final byte[] compressedBytes = (byte[]) in.readObject();
							final long fileSize;
							if(requestIndex == 0) fileSize = in.readLong();
							else fileSize = -1;
							final boolean isLast = in.readBoolean();
							
							//Running on the UI thread
							mainHandler.post(() -> {
								//Searching for a matching request
								for(FileDownloadRequest request : fileDownloadRequests) {
									if(request.requestID != requestID || !request.attachmentGUID.equals(guid)) continue;
									if(requestIndex == 0) request.setFileSize(fileSize);
									request.processFileFragment(ConnectionService.this, compressedBytes, requestIndex, isLast, getPackager());
									if(isLast) fileDownloadRequests.remove(request);
									break;
								}
							});
							
							break;
						}
						case wsFrameAttachmentReqConfirmed: { //Attachment data request received
							final short requestID = in.readShort();
							final String guid = in.readUTF();
							
							//Running on the UI thread
							mainHandler.post(() -> {
								//Searching for a matching request
								for(FileDownloadRequest request : fileDownloadRequests) {
									if(request.requestID != requestID || !request.attachmentGUID.equals(guid)) continue;
									request.stopTimer(true);
									request.onResponseReceived();
									break;
								}
							});
							break;
						}
						case wsFrameAttachmentReqFailed: { //Attachment data request failed
							final short requestID = in.readShort();
							final String guid = in.readUTF();
							
							//Running on the UI thread
							mainHandler.post(() -> {
								//Searching for a matching request
								for(FileDownloadRequest request : fileDownloadRequests) {
									if(request.requestID != requestID || !request.attachmentGUID.equals(guid)) continue;
									request.failDownload();
									break;
								}
							});
							break;
						}
						case wsFrameSendResult: {
							//Reading the info
							short requestID = in.readShort();
							final boolean success = in.readBoolean();
							
							//Getting the message response manager
							final MessageResponseManager messageResponseManager = messageSendRequests.get(requestID);
							if(messageResponseManager != null) {
								//Removing the request
								messageSendRequests.remove(requestID);
								messageResponseManager.stopTimer(false);
								
								//Running on the UI thread
								new Handler(Looper.getMainLooper()).post(() -> {
									//Telling the listener
									if(success) messageResponseManager.onSuccess();
									else messageResponseManager.onFail(messageSendExternalException);
								});
							}
						}
					}
				} catch(IOException | ClassNotFoundException | ClassCastException exception) {
					//Printing the stack trace
					exception.printStackTrace();
				}
			}
			
			@Override
			public void onClose(int uselessCode, String reasonString, boolean remote) {
				//Cancelling the mass retrieval if there is one in progress
				cancelMassRetrieval();
				
				//Checking if this is the most recent launch
				if(currentLaunchID == launchID) {
					//Getting the code from the message
					int code = -1;
					String errorCodeString = reasonString.substring(reasonString.lastIndexOf(' ') + 1);
					if(errorCodeString.matches("^\\d+$")) code = Integer.parseInt(errorCodeString);
					
					//Determining the broadcast value
					int clientReason;
					switch(code) {
						default:
							clientReason = intentResultCodeConnection;
							break;
						case resultBadRequest:
							clientReason = intentResultCodeBadRequest;
							break;
						case resultClientOutdated:
							clientReason = intentResultCodeClientOutdated;
							break;
						case resultServerOutdated:
							clientReason = intentResultCodeServerOutdated;
							break;
						case resultUnauthorized:
							clientReason = intentResultCodeUnauthorized;
					}
					
					//Setting the last connection result
					lastConnectionResult = clientReason;
					
					//Notifying the connection listeners
					broadcastState(stateDisconnected, clientReason, launchID);
					
					//Checking if a connection existed for reconnection and the preference is enabled
					if(flagDropReconnect && PreferenceManager.getDefaultSharedPreferences(MainApplication.getInstance()).getBoolean(MainApplication.getInstance().getResources().getString(R.string.preference_server_dropreconnect_key), false)) {
						//Reconnecting
						new Handler().postDelayed(() -> {
							if(getCurrentState() == stateDisconnected) ClientComm2.this.connect(getNextLaunchID());
						}, dropReconnectDelayMillis);
					}
					
					//Clearing the flags
					flagMarkEndTime = flagDropReconnect = false;
					
					//Posting the disconnected notification
					if(!shutdownRequested) postDisconnectedNotification(false);
					
					//Removing the scheduled ping
					//unschedulePing();
				}
			}
			
			@Override
			public void onError(Exception exception) {
				exception.printStackTrace();
			}
		}
		
		private String prepareHostname(String hostname) {
			//Checking if the hostname doesn't have a port
			if(!regExValidPort.matcher(hostname).find()) {
				//Adding the default port
				hostname += ':' + Integer.toString(Constants.defaultPort);
			}
			
			//Checking if the hostname doesn't have a protocol
			if(!regExValidProtocol.matcher(hostname).find()) {
				//Adding the default protocol
				hostname = Constants.defaultProtocol + hostname;
			}
			
			//Returning the hostname
			return hostname;
		}
	}
	
	private static class PackagerComm2 extends Packager {
		@Override
		byte[] packageData(byte[] data, int length) {
			try {
				return SharedValues.compressLegacyV2(data, length);
			} catch(IOException exception) {
				exception.printStackTrace();
				Crashlytics.logException(exception);
				
				return null;
			}
		}
		
		@Override
		byte[] unpackageData(byte[] data) {
			try {
				return SharedValues.decompressLegacyV2(data);
			} catch(IOException | DataFormatException exception) {
				exception.printStackTrace();
				
				return null;
			}
		}
	}
	
	static class PacketStruct {
		final int type;
		final byte[] content;
		Runnable sentRunnable;
		
		PacketStruct(int type, byte[] content) {
			this.type = type;
			this.content = content;
		}
		
		PacketStruct(int type, byte[] content, Runnable sentRunnable) {
			this(type, content);
			this.sentRunnable = sentRunnable;
		}
	}
	
	private void processMessageUpdate(List<Blocks.ConversationItem> structConversationItems, boolean sendNotifications) {
		//Creating and running the task
		//new MessageUpdateAsyncTask(this, getApplicationContext(), structConversationItems, sendNotifications).execute();
		addMessagingProcessingTask(new MessageUpdateAsyncTask(this, getApplicationContext(), structConversationItems, sendNotifications));
	}
	
	private void processMassRetrievalResult(List<Blocks.ConversationItem> structConversationItems, List<Blocks.ConversationInfo> structConversations) {
		/* //Stopping the timeout timer
		massRetrievalTimeoutHandler.removeCallbacks(massRetrievalTimeoutRunnable);
		
		//Calculating the progress
		massRetrievalProgress = 0;
		massRetrievalProgressCount = structConversationItems.size() + structConversations.size(); */
		
		//Adding the data
		if(massRetrievalThread == null) return;
		massRetrievalThread.registerInfo(this, structConversations, structConversationItems.size());
		massRetrievalThread.addPacket(this, 1, structConversationItems);
		massRetrievalThread.finish();
		
		//Sending a progress message
		/* LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(localBCMassRetrieval)
				.putExtra(Constants.intentParamState, intentExtraStateMassRetrievalProgress)
				.putExtra(Constants.intentParamSize, massRetrievalProgressCount)); */
		
		//Creating and running the task
		//new MassRetrievalAsyncTask(this, getApplicationContext(), structConversationItems, structConversations).execute();
		//addMessagingProcessingTask(new MassRetrievalAsyncTask(this, getApplicationContext(), structConversationItems, structConversations));
	}
	
	private void processChatInfoResponse(List<Blocks.ConversationInfo> structConversations) {
		//Creating the list values
		final ArrayList<ConversationManager.ConversationInfo> unavailableConversations = new ArrayList<>();
		final ArrayList<ConversationInfoRequest> availableConversations = new ArrayList<>();
		
		//Iterating over the conversations
		for(Blocks.ConversationInfo structConversationInfo : structConversations) {
			//Finding the conversation in the pending list
			ConversationInfoRequest request = null;
			synchronized(pendingConversations) {
				for(Iterator<ConversationInfoRequest> iterator = pendingConversations.iterator(); iterator.hasNext(); ) {
					//Getting the current request
					ConversationInfoRequest allRequests = iterator.next();
					
					//Skipping the remainder of the iteration if the pending conversation's GUID doesn't match the new conversation information's GUID
					if(!allRequests.conversationInfo.getGuid().equals(structConversationInfo.guid)) continue;
					
					//Setting the request
					request = allRequests;
					
					//Removing the request (it will be processed no matter what)
					iterator.remove();
					
					//Breaking from the loop
					break;
				}
				
				//Skipping the remainder of the iteration if no matching pending conversation could be found or the conversation is not in a valid state
				if(request == null || request.conversationInfo.getState() != ConversationManager.ConversationInfo.ConversationState.INCOMPLETE_SERVER) continue;
				
				//Checking if the conversation is available
				if(structConversationInfo.available) {
					//Setting the conversation details
					request.conversationInfo.setService(structConversationInfo.service);
					request.conversationInfo.setTitle(ConnectionService.this, structConversationInfo.name);
					//request.conversationInfo.setConversationColor(ConversationManager.ConversationInfo.getRandomColor());
					request.conversationInfo.setConversationColor(ConversationManager.ConversationInfo.getDefaultConversationColor(request.conversationInfo.getGuid()));
					request.conversationInfo.setConversationMembersCreateColors(structConversationInfo.members);
					request.conversationInfo.setState(ConversationManager.ConversationInfo.ConversationState.READY);
					
					//Marking the conversation as valid (and to be saved)
					availableConversations.add(request);
				}
				//Otherwise marking the conversation as invalid
				else unavailableConversations.add(request.conversationInfo);
			}
		}
		
		//Creating and running the asynchronous task
		//new SaveConversationInfoAsyncTask(getApplicationContext(), unavailableConversations, availableConversations).execute();
		addMessagingProcessingTask(new SaveConversationInfoAsyncTask(getApplicationContext(), unavailableConversations, availableConversations));
	}
	
	private void processModifierUpdate(List<Blocks.ModifierInfo> structModifiers, Packager packager) {
		//Creating and running the task
		//new ModifierUpdateAsyncTask(getApplicationContext(), structModifiers).execute();
		addMessagingProcessingTask(new ModifierUpdateAsyncTask(getApplicationContext(), structModifiers, packager));
	}
	
	/* boolean requestAttachmentInfo(String fileGuid, short requestID) {
		//Preparing to serialize the request
		try(ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
			//Adding the data
			out.writeByte(SharedValues.wsFrameAttachmentReq); //Message type - attachment request
			out.writeShort(requestID); //Request ID
			out.writeUTF(fileGuid); //File GUID
			out.writeInt(attachmentChunkSize); //Chunk size
			out.flush();
			
			//Sending the message
			wsClient.send(bos.toByteArray());
		} catch(IOException | NotYetConnectedException exception) {
			//Printing the stack trace
			exception.printStackTrace();
			Crashlytics.logException(exception);
			
			//Returning false
			return false;
		}
		
		//Returning true
		return true;
	} */
	
	boolean retrievePendingConversationInfo() {
		//Returning if the connection is not ready
		if(getCurrentState() != stateConnected) return false;
		
		//Sending a request and returning the result
		return currentConnectionManager.sendConversationInfoRequest(pendingConversations);
	}
	
	boolean isMassRetrievalInProgress() {
		return massRetrievalThread != null && massRetrievalThread.isInProgress();
	}
	
	boolean isMassRetrievalWaiting() {
		return massRetrievalThread != null && massRetrievalThread.isWaiting();
	}
	
	int getMassRetrievalProgress() {
		if(massRetrievalThread == null) return -1;
		return massRetrievalThread.getProgress();
	}
	
	int getMassRetrievalProgressCount() {
		if(massRetrievalThread == null) return -1;
		return massRetrievalThread.getProgressCount();
	}
	
	//Creating the constants
	static final byte messageSendSuccess = 0;
	static final byte messageSendInvalidContent = 1;
	static final byte messageSendFileTooLarge = 2;
	static final byte messageSendIOException = 3;
	static final byte messageSendNetworkException = 4;
	static final byte messageSendExternalException = 5;
	static final byte messageSendRequestExpired = 6;
	static final byte messageSendReferencesLost = 7;
	static final byte messageSendInternalException = 8;
	
	private static final int largestFileSize = 1024 * 1024 * 100; //100 MB
	
	void queueUploadRequest(FileUploadRequestCallbacks callbacks, Uri uri, ConversationManager.ConversationInfo conversationInfo, long attachmentID) {
		//Adding the request
		addUploadRequest(new FileUploadRequest(callbacks, uri, conversationInfo, attachmentID));
	}
	
	void queueUploadRequest(FileUploadRequestCallbacks callbacks, File file, ConversationManager.ConversationInfo conversationInfo, long attachmentID) {
		//Adding the request
		addUploadRequest(new FileUploadRequest(callbacks, file, conversationInfo, attachmentID));
	}
	
	private void addUploadRequest(FileUploadRequest request) {
		//Adding the task
		fileUploadRequestQueue.add(request);
		
		//Starting the thread if it isn't running
		if(fileUploadRequestThreadRunning.compareAndSet(false, true)) new FileUploadRequestThread(getApplicationContext(), this).start();
	}
	
	boolean addDownloadRequest(FileDownloadRequestCallbacks callbacks, long attachmentID, String attachmentGUID, String attachmentName) {
		//Getting the request ID
		short requestID = getNextRequestID();
		
		//Returning if there is no connection
		if(currentConnectionManager == null || currentConnectionManager.getState() != stateConnected) return false;
		
		//Sending the request
		boolean result = currentConnectionManager.addDownloadRequest(requestID, attachmentGUID);
		if(!result) return false;
		
		//Recording the request
		FileDownloadRequest request = new FileDownloadRequest(callbacks, requestID, attachmentID, attachmentGUID, attachmentName);
		fileDownloadRequests.add(request);
		request.startTimer();
		
		//Returning true
		return true;
	}
	
	FileDownloadRequest.ProgressStruct updateDownloadRequestAttachment(long attachmentID, FileDownloadRequestCallbacks callbacks) {
		for(FileDownloadRequest request : fileDownloadRequests)
			if(request.attachmentID == attachmentID) {
				request.callbacks = callbacks;
				return request.getProgress();
			}
		return null;
	}
	
	interface FileUploadRequestCallbacks {
		void onResponseReceived();
		
		void onStart();
		
		void onProgress(float progress);
		
		void onCopyFinished(File location);
		
		void onUploadFinished(byte[] checksum);
		
		void onFail(byte reason);
	}
	
	interface FileDownloadRequestCallbacks {
		void onResponseReceived();
		
		void onStart();
		
		void onProgress(float progress);
		
		void onFinish(File file);
		
		void onFail();
	}
	
	private static class FileUploadRequest {
		//Creating the callbacks
		final FileUploadRequestCallbacks callbacks;
		
		//Creating the request values
		//final ConversationManager.ConversationInfo conversationInfo;
		final long attachmentID;
		File sendFile;
		Uri sendUri;
		
		//Creating the conversation values
		final boolean conversationExists;
		final String conversationGUID;
		final String[] conversationMembers;
		final String conversationService;
		
		private FileUploadRequest(FileUploadRequestCallbacks callbacks, ConversationManager.ConversationInfo conversationInfo, long attachmentID) {
			//Setting the callbacks
			this.callbacks = callbacks;
			
			//Setting the request values
			this.attachmentID = attachmentID;
			
			if(conversationInfo.getState() == ConversationManager.ConversationInfo.ConversationState.READY) {
				conversationExists = true;
				conversationGUID = conversationInfo.getGuid();
				conversationMembers = null;
				conversationService = null;
			} else {
				conversationExists = false;
				conversationGUID = null;
				conversationMembers = conversationInfo.getNormalizedConversationMembersAsArray();
				conversationService = conversationInfo.getService();
			}
		}
		
		FileUploadRequest(FileUploadRequestCallbacks callbacks, File file, ConversationManager.ConversationInfo conversationInfo, long attachmentID) {
			//Calling the main constructor
			this(callbacks, conversationInfo, attachmentID);
			
			//Setting the source values
			sendFile = file;
			sendUri = null;
		}
		
		FileUploadRequest(FileUploadRequestCallbacks callbacks, Uri uri, ConversationManager.ConversationInfo conversationInfo, long attachmentID) {
			//Calling the main constructor
			this(callbacks, conversationInfo, attachmentID);
			
			//Setting the source values
			sendFile = null;
			sendUri = uri;
		}
	}
	
	static class FileDownloadRequest {
		//Creating the callbacks
		FileDownloadRequestCallbacks callbacks;
		
		//Creating the request values
		final short requestID;
		final String attachmentGUID;
		final long attachmentID;
		final String fileName;
		long fileSize = 0;
		//private List<FileDownloadRequest> removalList;
		
		FileDownloadRequest(FileDownloadRequestCallbacks callbacks, short requestID, long attachmentID, String attachmentGUID, String fileName) {
			//Setting the callbacks
			this.callbacks = callbacks;
			
			//Setting the request values
			this.requestID = requestID;
			this.attachmentID = attachmentID;
			this.attachmentGUID = attachmentGUID;
			this.fileName = fileName;
		}
		
		private static final long timeoutDelay = 20 * 1000; //20-second delay
		private final Handler handler = new Handler(Looper.getMainLooper());

		void startTimer() {
			handler.postDelayed(timeoutRunnable, timeoutDelay);
		}
		
		void stopTimer(boolean restart) {
			handler.removeCallbacks(timeoutRunnable);
			if(restart) handler.postDelayed(timeoutRunnable, timeoutDelay);
		}
		
		void failDownload() {
			stopTimer(false);
			if(attachmentWriterThread != null) attachmentWriterThread.stopThread();
			callbacks.onFail();
			
			removeRequestFromList();
		}
		
		void finishDownload(File file) {
			stopTimer(false);
			callbacks.onFinish(file);
			removeRequestFromList();
		}
		
		private void removeRequestFromList() {
			ConnectionService service = getInstance();
			if(service != null) service.fileDownloadRequests.remove(this);
		}
		
		void setFileSize(long value) {
			fileSize = value;
		}
		
		void onResponseReceived() {
			if(!isWaiting) return;
			isWaiting = false;
			callbacks.onResponseReceived();
		}
		
		ProgressStruct getProgress() {
			return new ProgressStruct(isWaiting, lastProgress);
		}
		
		private void updateProgress(float progress) {
			lastProgress = 0;
			callbacks.onProgress(progress);
		}
		AttachmentWriter attachmentWriterThread = null;
		private final Runnable timeoutRunnable = this::failDownload;
		boolean isWaiting = true;
		int lastIndex = -1;
		float lastProgress = 0;
		
		private void processFileFragment(Context context, final byte[] compressedBytes, int index, boolean isLast, Packager packager) {
			//Setting the state to receiving if it isn't already
			if(isWaiting) {
				isWaiting = false;
				callbacks.onResponseReceived();
			}
			
			//Checking if the index doesn't line up
			if(lastIndex + 1 != index) {
				//Failing the download
				failDownload();
				
				//Returning
				return;
			}
			
			//Resetting the timer
			stopTimer(!isLast);
			
			//Setting the last index
			lastIndex = index;
			
			//Checking if there is no save thread
			if(attachmentWriterThread == null) {
				//Creating and starting the attachment writer thread
				attachmentWriterThread = new AttachmentWriter(context.getApplicationContext(), attachmentID, fileName, fileSize, packager);
				attachmentWriterThread.start();
				callbacks.onStart();
			}
			
			//Adding the data struct
			attachmentWriterThread.dataQueue.add(new AttachmentWriterDataStruct(compressedBytes, isLast));
		}
		
		static class ProgressStruct {
			final boolean isWaiting;
			final float progress;
			
			ProgressStruct(boolean isWaiting, float progress) {
				this.isWaiting = isWaiting;
				this.progress = progress;
			}
		}
		
		private class AttachmentWriter extends Thread {
			//Creating the references
			private final WeakReference<Context> contextReference;
			
			//Creating the queue
			private final BlockingQueue<AttachmentWriterDataStruct> dataQueue = new LinkedBlockingQueue<>();
			
			//Creating the request values
			private final long attachmentID;
			private final String fileName;
			private final long fileSize;
			
			private final Packager packager;
			
			//Creating the process values
			private long bytesWritten;
			private boolean isRunning = true;
			
			AttachmentWriter(Context context, long attachmentID, String fileName, long fileSize, Packager packager) {
				//Setting the references
				contextReference = new WeakReference<>(context);
				
				//Setting the request values
				this.attachmentID = attachmentID;
				this.fileName = fileName;
				this.fileSize = fileSize;
				
				this.packager = packager;
			}
			
			@Override
			public void run() {
				//Getting the file paths
				File targetFileDir;
				{
					//Getting the context
					Context context = contextReference.get();
					if(context == null) {
						new Handler(Looper.getMainLooper()).post(FileDownloadRequest.this::failDownload);
						return;
					}
					
					targetFileDir = new File(MainApplication.getDownloadDirectory(context), Long.toString(attachmentID));
					if(!targetFileDir.exists()) targetFileDir.mkdir();
					else if(targetFileDir.isFile()) {
						Constants.recursiveDelete(targetFileDir);
						targetFileDir.mkdir();
					}
				}
				
				//Preparing to write to the file
				File targetFile = new File(targetFileDir, fileName);
				try(OutputStream outputStream = new FileOutputStream(targetFile)) {
					while(isRunning) {
						//Getting the data struct
						AttachmentWriterDataStruct dataStruct = dataQueue.poll(timeoutDelay, TimeUnit.MILLISECONDS);
						
						//Skipping the remainder of the iteration if the data struct is invalid
						if(dataStruct == null) continue;
						
						//Decompressing the bytes
						byte[] decompressedBytes = packager.unpackageData(dataStruct.compressedBytes);
						
						//Writing the bytes
						outputStream.write(decompressedBytes);
						
						//Adding to the bytes written
						bytesWritten += decompressedBytes.length;
						
						//Checking if the data is the last group
						if(dataStruct.isLast) {
							//Cleaning the thread
							//cleanThread();
							
							//Updating the database entry
							DatabaseManager.getInstance().updateAttachmentFile(attachmentID, MainApplication.getInstance(), targetFile);
							
							//Updating the state
							new Handler(Looper.getMainLooper()).post(() -> finishDownload(targetFile));
							
							//Returning
							return;
						} else {
							//Updating the progress
							new Handler(Looper.getMainLooper()).post(() -> updateProgress(((float) bytesWritten / fileSize)));
						}
						
						/* //Checking if the thread is still running
						if(isRunning) {
							dataStructsLock.lock();
							try {
								//Waiting for entries to appear
								if(dataStructs.isEmpty()) dataStructsCondition.await(timeoutDelay, TimeUnit.MILLISECONDS);
								
								//Checking if there are still no new items
								if(isRunning && dataStructs.isEmpty()) {
									//Stopping the thread
									isRunning = false;
									
									//Failing the download
									new Handler(Looper.getMainLooper()).post(FileDownloadRequest.this::failDownload);
								}
							} catch(InterruptedException exception) {
								//Stopping the thread
								isRunning = false;
								
								//Returning
								//return;
							} finally {
								dataStructsLock.unlock();
							}
						} */
					}
				} catch(IOException | OutOfMemoryError exception) {
					//Printing the stack trace
					exception.printStackTrace();
					Crashlytics.logException(exception);
					
					//Failing the download
					new Handler(Looper.getMainLooper()).post(FileDownloadRequest.this::failDownload);
					
					//Setting the thread as not running
					isRunning = false;
				} catch(InterruptedException exception) {
					//Printing the stack trace
					exception.printStackTrace();
					
					//Failing the download
					new Handler(Looper.getMainLooper()).post(FileDownloadRequest.this::failDownload);
					
					//Setting the thread as not running
					isRunning = false;
				}
				
				//Checking if the thread was stopped
				if(!isRunning) {
					//Cleaning up
					Constants.recursiveDelete(targetFileDir);
				}
			}
			
			void stopThread() {
				isRunning = false;
			}
		}
		
		static class AttachmentWriterDataStruct {
			final byte[] compressedBytes;
			final boolean isLast;
			
			AttachmentWriterDataStruct(byte[] compressedBytes, boolean isLast) {
				this.compressedBytes = compressedBytes;
				this.isLast = isLast;
			}
		}
	}
	
	private static class FileUploadRequestThread extends Thread {
		//Creating the constants
		private final float copyProgressValue = 0.2F;
		
		//Creating the reference values
		private final WeakReference<Context> contextReference;
		private final WeakReference<ConnectionService> serviceReference;
		
		//Creating the other values
		private final Handler handler = new Handler(Looper.getMainLooper());
		
		FileUploadRequestThread(Context context, ConnectionService service) {
			contextReference = new WeakReference<>(context);
			serviceReference = new WeakReference<>(service);
		}
		
		@Override
		public void run() {
			//Looping while there are requests in the queue
			ConnectionService service = null;
			FileUploadRequest request;
			requestLoop:
			while(!isInterrupted() &&
					(service = serviceReference.get()) != null &&
					(request = pushQueue(service)) != null) {
				//Clearing the reference to the service
				service = null;
				
				//Getting the callbacks
				FileUploadRequestCallbacks finalCallbacks = request.callbacks;
				
				//Telling the callbacks that the process has started
				handler.post(request.callbacks::onStart);
				
				//Checking if the request has no send file
				boolean copyFile = request.sendFile == null;
				if(copyFile) {
					//Checking if the URI is invalid
					if(request.sendUri == null) {
						//Calling the fail method
						handler.post(() -> finalCallbacks.onFail(messageSendInvalidContent));
						
						//Skipping the remainder of the iteration
						continue;
					}
					
					//Getting the context
					Context context = contextReference.get();
					if(context == null) {
						//Calling the fail method
						handler.post(() -> finalCallbacks.onFail(messageSendReferencesLost));
						
						//Skipping the remainder of the iteration
						continue;
					}
					
					//Verifying the file size
					try(Cursor cursor = context.getContentResolver().query(request.sendUri, null, null, null, null)) {
						if(cursor != null) {
							cursor.moveToFirst();
							long fileSize = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE));
							
							//Checking if the file size is too large to send
							if(fileSize > largestFileSize) {
								//Calling the fail method
								handler.post(() -> finalCallbacks.onFail(messageSendFileTooLarge));
								
								//Skipping the remainder of the iteration
								continue;
							}
						}
					}
					
					//Finding a valid file
					String fileName = Constants.getFileName(context, request.sendUri);
					if(fileName == null) fileName = Constants.defaultFileName;
					File targetFile = new File(Constants.findFreeFile(MainApplication.getUploadDirectory(context), Long.toString(System.currentTimeMillis())), fileName);
					//File targetFile = MainApplication.findUploadFileTarget(context, fileName);
					
					try {
						//Creating the targets
						if(!targetFile.getParentFile().mkdir()) throw new IOException("Couldn't make directory");
						//if(!targetFile.createNewFile()) throw new IOException();
					} catch(IOException exception) {
						//Printing the stack trace
						exception.printStackTrace();
						
						//Deleting the parent directory
						targetFile.getParentFile().delete();
						
						//Calling the fail method
						handler.post(() -> finalCallbacks.onFail(messageSendIOException));
						
						//Skipping the remainder of the iteration
						continue;
					}
					
					//Preparing to copy the file
					try(InputStream inputStream = context.getContentResolver().openInputStream(request.sendUri);
						OutputStream outputStream = new FileOutputStream(targetFile)) {
						//Clearing the reference to the context
						context = null;
						
						//Checking if the input stream is invalid
						if(inputStream == null) {
							//Calling the fail method
							handler.post(() -> finalCallbacks.onFail(messageSendInvalidContent));
							
							//Skipping the remainder of the iteration
							continue;
						}
						
						//Preparing to read the file
						long totalLength = inputStream.available();
						byte[] buffer = new byte[ConnectionService.attachmentChunkSize];
						int bytesRead;
						long totalBytesRead = 0;
						
						//Looping while there is data to read
						while((bytesRead = inputStream.read(buffer)) != -1) {
							//Writing the data to the output stream
							outputStream.write(buffer, 0, bytesRead);
							
							//Adding to the total bytes read
							totalBytesRead += bytesRead;
							
							//Updating the progress
							final long finalTotalBytesRead = totalBytesRead;
							handler.post(() -> finalCallbacks.onProgress((float) ((double) finalTotalBytesRead / (double) totalLength * copyProgressValue)));
						}
						
						//Flushing the output stream
						outputStream.flush();
						
						//Updating the database entry
						context = contextReference.get();
						if(context != null) DatabaseManager.getInstance().updateAttachmentFile(request.attachmentID, MainApplication.getInstance(), targetFile);
						context = null;
						
						//Setting the send file
						request.sendFile = targetFile;
						handler.post(() -> finalCallbacks.onCopyFinished(targetFile));
					} catch(IOException exception) {
						//Printing the stack trace
						exception.printStackTrace();
						
						//Deleting the target file
						targetFile.delete();
						targetFile.getParentFile().delete();
						
						//Calling the fail method
						handler.post(() -> finalCallbacks.onFail(messageSendIOException));
						
						//Clearing the reference to the context
						context = null;
						
						//Skipping the remainder of the iteration
						continue;
					}
				}
				
				//Checking if the file is invalid
				/* if(request.sendFile == null || !request.sendFile.exists()) {
					//Calling the fail method
					handler.post(() -> finalCallbacks.onFail(messageSendInvalidContent));
					
					//Skipping the remainder of the iteration
					continue;
				} */
				
				//Getting the connection service
				ConnectionService connectionService = ConnectionService.getInstance();
				
				//Checking if the service isn't ready
				if(connectionService == null || connectionService.getCurrentState() != stateConnected) {
					//Calling the fail method
					handler.post(() -> finalCallbacks.onFail(messageSendNetworkException));
					
					//Skipping the remainder of the iteration
					continue;
				}
				
				//Getting the request ID and the hash algorithm
				short requestID = connectionService.getNextRequestID();
				String hashAlgorithm = connectionService.currentConnectionManager.getHashAlgorithm();
				
				//Invalidating the connection service
				connectionService = null;
				
				//Getting the message digest
				MessageDigest messageDigest;
				try {
					messageDigest = MessageDigest.getInstance(hashAlgorithm);
				} catch(NoSuchAlgorithmException exception) {
					//Printing the stack trace
					exception.printStackTrace();
					
					//Calling the fail method
					handler.post(() -> finalCallbacks.onFail(messageSendIOException));
					
					//Skipping the remainder of the iteration
					continue;
				}
				
				//Setting up the streams
				try(FileInputStream srcIS = new FileInputStream(request.sendFile); DigestInputStream inputStream = new DigestInputStream(srcIS, messageDigest)) {
					//Preparing to read the file
					long totalLength = inputStream.available();
					byte[] buffer = new byte[ConnectionService.attachmentChunkSize];
					int bytesRead;
					long totalBytesRead = 0;
					int requestIndex = 0;
					
					//Checking if the file size is too large to send
					if(totalLength > largestFileSize) {
						//Calling the fail method
						handler.post(() -> finalCallbacks.onFail(messageSendFileTooLarge));
						
						//Skipping the remainder of the iteration
						continue;
					}
					
					//Looping while there is data to read
					while((bytesRead = inputStream.read(buffer)) != -1) {
						//Adding to the total bytes read
						totalBytesRead += bytesRead;
						
						//Compressing the data
						/* compressor = new Deflater();
						compressor.setInput(buffer, 0, bytesRead);
						compressor.finish();
						int compressedLen = compressor.deflate(compressedData);
						compressor.end();
						compressedData = Arrays.copyOf(compressedData, compressedLen); */
						
						//Getting the connection manager
						ConnectionManager connectionManager = getStaticConnectionManager();
						if(connectionManager == null) {
							//Failing the request
							handler.post(() -> finalCallbacks.onFail(messageSendNetworkException));
							return;
						}
						
						//Preparing the data for upload
						byte[] preparedData = connectionManager.getPackager().packageData(buffer, bytesRead);
						
						//Checking if the data couldn't be processed
						if(preparedData == null) {
							//Failing the request
							handler.post(() -> finalCallbacks.onFail(messageSendInternalException));
							
							//Breaking from the loop
							continue requestLoop;
						}
						
						//Uploading the chunk
						boolean uploadResult;
						if(request.conversationExists) {
							uploadResult = connectionManager.uploadFilePacket(requestID, requestIndex, request.conversationGUID, preparedData, request.sendFile.getName(), totalBytesRead >= totalLength);
						} else {
							uploadResult = connectionManager.uploadFilePacket(requestID, requestIndex, request.conversationMembers, preparedData, request.sendFile.getName(), request.conversationService, totalBytesRead >= totalLength);
						}
						
						//Validating the result
						if(!uploadResult) {
							//Failing the request
							handler.post(() -> finalCallbacks.onFail(messageSendInternalException));
							
							//Breaking from the loop
							continue requestLoop;
						}
						
						//Updating the progress
						final long finalTotalBytesRead = totalBytesRead;
						handler.post(() -> finalCallbacks.onProgress(copyFile ?
								(float) (copyProgressValue + (double) finalTotalBytesRead / (double) totalLength * (1F - copyProgressValue)) :
								(float) finalTotalBytesRead / (float) totalLength));
						
						//Adding to the request index
						requestIndex++;
					}
					
					//Getting the checksum
					byte[] checksum = messageDigest.digest();
					
					//Running on the main thread
					handler.post(() -> {
						//Getting the connection service
						ConnectionService newConnectionService = ConnectionService.getInstance();
						if(newConnectionService == null) {
							finalCallbacks.onFail(messageSendNetworkException);
							return;
						}
						
						//Notifying the callback listener
						finalCallbacks.onUploadFinished(checksum);
						
						//Creating the response manager
						ConnectionService.MessageResponseManager responseManager = new ConnectionService.MessageResponseManager() {
							//Forwarding the event to the callbacks
							@Override
							void onSuccess() {
								finalCallbacks.onResponseReceived();
							}
							
							@Override
							void onFail(byte resultCode) {
								finalCallbacks.onFail(resultCode);
							}
						};
						
						//Adding the request and starting the timer
						newConnectionService.messageSendRequests.put(requestID, responseManager);
						responseManager.startTimer();
					});
					
					//Saving the checksum
					DatabaseManager.getInstance().updateAttachmentChecksum(request.attachmentID, checksum);
				} catch(IOException | OutOfMemoryError exception) {
					//Printing the stack trace
					exception.printStackTrace();
					
					//Calling the fail method
					handler.post(() -> finalCallbacks.onFail(messageSendIOException));
					
					//Skipping the remainder of the iteration
					//continue;
				} catch(WebsocketNotConnectedException exception) {
					//Calling the fail method
					handler.post(() -> finalCallbacks.onFail(messageSendNetworkException));
					
					//Skipping the remainder of the iteration
					//continue;
				}
			}
			
			//Telling the service that the thread is finished
			if(service != null) service.fileUploadRequestThreadRunning.set(false);
		}
		
		private FileUploadRequest pushQueue(ConnectionService service) {
			if(service == null) return null;
			return service.fileUploadRequestQueue.poll();
		}
		
		/* private FileUploadRequest pushQueue() {
			//Getting the service
			ConnectionService connectionService = superclassReference.get();
			if(connectionService == null) return null;
			
			//Locking the queue
			synchronized(connectionService.fileUploadRequestQueue) {
				//Returning null if the queue is empty
				if(connectionService.fileUploadRequestQueue.isEmpty()) return null;
				
				//Removing the first item from the queue and returning it
				FileUploadRequest request = connectionService.fileUploadRequestQueue.get(0);
				connectionService.fileUploadRequestQueue.remove(0);
				return request;
			}
		} */
	}
	
	private static class MassRetrievalThread extends Thread {
		//Creating the reference values
		static final long startTimeout = 40 * 1000; //The timeout duration directly after requesting a mass retrieval - 40 seconds
		static final long intervalTimeout = 10 * 1000; //The timeout duration between message packets - 10 seconds
		
		private static final int stateWaiting = 0;
		private static final int stateRegistered = 1;
		private static final int stateDownloading = 2;
		private static final int stateFailed = 3;
		private static final int stateFinished = 4;
		
		//Creating the start information
		private int currentState = stateWaiting;
		private final WeakReference<Context> contextReference;
		private List<Blocks.ConversationInfo> conversationList;
		private int messageCount;
		private final AtomicInteger atomicMessageProgress = new AtomicInteger();
		
		//Creating the timer values
		private final Handler handler = new Handler();
		private final Runnable callbackFail;
		
		//Creating the packet values
		private int lastMessagePacketIndex = 0;
		private final BlockingQueue<MessagePacket> messagePacketQueue = new LinkedBlockingQueue<>();
		
		MassRetrievalThread(Context context) {
			//Establishing the references
			contextReference = new WeakReference<>(context);
			
			//Sending a start broadcast
			LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(localBCMassRetrieval).putExtra(Constants.intentParamState, intentExtraStateMassRetrievalStarted));
			
			//Starting the timeout timer
			callbackFail = () -> {
				Context newContext = contextReference.get();
				if(newContext != null) cancel(newContext);
			};
			handler.postDelayed(callbackFail, startTimeout);
			
			//Setting the state
			currentState = stateWaiting;
		}
		
		void registerInfo(Context context, List<Blocks.ConversationInfo> conversationList, int messageCount) {
			//Handling the state
			if(currentState != stateWaiting) return;
			currentState = stateRegistered;
			
			//Setting the information
			this.conversationList = conversationList;
			this.messageCount = messageCount;
			
			//Restarting the timeout timer with the packet delay
			handler.removeCallbacks(callbackFail);
			handler.postDelayed(callbackFail, intervalTimeout);
			
			//Sending a broadcast
			LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(localBCMassRetrieval).putExtra(Constants.intentParamState, intentExtraStateMassRetrievalProgress).putExtra(Constants.intentParamSize, messageCount));
			
			//Starting the thread
			start();
		}
		
		void addPacket(Context context, int index, List<Blocks.ConversationItem> itemList) {
			//Handling the state
			if(currentState != stateDownloading && currentState != stateRegistered) return;
			currentState = stateDownloading;
			
			//Checking if the indices don't line up or the list is invalid
			if(lastMessagePacketIndex + 1 != index || itemList == null) {
				//Cancelling the task
				cancel(context);
				
				//Returning
				return;
			}
			
			//Updating the counters
			lastMessagePacketIndex = index;
			
			//Restarting the timeout timer
			handler.removeCallbacks(callbackFail);
			handler.postDelayed(callbackFail, intervalTimeout);
			
			//Queueing the packet
			messagePacketQueue.add(new MessagePacket(itemList));
		}
		
		void finish() {
			//Handling the state
			if(currentState != stateDownloading && currentState != stateRegistered) return;
			currentState = stateFinished;
			
			//Stopping the timeout timer
			handler.removeCallbacks(callbackFail);
			
			//Queueing a finish flag packet (to use as a message that the process is finished)
			messagePacketQueue.add(new MessagePacket());
		}
		
		void cancel(Context context) {
			//Returning if there is no mass retrieval in progress
			if(!isInProgress()) return;
			
			//Setting the state
			currentState = stateFailed;
			
			//Stopping the timeout timer
			handler.removeCallbacks(callbackFail);
			
			//Sending a state broadcast
			LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(localBCMassRetrieval).putExtra(Constants.intentParamState, intentExtraStateMassRetrievalFailed));
			
			//Updating the state
			currentState = stateFailed;
			
			//Interrupting the thread if it is running
			interrupt();
		}
		
		@Override
		public void run() {
			//Writing the conversations to disk
			List<ConversationManager.ConversationInfo> conversationInfoList = new ArrayList<>();
			{
				Context context = contextReference.get();
				if(context == null) return;
				
				for(Blocks.ConversationInfo structConversation : conversationList) {
					conversationInfoList.add(DatabaseManager.getInstance().addReadyConversationInfo(context, structConversation));
				}
			}
			
			//Reading from the queue
			int messageCountReceived = 0;
			try {
				while(!isInterrupted()) {
					//Getting the list
					MessagePacket messagePacket = messagePacketQueue.take();
					
					//Checking if the packet is a finish flag
					if(messagePacket.isFinishFlag()) {
						//Sorting the conversations
						Collections.sort(conversationInfoList, ConversationManager.conversationComparator);
						
						//Running on the main thread
						handler.post(() -> {
							//Getting the context
							Context context = contextReference.get();
							if(context == null) return;
							
							//Setting the conversations in memory
							ArrayList<ConversationManager.ConversationInfo> sharedConversations = ConversationManager.getConversations();
							if(sharedConversations != null) {
								sharedConversations.clear();
								sharedConversations.addAll(conversationInfoList);
							}
							
							//Sending the mass retrieval broadcast
							LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(localBCMassRetrieval).putExtra(Constants.intentParamState, intentExtraStateMassRetrievalFinished));
							
							//Updating the conversation activity list
							LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ConversationsBase.localBCConversationUpdate));
							
							//Setting the current state
							currentState = stateFinished;
						});
						
						//Returning
						return;
					}
					
					//Getting the context
					Context context = contextReference.get();
					if(context == null) return;
					
					//Adding the messages
					for(Blocks.ConversationItem structItem : messagePacket.getList()) {
						//Cleaning the conversation item
						cleanConversationItem(structItem);
						
						//Finding the parent conversation
						ConversationManager.ConversationInfo parentConversation = null;
						for(ConversationManager.ConversationInfo conversationInfo : conversationInfoList) {
							if(!structItem.chatGuid.equals(conversationInfo.getGuid())) continue;
							parentConversation = conversationInfo;
						}
						if(parentConversation == null) continue;
						
						//Writing the item
						ConversationManager.ConversationItem conversationItem = DatabaseManager.getInstance().addConversationItem(structItem, parentConversation);
						if(conversationItem == null) continue;
						
						//Updating the parent conversation's last item
						if(parentConversation.getLastItem() == null || parentConversation.getLastItem().getDate() < conversationItem.getDate())
							parentConversation.setLastItem(conversationItem.toLightConversationItemSync(context));
					}
					
					//Updating the progress
					messageCountReceived += messagePacket.getList().size();
					atomicMessageProgress.set(messageCountReceived);
					LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(localBCMassRetrieval).putExtra(Constants.intentParamState, intentExtraStateMassRetrievalProgress).putExtra(Constants.intentParamProgress, messageCountReceived));
					
				}
			} catch(InterruptedException exception) {
				exception.printStackTrace();
			}
		}
		
		int getProgress() {
			return atomicMessageProgress.get();
		}
		
		int getProgressCount() {
			return messageCount;
		}
		
		boolean isInProgress() {
			return currentState == stateWaiting || currentState == stateRegistered || currentState == stateDownloading;
		}
		
		boolean isWaiting() {
			return currentState == stateWaiting;
		}
	}
	
	boolean sendMessage(String chatGUID, String message, MessageResponseManager responseListener) {
		//Checking if the client isn't ready
		if(getCurrentState() != stateConnected) {
			//Telling the response listener
			responseListener.onFail(messageSendNetworkException);
			
			//Returning false
			return false;
		}
		
		//Getting the request ID
		short requestID = getNextRequestID();
		
		//Validating the connection
		if(currentConnectionManager == null || currentConnectionManager.getState() != stateConnected) {
			responseListener.onFail(messageSendNetworkException);
			return false;
		}
		
		//Sending the message
		boolean result = currentConnectionManager.sendMessage(requestID, chatGUID, message);
		
		//Validating the result
		if(!result) {
			//Telling the response listener
			responseListener.onFail(messageSendIOException);
			
			//Returning false
			return false;
		}
		
		//Adding the request
		messageSendRequests.put(requestID, responseListener);
		
		//Starting the timer
		responseListener.startTimer();
		
		//Returning true
		return true;
	}
	
	boolean sendMessage(String[] chatRecipients, String message, String service, MessageResponseManager responseListener) {
		//Checking if the client isn't ready
		if(getCurrentState() != stateConnected) {
			//Telling the response listener
			responseListener.onFail(messageSendNetworkException);
			
			//Returning false
			return false;
		}
		
		//Getting the request ID
		short requestID = getNextRequestID();
		
		//Validating the connection
		if(currentConnectionManager == null || currentConnectionManager.getState() != stateConnected) {
			responseListener.onFail(messageSendNetworkException);
			return false;
		}
		
		//Sending the message
		boolean result = currentConnectionManager.sendMessage(requestID, chatRecipients, message, service);
		
		//Validating the result
		if(!result) {
			//Telling the response listener
			responseListener.onFail(messageSendIOException);
			
			//Returning false
			return false;
		}
		
		//Adding the request
		messageSendRequests.put(requestID, responseListener);
		
		//Starting the timer
		responseListener.startTimer();
		
		//Returning true
		return true;
	}
	
	boolean requestMassRetrieval() {
		//Returning false if the client isn't ready or a mass retrieval is already in progress
		if((massRetrievalThread != null && massRetrievalThread.isInProgress()) || getCurrentState() != stateConnected) return false;
		
		//Sending the request
		boolean result = currentConnectionManager.requestRetrievalAll();
		
		//Validating the result
		if(!result) return false;
		
		//Starting the mass retrieval manager thread thing
		massRetrievalThread = new MassRetrievalThread(getApplicationContext());
		
		//Sending the broadcast
		//LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(localBCMassRetrieval).putExtra(Constants.intentParamState, intentExtraStateMassRetrievalStarted));
		
		//Returning true
		return true;
	}
	
	private void cancelMassRetrieval() {
		//Forwarding the request
		if(massRetrievalThread != null) massRetrievalThread.cancel(this);
	}
	
	private static class MessagePacket {
		private final boolean isFinishFlag;
		private final List<Blocks.ConversationItem> messageList;
		
		MessagePacket() {
			isFinishFlag = true;
			messageList = null;
		}
		
		MessagePacket(List<Blocks.ConversationItem> messageList) {
			isFinishFlag = false;
			this.messageList = messageList;
		}
		
		boolean isFinishFlag() {
			return isFinishFlag;
		}
		
		List<Blocks.ConversationItem> getList() {
			return messageList;
		}
	}
	
	/* boolean sendFile(short requestID, int requestIndex, String chatGUID, byte[] fileBytes, String fileName, boolean isLast, MessageResponseManager responseListener) {
		//Returning false if the client isn't ready
		if(wsClient == null || !wsClient.isOpen()) return false;
		
		//Checking if a response listener has been provided
		if(responseListener != null) {
			//Getting if a matching listener exists in the list
			boolean hasListener = false;
			for(int i = 0; i < messageSendRequests.size(); i++)
				if(messageSendRequests.valueAt(i) == responseListener) {
					hasListener = true;
					break;
				}
			
			//Adding the listener to list if it isn't already there
			if(!hasListener) messageSendRequests.put(requestID, responseListener);
		}
		
		//Preparing to serialize the request
		try(ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
			//Adding the data
			out.writeByte(SharedValues.wsFrameSendFileExisting); //Message type - send existing file
			out.writeLong(requestTime); //Request time
			out.writeUTF(chatGUID); //Chat GUID
			out.writeInt(requestIndex); //Request index
			out.writeObject(fileBytes); //File bytes
			out.reset();
			if(requestIndex == 0) out.writeUTF(fileName);
			out.writeBoolean(isLast); //Is last message
			out.flush();
			
			//Sending the message
			wsClient.send(bos.toByteArray());
		} catch(IOException exception) {
			//Printing the stack trace
			exception.printStackTrace();
			
			//Returning false
			return false;
		}
		
		//Starting the timer if this is the last message
		if(isLast) {
			//Getting the response listener
			if(responseListener == null)
				responseListener = fileSendRequests.get(requestTime);
			
			//Removing it from the transfer list
			fileSendRequests.remove(requestTime);
			
			//Adding the request
			messageSendRequests.put(requestTime, responseListener);
			
			//Starting the timer
			responseListener.startTimer();
		}
		
		//Returning true
		return true;
	}
	
	boolean sendFile(long requestTime, int requestIndex, String[] chatRecipients, String service, byte[] fileBytes, String fileName, boolean isLast, MessageResponseManager responseListener) {
		//Returning false if the client isn't ready
		if(wsClient == null || !wsClient.isOpen()) return false;
		
		//Checking if a response listener has been provided
		if(responseListener != null) {
			//Getting if a matching listener exists in the list
			boolean hasListener = false;
			for(int i = 0; i < fileSendRequests.size(); i++)
				if(fileSendRequests.valueAt(i) == responseListener) {
					hasListener = true;
					break;
				}
			
			//Adding the listener to list if it isn't already there
			if(!hasListener) fileSendRequests.put(requestTime, responseListener);
		}
		
		//Preparing to serialize the request
		try(ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
			//Adding the data
			out.writeByte(SharedValues.wsFrameSendFileNew); //Message type - send new file
			out.writeLong(requestTime); //Request time
			out.writeObject(chatRecipients); //Chat recipients
			out.writeInt(requestIndex); //Request index
			out.writeObject(fileBytes); //File bytes
			out.reset();
			if(requestIndex == 0) {
				out.writeUTF(fileName); //File name
				out.writeUTF(service); //Service
			}
			out.writeBoolean(isLast); //Is last message
			out.flush();
			
			//Sending the message
			wsClient.send(bos.toByteArray());
		} catch(IOException exception) {
			//Printing the stack trace
			exception.printStackTrace();
			
			//Returning false
			return false;
		}
		
		//Starting the timer if this is the last message
		if(isLast) {
			//Getting the response listener
			if(responseListener == null)
				responseListener = fileSendRequests.get(requestTime);
			
			//Removing it from the transfer list
			fileSendRequests.remove(requestTime);
			
			//Adding the request
			fileSendRequests.put(requestTime, responseListener);
			
			//Starting the timer
			responseListener.startTimer();
		}
		
		//Returning true
		return true;
	} */
	
	private void schedulePing() {
		//Scheduling the ping
		((AlarmManager) getSystemService(ALARM_SERVICE)).setWindow(AlarmManager.ELAPSED_REALTIME_WAKEUP,
				SystemClock.elapsedRealtime() + keepAliveMillis - keepAliveWindowMillis,
				keepAliveWindowMillis * 2,
				pingPendingIntent);
	}
	
	/* private void unschedulePing() {
		//Cancelling the timer
		((AlarmManager) getSystemService(ALARM_SERVICE)).cancel(pingPendingIntent);
	} */
	
	//TODO use when API 23 is obsolete
	/* private static class ScheduledPingAlarm extends AlarmManager.OnAlarmListener {
		
		@Override
		public void onAlarm() {
		
		}
	} */
	
	private boolean retrieveMessagesSince(long timeLower, long timeUpper) {
		//Returning false if the connection isn't ready
		if(getCurrentState() != stateConnected) return false;
		
		//Sending the request
		return currentConnectionManager.requestRetrievalTime(timeLower, timeUpper);
	}
	
	short getNextRequestID() {
		return ++currentRequestID;
	}
	
	static abstract class MessageResponseManager {
		abstract void onSuccess();
		
		abstract void onFail(byte resultCode);
		
		private static final long timeoutDelay = 20 * 1000; //20-second delay
		private final Handler handler = new Handler(Looper.getMainLooper());
		private final Runnable timeoutRunnable = () -> {
			//Calling the fail method
			onFail(messageSendRequestExpired);
			
			//Getting the connection service
			ConnectionService connectionService = getInstance();
			if(connectionService == null) return;
			
			//Removing the item
			for(int i = 0; i < connectionService.messageSendRequests.size(); i++) {
				if(!connectionService.messageSendRequests.valueAt(i).equals(MessageResponseManager.this))
					continue;
				connectionService.messageSendRequests.removeAt(i);
				break;
			}
		};
		
		void startTimer() {
			handler.postDelayed(timeoutRunnable, timeoutDelay);
		}
		
		void stopTimer(boolean restart) {
			handler.removeCallbacks(timeoutRunnable);
			if(restart) handler.postDelayed(timeoutRunnable, timeoutDelay);
		}
	}
	
	private ArrayList<String> structConversationItemsToUsers(List<SharedValues.ConversationItem> structConversationItems) {
		//Creating the users list
		ArrayList<String> users = new ArrayList<>();
		
		//Iterating over the struct conversation items
		for(SharedValues.ConversationItem structConversationItem : structConversationItems) {
			//Getting the users
			String[] usersInStruct;
			if(structConversationItem instanceof SharedValues.MessageInfo)
				usersInStruct = new String[]{((SharedValues.MessageInfo) structConversationItem).sender};
			else if(structConversationItem instanceof SharedValues.GroupActionInfo)
				usersInStruct = new String[]{((SharedValues.GroupActionInfo) structConversationItem).agent, ((SharedValues.GroupActionInfo) structConversationItem).other};
			else if(structConversationItem instanceof SharedValues.ChatRenameActionInfo)
				usersInStruct = new String[]{((SharedValues.ChatRenameActionInfo) structConversationItem).agent};
			else continue;
			
			//Adding the user to the list if they're valid
			for(String user : usersInStruct) if(user != null) users.add(user);
		}
		
		//Returning the users list
		return users;
	}
	
	/* private void loadUsersSync(List<SharedValues.ConversationItem> structConversationItems) {
		//Loading the users
		for(String user : structConversationItemsToUsers(structConversationItems))
			UserCacheHelper.loadUser(ConnectionService.this, user);
	} */
	
	public static class ServiceStartBoot extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			//Returning if the service is not a boot service
			if(!"android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) return;
			
			//Starting the service
			Intent serviceIntent = new Intent(context, ConnectionService.class);
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(serviceIntent);
			else context.startService(serviceIntent);
		}
	}
	
	private static class TransferConversationStruct {
		final String guid;
		final ConversationManager.ConversationInfo.ConversationState state;
		final String name;
		final List<ConversationManager.ConversationItem> conversationItems;
		
		TransferConversationStruct(String guid, ConversationManager.ConversationInfo.ConversationState state, String name, List<ConversationManager.ConversationItem> conversationItems) {
			this.guid = guid;
			this.state = state;
			this.name = name;
			this.conversationItems = conversationItems;
		}
	}
	
	private static class ConversationInfoRequest {
		final ConversationManager.ConversationInfo conversationInfo;
		final boolean sendNotifications;
		
		ConversationInfoRequest(ConversationManager.ConversationInfo conversationInfo, boolean sendNotifications) {
			this.conversationInfo = conversationInfo;
			this.sendNotifications = sendNotifications;
		}
	}
	
	private static ConversationManager.ConversationInfo findConversationInMemory(long localID) {
		//Checking if the conversations are loaded in memory
		ArrayList<ConversationManager.ConversationInfo> conversations = ConversationManager.getConversations();
		if(conversations == null) return null;
		
		//Returning the first matching conversation
		for(ConversationManager.ConversationInfo conversation : conversations) {
			if(conversation.getLocalID() != localID) continue;
			return conversation;
		}
		
		//Returning null
		return null;
	}
	
	private static class FetchConversationRequests extends AsyncTask<Void, Void, List<ConversationManager.ConversationInfo>> {
		private final WeakReference<ConnectionService> serviceReference;
		
		FetchConversationRequests(ConnectionService serviceInstance) {
			//Setting the context reference
			serviceReference = new WeakReference<>(serviceInstance);
		}
		
		@Override
		protected List<ConversationManager.ConversationInfo> doInBackground(Void... parameters) {
			Context context = serviceReference.get();
			if(context == null) return null;
			
			//Fetching the incomplete conversations
			return DatabaseManager.getInstance().fetchConversationsWithState(context, ConversationManager.ConversationInfo.ConversationState.INCOMPLETE_SERVER);
		}
		
		@Override
		protected void onPostExecute(List<ConversationManager.ConversationInfo> conversations) {
			//Getting the service instance
			ConnectionService service = serviceReference.get();
			if(service == null) return;
			
			//Copying the conversations to the pending list
			synchronized(service.pendingConversations) {
				for(ConversationManager.ConversationInfo conversation : conversations)
					service.pendingConversations.add(new ConversationInfoRequest(conversation, true));
			}
			
			//Requesting a conversation info fetch
			service.retrievePendingConversationInfo();
			
		}
	}
	
	private static class MessageUpdateAsyncTask extends QueueTask<Void, Void> {
		//Creating the reference values
		private final WeakReference<ConnectionService> serviceReference;
		private final WeakReference<Context> contextReference;
		
		//Creating the request values
		private final List<Blocks.ConversationItem> structConversationItems;
		private final boolean sendNotifications;
		
		//Creating the conversation lists
		private final List<ConversationManager.ConversationItem> newCompleteConversationItems = new ArrayList<>();
		private final List<ConversationManager.ConversationInfo> completeConversations = new ArrayList<>();
		
		//Creating the caches
		private ArrayList<Long> loadedConversationsCache;
		
		MessageUpdateAsyncTask(ConnectionService serviceInstance, Context context, List<Blocks.ConversationItem> structConversationItems, boolean sendNotifications) {
			//Setting the references
			serviceReference = new WeakReference<>(serviceInstance);
			contextReference = new WeakReference<>(context);
			
			//Setting the values
			this.structConversationItems = structConversationItems;
			this.sendNotifications = sendNotifications;
			
			//Getting the caches
			loadedConversationsCache = new ArrayList<>(Messaging.getLoadedConversations());
		}
		
		@Override
		protected Void doInBackground() {
			//Getting the context
			Context context = contextReference.get();
			if(context == null) return null;
			
			//Iterating over the conversations from the received messages
			Collections.sort(structConversationItems, (value1, value2) -> Long.compare(value1.date, value2.date));
			List<String> processedConversations = new ArrayList<>();
			List<ConversationManager.ConversationInfo> incompleteServerConversations = new ArrayList<>();
			for(Blocks.ConversationItem conversationItemStruct : structConversationItems) {
				//Cleaning the conversation item
				cleanConversationItem(conversationItemStruct);
				
				//Creating the parent conversation variable
				ConversationManager.ConversationInfo parentConversation = null;
				
				//Checking if the message is contained in a pending conversation
				{
					ConnectionService service = serviceReference.get();
					if(service != null) {
						synchronized(service.pendingConversations) {
							for(ConversationInfoRequest request : service.pendingConversations)
								if(conversationItemStruct.chatGuid.equals(request.conversationInfo.getGuid())) {
									parentConversation = request.conversationInfo;
									break;
								}
						}
					}
				}
				
				//Otherwise checking if the conversation is contained in a current conversation
				if(parentConversation == null)
					for(ConversationManager.ConversationInfo conversationInfo : completeConversations)
						if(conversationItemStruct.chatGuid.equals(conversationInfo.getGuid())) {
							parentConversation = conversationInfo;
							break;
						}
				if(parentConversation == null)
					for(ConversationManager.ConversationInfo conversationInfo : incompleteServerConversations)
						if(conversationItemStruct.chatGuid.equals(conversationInfo.getGuid())) {
							parentConversation = conversationInfo;
							break;
						}
				
				//Otherwise retrieving / creating the conversation from the database
				if(parentConversation == null) {
					parentConversation = DatabaseManager.getInstance().addRetrieveServerCreatedConversationInfo(context, conversationItemStruct.chatGuid);
					//Skipping the remainder of the iteration if the conversation is still invalid (a database error occurred)
					if(parentConversation == null) continue;
				}
				
				//Checking if the conversation hasn't yet been processed
				if(!processedConversations.contains(conversationItemStruct.chatGuid)) {
					//Marking the conversation as processed
					processedConversations.add(conversationItemStruct.chatGuid);
					
					//Fetching the conversation info
					//ConversationManager.ConversationInfo conversationInfo = DatabaseManager.fetchConversationInfo(ConnectionService.this, writableDatabase, conversationItemStruct.chatGuid);
					//if(conversationInfo == null) continue;
					
					//Sorting the conversation
					if(parentConversation.getState() == ConversationManager.ConversationInfo.ConversationState.READY) completeConversations.add(parentConversation);
					else if(parentConversation.getState() == ConversationManager.ConversationInfo.ConversationState.INCOMPLETE_SERVER) incompleteServerConversations.add(parentConversation);
				}
				
				//Adding the conversation item to the database
				ConversationManager.ConversationItem conversationItem = DatabaseManager.getInstance().addConversationItemReplaceGhost(conversationItemStruct, parentConversation);
				
				//Skipping the remainder of the iteration if the conversation item is invalid
				if(conversationItem == null) continue;
				
				//Checking the conversation item's influence
				if(conversationItem instanceof ConversationManager.GroupActionInfo) {
					//Converting the item to a group action info
					ConversationManager.GroupActionInfo groupActionInfo = (ConversationManager.GroupActionInfo) conversationItem;
					
					//Adding or removing the member on disk
					if(groupActionInfo.actionType == Constants.groupActionInvite) {
						DatabaseManager.getInstance().addConversationMember(parentConversation.getLocalID(), groupActionInfo.other, groupActionInfo.color = parentConversation.getNextUserColor());
					} else if(groupActionInfo.actionType == Constants.groupActionLeave) DatabaseManager.getInstance().removeConversationMember(parentConversation.getLocalID(), groupActionInfo.other);
				} else if(conversationItem instanceof ConversationManager.ChatRenameActionInfo) {
					//Writing the new title to the database
					DatabaseManager.getInstance().updateConversationTitle(((ConversationManager.ChatRenameActionInfo) conversationItem).title, parentConversation.getLocalID());
				}
				
				//Checking if the conversation is complete
				if(parentConversation.getState() == ConversationManager.ConversationInfo.ConversationState.READY) {
					//Recording the conversation item
					newCompleteConversationItems.add(conversationItem);
					
					//Incrementing the unread count
					if(!loadedConversationsCache.contains(parentConversation.getLocalID()) && (conversationItem instanceof ConversationManager.MessageInfo && !((ConversationManager.MessageInfo) conversationItem).isOutgoing())) DatabaseManager.getInstance().incrementUnreadMessageCount(parentConversation.getLocalID());
				}
				//Otherwise updating the last conversation item
				else if(parentConversation.getLastItem() == null || parentConversation.getLastItem().getDate() < conversationItem.getDate())
					parentConversation.setLastItem(conversationItem.toLightConversationItemSync(context));
			}
			
			{
				ConnectionService service = serviceReference.get();
				if(service != null) {
					//Checking if there are incomplete conversations
					if(!incompleteServerConversations.isEmpty()) {
						//Adding the incomplete conversations to the pending conversations
						synchronized(service.pendingConversations) {
							for(ConversationManager.ConversationInfo conversation : incompleteServerConversations)
								service.pendingConversations.add(new ConversationInfoRequest(conversation, sendNotifications));
						}
					}
				}
			}
			
			//Returning
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			//Getting the context
			Context context = contextReference.get();
			if(context == null) return;
			
			//Sorting the complete conversation items
			Collections.sort(newCompleteConversationItems, ConversationManager.conversationItemComparator);
			
			//Getting the loaded conversations
			//List<Long> foregroundConversations = Messaging.getForegroundConversations();
			List<Long> loadedConversations = Messaging.getLoadedConversations();
			
			//Checking if the conversations are loaded in memory
			ArrayList<ConversationManager.ConversationInfo> conversations = ConversationManager.getConversations();
			if(conversations != null) {
				//Sorting the conversation items by conversation
				LongSparseArray<List<ConversationManager.ConversationItem>> newCompleteConversationGroups = new LongSparseArray<>();
				for(ConversationManager.ConversationItem conversationItem : newCompleteConversationItems) {
					List<ConversationManager.ConversationItem> list = newCompleteConversationGroups.get(conversationItem.getConversationInfo().getLocalID());
					if(list == null) {
						list = new ArrayList<>();
						list.add(conversationItem);
						newCompleteConversationGroups.put(conversationItem.getConversationInfo().getLocalID(), list);
					} else list.add(conversationItem);
				}
				
				//Iterating over the conversation groups
				for(int i = 0; i < newCompleteConversationGroups.size(); i++) {
					//Attempting to find the associated parent conversation
					ConversationManager.ConversationInfo parentConversation = findConversationInMemory(newCompleteConversationGroups.keyAt(i));
					
					//Skipping the remainder of the iteration if no parent conversation could be found
					if(parentConversation == null) continue;
					
					//Getting the conversation items
					List<ConversationManager.ConversationItem> conversationItems = newCompleteConversationGroups.valueAt(i);
					
					//Adding the conversation items if the conversation is loaded
					//if(loadedConversations.contains(parentConversation.getLocalID()))
					
					//Add items no matter if the conversation is loaded (will simply check if conversation items are still available in memory)
					{
						boolean addItemResult = parentConversation.addConversationItems(context, conversationItems);
						//Setting the last item if the conversation items couldn't be added
						if(!addItemResult) parentConversation.setLastItemUpdate(context, conversationItems.get(conversationItems.size() - 1));
					}
					
					//Iterating over the conversation items
					for(ConversationManager.ConversationItem conversationItem : conversationItems) {
						//Setting the conversation item's parent conversation to the found one (the one provided from the DB is not the same as the one in memory)
						conversationItem.setConversationInfo(parentConversation);
						
						//if(parentConversation.getState() != ConversationManager.ConversationInfo.ConversationState.READY) continue;
						
						//Incrementing the conversation's unread count
						if(conversationItem instanceof ConversationManager.MessageInfo && !((ConversationManager.MessageInfo) conversationItem).isOutgoing()) parentConversation.setUnreadMessageCount(parentConversation.getUnreadMessageCount() + 1);
						parentConversation.updateUnreadStatus(context);
						
						//Renaming the conversation
						if(conversationItem instanceof ConversationManager.ChatRenameActionInfo) parentConversation.setTitle(context, ((ConversationManager.ChatRenameActionInfo) conversationItem).title);
						else if(conversationItem instanceof ConversationManager.GroupActionInfo) {
							//Converting the item to a group action info
							ConversationManager.GroupActionInfo groupActionInfo = (ConversationManager.GroupActionInfo) conversationItem;
							
							//Finding the conversation member
							ConversationManager.MemberInfo member = parentConversation.findConversationMember(groupActionInfo.other);
							
							if(groupActionInfo.actionType == Constants.groupActionInvite) {
								//Adding the member in memory
								if(member == null) {
									member = new ConversationManager.MemberInfo(groupActionInfo.other, groupActionInfo.color);
									parentConversation.getConversationMembers().add(member);
								}
							} else if(groupActionInfo.actionType == Constants.groupActionLeave) {
								//Removing the member in memory
								if(member != null && parentConversation.getConversationMembers().contains(member))
									parentConversation.getConversationMembers().remove(member);
							}
						}
					}
				}
			}
			
			for(ConversationManager.ConversationItem conversationItem : newCompleteConversationItems) {
				//Sending notifications
				if(conversationItem instanceof ConversationManager.MessageInfo) NotificationUtils.sendNotification(context, (ConversationManager.MessageInfo) conversationItem);
				
				//Downloading the items automatically (if requested)
				if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preference_storage_autodownload_key), false) && conversationItem instanceof ConversationManager.MessageInfo) {
					for(ConversationManager.AttachmentInfo attachmentInfo : ((ConversationManager.MessageInfo) conversationItem).getAttachments())
						attachmentInfo.downloadContent(context);
				}
			}
			
			//Re-inserting the modified conversations
			for(ConversationManager.ConversationInfo conversationInfo : completeConversations) {
				conversationInfo = findConversationInMemory(conversationInfo.getLocalID());
				if(conversationInfo != null) ConversationManager.sortConversation(conversationInfo);
			}
			
			//Updating the conversation activity list
			LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ConversationsBase.localBCConversationUpdate));
			/* for(Conversations.ConversationsCallbacks callbacks : MainApplication.getConversationsActivityCallbacks())
				callbacks.updateList(true); */
			
			//Contacting the server for the pending conversations
			ConnectionService service = serviceReference.get();
			if(service != null && !service.pendingConversations.isEmpty())
				service.retrievePendingConversationInfo();
		}
	}
	
	/* private static class MassRetrievalAsyncTask extends QueueTask<Integer, List<ConversationManager.ConversationInfo>> {
		//Creating the task values
		private final WeakReference<ConnectionService> serviceReference;
		private final WeakReference<Context> contextReference;
		
		private final List<SharedValues.ConversationItem> structConversationItems;
		private final List<SharedValues.ConversationInfo> structConversationInfos;
		
		MassRetrievalAsyncTask(ConnectionService serviceInstance, Context context, List<SharedValues.ConversationItem> structConversationItems, List<SharedValues.ConversationInfo> structConversationInfos) {
			//Setting the references
			serviceReference = new WeakReference<>(serviceInstance);
			contextReference = new WeakReference<>(context);
			
			//Setting the values
			this.structConversationItems = structConversationItems;
			this.structConversationInfos = structConversationInfos;
		}
		
		@Override
		protected List<ConversationManager.ConversationInfo> doInBackground() {
			//Getting the context
			Context context = contextReference.get();
			if(context == null) return null;
			
			//Creating the conversation list
			List<ConversationManager.ConversationInfo> conversationInfoList = new ArrayList<>();
			
			//Creating the progress value
			int progress = 0;
			
			//Iterating over the conversations
			for(SharedValues.ConversationInfo structConversation : structConversationInfos) {
				//Writing the conversation
				conversationInfoList.add(DatabaseManager.getInstance().addReadyConversationInfo(context, structConversation));
				
				//Publishing the progress
				publishProgress(++progress);
			}
			
			//Adding the messages
			for(SharedValues.ConversationItem structItem : structConversationItems) {
				//Cleaning the conversation item
				cleanConversationItem(structItem);
				
				//Finding the parent conversation
				ConversationManager.ConversationInfo parentConversation = null;
				for(ConversationManager.ConversationInfo conversationInfo : conversationInfoList) {
					if(!structItem.chatGuid.equals(conversationInfo.getGuid())) continue;
					parentConversation = conversationInfo;
				}
				if(parentConversation == null) continue;
				
				//Writing the item
				ConversationManager.ConversationItem conversationItem = DatabaseManager.getInstance().addConversationItem(structItem, parentConversation);
				if(conversationItem == null) continue;
				
				//Updating the parent conversation's last item
				if(parentConversation.getLastItem() == null || parentConversation.getLastItem().getDate() < conversationItem.getDate())
					parentConversation.setLastItem(conversationItem.toLightConversationItemSync(context));
				
				//Publishing the progress
				publishProgress(++progress);
			}
			
			//Sorting the conversations
			Collections.sort(conversationInfoList, ConversationManager.conversationComparator);
			
			//Returning the list
			return conversationInfoList;
		}
		
		@Override
		protected void onProgressUpdate(Integer progress) {
			//Getting the service
			ConnectionService service = serviceReference.get();
			if(service == null) return;
			
			//Updating the progress in the service
			service.massRetrievalProgress = progress;
			
			//Sending a broadcast
			LocalBroadcastManager.getInstance(service).sendBroadcast(new Intent(localBCMassRetrieval).putExtra(Constants.intentParamState, intentExtraStateMassRetrievalProgress).putExtra(Constants.intentParamProgress, service.massRetrievalProgress));
		}
		
		@Override
		protected void onPostExecute(List<ConversationManager.ConversationInfo> conversations) {
			//Getting the context
			Context context = contextReference.get();
			if(context == null) return;
			
			//Sending a broadcast
			LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(localBCMassRetrieval).putExtra(Constants.intentParamState, intentExtraStateMassRetrievalFinished));
			
			//Setting the conversations in memory
			ArrayList<ConversationManager.ConversationInfo> sharedConversations = ConversationManager.getConversations();
			if(sharedConversations != null) {
				sharedConversations.clear();
				sharedConversations.addAll(conversations);
			}
			
			//Updating the service
			ConnectionService service = serviceReference.get();
			if(service != null) service.massRetrievalInProgress = false;
			
			//Updating the conversation activity list
			LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ConversationsBase.localBCConversationUpdate));
			//for(Conversations.ConversationsCallbacks callbacks : MainApplication.getConversationsActivityCallbacks()) callbacks.updateList(false);
		}
	} */
	
	private static class SaveConversationInfoAsyncTask extends QueueTask<Void, Void> {
		private final WeakReference<Context> contextReference;
		private final List<ConversationManager.ConversationInfo> unavailableConversations;
		private final List<ConversationInfoRequest> availableConversations;
		private final LongSparseArray<List<ConversationManager.ConversationItem>> availableConversationItems = new LongSparseArray<>();
		private final HashMap<ConversationManager.ConversationInfo, TransferConversationStruct> transferredConversations = new HashMap<>();
		
		SaveConversationInfoAsyncTask(Context context, List<ConversationManager.ConversationInfo> unavailableConversations, List<ConversationInfoRequest> availableConversations) {
			//Setting the references
			contextReference = new WeakReference<>(context);
			
			//Setting the lists
			this.unavailableConversations = unavailableConversations;
			this.availableConversations = availableConversations;
		}
		
		@Override
		protected Void doInBackground() {
			//Getting the context
			Context context = contextReference.get();
			
			//Returning if the context is invalid
			if(context == null) return null;
			
			//Removing the unavailable conversations from the database
			for(ConversationManager.ConversationInfo conversation : unavailableConversations) DatabaseManager.getInstance().deleteConversation(conversation);
			
			//Checking if there are any available conversations
			if(!availableConversations.isEmpty()) {
				//Iterating over the conversations
				for(Iterator<ConversationInfoRequest> iterator = availableConversations.iterator(); iterator.hasNext(); ) {
					//Getting the conversation
					ConversationManager.ConversationInfo availableConversation = iterator.next().conversationInfo;
					
					//Reading and recording the conversation's items
					List<ConversationManager.ConversationItem> conversationItems = DatabaseManager.getInstance().loadConversationItems(availableConversation);
					availableConversationItems.put(availableConversation.getLocalID(), conversationItems);
					
					//Searching for a matching conversation in the database
					ConversationManager.ConversationInfo clientConversation = DatabaseManager.getInstance().findConversationInfoWithMembers(context, Constants.normalizeAddresses(availableConversation.getConversationMembersAsCollection()), availableConversation.getService(), true);
					
					//Checking if a client conversation has been found
					if(clientConversation != null) {
						//Switching the conversation item ownership to the new client conversation
						DatabaseManager.getInstance().switchMessageOwnership(availableConversation.getLocalID(), clientConversation.getLocalID());
						for(ConversationManager.ConversationItem item : conversationItems) item.setConversationInfo(clientConversation);
						
						//Recording the conversation details
						transferredConversations.put(clientConversation, new TransferConversationStruct(availableConversation.getGuid(),
								ConversationManager.ConversationInfo.ConversationState.READY,
								availableConversation.getStaticTitle(),
								conversationItems));
						
						//Deleting the available conversation
						DatabaseManager.getInstance().deleteConversation(availableConversation);
						unavailableConversations.add(availableConversation);
						
						//Updating the client conversation
						DatabaseManager.getInstance().copyConversationInfo(availableConversation, clientConversation, false);
						
						//Marking the the available conversation as invalid (to be deleted)
						iterator.remove();
					} else {
						//Updating the available conversation
						DatabaseManager.getInstance().updateConversationInfo(availableConversation, true);
					}
				}
			}
			
			//Loading the conversation members from the device's contacts
			/* for(ConversationInfoRequest conversationRequest : availableConversations)
				for(String user : conversationRequest.conversationInfo.getConversationMembersAsArray())
					UserCacheHelper.loadUser(context, user); */
			
			//Returning
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			//Getting the context
			Context context = contextReference.get();
			
			//Sorting the available conversations
			Collections.sort(availableConversations, (request1, request2) -> {
				long date1 = request1.conversationInfo.getLastItem() == null ? Long.MIN_VALUE : request1.conversationInfo.getLastItem().getDate();
				long date2 = request2.conversationInfo.getLastItem() == null ? Long.MIN_VALUE : request2.conversationInfo.getLastItem().getDate();
				return Long.compare(date1, date2);
			});
			
			//Sending notifications
			if(context != null)
				for(ConversationInfoRequest conversationInfoRequest : availableConversations)
					if(conversationInfoRequest.sendNotifications)
						for(ConversationManager.ConversationItem conversationItem : availableConversationItems.get(conversationInfoRequest.conversationInfo.getLocalID()))
							if(conversationItem instanceof ConversationManager.MessageInfo)
								NotificationUtils.sendNotification(context, (ConversationManager.MessageInfo) conversationItem);
			
			//Checking if the conversations are available in memory
			ArrayList<ConversationManager.ConversationInfo> conversations = ConversationManager.getConversations();
			if(conversations != null) {
				//Removing the unavailable conversations from memory
				for(ConversationManager.ConversationInfo unavailableConversation : unavailableConversations) {
					for(Iterator<ConversationManager.ConversationInfo> iterator = conversations.iterator(); iterator.hasNext(); ) {
						if(unavailableConversation.getGuid().equals(iterator.next().getGuid())) {
							iterator.remove();
							break;
						}
					}
				}
				
				//Iterating over the available conversations
				for(ConversationInfoRequest conversationInfoRequest : availableConversations) {
					//Adding the available conversations in memory
					ConversationManager.addConversation(conversationInfoRequest.conversationInfo);
					
					//Adding the unread messages
					conversationInfoRequest.conversationInfo.setUnreadMessageCount(availableConversationItems.get(conversationInfoRequest.conversationInfo.getLocalID()).size());
					conversationInfoRequest.conversationInfo.updateUnreadStatus(context);
					//availableConversation.updateView(ConnectionService.this);
				}
				
				//Updating the transferred conversations
				if(context != null) {
					for(Map.Entry<ConversationManager.ConversationInfo, TransferConversationStruct> pair : transferredConversations.entrySet()) {
						//Retrieving the pair values
						ConversationManager.ConversationInfo conversationInfo = findConversationInMemory(pair.getKey().getLocalID());
						if(conversationInfo == null) continue;
						
						TransferConversationStruct transferData = pair.getValue();
						conversationInfo.setGuid(transferData.guid);
						conversationInfo.setState(transferData.state);
						conversationInfo.setTitle(context, transferData.name);
						if(Messaging.getLoadedConversations().contains(conversationInfo.getLocalID())) conversationInfo.addConversationItems(context, transferData.conversationItems);
						//conversationInfo.setUnreadMessageCount(conversationInfo.getUnreadMessageCount() + transferData.conversationItems.size());
						//conversationInfo.updateUnreadStatus();
					}
				}
			}
			
			//Updating the conversation activity list
			if(context != null) LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ConversationsBase.localBCConversationUpdate));
			/* for(Conversations.ConversationsCallbacks callbacks : MainApplication.getConversationsActivityCallbacks())
				callbacks.updateList(true); */
			
			//Updating the user views
			/* if(context != null)
				for(ConversationInfoRequest conversationInfoRequest : availableConversations)
					conversationInfoRequest.conversationInfo.updateViewUser(context); */
		}
	}
	
	private static class ModifierUpdateAsyncTask extends QueueTask<Void, Void> {
		//Creating the reference values
		private final WeakReference<Context> contextReference;
		
		//Creating the request values
		private final List<Blocks.ModifierInfo> structModifiers;
		private final List<ConversationManager.StickerInfo> stickerModifiers = new ArrayList<>();
		private final List<ConversationManager.TapbackInfo> tapbackModifiers = new ArrayList<>();
		private final List<TapbackRemovalStruct> tapbackRemovals = new ArrayList<>();
		
		private final Packager packager;
		
		ModifierUpdateAsyncTask(Context context, List<Blocks.ModifierInfo> structModifiers, Packager packager) {
			contextReference = new WeakReference<>(context);
			
			this.structModifiers = structModifiers;
			
			this.packager = packager;
		}
		
		@Override
		protected Void doInBackground() {
			//Getting the context
			Context context = contextReference.get();
			if(context == null) return null;
			
			//Iterating over the modifiers
			for(Blocks.ModifierInfo modifierInfo : structModifiers) {
				//Checking if the modifier is an activity status modifier
				if(modifierInfo instanceof Blocks.ActivityStatusModifierInfo) {
					//Casting to the activity status modifier
					Blocks.ActivityStatusModifierInfo activityStatusModifierInfo = (Blocks.ActivityStatusModifierInfo) modifierInfo;
					
					//Updating the modifier in the database
					DatabaseManager.getInstance().updateMessageState(activityStatusModifierInfo.message, activityStatusModifierInfo.state, activityStatusModifierInfo.dateRead);
				}
				//Otherwise checking if the modifier is a sticker update
				else if(modifierInfo instanceof Blocks.StickerModifierInfo) {
					//Updating the modifier in the database
					Blocks.StickerModifierInfo stickerInfo = (Blocks.StickerModifierInfo) modifierInfo;
					try {
						stickerInfo.image = packager.unpackageData(stickerInfo.image);
						ConversationManager.StickerInfo sticker = DatabaseManager.getInstance().addMessageSticker(stickerInfo);
						if(sticker != null) stickerModifiers.add(sticker);
					} catch(OutOfMemoryError exception) {
						exception.printStackTrace();
						Crashlytics.logException(exception);
					}
				}
				//Otherwise checking if the modifier is a tapback update
				else if(modifierInfo instanceof Blocks.TapbackModifierInfo) {
					//Getting the tapback modifier
					Blocks.TapbackModifierInfo tapbackModifierInfo = (Blocks.TapbackModifierInfo) modifierInfo;
					
					//Checking if the tapback is negative
					if(tapbackModifierInfo.code >= SharedValues.TapbackModifierInfo.tapbackBaseRemove) {
						//Deleting the modifier in the database
						DatabaseManager.getInstance().removeMessageTapback(tapbackModifierInfo);
						tapbackRemovals.add(new TapbackRemovalStruct(tapbackModifierInfo.sender, tapbackModifierInfo.message, tapbackModifierInfo.messageIndex));
					} else {
						//Updating the modifier in the database
						ConversationManager.TapbackInfo tapback = DatabaseManager.getInstance().addMessageTapback(tapbackModifierInfo);
						if(tapback != null) tapbackModifiers.add(tapback);
					}
				}
			}
			
			//Returning
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			//Getting the context
			Context context = contextReference.get();
			if(context == null) return;
			
			//Iterating over the modifier structs
			for(Blocks.ModifierInfo modifierInfo : structModifiers) {
				//Finding the referenced item
				ConversationManager.ConversationItem conversationItem;
				ConversationManager.MessageInfo messageInfo = null;
				for(ConversationManager.ConversationInfo loadedConversation : ConversationManager.getLoadedConversations()) {
					conversationItem = loadedConversation.findConversationItem(modifierInfo.message);
					if(conversationItem == null) continue;
					if(!(conversationItem instanceof ConversationManager.MessageInfo)) break;
					messageInfo = (ConversationManager.MessageInfo) conversationItem;
					break;
				}
				
				//Skipping the remainder of the iteration if the message info is invalid (wasn't found)
				if(messageInfo == null) return;
				
				//Checking if the modifier is an activity status modifier
				if(modifierInfo instanceof Blocks.ActivityStatusModifierInfo) {
					//Getting the modifier
					Blocks.ActivityStatusModifierInfo activityStatusModifierInfo = (Blocks.ActivityStatusModifierInfo) modifierInfo;
					
					//Updating the message
					messageInfo.setMessageState(activityStatusModifierInfo.state);
					messageInfo.setDateRead(activityStatusModifierInfo.dateRead);
					
					//Getting the parent conversation
					ConversationManager.ConversationInfo parentConversation = messageInfo.getConversationInfo();
					
					//Updating the activity state target
					parentConversation.tryActivityStateTarget(messageInfo, true, context);
					
					/* //Checking if the message is the activity state target
					if(parentConversation.getActivityStateTarget() == messageInfo) {
						//Updating the message's activity state display
						messageInfo.updateActivityStateDisplay(context);
					} else {
						//Comparing (and replacing) the conversation's activity state target
						parentConversation.tryActivityStateTarget(messageInfo, true, context);
					} */
				}
			}
			
			//Iterating over the sticker modifiers
			for(ConversationManager.StickerInfo sticker : stickerModifiers) {
				//Finding the referenced item
				ConversationManager.ConversationItem conversationItem;
				ConversationManager.MessageInfo messageInfo = null;
				for(ConversationManager.ConversationInfo loadedConversation : ConversationManager.getForegroundConversations()) {
					conversationItem = loadedConversation.findConversationItem(sticker.getMessageID());
					if(conversationItem == null) continue;
					if(!(conversationItem instanceof ConversationManager.MessageInfo)) break;
					messageInfo = (ConversationManager.MessageInfo) conversationItem;
					break;
				}
				
				//Skipping the remainder of the iteration if the message info is invalid (wasn't found)
				if(messageInfo == null) return;
				
				//Updating the message
				messageInfo.addLiveSticker(sticker, context);
			}
			
			//Iterating over the tapback modifiers
			for(ConversationManager.TapbackInfo tapback : tapbackModifiers) {
				//Finding the referenced item
				ConversationManager.MessageInfo messageInfo = null;
				for(ConversationManager.ConversationInfo loadedConversation : ConversationManager.getForegroundConversations()) {
					ConversationManager.ConversationItem conversationItem;
					conversationItem = loadedConversation.findConversationItem(tapback.getMessageID());
					if(conversationItem == null) continue;
					if(!(conversationItem instanceof ConversationManager.MessageInfo)) break;
					messageInfo = (ConversationManager.MessageInfo) conversationItem;
					break;
				}
				
				//Skipping the remainder of the iteration if the message info is invalid (wasn't found)
				if(messageInfo == null) return;
				
				//Updating the message
				messageInfo.addLiveTapback(tapback, context);
			}
			
			//Iterating over the removed tapbacks
			for(TapbackRemovalStruct tapback : tapbackRemovals) {
				//Finding the referenced item
				ConversationManager.ConversationItem conversationItem;
				ConversationManager.MessageInfo messageInfo = null;
				for(ConversationManager.ConversationInfo loadedConversation : ConversationManager.getForegroundConversations()) {
					conversationItem = loadedConversation.findConversationItem(tapback.message);
					if(conversationItem == null) continue;
					if(!(conversationItem instanceof ConversationManager.MessageInfo)) break;
					messageInfo = (ConversationManager.MessageInfo) conversationItem;
					break;
				}
				
				//Skipping the remainder of the iteration if the message info is invalid (wasn't found)
				if(messageInfo == null) return;
				
				//Updating the message
				messageInfo.removeLiveTapback(tapback.sender, tapback.messageIndex, context);
			}
		}
		
		private static class TapbackRemovalStruct {
			final String sender;
			final String message;
			final int messageIndex;
			
			TapbackRemovalStruct(String sender, String message, int messageIndex) {
				this.sender = sender;
				this.message = message;
				this.messageIndex = messageIndex;
			}
		}
	}
	
	private static abstract class QueueTask<Progress, Result> {
		//abstract void onPreExecute();
		abstract Result doInBackground();
		
		void onPostExecute(Result value) {}
		
		void publishProgress(Progress progress) {
			new Handler(Looper.getMainLooper()).post(() -> onProgressUpdate(progress));
		}
		
		void onProgressUpdate(Progress progress) {}
	}
	
	void addMessagingProcessingTask(QueueTask<?, ?> task) {
		//Adding the task
		messageProcessingQueue.add(task);
		
		//Starting the thread if it isn't running
		if(messageProcessingQueueThreadRunning.compareAndSet(false, true)) new MessageProcessingThread(this).start();
	}
	
	private static class MessageProcessingThread extends Thread {
		private final WeakReference<ConnectionService> serviceReference;
		
		MessageProcessingThread(ConnectionService service) {
			serviceReference = new WeakReference<>(service);
		}
		
		@Override
		public void run() {
			//Creating the handler
			Handler handler = new Handler(Looper.getMainLooper());
			
			//Looping while the thread is alive
			ConnectionService service = null;
			QueueTask<?, ?> task;
			while(!isInterrupted() &&
					(service = serviceReference.get()) != null &&
					(task = pushQueue(service)) != null) {
				//Clearing the reference to the service
				service = null;
				
				//Running the task
				runTask(task, handler);
			}
			
			//Telling the service that the thread is finished
			if(service != null) service.messageProcessingQueueThreadRunning.set(false);
		}
		
		private QueueTask<?, ?> pushQueue(ConnectionService service) {
			if(service == null) return null;
			return service.messageProcessingQueue.poll();
		}
		
		private <Result> void runTask(QueueTask<?, Result> task, Handler handler) {
			Result value = task.doInBackground();
			handler.post(() -> task.onPostExecute(value));
		}
	}
	
	static void cleanConversationItem(Blocks.ConversationItem conversationItem) {
		//Invalidating text if it is empty
		if(conversationItem instanceof Blocks.MessageInfo) {
			Blocks.MessageInfo messageInfo = (Blocks.MessageInfo) conversationItem;
			if(messageInfo.text != null && messageInfo.text.isEmpty())
				messageInfo.text = null;
			if(messageInfo.sendEffect != null && messageInfo.sendEffect.isEmpty())
				messageInfo.sendEffect = null;
		} else if(conversationItem instanceof Blocks.ChatRenameActionInfo) {
			Blocks.ChatRenameActionInfo chatRenameActionInfo = (Blocks.ChatRenameActionInfo) conversationItem;
			if(chatRenameActionInfo.newChatName != null && chatRenameActionInfo.newChatName.isEmpty())
				chatRenameActionInfo.newChatName = null;
		}
	}
}