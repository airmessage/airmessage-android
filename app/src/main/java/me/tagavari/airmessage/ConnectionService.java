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
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.LongSparseArray;
import android.util.SparseArray;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ClientHandshakeBuilder;
import org.java_websocket.handshake.ServerHandshake;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import me.tagavari.airmessage.common.SharedValues;

public class ConnectionService extends Service {
	//Creating the reference values
	private static final int[] applicableCommunicationsVersions = {SharedValues.mmCommunicationsVersion/*, SharedValues.mmCommunicationsVersion - 1*/};
	
	static final String localBCResult = "LocalMSG-ConnectionService-Result";
	static final String localBCMassRetrieval = "LocalMSG-ConnectionService-MassRetrievalProgress";
	static final String BCPingTimer = "me.tagavari.airmessage.ConnectionService-StartPing";
	
	static final byte intentResultValueSuccess = 0;
	static final byte intentResultValueInternalException = 1;
	static final byte intentResultValueBadRequest = 2;
	static final byte intentResultValueClientOutdated = 3;
	static final byte intentResultValueServerOutdated = 4;
	static final byte intentResultValueUnauthorized = 5;
	static final byte intentResultValueConnection = 6;
	
	static final byte intentExtraStateMassRetrievalStarted = 0;
	static final byte intentExtraStateMassRetrievalProgress = 1;
	static final byte intentExtraStateMassRetrievalFinished = 2;
	static final byte intentExtraStateMassRetrievalFailed = 3;
	
	static final String selfIntentActionConnect = "connect";
	static final String selfIntentActionDisconnect = "disconnect";
	static final String selfIntentActionStop = "stop";
	
	static final int attachmentChunkSize = 1024 * 1024; //1 MiB
	static final int keepAliveMillis = 10 * 60 * 1000; //10 minutes
	static final int keepAliveWindowMillis = 5 * 60 * 1000; //5 minutes
	
	private static final Pattern regExValidPort = Pattern.compile("(:([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5]?))$");
	private static final Pattern regExValidProtocol = Pattern.compile("^ws(s?)://");
	
	private PendingIntent pingPendingIntent;
	
	//Creating the access values
	private static WeakReference<ConnectionService> serviceReference = null;
	
