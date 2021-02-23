package me.tagavari.airmessage.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.CompletableSubject;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.connection.exception.AMRequestException;
import me.tagavari.airmessage.enums.ConnectionErrorCode;
import me.tagavari.airmessage.enums.ConnectionState;
import me.tagavari.airmessage.enums.MessageSendErrorCode;
import me.tagavari.airmessage.helper.NotificationHelper;
import me.tagavari.airmessage.redux.ReduxEmitterNetwork;
import me.tagavari.airmessage.util.ConversationTarget;

public class ConnectionService extends Service {
	//Creating the constants
	public static final String selfIntentActionConnect = "connect";
	public static final String selfIntentActionDisconnect = "disconnect";
	public static final String selfIntentActionStop = "stop";
	
	public static final String selfIntentExtraConfig = "configuration_mode";
	public static final String selfIntentExtraTemporary = "temporary_mode";
	public static final String selfIntentExtraForeground = "foreground";
	
	private static final long temporaryModeExpiry = 10 * 1000; //10 seconds
	
	private static final List<PendingMessage> pendingMessageList = new ArrayList<>();
	
	//Creating the instance value
	private static ConnectionService instanceReference = null;
	
	//Creating the binder
	private final IBinder binder = new ConnectionBinder();
	
	//Creating the state values
	private boolean isBound = false;
	private boolean isForeground;
	private boolean configurationMode = false;
	private boolean temporaryMode = false;
	private Runnable temporaryModeRunnable = this::stopSelf;
	
	private boolean isInitialConnectionUpdate = true;
	
	//Creating the connection values
	private ConnectionManager connectionManager;
	
	//Creating the disposable values
	private final CompositeDisposable compositeDisposable = new CompositeDisposable();
	
	//Creating the handler
	private final Handler handler = new Handler(Looper.getMainLooper());
	
	/**
	 * Gets the current instance of the connection service
	 */
	@Nullable
	public static ConnectionService getInstance() {
		return instanceReference;
	}
	
	/**
	 * Gets the current instance of the connection service's connection manager
	 */
	@Nullable
	public static ConnectionManager getConnectionManager() {
		if(instanceReference == null) return null;
		return instanceReference.connectionManager;
	}
	
	/**
	 * Add a message request that will be sent when the service connects
	 * @param message The message text to send
	 * @param conversationGUID The GUID of the conversation to send the message to
	 * @return A completable to represent this task
	 */
	public static Completable addPendingMessage(String message, String conversationGUID) {
		CompletableSubject subject = CompletableSubject.create();
		pendingMessageList.add(new PendingMessage(subject, message, conversationGUID));
		return subject;
	}
	
	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		//Updating the state value
		isBound = true;
		
		//Disabling temporary mode and returning to the background
		if(temporaryMode) {
			setTemporaryMode(false);
			setServiceForegroundState(false);
		}
		
		//Returning access to the connection manager
		return binder;
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		//Updating the state value
		isBound = false;
		
		//Just use onBind()
		return false;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		//Initializing the connection manager
		connectionManager = new ConnectionManager(this);
		
		//Setting the service
		instanceReference = this;
		
		//Subscribing to updates in connection changes
		compositeDisposable.addAll(
				ReduxEmitterNetwork.getConnectionStateSubject().subscribe(event -> updateState(event.getState())),
				ReduxEmitterNetwork.getConnectionConfigurationSubject().subscribe(this::setConfigurationMode)
		);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent != null) {
			//Updating configuration mode
			if(intent.hasExtra(selfIntentExtraConfig)) setConfigurationMode(intent.getBooleanExtra(selfIntentExtraConfig, false));
			
			//If the service is bound, don't enable temporary mode and just let Android take care of finishing the service
			setTemporaryMode(intent.getBooleanExtra(selfIntentExtraTemporary, false) && !isBound);
			
			//Checking if a quit has been requested
			if(selfIntentActionStop.equals(intent.getAction())) {
				if(!isBound) {
					//Shutting down the connection service
					stopForeground(true);
					stopSelf();
				}
				
				//Returning not sticky
				return START_NOT_STICKY;
			} else if(selfIntentActionDisconnect.equals(intent.getAction())) {
				//Disconnecting
				connectionManager.disconnect(ConnectionErrorCode.user);
			} else if(selfIntentActionConnect.equals(intent.getAction())) {
				//Reconnecting
				connectionManager.connect();
			}
			
			//Starting the service in the foreground if required
			if(intent.getBooleanExtra(selfIntentExtraForeground, false)) {
				setServiceForegroundState(true);
			}
		}
		
		//Keep this service running
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		//Unsubscribing from connection updates
		compositeDisposable.clear();
		
		//Cleaning up the connection manager
		connectionManager.disconnect(ConnectionErrorCode.user);
		connectionManager.close(this);
		
		//Removing the connection service reference
		instanceReference = null;
	}
	
	/**
	 * Handles responses to a change in connection state
	 */
	private void updateState(@ConnectionState int state) {
		//Updating the foreground notification
		if(isForeground) {
			Notification notification = getDisplayNotification(state);
			NotificationManager notificationManager = getSystemService(NotificationManager.class);
			notificationManager.notify(NotificationHelper.notificationIDConnectionService, notification);
		}
		
		//Ignore connection updates triggered directly when the service attaches to the observable
		if(!isInitialConnectionUpdate && !pendingMessageList.isEmpty()) {
			if(state == ConnectionState.connected) {
				//Sending any pending messages
				for(PendingMessage pendingMessage : pendingMessageList) {
					connectionManager.sendMessage(new ConversationTarget.AppleLinked(pendingMessage.conversationGUID), pendingMessage.message).subscribe(pendingMessage.completableSubject);
				}
				pendingMessageList.clear();
			} else if(state == ConnectionState.disconnected) {
				//Failing any pending messages
				for(PendingMessage pendingMessage : pendingMessageList) {
					pendingMessage.completableSubject.onError(new AMRequestException(MessageSendErrorCode.localNetwork));
				}
				pendingMessageList.clear();
			}
		}
		
		isInitialConnectionUpdate = false;
	}
	
	/**
	 * Updates whether this service is a foreground service or a background service
	 */
	public void setServiceForegroundState(boolean foreground) {
		//Ignoring if no changes are to be made
		if(isForeground == foreground) return;
		
		//Setting the value
		isForeground = foreground;
		
		if(foreground) {
			//Starting the foreground state
			startForeground(NotificationHelper.notificationIDConnectionService, getDisplayNotification());
		} else {
			//Stopping the foreground state
			stopForeground(true);
		}
	}
	
	/**
	 * Sets if this service should update its notification to reflect configuration mode status
	 */
	public void setConfigurationMode(boolean configurationMode) {
		if(this.configurationMode == configurationMode) return;
		
		//Updating the value
		this.configurationMode = configurationMode;
		
		//Updating the foreground notification
		if(isForeground) {
			Notification notification = getDisplayNotification(connectionManager.getState());
			NotificationManager notificationManager = getSystemService(NotificationManager.class);
			notificationManager.notify(NotificationHelper.notificationIDConnectionService, notification);
		}
	}
	
	/**
	 * Sets if this service should automatically stop itself after a set amount of time
	 */
	public void setTemporaryMode(boolean temporaryMode) {
		if(this.temporaryMode == temporaryMode) return;
		
		//Updating the value
		this.temporaryMode = temporaryMode;
		
		//Updating the timer
		if(temporaryMode) {
			handler.postDelayed(temporaryModeRunnable, temporaryModeExpiry);
		} else {
			handler.removeCallbacks(temporaryModeRunnable);
		}
	}
	
	/**
	 * Refreshes the temporary mode expiry clock
	 */
	public void refreshTemporaryMode() {
		handler.removeCallbacks(temporaryModeRunnable);
		handler.postDelayed(temporaryModeRunnable, temporaryModeExpiry);
	}
	
	/**
	 * Gets the notification to display based on the current state of the service
	 * @return A notification to represent this service
	 */
	private Notification getDisplayNotification() {
		return getDisplayNotification(connectionManager.getState());
	}
	
	/**
	 * Gets the notification to display based on the current state of the service
	 * @param connectionState The state of the connection
	 * @return A notification to represent this service
	 */
	private Notification getDisplayNotification(@ConnectionState int connectionState) {
		if(configurationMode) {
			return NotificationHelper.getConnectionConfigurationNotification(this);
		} if(temporaryMode) {
			return NotificationHelper.getTemporaryModeNotification(this);
		} else if(connectionState == ConnectionState.disconnected) {
			return NotificationHelper.getConnectionOfflineNotification(this, false);
		} else {
			return NotificationHelper.getConnectionBackgroundNotification(this, connectionState == ConnectionState.connected, false);
		}
	}
	
	public class ConnectionBinder extends Binder {
		public ConnectionService getConnectionService() {
			return ConnectionService.this;
		}
		
		public ConnectionManager getConnectionManager() {
			return connectionManager;
		}
	}
	
	private static class PendingMessage {
		final CompletableSubject completableSubject;
		final String message;
		final String conversationGUID;
		
		public PendingMessage(CompletableSubject completableSubject, String message, String conversationGUID) {
			this.completableSubject = completableSubject;
			this.message = message;
			this.conversationGUID = conversationGUID;
		}
	}
}