	//Creating the connection values
	static String hostname = null;
	static String password = null;
	static byte lastConnectionResult = -1;
	private boolean connectionEstablishedForRetrieval = false;
	private boolean connectionEstablishedForReconnect = false;
	private boolean massRetrievalInProgress = false;
	private int massRetrievalProgress = -1;
	private int massRetrievalProgressCount = -1;
	private final Handler massRetrievalTimeoutHandler = new Handler();
	private final Runnable massRetrievalTimeoutRunnable = () -> {
		//Returning if the state matches
		if(!massRetrievalInProgress) return;
		
		//Setting the variable
		massRetrievalInProgress = false;
		
		//Sending a broadcast
		LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(localBCMassRetrieval).putExtra(Constants.intentParamState, intentExtraStateMassRetrievalFailed));
	};
	//private static final long massRetrievalTimeout = 2 * 1000; //2 seconds
	private static final long massRetrievalTimeout = 60 * 1000; //1 minute
	private int activeCommunicationsVersion = -1;
	
	private final List<FileSendRequest> fileSendRequestQueue = new ArrayList<>();
	private Thread fileSendRequestThread = null;
	
	private static byte nextLaunchID = 0;
	
	//Creating the broadcast receivers
	private final BroadcastReceiver pingBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			//Pinging the server
			if(wsClient == null || !wsClient.isOpen()) return;
			wsClient.sendPing();
			
			//Rescheduling the ping
			schedulePing();
		}
	};
	private final BroadcastReceiver networkStateChangeBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			//Returning if automatic reconnects are disabled
			if(!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preference_server_networkreconnect_key), false)) return;
			
			//Reconnecting
			if(wsClient == null || !wsClient.isOpen()) reconnect();
		}
	};
	
	//Creating the other values
	private final SparseArray<MessageResponseManager> messageSendRequests = new SparseArray<>();
	//private final LongSparseArray<MessageResponseManager> fileSendRequests = new LongSparseArray<>();
	//private short nextRequestID = (short) (new Random(System.currentTimeMillis()).nextInt((int) Short.MAX_VALUE - (int) Short.MIN_VALUE + 1) + Short.MIN_VALUE);
	private short nextRequestID = (short) new Random(System.currentTimeMillis()).nextInt(1 << 16);
	private boolean isShuttingDown = false;
	
	private MMWebSocketClient wsClient = null;
	private final ArrayList<ConversationInfoRequest> pendingConversations = new ArrayList<>();
	private final Handler mainHandler = new Handler(Looper.getMainLooper());
	
	static ConnectionService getInstance() {
		return serviceReference == null ? null : serviceReference.get();
	}
	
	static int getActiveCommunicationsVersion() {
		//Getting the instance
		ConnectionService connectionService = getInstance();
		if(connectionService == null) return -1;
		
		//Returning the active communications version
		return connectionService.activeCommunicationsVersion;
	}
	
	static byte getNextLaunchID() {
		return nextLaunchID++;
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
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//Getting the intent action
		String intentAction = intent == null ? null : intent.getAction();
		
		//Checking if a stop has been requested or the connection address is invalid
		if(selfIntentActionStop.equals(intentAction) || (hostname == null || hostname.isEmpty())) {
			//Denying the existence of the connection for reconnection
			connectionEstablishedForReconnect = false;
			
			//Stopping the service
			stopSelf();
			
			//Setting the service as shutting down
			isShuttingDown = true;
			
			//Returning not sticky
			return START_NOT_STICKY;
		}
		
		//Checking if a disconnect has been requested
		if(selfIntentActionDisconnect.equals(intentAction)) {
			//Denying the existence of the connection for reconnection
			connectionEstablishedForReconnect = false;
			
			//Disconnecting
			disconnect();
			
			//Updating the notification
			postDisconnectedNotification(true);
		}
		//Reconnecting the client if requested
		else if(wsClient == null || wsClient.isClosed() || selfIntentActionConnect.equals(intentAction)) connect(intent != null && intent.hasExtra(Constants.intentParamLaunchID) ? intent.getByteExtra(Constants.intentParamLaunchID, (byte) 0) : getNextLaunchID());
		
		//Setting the service as not shutting down
		isShuttingDown = false;
		
		//Calling the listeners
		//for(ServiceStartCallback callback : startCallbacks) callback.onServiceStarted(this);
		//startCallbacks.clear();
		
		//Returning sticky service
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		//Disconnecting
		disconnect();
		
		//Unregistering the broadcast receivers
		unregisterReceiver(networkStateChangeBroadcastReceiver);
		unregisterReceiver(pingBroadcastReceiver);
	}
	
	static String prepareHostname(String hostname) {
		//Checking if the hostname doesn't have a port
		if(!regExValidPort.matcher(hostname).find()) {
			//Adding the default port
			hostname += Constants.defaultPort;
		}
		
		//Checking if the hostname doesn't have a protocol
		if(!regExValidProtocol.matcher(hostname).find()) {
			//Adding the default protocol
			hostname = Constants.defaultProtocol + hostname;
		}
		
		//Returning the hostname
		return hostname;
	}
	
	private void connect(byte launchID) {
		//Checking if there is no hostname
		if(hostname == null || hostname.isEmpty()) {
			//Retrieving the data from the shared preferences
			SharedPreferences sharedPrefs = getSharedPreferences(MainApplication.sharedPreferencesFile, Context.MODE_PRIVATE);
			hostname = sharedPrefs.getString(MainApplication.sharedPreferencesKeyHostname, "");
			password = sharedPrefs.getString(MainApplication.sharedPreferencesKeyPassword, "");
		}
		
		//Preparing the hostname
		hostname = prepareHostname(hostname);
		
		//Checking if the client is valid
		if(wsClient != null) {
			//Denying the existence of the connection for reconnection
			connectionEstablishedForReconnect = false;
			
			//Closing the client
			wsClient.close();
		}
		
		//Preparing the WS client
		try {
			//Creating the target
			URI target = new URI(hostname);
			
			//Creating the WS client
			wsClient = new MMWebSocketClient(launchID, target, new DraftMMS());
			wsClient.setConnectionLostTimeout(0);
			
			//Checking if the scheme is WSS (secure)
			if(target.getScheme().equals("wss")) {
				//Creating the SSL context
				SSLContext sslContext = SSLContext.getInstance("TLS");
				
				TrustManager[] trustAllCerts = new TrustManager[]{
						new X509TrustManager() {
							public java.security.cert.X509Certificate[] getAcceptedIssuers() {
								return new X509Certificate[0];
							}
							
							public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
							}
							
							public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
							}
						}
				};
				
				sslContext.init(null, trustAllCerts, new SecureRandom());
				
				//Using the secure socket
				wsClient.setSocket(sslContext.getSocketFactory().createSocket());
			}
		} catch(Exception exception) {
			//Printing the stack trace
			exception.printStackTrace();
			
			//Notifying the connection listeners
			LocalBroadcastManager.getInstance(ConnectionService.this).sendBroadcast(new Intent(localBCResult)
					.putExtra(Constants.intentParamResult, intentResultValueInternalException)
					.putExtra(Constants.intentParamLaunchID, launchID));
			
			//Updating the notification state
			postDisconnectedNotification(false);
			
			//Finishing the service
			//finishService();
			
			//Returning
			return;
		}
		
		//Connecting
		wsClient.connect();
		
		//Starting the service as a foreground service
		startForeground(-1, getBackgroundNotification(false));
	}
	
	public void disconnect() {
		//Returning if the client is not open
		if(wsClient == null || wsClient.isClosed()) return;
		
		//Denying the existence of the connection for reconnection
		connectionEstablishedForReconnect = false;
		
		//Closing the connection
		wsClient.close();
	}
	
	public void reconnect() {
		connect(getNextLaunchID());
	}
	
	private void postConnectedNotification(boolean isConnected) {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(-1, getBackgroundNotification(isConnected));
	}
	
	private void postDisconnectedNotification(boolean silent) {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(-1, getOfflineNotification(silent));
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
			//Setting the last connection result
			lastConnectionResult = intentResultValueSuccess;
			
			//Notifying the connection listeners
			LocalBroadcastManager.getInstance(ConnectionService.this).sendBroadcast(new Intent(localBCResult)
					.putExtra(Constants.intentParamResult, intentResultValueSuccess)
					.putExtra(Constants.intentParamLaunchID, launchID));
			
			//Recording the server versions
			{
				String commVer = handshake.getFieldValue(SharedValues.headerCommVer);
				if(commVer.matches("^\\d+$")) activeCommunicationsVersion = Integer.parseInt(commVer);
			}
			
			//Retrieving the pending conversation info
			retrievePendingConversationInfo();
			
			//Updating the notification
			postConnectedNotification(true);
			
			//Setting the connection as existing
			connectionEstablishedForRetrieval = connectionEstablishedForReconnect = true;
			
			//Getting the last connection time
			SharedPreferences sharedPrefs = getSharedPreferences(MainApplication.sharedPreferencesFile, Context.MODE_PRIVATE);
			String lastConnectionHostname = sharedPrefs.getString(MainApplication.sharedPreferencesKeyLastConnectionHostname, null);
			
			//Checking if the last connection is the same as the current one
			if(hostname.equals(lastConnectionHostname)) {
				//Getting the last connection time
				long lastConnectionTime = sharedPrefs.getLong(MainApplication.sharedPreferencesKeyLastConnectionTime, -1);
				
				//Fetching the messages since the last connection time
				retrieveMessagesSince(lastConnectionTime, System.currentTimeMillis());
			}
			
			//Scheduling the ping
			schedulePing();
		}
		
		@Override
		public void onMessage(String message) {}
		
		@Override
		public void onMessage(ByteBuffer bytes) {
			//Updating the scheduled ping
			schedulePing();
			
			//Processing the message
			byte[] array = new byte[bytes.remaining()];
			bytes.get(array);
			
			try(ByteArrayInputStream bis = new ByteArrayInputStream(array); ObjectInputStream in = new ObjectInputStream(bis)) {
				switch(in.readByte()) { //Reading the message type and making a switch statement
					case SharedValues.wsFrameUpdate: { //New messages received
						final ArrayList<SharedValues.ConversationItem> receivedItems = (ArrayList<SharedValues.ConversationItem>) in.readObject();
						
						//Processing the messages
						processMessageUpdate(receivedItems, true);
						
						break;
					}
					case SharedValues.wsFrameTimeRetrieval: { //Time retrieval
						final ArrayList<SharedValues.ConversationItem> receivedItems = (ArrayList<SharedValues.ConversationItem>) in.readObject();
						
						//Processing the messages
						processMessageUpdate(receivedItems, true);
						
						break;
					}
					case SharedValues.wsFrameMassRetrieval: { //Mass retrieval
						//Breaking if the client isn't looking for a mass retrieval
						if(!massRetrievalInProgress) break;
						
						//Reading the data
						final ArrayList<SharedValues.ConversationItem> receivedItems = (ArrayList<SharedValues.ConversationItem>) in.readObject();
						final ArrayList<SharedValues.ConversationInfo> receivedConversations = (ArrayList<SharedValues.ConversationInfo>) in.readObject();
						
						//Processing the messages
						processMassRetrievalResult(receivedItems, receivedConversations);
						
						break;
					}
					case SharedValues.wsFrameChatInfo: { //Chat information
						final ArrayList<SharedValues.ConversationInfo> receivedItems = (ArrayList<SharedValues.ConversationInfo>) in.readObject();
						
						//Processing the conversations
						processChatInfoResponse(receivedItems);
						
						break;
					}
					case SharedValues.wsFrameModifierUpdate: { //Message modifier update
						final ArrayList<SharedValues.ModifierInfo> receivedItems = (ArrayList<SharedValues.ModifierInfo>) in.readObject();
						
						//Processing the conversations
						processModifierUpdate(receivedItems);
						
						break;
					}
					case SharedValues.wsFrameAttachmentReq: { //Attachment data received
						//Reading the data
						final String guid = in.readUTF();
						final short requestID = in.readShort();
						final int requestIndex = in.readInt();
						final byte[] compressedBytes = (byte[]) in.readObject();
						final long fileSize;
						if(requestIndex == 0) fileSize = in.readLong();
						else fileSize = -1;
						final boolean isLast = in.readBoolean();
						
						//Running in the UI thread
						mainHandler.post(new Runnable() {
							@Override
							public void run() {
								//Processing the message update
								ConversationManager.processFileFragmentData(ConnectionService.this, guid, requestID, compressedBytes, requestIndex, isLast, fileSize);
							}
						});
						
						break;
					}
					case SharedValues.wsFrameAttachmentReqConfirmed: { //Attachment data request received
						final short requestID = in.readShort();
						final String guid = in.readUTF();
						
						//Running in the UI thread
						mainHandler.post(() -> ConversationManager.processFileFragmentConfirmed(guid, requestID));
						break;
					}
					case SharedValues.wsFrameAttachmentReqFailed: { //Attachment data request failed
						final short requestID = in.readShort();
						final String guid = in.readUTF();
						
						//Running in the UI thread
						mainHandler.post(() -> ConversationManager.processFileFragmentFailed(guid, requestID));
						break;
					}
					case SharedValues.wsFrameSendResult: {
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
							new Handler(Looper.getMainLooper()).post(new Runnable() {
								@Override
								public void run() {
									//Telling the listener
									if(success) messageResponseManager.onSuccess();
									else messageResponseManager.onFail(messageSendExternalException);
								}
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
			//Getting the code from the message
			int code = -1;
			String errorCodeString = reasonString.substring(reasonString.lastIndexOf(' ') + 1);
			if(errorCodeString.matches("^\\d+$")) code = Integer.parseInt(errorCodeString);
			
			//Determining the broadcast value
			byte clientReason;
			switch(code) {
				default:
					clientReason = intentResultValueConnection;
					break;
				case SharedValues.resultBadRequest:
					clientReason = intentResultValueBadRequest;
					break;
				case SharedValues.resultClientOutdated:
					clientReason = intentResultValueClientOutdated;
					break;
				case SharedValues.resultServerOutdated:
					clientReason = intentResultValueServerOutdated;
					break;
				case SharedValues.resultUnauthorized:
					clientReason = intentResultValueUnauthorized;
			}
			
			//Setting the last connection result
			lastConnectionResult = clientReason;
			
			//Notifying the connection listeners
			LocalBroadcastManager.getInstance(ConnectionService.this).sendBroadcast(new Intent(localBCResult)
					.putExtra(Constants.intentParamResult, clientReason)
					.putExtra(Constants.intentParamLaunchID, launchID));
			
			//Checking if a connection existed for retrieval
			if(connectionEstablishedForRetrieval) {
				//Writing the time to shared preferences
				SharedPreferences sharedPrefs = getSharedPreferences(MainApplication.sharedPreferencesFile, Context.MODE_PRIVATE);
				SharedPreferences.Editor editor = sharedPrefs.edit();
				editor.putLong(MainApplication.sharedPreferencesKeyLastConnectionTime, System.currentTimeMillis());
				editor.putString(MainApplication.sharedPreferencesKeyLastConnectionHostname, hostname);
				editor.commit();
			}
			
			//Checking if a connection existed for reconnection and the preference is enabled
			if(connectionEstablishedForReconnect && PreferenceManager.getDefaultSharedPreferences(MainApplication.getInstance()).getBoolean(MainApplication.getInstance().getResources().getString(R.string.preference_server_dropreconnect_key), false)) {
				//Reconnecting
				ConnectionService.this.connect(getNextLaunchID());
			}
			
			//Setting the connection as nonexistent
			connectionEstablishedForRetrieval = connectionEstablishedForReconnect = false;
			
			//Posting the disconnected notification
			if(!isShuttingDown) postDisconnectedNotification(false);
			
			//Cancelling the mass retrieval if there is one in progress
			if(massRetrievalInProgress && massRetrievalProgress == -1) cancelMassRetrieval();
			
			//Removing the scheduled ping
			//unschedulePing();
		}
		
		@Override
		public void onError(Exception exception) {
			exception.printStackTrace();
		}
	}
	
	private void processMessageUpdate(ArrayList<SharedValues.ConversationItem> structConversationItems, boolean sendNotifications) {
		//Creating and running the task
		new MessageUpdateAsyncTask(this, getApplicationContext(), structConversationItems, sendNotifications).execute();
	}
	
	private void processMassRetrievalResult(ArrayList<SharedValues.ConversationItem> structConversationItems, ArrayList<SharedValues.ConversationInfo> structConversations) {
		//Stopping the timeout timer
		massRetrievalTimeoutHandler.removeCallbacks(massRetrievalTimeoutRunnable);
		
		//Calculating the progress
		massRetrievalProgress = 0;
		massRetrievalProgressCount = structConversationItems.size() + structConversations.size();
		
		//Sending a progress message
		LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(localBCMassRetrieval)
				.putExtra(Constants.intentParamState, intentExtraStateMassRetrievalProgress)
				.putExtra(Constants.intentParamSize, massRetrievalProgressCount));
		
		//Creating and running the task
		new MassRetrievalAsyncTask(this, getApplicationContext(), structConversationItems, structConversations).execute();
	}
	
	private void processChatInfoResponse(ArrayList<SharedValues.ConversationInfo> structConversations) {
		//Creating the list values
		final ArrayList<ConversationManager.ConversationInfo> unavailableConversations = new ArrayList<>();
		final ArrayList<ConversationInfoRequest> availableConversations = new ArrayList<>();
		
		//Iterating over the conversations
		for(SharedValues.ConversationInfo structConversationInfo : structConversations) {
			//Finding the conversation in the pending list
			ConversationInfoRequest request = null;
			synchronized(pendingConversations) {
				for(Iterator<ConversationInfoRequest> iterator = pendingConversations.iterator(); iterator.hasNext();) {
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
		new SaveConversationInfoAsyncTask(getApplicationContext(), unavailableConversations, availableConversations).execute();
	}
	
	private void processModifierUpdate(ArrayList<SharedValues.ModifierInfo> structModifiers) {
		//Creating and running the task
		new ModifierUpdateAsyncTask(getApplicationContext(), structModifiers).execute();
	}
	
	boolean requestAttachmentInfo(String fileGuid, short requestID) {
		//Returning if the client isn't ready
		if(wsClient == null || !wsClient.isOpen()) return false;
		
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
		} catch(Exception exception) {
			//Printing the stack trace
			exception.printStackTrace();
			
			//Returning false
			return false;
		}
		
		//Returning true
		return true;
	}
	
	void retrievePendingConversationInfo() {
		//Locking the pending conversations
		synchronized(pendingConversations) {
			//Returning if there are no pending conversations
			if(pendingConversations.isEmpty()) return;
			
			//Converting the conversation info list to a string list
			ArrayList<String> list = new ArrayList<>();
			for(ConversationInfoRequest conversationInfoRequest : pendingConversations)
				list.add(conversationInfoRequest.conversationInfo.getGuid());
			
			//Requesting information on new conversations
			try(ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
				//Adding the data
				out.writeByte(SharedValues.wsFrameChatInfo); //Message type - chat info
				out.writeObject(list); //Conversation list
				out.flush();
				
				//Sending the message
				wsClient.send(bos.toByteArray());
			} catch(Exception exception) {
				//Printing the stack trace
				exception.printStackTrace();
			}
		}
	}
	
	boolean isConnected() {
		return wsClient != null && wsClient.isOpen();
	}
	
	boolean isConnecting() {
		return wsClient != null && wsClient.isConnecting();
	}
	
	boolean isMassRetrievalInProgress() {
		return massRetrievalInProgress;
	}
	
	int getMassRetrievalProgress() {
		return massRetrievalProgress;
	}
	
	int getMassRetrievalProgressCount() {
		return massRetrievalProgressCount;
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
	
	private static final int largestFileSize = 1024 * 1024 * 100; //100 MB
	
	void queueUploadRequest(FileSendRequestCallbacks callbacks, Uri uri, ConversationManager.ConversationInfo conversationInfo, long attachmentID) {
		//Adding the request
		synchronized(fileSendRequestQueue) {
			fileSendRequestQueue.add(new FileSendRequest(callbacks, uri, conversationInfo, attachmentID));
		}
		
		//Notifying the queue
		notifyQueue();
	}
	
	void queueUploadRequest(FileSendRequestCallbacks callbacks, File file, ConversationManager.ConversationInfo conversationInfo, long attachmentID) {
		//Adding the request
		synchronized(fileSendRequestQueue) {
			fileSendRequestQueue.add(new FileSendRequest(callbacks, file, conversationInfo, attachmentID));
		}
		
		//Notifying the queue
		notifyQueue();
	}
	
	void notifyQueue() {
		//Returning if the thread isn't running
		if(fileSendRequestThread != null && fileSendRequestThread.getState() != Thread.State.TERMINATED) return;
		
		//Creating and starting the thread
		fileSendRequestThread = new FileSendRequestThread(getApplicationContext(), this);
		fileSendRequestThread.start();
	}
	
	interface FileSendRequestCallbacks {
		void onStart();
		
		void onCopyFinished(File location);
		
		void onUploadFinished(byte[] checksum);
		
		void onResponseReceived();
		
		void onFail(byte reason);
		
		void onProgress(float progress);
	}
	
	static class FileSendRequest {
		//Creating the callbacks
		final FileSendRequestCallbacks callbacks;
		
		//Creating the request values
		final ConversationManager.ConversationInfo conversationInfo;
		final long attachmentID;
		File sendFile;
		Uri sendUri;
		
		private FileSendRequest(FileSendRequestCallbacks callbacks, ConversationManager.ConversationInfo conversationInfo, long attachmentID) {
			//Setting the callbacks
			this.callbacks = callbacks;
			
			//Setting the request values
			this.conversationInfo = conversationInfo;
			this.attachmentID = attachmentID;
		}
		
		FileSendRequest(FileSendRequestCallbacks callbacks, File file, ConversationManager.ConversationInfo conversationInfo, long attachmentID) {
			//Calling the main constructor
			this(callbacks, conversationInfo, attachmentID);
			
			//Setting the source values
			sendFile = file;
			sendUri = null;
		}
		
		FileSendRequest(FileSendRequestCallbacks callbacks, Uri uri, ConversationManager.ConversationInfo conversationInfo, long attachmentID) {
			//Calling the main constructor
			this(callbacks, conversationInfo, attachmentID);
			
			//Setting the source values
			sendFile = null;
			sendUri = uri;
		}
	}
	
	static class FileSendRequestThread extends Thread {
		//Creating the reference values
		private final WeakReference<Context> contextReference;
		private final WeakReference<ConnectionService> superclassReference;
		
		//Creating the other values
		private final Handler handler = new Handler(Looper.getMainLooper());
		
		FileSendRequestThread(Context context, ConnectionService superclass) {
			//Setting the references
			contextReference = new WeakReference<>(context);
			superclassReference = new WeakReference<>(superclass);
		}
		
		@Override
		public void run() {
			//Looping while there are requests in the queue
			FileSendRequest request;
			while((request = pushQueue()) != null) {
				final FileSendRequestCallbacks finalCallbacks = request.callbacks;
				//Telling the callbacks that the process has started
				handler.post(request.callbacks::onStart);
				
				//Getting the context
				Context context = contextReference.get();
				if(context == null) {
					//Calling the fail method
					handler.post(() -> finalCallbacks.onFail(messageSendReferencesLost));
					
					//Skipping the remainder of the iteration
					continue;
				}
				
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
					
					//Finding a valid file
					String fileName = Constants.getFileName(context, request.sendUri);
					if(fileName == null) fileName = Constants.defaultFileName;
					File targetFile = MainApplication.findUploadFileTarget(context, fileName);
					
					try {
						//Creating the targets
						if(!targetFile.getParentFile().mkdir()) throw new IOException();
						if(!targetFile.createNewFile()) throw new IOException();
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
						//Checking if the input stream is invalid
						if(inputStream == null) {
							//Calling the fail method
							handler.post(() -> finalCallbacks.onFail(messageSendInvalidContent));
							
							//Skipping the remainder of the iteration
							continue;
						}
						
						//Preparing to read the file
						int totalLength = inputStream.available();
						byte[] buffer = new byte[ConnectionService.attachmentChunkSize];
						int bytesRead;
						int totalBytesRead = 0;
						
						//Looping while there is data to read
						while((bytesRead = inputStream.read(buffer)) != -1) {
							//Writing the data to the output stream
							outputStream.write(buffer, 0, bytesRead);
							
							//Adding to the total bytes read
							totalBytesRead += bytesRead;
							
							//Updating the progress
							final int finalTotalBytesRead = totalBytesRead;
							handler.post(() -> finalCallbacks.onProgress((float) finalTotalBytesRead / (float) totalLength * 0.5F));
						}
						
						//Flushing the output stream
						outputStream.flush();
						
						//Updating the database entry
						DatabaseManager.updateAttachmentFile(DatabaseManager.getWritableDatabase(context), request.attachmentID, targetFile);
						
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
				if(connectionService == null || connectionService.wsClient == null) {
					//Calling the fail method
					handler.post(() -> finalCallbacks.onFail(messageSendNetworkException));
					
					//Skipping the remainder of the iteration
					continue;
				}
				
				//Getting the request ID
				short requestID = connectionService.getNextRequestID();
				
				//Getting the message digest
				MessageDigest messageDigest;
				try {
					messageDigest = MessageDigest.getInstance(SharedValues.hashAlgorithm);
				} catch(NoSuchAlgorithmException exception) {
					//Printing the stack trace
					exception.printStackTrace();
					
					//Calling the fail method
					handler.post(() -> finalCallbacks.onFail(messageSendIOException));
					
					//Skipping the remainder of the iteration
					continue;
				}
				
				//Setting up the streams
				try(InputStream inputStream = new FileInputStream(request.sendFile);
					DigestInputStream digestInputStream = new DigestInputStream(inputStream, messageDigest)) {
					//Preparing to read the file
					int totalLength = digestInputStream.available();
					byte[] buffer = new byte[ConnectionService.attachmentChunkSize];
					int bytesRead;
					int totalBytesRead = 0;
					int requestIndex = 0;
					
					//Checking if the file size is too large to send
					if(totalLength > largestFileSize) {
						//Calling the fail method
						handler.post(() -> finalCallbacks.onFail(messageSendFileTooLarge));
						
						//Skipping the remainder of the iteration
						continue;
					}
					
					//Looping while there is data to read
					while((bytesRead = digestInputStream.read(buffer)) != -1) {
						//Adding to the total bytes read
						totalBytesRead += bytesRead;
						
						//Uploading the data
						byte[] compressedData = SharedValues.compress(buffer, bytesRead);
						
						//Preparing to serialize the request
						try(ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
							if(request.conversationInfo.getState() == ConversationManager.ConversationInfo.ConversationState.READY) {
								//Adding the data
								out.writeByte(SharedValues.wsFrameSendFileExisting); //Message type - send existing file
								out.writeShort(requestID); //Request identifier
								out.writeInt(requestIndex); //Request index
								out.writeUTF(request.conversationInfo.getGuid()); //Chat GUID
								out.writeObject(compressedData); //File bytes
								out.reset();
								if(requestIndex == 0) out.writeUTF(request.sendFile.getName());
								out.writeBoolean(totalBytesRead >= totalLength); //Is last message
								out.flush();
							} else {
								//Adding the data
								out.writeByte(SharedValues.wsFrameSendFileNew); //Message type - send new file
								out.writeShort(requestID); //Request identifier
								out.writeInt(requestIndex); //Request index
								out.writeObject(request.conversationInfo.getNormalizedConversationMembersAsArray()); //Chat recipients
								out.writeObject(compressedData); //File bytes
								out.reset();
								if(requestIndex == 0) {
									out.writeUTF(request.sendFile.getName()); //File name
									out.writeUTF(request.conversationInfo.getService()); //Service
								}
								out.writeBoolean(totalBytesRead >= totalLength); //Is last message
								out.flush();
							}
							
							//Sending the message
							connectionService.wsClient.send(bos.toByteArray());
						}
						
						//Updating the progress
						final int finalTotalBytesRead = totalBytesRead;
						handler.post(() -> finalCallbacks.onProgress(copyFile ?
								(float) finalTotalBytesRead / (float) totalLength * 100F :
								0.5F + (float) finalTotalBytesRead / (float) totalLength * 0.5F));
						
						//Adding to the request index
						requestIndex++;
					}
					
					//Getting the checksum
					byte[] checksum = messageDigest.digest();
					
					//Running on the main thread
					handler.post(() -> {
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
						connectionService.messageSendRequests.put(requestID, responseManager);
						responseManager.startTimer();
					});
					
					//Saving the checksum
					DatabaseManager.updateAttachmentChecksum(DatabaseManager.getWritableDatabase(context), request.attachmentID, checksum);
				} catch(IOException exception) {
					//Calling the fail method
					handler.post(() -> finalCallbacks.onFail(messageSendIOException));
					
					//Skipping the remainder of the iteration
					continue;
				} catch(WebsocketNotConnectedException exception) {
					//Calling the fail method
					handler.post(() -> finalCallbacks.onFail(messageSendNetworkException));
					
					//Skipping the remainder of the iteration
					continue;
				}
			}
		}
		
		private FileSendRequest pushQueue() {
			//Getting the service
			ConnectionService connectionService = superclassReference.get();
			if(connectionService == null) return null;
			
			//Locking the queue
			synchronized(connectionService.fileSendRequestQueue) {
				//Returning null if the queue is empty
				if(connectionService.fileSendRequestQueue.isEmpty()) return null;
				
				//Removing the first item from the queue and returning it
				FileSendRequest request = connectionService.fileSendRequestQueue.get(0);
				connectionService.fileSendRequestQueue.remove(0);
				return request;
			}
		}
	}
	
	boolean sendMessage(String chatGUID, String message, MessageResponseManager responseListener) {
		//Checking if the client isn't ready
		if(!isConnected()) {
			//Telling the response listener
			responseListener.onFail(messageSendNetworkException);
			
			//Returning false
			return false;
		}
		
		//Getting the request ID
		short requestID = getNextRequestID();
		
		//Preparing to serialize the request
		try(ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
			//Adding the data
			out.writeByte(SharedValues.wsFrameSendTextExisting); //Message type - send existing text
			out.writeShort(requestID); //Request ID
			out.writeUTF(chatGUID); //Chat GUID
			out.writeUTF(message); //Message
			out.flush();
			
			//Sending the message
			wsClient.send(bos.toByteArray());
		} catch(IOException exception) {
			//Printing the stack trace
			exception.printStackTrace();
			
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
		if(!isConnected()) {
			//Telling the response listener
			responseListener.onFail(messageSendNetworkException);
			
			//Returning false
			return false;
		}
		
		//Getting the request ID
		short requestID = getNextRequestID();
		
		//Preparing to serialize the request
		try(ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
			//Adding the data
			out.writeByte(SharedValues.wsFrameSendTextNew); //Message type - send new text
			out.writeShort(requestID); //Request ID
			out.writeObject(chatRecipients); //Chat recipients
			out.writeUTF(message); //Message
			out.writeUTF(service); //Service
			out.flush();
			
			//Sending the message
			wsClient.send(bos.toByteArray());
		} catch(IOException exception) {
			//Printing the stack trace
			exception.printStackTrace();
			
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
	
	boolean requestMassRetrieval(Context context) {
		//Returning false if the client isn't ready or a mass retrieval is already in progress
		if(wsClient == null || !wsClient.isOpen() || massRetrievalInProgress) return false;
		
		//Preparing to serialize the request
		try(ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
			//Adding the data
			out.writeByte(SharedValues.wsFrameMassRetrieval); //Message type - Mass retrieval request
			out.flush();
			
			//Sending the message
			wsClient.send(bos.toByteArray());
		} catch(Exception exception) {
			//Printing the stack trace
			exception.printStackTrace();
			
			//Returning false
			return false;
		}
		
		//Setting the mass retrieval values
		massRetrievalInProgress = true;
		LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
		
		//Starting the timeout
		massRetrievalTimeoutHandler.postDelayed(massRetrievalTimeoutRunnable, massRetrievalTimeout);
		
		//Resetting the progress
		massRetrievalProgress = -1;
		massRetrievalProgressCount = -1;
		
		//Sending the broadcast
		localBroadcastManager.sendBroadcast(new Intent(localBCMassRetrieval).putExtra(Constants.intentParamState, intentExtraStateMassRetrievalStarted));
		
		//Returning true
		return true;
	}
	
	private void cancelMassRetrieval() {
		//Setting the variable
		massRetrievalInProgress = false;
		
		//Sending a broadcast
		LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(localBCMassRetrieval).putExtra(Constants.intentParamState, intentExtraStateMassRetrievalFailed));
		
		//Stopping the timeout timer
		massRetrievalTimeoutHandler.removeCallbacks(massRetrievalTimeoutRunnable);
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
		//Returning if the client isn't ready
		if(wsClient == null || !wsClient.isOpen()) return false;
		
		//Preparing to serialize the request
		try(ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
			//Adding the data
			out.writeByte(SharedValues.wsFrameTimeRetrieval); //Message type - time-based retrieval
			out.writeLong(timeLower); //Lower time
			out.writeLong(timeUpper); //Upper time
			out.flush();
			
			//Sending the message
			wsClient.send(bos.toByteArray());
		} catch(Exception exception) {
			//Printing the stack trace
			exception.printStackTrace();
			
			//Returning false
			return false;
		}
		
		//Returning true
		return true;
	}
	
	short getNextRequestID() {
		return nextRequestID++;
	}
	
	static abstract class MessageResponseManager {
		abstract void onSuccess();
		
		abstract void onFail(byte resultCode);
		
		private CountDownTimer timer = new CountDownTimer(30 * 1000, 30 * 1000) { //30 seconds
			@Override
			public void onTick(long l) {}
			
			@Override
			public void onFinish() {
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
			}
		};
		
		void startTimer() {
			timer.start();
		}
		
		void stopTimer(boolean restart) {
			timer.cancel();
			if(restart) timer.start();
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
	
	private static class DraftMMS extends Draft_6455 {
		@Override
		public ClientHandshakeBuilder postProcessHandshakeRequestAsClient(ClientHandshakeBuilder request) {
			//Calling the super method
			super.postProcessHandshakeRequestAsClient(request);
			
			//Building the delimited applicable communications version string
			StringBuilder applicableVersionsSB = new StringBuilder(Integer.toString(applicableCommunicationsVersions[0]));
			for(int i = 1; i < applicableCommunicationsVersions.length; i++) applicableVersionsSB.append('|').append(applicableCommunicationsVersions[i]);
			
			//Adding the communications version and password to the handshake request
			request.put(SharedValues.headerCommVer, applicableVersionsSB.toString());
			request.put(SharedValues.headerPassword, password);
			
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
			if( this == o ) return true;
			if( o == null || getClass() != o.getClass() ) return false;
			
			DraftMMS that = ( DraftMMS ) o;
			
			return getExtension() != null ? getExtension().equals( that.getExtension() ) : that.getExtension() == null;
		}
		
		@Override
		public Draft copyInstance() {
			return new DraftMMS();
		}
	}
	
	public static class ServiceStart extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			//Returning if the action doesn't match or the preference isn't enabled
			if(!"android.intent.action.BOOT_COMPLETED".equals(intent.getAction())/* || !PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preference_server_connectionboot_key), false)*/) return;
			
			//Starting the service
			Intent serviceIntent = new Intent(context, ConnectionService.class);
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				context.startForegroundService(serviceIntent);
			} else {
				context.startService(serviceIntent);
			}
		}
	}
	
	private static class TransferConversationStruct {
		String guid;
		ConversationManager.ConversationInfo.ConversationState state;
		String name;
		ArrayList<ConversationManager.ConversationItem> conversationItems;
		
		TransferConversationStruct(String guid, ConversationManager.ConversationInfo.ConversationState state, String name, ArrayList<ConversationManager.ConversationItem> conversationItems) {
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
	
	private static class FetchConversationRequests extends AsyncTask<Void, Void, ArrayList<ConversationManager.ConversationInfo>> {
		private final WeakReference<ConnectionService> serviceReference;
		
		FetchConversationRequests(ConnectionService serviceInstance) {
			//Setting the context reference
			serviceReference = new WeakReference<>(serviceInstance);
		}
		
		@Override
		protected ArrayList<ConversationManager.ConversationInfo> doInBackground(Void... parameters) {
			Context context = serviceReference.get();
			if(context == null) return null;
			
			//Fetching the incomplete conversations
			return DatabaseManager.fetchConversationsWithState(context, ConversationManager.ConversationInfo.ConversationState.INCOMPLETE_SERVER);
		}
		
		@Override
		protected void onPostExecute(ArrayList<ConversationManager.ConversationInfo> conversations) {
			//Getting the service instance
			ConnectionService service = serviceReference.get();
			if(service == null) return;
			
			//Copying the conversations to the pending list
			synchronized(service.pendingConversations) {
				for(ConversationManager.ConversationInfo conversation : conversations)
					service.pendingConversations.add(new ConversationInfoRequest(conversation, true));
			}
			
			//Requesting a conversation info fetch if they haven't been automatically requested (the client connected before the conversations were fetched)
			if(service.wsClient != null && service.wsClient.isOpen())
				service.retrievePendingConversationInfo();
		}
	}
	
	private static class MessageUpdateAsyncTask extends AsyncTask<Void, Void, Void> {
		//Creating the reference values
		private final WeakReference<ConnectionService> serviceReference;
		private final WeakReference<Context> contextReference;
		
		//Creating the request values
		private final List<SharedValues.ConversationItem> structConversationItems;
		private final boolean sendNotifications;
		
		//Creating the conversation lists
		private final ArrayList<ConversationManager.ConversationItem> newCompleteConversationItems = new ArrayList<>();
		private final ArrayList<ConversationManager.ConversationInfo> completeConversations = new ArrayList<>();
		
		//Creating the caches
		private ArrayList<Long> loadedConversationsCache;
		
		MessageUpdateAsyncTask(ConnectionService serviceInstance, Context context, List<SharedValues.ConversationItem> structConversationItems, boolean sendNotifications) {
			//Setting the references
			serviceReference = new WeakReference<>(serviceInstance);
			contextReference = new WeakReference<>(context);
			
			//Setting the values
			this.structConversationItems = structConversationItems;
			this.sendNotifications = sendNotifications;
		}
		
		@Override
		protected void onPreExecute() {
			//Getting the caches
			loadedConversationsCache = new ArrayList<>(Messaging.getLoadedConversations());
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			//Getting the context
			Context context = contextReference.get();
			if(context == null) return null;
			
			//Getting the writable database
			SQLiteDatabase writableDatabase = DatabaseManager.getWritableDatabase(context);
			
			//Iterating over the conversations from the received messages
			Collections.sort(structConversationItems, (value1, value2) -> Long.compare(value1.date, value2.date));
			ArrayList<String> processedConversations = new ArrayList<>();
			ArrayList<ConversationManager.ConversationInfo> incompleteConversations = new ArrayList<>();
			for(SharedValues.ConversationItem conversationItemStruct : structConversationItems) {
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
					for(ConversationManager.ConversationInfo conversationInfo : incompleteConversations)
						if(conversationItemStruct.chatGuid.equals(conversationInfo.getGuid())) {
							parentConversation = conversationInfo;
							break;
						}
				
				//Otherwise retrieving / creating the conversation from the database
				if(parentConversation == null) {
					parentConversation = DatabaseManager.addRetrieveServerCreatedConversationInfo(writableDatabase, context, conversationItemStruct.chatGuid);
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
					else incompleteConversations.add(parentConversation);
				}
				
				//Adding the conversation item to the database
				ConversationManager.ConversationItem conversationItem = DatabaseManager.addConversationItemReplaceGhost(writableDatabase, conversationItemStruct, parentConversation);
				
				//Skipping the remainder of the iteration if the conversation item is invalid
				if(conversationItem == null) continue;
				
				//Checking the conversation item's influence
				if(conversationItem instanceof ConversationManager.GroupActionInfo) {
					//Converting the item to a group action info
					ConversationManager.GroupActionInfo groupActionInfo = (ConversationManager.GroupActionInfo) conversationItem;
					
					//Adding or removing the member on disk
					if(groupActionInfo.actionType == Constants.groupActionInvite) {
						DatabaseManager.addConversationMember(writableDatabase, parentConversation.getLocalID(), groupActionInfo.other, groupActionInfo.color = parentConversation.getNextUserColor());
					}
					else if(groupActionInfo.actionType == Constants.groupActionLeave) DatabaseManager.removeConversationMember(writableDatabase, parentConversation.getLocalID(), groupActionInfo.other);
				} else if(conversationItem instanceof ConversationManager.ChatRenameActionInfo) {
					//Writing the new title to the database
					DatabaseManager.updateConversationTitle(writableDatabase, ((ConversationManager.ChatRenameActionInfo) conversationItem).title, parentConversation.getLocalID());
				}
				
				//Adding the conversation item to the conversation and the list if the conversation is complete
				if(parentConversation.getState() == ConversationManager.ConversationInfo.ConversationState.READY)
					newCompleteConversationItems.add(conversationItem);
				//Otherwise updating the last conversation item
				else if(parentConversation.getLastItem() == null || parentConversation.getLastItem().getDate() < conversationItem.getDate())
					parentConversation.setLastItem(conversationItem.toLightConversationItemSync(context));
				
				//Incrementing the unread count
				if(!loadedConversationsCache.contains(parentConversation.getLocalID())) DatabaseManager.incrementUnreadMessageCount(writableDatabase, parentConversation.getLocalID());
			}
			
			{
				ConnectionService service = serviceReference.get();
				if(service != null) {
					//Loading the users
					//service.loadUsersSync(structConversationItems);
					
					//Checking if there are incomplete conversations
					if(!incompleteConversations.isEmpty()) {
						//Adding the incomplete conversations to the pending conversations
						synchronized(service.pendingConversations) {
							for(ConversationManager.ConversationInfo conversation : incompleteConversations)
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
			List<Long> foregroundConversations = Messaging.getForegroundConversations();
			List<Long> loadedConversations = Messaging.getLoadedConversations();
			
			//Checking if the conversations are loaded in memory
			ArrayList<ConversationManager.ConversationInfo> conversations = ConversationManager.getConversations();
			if(conversations != null) {
				//Iterating over the new conversation items
				for(ConversationManager.ConversationItem conversationItem : newCompleteConversationItems) {
					//Attempting to find the associated parent conversation
					ConversationManager.ConversationInfo parentConversation = findConversationInMemory(conversationItem.getConversationInfo().getLocalID());
					
					//Skipping the remainder of the iteration if no parent conversation could be found
					if(parentConversation == null) continue;
					
					//Setting the conversation item's parent conversation to the found one (the one provided from the DB is not the same as the one in memory)
					conversationItem.setConversationInfo(parentConversation);
					
					//if(parentConversation.getState() != ConversationManager.ConversationInfo.ConversationState.READY) continue;
					
					//Incrementing the conversation's unread count
					parentConversation.setUnreadMessageCount(parentConversation.getUnreadMessageCount() + 1);
					parentConversation.updateUnreadStatus();
					
					//Checking if the conversation is loaded
					if(loadedConversations.contains(parentConversation.getLocalID())) {
						//Adding the conversation item to its parent conversation
						parentConversation.addConversationItem(context, conversationItem);
						
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
						
						//Checking if the conversation is in the foreground
						if(foregroundConversations.contains(parentConversation.getLocalID())) {
							//Displaying the send effect
							if(conversationItem instanceof ConversationManager.MessageInfo && !((ConversationManager.MessageInfo) conversationItem).getSendEffect().isEmpty())
								parentConversation.requestScreenEffect(((ConversationManager.MessageInfo) conversationItem).getSendEffect());
						}
					} else {
						//Updating the parent conversation's latest item
						parentConversation.setLastItem(context, conversationItem);
					}
					
					//Sending notifications
					if(conversationItem instanceof ConversationManager.MessageInfo)
						NotificationUtils.sendNotification(context, (ConversationManager.MessageInfo) conversationItem);
					
					//Downloading the items automatically (if requested)
					if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preference_storage_autodownload_key), false) &&
							conversationItem instanceof ConversationManager.MessageInfo) {
						for(ConversationManager.AttachmentInfo attachmentInfo : ((ConversationManager.MessageInfo) conversationItem).getAttachments())
							attachmentInfo.downloadContent(context);
					}
					
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
			
			//Contacting the server for the pending conversation info
			ConnectionService service = serviceReference.get();
			if(service != null && !service.pendingConversations.isEmpty())
				service.retrievePendingConversationInfo();
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
	
	private static class MassRetrievalAsyncTask extends AsyncTask<Void, Integer, List<ConversationManager.ConversationInfo>> {
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
		protected List<ConversationManager.ConversationInfo> doInBackground(Void... parameters) {
			//Getting the context
			Context context = contextReference.get();
			if(context == null) return null;
			
			//Getting a writable database instance
			SQLiteDatabase writableDatabase = DatabaseManager.getWritableDatabase(context);
			
			//Creating the conversation list
			List<ConversationManager.ConversationInfo> conversationInfoList = new ArrayList<>();
			
			//Creating the progress value
			int progress = 0;
			
			//Iterating over the conversations
			for(SharedValues.ConversationInfo structConversation : structConversationInfos) {
				//Writing the conversation
				conversationInfoList.add(DatabaseManager.addReadyConversationInfo(writableDatabase, context, structConversation));
				
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
				ConversationManager.ConversationItem conversationItem = DatabaseManager.addConversationItem(writableDatabase, structItem, parentConversation);
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
		protected void onProgressUpdate(Integer... values) {
			//Getting the service
			ConnectionService service = serviceReference.get();
			if(service == null) return;
			
			//Updating the progress in the service
			service.massRetrievalProgress = values[0];
			
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
			/* for(Conversations.ConversationsCallbacks callbacks : MainApplication.getConversationsActivityCallbacks())
				callbacks.updateList(false); */
		}
	}
	
	private static class SaveConversationInfoAsyncTask extends AsyncTask<Void, Void, Void> {
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
		protected Void doInBackground(Void... params) {
			//Getting the context
			Context context = contextReference.get();
			
			//Returning if the context is invalid
			if(context == null) return null;
			
			//Getting the database
			SQLiteDatabase writableDatabase = DatabaseManager.getWritableDatabase(context);
			
			//Deleting the unavailable conversations from the database
			for(ConversationManager.ConversationInfo conversation : unavailableConversations) DatabaseManager.deleteConversation(writableDatabase, conversation);
			
			//Checking if there are any available conversations
			if(!availableConversations.isEmpty()) {
				//Iterating over the conversations
				for(Iterator<ConversationInfoRequest> iterator = availableConversations.iterator(); iterator.hasNext(); ) {
					//Getting the conversation
					ConversationManager.ConversationInfo availableConversation = iterator.next().conversationInfo;
					
					//Reading and recording the conversation's items
					ArrayList<ConversationManager.ConversationItem> conversationItems = DatabaseManager.loadConversationItems(writableDatabase, availableConversation);
					availableConversationItems.put(availableConversation.getLocalID(), conversationItems);
					
					//Searching for a matching conversation in the database
					ConversationManager.ConversationInfo clientConversation = DatabaseManager.findConversationInfoWithMembers(writableDatabase, context, Constants.normalizeAddresses(availableConversation.getConversationMembersAsCollection()), availableConversation.getService(), true);
					
					//Checking if a client conversation has been found
					if(clientConversation != null) {
						//Switching the conversation item ownership to the new client conversation
						DatabaseManager.switchMessageOwnership(writableDatabase, availableConversation.getLocalID(), clientConversation.getLocalID());
						for(ConversationManager.ConversationItem item : conversationItems) item.setConversationInfo(clientConversation);
						
						//Recording the conversation details
						transferredConversations.put(clientConversation, new TransferConversationStruct(availableConversation.getGuid(),
								ConversationManager.ConversationInfo.ConversationState.READY,
								availableConversation.getStaticTitle(),
								conversationItems));
						
						//Deleting the available conversation
						DatabaseManager.deleteConversation(writableDatabase, availableConversation);
						unavailableConversations.add(availableConversation);
						
						//Updating the client conversation
						DatabaseManager.copyConversationInfo(writableDatabase, availableConversation, clientConversation, false);
						
						//Marking the the available conversation as invalid (to be deleted)
						iterator.remove();
					} else {
						//Updating the available conversation
						DatabaseManager.updateConversationInfo(writableDatabase, availableConversation, true);
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
				for(ConversationManager.ConversationInfo unavailableConversation : unavailableConversations)
					if(conversations.contains(unavailableConversation))
						conversations.remove(unavailableConversation);
				
				//Adding the available conversations in memory and updating their views
				for(ConversationInfoRequest conversationInfoRequest : availableConversations) {
					ConversationManager.addConversation(conversationInfoRequest.conversationInfo);
					//availableConversation.updateView(ConnectionService.this);
				}
				
				//Updating the transferred conversations
				if(context != null) {
					for(Map.Entry<ConversationManager.ConversationInfo, TransferConversationStruct> pair : transferredConversations.entrySet()) {
						//Retrieving the pair values
						ConversationManager.ConversationInfo conversationInfo = pair.getKey();
						TransferConversationStruct transferData = pair.getValue();
						conversationInfo.setGuid(transferData.guid);
						conversationInfo.setState(transferData.state);
						conversationInfo.setTitle(context, transferData.name);
						for(ConversationManager.ConversationItem item : transferData.conversationItems) conversationInfo.addConversationItem(context, item);
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
	
	private static class ModifierUpdateAsyncTask extends AsyncTask<Void, Void, Void> {
		//Creating the reference values
		private final WeakReference<Context> contextReference;
		
		//Creating the request values
		private final List<SharedValues.ModifierInfo> structModifiers;
		private final List<ConversationManager.StickerInfo> stickerModifiers = new ArrayList<>();
		private final List<ConversationManager.TapbackInfo> tapbackModifiers = new ArrayList<>();
		private final List<TapbackRemovalStruct> tapbackRemovals = new ArrayList<>();
		
		ModifierUpdateAsyncTask(Context context, List<SharedValues.ModifierInfo> structModifiers) {
			//Setting the references
			contextReference = new WeakReference<>(context);
			
			//Setting the values
			this.structModifiers = structModifiers;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			//Getting the context
			Context context = contextReference.get();
			if(context == null) return null;
			
			//Getting the writable database
			SQLiteDatabase writableDatabase = DatabaseManager.getWritableDatabase(context);
			
			//Iterating over the modifiers
			for(SharedValues.ModifierInfo modifierInfo : structModifiers) {
				//Checking if the modifier is an activity status modifier
				if(modifierInfo instanceof SharedValues.ActivityStatusModifierInfo) {
					//Casting to the activity status modifier
					SharedValues.ActivityStatusModifierInfo activityStatusModifierInfo = (SharedValues.ActivityStatusModifierInfo) modifierInfo;
					
					//Updating the modifier in the database
					DatabaseManager.updateMessageState(writableDatabase, activityStatusModifierInfo.message, activityStatusModifierInfo.state, activityStatusModifierInfo.dateRead);
				}
				//Otherwise checking if the modifier is a sticker update
				else if(modifierInfo instanceof SharedValues.StickerModifierInfo) {
					//Updating the modifier in the database
					ConversationManager.StickerInfo sticker = DatabaseManager.addMessageSticker(writableDatabase, (SharedValues.StickerModifierInfo) modifierInfo);
					if(sticker != null) stickerModifiers.add(sticker);
				}
				//Otherwise checking if the modifier is a tapback update
				else if(modifierInfo instanceof SharedValues.TapbackModifierInfo) {
					//Getting the tapback modifier
					SharedValues.TapbackModifierInfo tapbackModifierInfo = (SharedValues.TapbackModifierInfo) modifierInfo;
					
					//Checking if the tapback is negative
					if(tapbackModifierInfo.code >= SharedValues.TapbackModifierInfo.tapbackBaseRemove) {
						//Deleting the modifier in the database
						DatabaseManager.removeMessageTapback(writableDatabase, tapbackModifierInfo);
						tapbackRemovals.add(new TapbackRemovalStruct(tapbackModifierInfo.sender, tapbackModifierInfo.message, tapbackModifierInfo.messageIndex));
					} else {
						//Updating the modifier in the database
						ConversationManager.TapbackInfo tapback = DatabaseManager.addMessageTapback(writableDatabase, tapbackModifierInfo);
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
			for(SharedValues.ModifierInfo modifierInfo : structModifiers) {
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
				if(modifierInfo instanceof SharedValues.ActivityStatusModifierInfo) {
					//Getting the modifier
					SharedValues.ActivityStatusModifierInfo activityStatusModifierInfo = (SharedValues.ActivityStatusModifierInfo) modifierInfo;
					
					//Updating the message
					messageInfo.setMessageState(activityStatusModifierInfo.state);
					messageInfo.setDateRead(activityStatusModifierInfo.dateRead);
					
					//Getting the parent conversation
					ConversationManager.ConversationInfo parentConversation = messageInfo.getConversationInfo();
					
					//Checking if the message is the activity state target
					if(parentConversation.getActivityStateTarget() == messageInfo) {
						//Updating the message's activity state display
						messageInfo.updateActivityStateDisplay(context);
					} else {
						//Comparing (and replacing) the conversation's activity state target
						parentConversation.tryActivityStateTarget(messageInfo, true, context);
					}
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
				messageInfo.addLiveSticker(sticker);
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
				messageInfo.addLiveTapback(tapback);
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
				messageInfo.removeLiveTapback(tapback.sender, tapback.messageIndex);
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
	
	static void cleanConversationItem(SharedValues.ConversationItem conversationItem) {
		//Invalidating text if it is empty
		if(conversationItem instanceof SharedValues.MessageInfo) {
			SharedValues.MessageInfo messageInfo = (SharedValues.MessageInfo) conversationItem;
			if(messageInfo.text != null && messageInfo.text.isEmpty())
				messageInfo.text = null;
			if(messageInfo.sendEffect != null && messageInfo.sendEffect.isEmpty())
				messageInfo.sendEffect = null;
		} else if(conversationItem instanceof SharedValues.ChatRenameActionInfo) {
			SharedValues.ChatRenameActionInfo chatRenameActionInfo = (SharedValues.ChatRenameActionInfo) conversationItem;
			if(chatRenameActionInfo.newChatName != null && chatRenameActionInfo.newChatName.isEmpty())
				chatRenameActionInfo.newChatName = null;
		}
	}
}