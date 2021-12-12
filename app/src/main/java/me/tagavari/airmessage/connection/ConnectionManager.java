package me.tagavari.airmessage.connection;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.CompletableSubject;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.SingleSubject;
import kotlin.Pair;
import kotlin.Unit;
import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.activity.Messaging;
import me.tagavari.airmessage.common.Blocks;
import me.tagavari.airmessage.connection.comm4.ClientComm4;
import me.tagavari.airmessage.connection.comm5.ClientComm5;
import me.tagavari.airmessage.connection.exception.AMRemoteUpdateException;
import me.tagavari.airmessage.connection.exception.AMRequestException;
import me.tagavari.airmessage.connection.listener.CommunicationsManagerListener;
import me.tagavari.airmessage.connection.request.FileFetchRequest;
import me.tagavari.airmessage.connection.request.MassRetrievalRequest;
import me.tagavari.airmessage.connection.task.ChatResponseTask;
import me.tagavari.airmessage.connection.task.MessageUpdateTask;
import me.tagavari.airmessage.connection.task.ModifierUpdateTask;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.data.SharedPreferencesManager;
import me.tagavari.airmessage.enums.*;
import me.tagavari.airmessage.helper.ConversationColorHelper;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.StickerInfo;
import me.tagavari.airmessage.messaging.TapbackInfo;
import me.tagavari.airmessage.redux.*;
import me.tagavari.airmessage.util.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ConnectionManager {
	private static final String TAG = ConnectionManager.class.getSimpleName();
	
	//Constants
	private static final List<CommunicationsManagerFactory> communicationsPriorityList = Arrays.asList(ClientComm5::new, ClientComm4::new);
	
	private static final long pingExpiryTime = 40 * 1000; //40 seconds
	private static final long keepAliveMillis = 20 * 60 * 1000; //30 * 60 * 1000; //20 minutes
	private static final long keepAliveWindowMillis = 5 * 60 * 1000; //5 minutes
	private static final long[] immediateReconnectDelayMillis = {1000, 2 * 1000}; //1 second, 2 seconds
	private static final long backgroundReconnectFrequencyMillis = 10 * 60 * 1000; //10 minutes
	
	private static final long requestTimeoutSeconds = 24;
	
	private static final String intentActionPing = "me.tagavari.airmessage.connection.ConnectionManager-Ping";
	private static final String intentActionBackgroundReconnect = "me.tagavari.airmessage.connection.ConnectionManager-BackgroundReconnect";
	
	//Schedulers
	private final Scheduler uploadScheduler = Schedulers.from(Executors.newSingleThreadExecutor(), true);
	
	//Handler
	private final Handler handler = new Handler(Looper.getMainLooper());
	
	//Random
	private final Random random = new Random();
	
	//Receivers
	private final BroadcastReceiver pingBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(isConnected()) testConnection();
			else pingExpiryRunnable.run();
		}
	};
	private final BroadcastReceiver backgroundReconnectBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			connectFromList(getContext(), 0);
		}
	};
	private final Runnable pingExpiryRunnable = () -> disconnect(ConnectionErrorCode.connection);
	
	//Intents
	private final PendingIntent pingPendingIntent, reconnectPendingIntent;
	
	//Connection values
	private CommunicationsManager<?> communicationsManager = null;
	private final Runnable immediateReconnectRunnable = () -> connectFromList(getContext(), 0);
	private int immediateReconnectIndex = 0;
	
	//Connection state values
	/*
	 * An up-to-date value that represents whether we're disconnected, connecting, or connected
	 */
	@ConnectionState private int connState = ConnectionState.disconnected;
	/*
	 * Represents the way in which we respond to connections and disconnections
	 * user - display connection state 1:1, switch to immediate if connection established, otherwise switch to background
	 * immediate - display connection state as "connecting", perform connections rapidly; only to be used after a connection has been established then lost
	 * background - display connection state as "disconnected", perform connections in the background every so often
	 */
	@ConnectionMode private int connMode = ConnectionMode.user;
	private short currentRequestID = 0;
	private int currentCommunicationsIndex = 0;
	
	//Request state values
	private final Map<String, ConversationInfo> pendingConversations = new HashMap<>();
	private boolean isMassRetrievalInProgress = false;
	private boolean isPendingSync = false;
	
	//Server information
	@Nullable
	private String serverInstallationID, serverDeviceName, serverSystemVersion, serverSoftwareVersion;
	private boolean serverSupportsFaceTime;
	
	//Composite disposable
	private final CompositeDisposable compositeDisposable = new CompositeDisposable();
	
	//Response values
	private final Map<Short, RequestSubject<?, ?>> idRequestSubjectMap = new HashMap<>(); //For ID-based requests
	private SingleSubject<String> faceTimeLinkSubject = null;
	private CompletableSubject faceTimeInitiateSubject = null;

	//State values
	private boolean disableReconnections = false;
	@Nullable private ConnectionOverride<?> connectionOverride = null;
	
	public ConnectionManager(Context context) {
		//Registering broadcast listeners
		pingPendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(intentActionPing), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
		context.registerReceiver(pingBroadcastReceiver, new IntentFilter(intentActionPing));
		reconnectPendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(intentActionBackgroundReconnect), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
		context.registerReceiver(backgroundReconnectBroadcastReceiver, new IntentFilter(intentActionBackgroundReconnect));
		
		//Loading pending conversations from the database
		Single.fromCallable(() -> DatabaseManager.getInstance().fetchConversationsWithState(context, ConversationState.incompleteServer))
				.observeOn(Schedulers.single())
				.subscribeOn(AndroidSchedulers.mainThread())
				.doOnSuccess(conversations -> {
					for(ConversationInfo conversation : conversations) {
						pendingConversations.put(conversation.getGUID(), conversation);
					}
				}).subscribe();
	}
	
	/**
	 * Cleans up this connection manager
	 */
	public void close(Context context) {
		//Clearing all subscriptions
		compositeDisposable.clear();
		
		//Shutting down schedulers
		uploadScheduler.shutdown();
		
		//Unregistering the receivers
		context.unregisterReceiver(pingBroadcastReceiver);
		context.unregisterReceiver(backgroundReconnectBroadcastReceiver);
		
		//Cancelling connection test timers
		cancelConnectionTest(context);
		
		//Cancelling all reconnection timers
		stopCurrentMode();
	}
	
	private Context getContext() {
		return MainApplication.getInstance();
	}
	
	//Listener values
	private final CommunicationsManagerListener communicationsManagerListener = new CommunicationsManagerListener() {
		@Override
		public void onOpen(String installationID, String deviceName, String systemVersion, String softwareVersion, boolean supportsFaceTime) {
			//Recording the server information
			serverInstallationID = installationID;
			serverDeviceName = deviceName;
			serverSystemVersion = systemVersion;
			serverSoftwareVersion = softwareVersion;
			serverSupportsFaceTime = supportsFaceTime;
			
			//Updating shared preferences
			long lastConnectionTime = SharedPreferencesManager.getLastConnectionTime(getContext());
			SharedPreferencesManager.setLastConnectionTime(getContext(), System.currentTimeMillis());
			SharedPreferencesManager.setServerSupportsFaceTime(getContext(), serverSupportsFaceTime);
			
			//Updating the state
			if(connMode != ConnectionMode.user) stopCurrentMode();
			connMode = ConnectionMode.user;
			
			connState = ConnectionState.connected;
			emitStateConnected();

			//Updating the FaceTime state
			ReduxEmitterNetwork.getServerFaceTimeSupportSubject().onNext(serverSupportsFaceTime);
			
			//Checking if an installation ID was provided
			boolean isNewServer; //Is this server different from the one we connected to last time?
			boolean isNewServerSinceSync; //Is this server different from the one we connected to last time, since we last synced our messages?
			if(installationID != null) {
				//Getting the last installation ID
				String lastInstallationID = SharedPreferencesManager.getLastConnectionInstallationID(getContext());
				String lastInstallationIDSinceSync = SharedPreferencesManager.getLastSyncInstallationID(getContext());
				
				//If the installation ID changed, we are connected to a new server
				isNewServer = !installationID.equals(lastInstallationID);
				
				//Updating the saved value
				if(isNewServer) SharedPreferencesManager.setLastConnectionInstallationID(getContext(), installationID);
				
				//"notrigger" is assigned to this value when upgrading from 0.5.X to prevent sync prompts after upgrading
				if("notrigger".equals(lastInstallationIDSinceSync)) {
					//Don't sync messages
					isNewServerSinceSync = false;
					
					//Update the saved value for next time
					SharedPreferencesManager.setLastSyncInstallationID(getContext(), installationID);
				} else {
					isNewServerSinceSync = !installationID.equals(lastInstallationIDSinceSync);
				}
			} else {
				//No way to tell
				isNewServer = false;
				isNewServerSinceSync = false;
			}
			
			//Retrieving the pending conversation info
			fetchPendingConversations();
			
			//Checking if we are connected to a new server
			if(isNewServer) {
				//Resetting the last message ID
				SharedPreferencesManager.removeLastServerMessageID(getContext());
			} else {
				long lastServerMessageID = SharedPreferencesManager.getLastServerMessageID(getContext());
				
				//Fetching missed messages
				if(communicationsManager.isFeatureSupported(ConnectionFeature.idBasedRetrieval) && lastServerMessageID != -1) {
					//Fetching messages since the last message ID
					requestMessagesIDRange(lastServerMessageID, lastConnectionTime, System.currentTimeMillis());
				} else {
					//Fetching the messages since the last connection time
					requestMessagesTimeRange(lastConnectionTime, System.currentTimeMillis());
				}
			}
			
			//Checking if we are connected to a new server since syncing (and thus should prompt the user to sync)
			if(isNewServerSinceSync) {
				//Setting the state
				isPendingSync = true;
				
				//Sending an update
				ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.Sync(serverInstallationID, serverDeviceName));
			}
		}
		
		@Override
		public void onClose(@ConnectionErrorCode int errorCode) {
			//Getting if we have already established a connection
			boolean connectionEstablished = connState == ConnectionState.connected;
			
			//Updating the state
			connState = ConnectionState.disconnected;
			
			//Cleaning up after an established connection
			if(connectionEstablished) {
				//Failing all pending requests
				for(RequestSubject<?, ?> subject : new ArrayList<>(idRequestSubjectMap.values())) subject.onExpire();
				
				//Clearing the pending sync state
				isPendingSync = false;
				
				//Cancelling connection test timers
				cancelConnectionTest(getContext());

				//Removing any pending updates
				ReduxEmitterNetwork.getRemoteUpdateSubject().onNext(Optional.empty());
			}
			
			//Checking if the disconnection is recoverable
			if(errorCode == ConnectionErrorCode.connection || errorCode == ConnectionErrorCode.internet || errorCode == ConnectionErrorCode.externalError || errorCode == ConnectionErrorCode.connectOtherLocation) {
				//Checking if we have yet to establish a proper connection, and there are older protocol versions available to use
				if(!connectionEstablished && currentCommunicationsIndex + 1 < communicationsPriorityList.size()) {
					//Leave the state as connecting and fall back to an older protocol
					boolean connected = connectFromList(getContext(), currentCommunicationsIndex + 1);
					if(connected) return;
				}
				
				//Checking if we have already established a connection, and we are allowed to run automatic reconnections
				if((connectionEstablished || connMode == ConnectionMode.immediate) && !disableReconnections) {
					//Trying to start an immediate reconnection
					boolean result;
					if(connMode != ConnectionMode.immediate) {
						stopCurrentMode();
						connMode = ConnectionMode.immediate;
						
						result = scheduleImmediateReconnect(true);
						
						if(result) emitStateConnecting();
					} else {
						result = scheduleImmediateReconnect(false);
					}
					
					//If we succeeded in scheduling an immediate reconnect, don't switch to background mode
					if(result) return;
				}
			}
			
			if(!disableReconnections) {
				//Starting background mode
				if(connMode != ConnectionMode.background) {
					connMode = ConnectionMode.background;
					scheduleRepeatingBackgroundReconnect(getContext());
					emitStateDisconnected(errorCode);
				} else return; //Don't spam the user with disconnected notifications
			}
			
			emitStateDisconnected(errorCode);
		}
		
		@Override
		public void onPacket() {
			if(connState == ConnectionState.connected) {
				//Updating the last connection time
				SharedPreferencesManager.setLastConnectionTime(getContext(), System.currentTimeMillis());
				
				//Resetting connection tests
				resetConnectionTest(getContext());
			}
		}
		
		@Override
		public void onMessageUpdate(Collection<Blocks.ConversationItem> data) {
			//Filtering out data that would be received over FCM
			Collection<Blocks.ConversationItem> filteredData;
			if(communicationsManager.getDataProxyType() == ProxyType.connect && communicationsManager.isFeatureSupported(ConnectionFeature.payloadPushNotifications)) {
				filteredData = data.stream().filter(item -> !(item instanceof Blocks.MessageInfo && ((Blocks.MessageInfo) item).sender != null)).collect(Collectors.toList());
			} else {
				filteredData = data;
			}
			if(filteredData.isEmpty()) return;
			
			//Loading the foreground conversations (needs to be done on the main thread)
			Single.fromCallable(Messaging::getForegroundConversations)
					.subscribeOn(AndroidSchedulers.mainThread())
					.flatMap(foregroundConversations -> MessageUpdateTask.create(getContext(), foregroundConversations, filteredData, false))
					.observeOn(AndroidSchedulers.mainThread())
					.doOnSuccess(response -> {
						//Emitting any generated events
						for(ReduxEventMessaging event : response.getEvents()) {
							ReduxEmitterNetwork.getMessageUpdateSubject().onNext(event);
						}
						
						//Fetching pending conversations
						addPendingConversations(response.getIncompleteServerConversations());
					}).subscribe();
		}
		
		@Override
		public void onMassRetrievalStart(short requestID, Collection<Blocks.ConversationInfo> conversations, int messageCount) {
			//Getting the request
			RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest> subject = (RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest>) idRequestSubjectMap.get(requestID);
			if(subject == null) return;
			
			//Initializing the request
			MassRetrievalRequest massRetrievalRequest = subject.getRequestData();
			compositeDisposable.add(
					massRetrievalRequest.handleInitialInfo(conversations, messageCount)
							.subscribe((addedConversations) -> {
								//Getting the request
								RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest> localSubject = (RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest>) idRequestSubjectMap.get(requestID);
								if(localSubject == null) return;
								
								ReduxEventMassRetrieval event = new ReduxEventMassRetrieval.Start(massRetrievalRequest.getRequestID(), addedConversations, messageCount);
								localSubject.get().onNext(event);
								ReduxEmitterNetwork.getMassRetrievalUpdateSubject().onNext(event);
							}, (error) -> {
								//Getting the request
								RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest> localSubject = (RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest>) idRequestSubjectMap.get(requestID);
								if(localSubject == null) return;
								
								if(error instanceof IllegalArgumentException) {
									localSubject.onError(new AMRequestException(MassRetrievalErrorCode.localBadResponse, error));
								} else {
									localSubject.onError(new AMRequestException(MassRetrievalErrorCode.unknown, error));
								}
								idRequestSubjectMap.remove(requestID);
							})
			);
		}
		
		@Override
		public void onMassRetrievalUpdate(short requestID, int responseIndex, Collection<Blocks.ConversationItem> data) {
			//Getting the request
			RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest> subject = (RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest>) idRequestSubjectMap.get(requestID);
			if(subject == null) return;
			
			//Saving the data
			MassRetrievalRequest massRetrievalRequest = subject.getRequestData();
			compositeDisposable.add(
					massRetrievalRequest.handleMessages(getContext(), responseIndex, data)
							.subscribe((addedItems) -> {
								//Getting the request
								RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest> localSubject = (RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest>) idRequestSubjectMap.get(requestID);
								if(localSubject == null) return;
								
								ReduxEventMassRetrieval event = new ReduxEventMassRetrieval.Progress(massRetrievalRequest.getRequestID(), addedItems, massRetrievalRequest.getMessagesReceived(), massRetrievalRequest.getTotalMessageCount());
								
								localSubject.get().onNext(event);
								ReduxEmitterNetwork.getMassRetrievalUpdateSubject().onNext(event);
							}, (error) -> {
								//Getting the request
								RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest> localSubject = (RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest>) idRequestSubjectMap.get(requestID);
								if(localSubject == null) return;
								
								if(error instanceof IllegalArgumentException) {
									localSubject.onError(new AMRequestException(MassRetrievalErrorCode.localBadResponse, error));
								} else {
									localSubject.onError(new AMRequestException(MassRetrievalErrorCode.unknown, error));
								}
								idRequestSubjectMap.remove(requestID);
							})
			);
		}
		
		@Override
		public void onMassRetrievalComplete(short requestID) {
			//Getting the request
			RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest> subject = (RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest>) idRequestSubjectMap.get(requestID);
			if(subject == null) return;
			
			//Completing the request
			MassRetrievalRequest massRetrievalRequest = subject.getRequestData();
			compositeDisposable.add(
					massRetrievalRequest.complete()
							.subscribe(() -> {
								//Getting the request
								RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest> localSubject = (RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest>) idRequestSubjectMap.get(requestID);
								if(localSubject == null) return;
								
								ReduxEventMassRetrieval event = new ReduxEventMassRetrieval.Complete(massRetrievalRequest.getRequestID());
								localSubject.get().onNext(event);
								ReduxEmitterNetwork.getMassRetrievalUpdateSubject().onNext(event);
								localSubject.onComplete();
							}, (error) -> {
								//Getting the request
								RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest> localSubject = (RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest>) idRequestSubjectMap.get(requestID);
								if(localSubject == null) return;
								
								massRetrievalRequest.cancel();
								localSubject.onError(new AMRequestException(MassRetrievalErrorCode.localBadResponse));
								idRequestSubjectMap.remove(requestID);
							})
			);
		}
		
		@Override
		public void onMassRetrievalFail(short requestID) {
			//Getting the request
			RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest> subject = (RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest>) idRequestSubjectMap.get(requestID);
			if(subject == null) return;
			
			//Cancelling the request
			MassRetrievalRequest massRetrievalRequest = subject.getRequestData();
			try {
				massRetrievalRequest.cancel();
			} catch(IOException exception) {
				exception.printStackTrace();
			}
			
			//Failing the request
			subject.onError(new AMRequestException(MassRetrievalErrorCode.localBadResponse));
			idRequestSubjectMap.remove(requestID);
		}
		
		@Override
		public void onMassRetrievalFileStart(short requestID, String fileGUID, String fileName, @Nullable String downloadFileName, @Nullable String downloadFileType, @Nullable Function<OutputStream, OutputStream> streamWrapper) {
			//Getting the request
			RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest> subject = (RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest>) idRequestSubjectMap.get(requestID);
			if(subject == null) return;
			
			//Initializing the attachment request
			MassRetrievalRequest massRetrievalRequest = subject.getRequestData();
			compositeDisposable.add(
					massRetrievalRequest.initializeAttachment(getContext(), fileGUID, fileName, downloadFileName, downloadFileType, streamWrapper)
							.subscribe(() -> {
								//Getting the request
								RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest> localSubject = (RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest>) idRequestSubjectMap.get(requestID);
								if(localSubject == null) return;
								
								ReduxEventMassRetrieval event = new ReduxEventMassRetrieval.File(massRetrievalRequest.getRequestID());
								localSubject.get().onNext(event);
								ReduxEmitterNetwork.getMassRetrievalUpdateSubject().onNext(event);
							}, (error) -> {
								//Getting the request
								RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest> localSubject = (RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest>) idRequestSubjectMap.get(requestID);
								if(localSubject == null) return;
								
								if(error instanceof IOException) {
									localSubject.onError(new AMRequestException(MassRetrievalErrorCode.localIO, error));
								} else if(error instanceof IllegalArgumentException) {
									localSubject.onError(new AMRequestException(MassRetrievalErrorCode.localBadResponse, error));
								} else {
									localSubject.onError(new AMRequestException(MassRetrievalErrorCode.unknown, error));
								}
								idRequestSubjectMap.remove(requestID);
							})
			);
		}
		
		@Override
		public void onMassRetrievalFileProgress(short requestID, int responseIndex, String fileGUID, byte[] fileData) {
			//Getting the request
			RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest> subject = (RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest>) idRequestSubjectMap.get(requestID);
			if(subject == null) return;
			
			//Writing the data
			MassRetrievalRequest massRetrievalRequest = subject.getRequestData();
			compositeDisposable.add(
					massRetrievalRequest.writeChunkAttachment(fileGUID, responseIndex, fileData)
							.subscribe(() -> {
								//Getting the request
								RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest> localSubject = (RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest>) idRequestSubjectMap.get(requestID);
								if(localSubject == null) return;
								
								ReduxEventMassRetrieval event = new ReduxEventMassRetrieval.File(massRetrievalRequest.getRequestID());
								localSubject.get().onNext(event);
								ReduxEmitterNetwork.getMassRetrievalUpdateSubject().onNext(event);
							}, (error) -> {
								//Getting the request
								RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest> localSubject = (RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest>) idRequestSubjectMap.get(requestID);
								if(localSubject == null) return;
								
								if(error instanceof IOException) {
									localSubject.onError(new AMRequestException(MassRetrievalErrorCode.localIO, error));
								} else if(error instanceof IllegalArgumentException) {
									localSubject.onError(new AMRequestException(MassRetrievalErrorCode.localBadResponse, error));
								} else {
									localSubject.onError(new AMRequestException(MassRetrievalErrorCode.unknown, error));
								}
								idRequestSubjectMap.remove(requestID);
							})
			);
		}
		
		@Override
		public void onMassRetrievalFileComplete(short requestID, String fileGUID) {
			//Getting the request
			RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest> subject = (RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest>) idRequestSubjectMap.get(requestID);
			if(subject == null) return;
			
			//Writing the data
			MassRetrievalRequest massRetrievalRequest = subject.getRequestData();
			compositeDisposable.add(
					massRetrievalRequest.finishAttachment(getContext(), fileGUID)
							.subscribe(() -> {
								//Getting the request
								RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest> localSubject = (RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest>) idRequestSubjectMap.get(requestID);
								if(localSubject == null) return;
								
								ReduxEventMassRetrieval event = new ReduxEventMassRetrieval.File(massRetrievalRequest.getRequestID());
								localSubject.get().onNext(event);
								ReduxEmitterNetwork.getMassRetrievalUpdateSubject().onNext(event);
							}, (error) -> {
								//Getting the request
								RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest> localSubject = (RequestSubject.Publish<ReduxEventMassRetrieval, MassRetrievalRequest>) idRequestSubjectMap.get(requestID);
								if(localSubject == null) return;
								
								if(error instanceof IOException) {
									localSubject.onError(new AMRequestException(MassRetrievalErrorCode.localIO));
								} else if(error instanceof IllegalArgumentException) {
									localSubject.onError(new AMRequestException(MassRetrievalErrorCode.localBadResponse));
								} else {
									localSubject.onError(new AMRequestException(MassRetrievalErrorCode.unknown));
								}
								idRequestSubjectMap.remove(requestID);
							})
			);
		}
		
		@Override
		public void onConversationUpdate(Collection<Blocks.ConversationInfo> data) {
			List<ConversationInfo> unavailableConversations = new ArrayList<>();
			List<ConversationInfo> availableConversations = new ArrayList<>();
			
			for(Blocks.ConversationInfo structConversationInfo : data) {
				//Finding the conversation in the pending list
				ConversationInfo conversationInfo = pendingConversations.get(structConversationInfo.guid);
				if(conversationInfo == null) continue;
				pendingConversations.remove(structConversationInfo.guid);
				
				//Ignoring if the conversation is not in the state 'incomplete server'
				if(conversationInfo.getState() != ConversationState.incompleteServer) continue;
				
				//Checking if the conversation is available
				if(structConversationInfo.available) {
					//Setting the conversation details
					conversationInfo.setServiceType(structConversationInfo.service);
					conversationInfo.setTitle(structConversationInfo.name);
					conversationInfo.setConversationColor(ConversationColorHelper.getDefaultConversationColor(conversationInfo.getGUID()));
					conversationInfo.setMembers(ConversationColorHelper.getColoredMembers(Arrays.asList(structConversationInfo.members), conversationInfo.getConversationColor(), conversationInfo.getGUID()));
					conversationInfo.setState(ConversationState.ready);
					
					//Marking the conversation as valid (and to be saved)
					availableConversations.add(conversationInfo);
				}
				//Otherwise marking the conversation as invalid
				else unavailableConversations.add(conversationInfo);
			}
			
			//Creating and running the asynchronous task
			ChatResponseTask.create(getContext(), availableConversations, unavailableConversations)
					.doOnSuccess(result -> {
						ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.ConversationUpdate(result.getAvailableConversationItems(), result.getTransferredConversations()));
					}).subscribe();
		}
		
		@Override
		public void onModifierUpdate(Collection<Blocks.ModifierInfo> data) {
			//Filtering out data that would be received over FCM
			Collection<Blocks.ModifierInfo> filteredData;
			if(communicationsManager.getDataProxyType() == ProxyType.connect && communicationsManager.isFeatureSupported(ConnectionFeature.payloadPushNotifications)) {
				filteredData = data.stream().filter(item -> !(item instanceof Blocks.TapbackModifierInfo && ((Blocks.TapbackModifierInfo) item).sender != null)).collect(Collectors.toList());
			} else {
				filteredData = data;
			}
			if(filteredData.isEmpty()) return;
			
			//Writing modifiers to disk
			ModifierUpdateTask.create(getContext(), filteredData).doOnSuccess(result -> {
				//Pushing emitter updates
				for(ActivityStatusUpdate statusUpdate : result.getActivityStatusUpdates()) {
					ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.MessageState(statusUpdate.getMessageID(), statusUpdate.getMessageState(), statusUpdate.getDateRead()));
				}
				for(Pair<StickerInfo, ModifierMetadata> sticker : result.getStickerModifiers()) ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.StickerAdd(sticker.getFirst(), sticker.getSecond()));
				for(Pair<TapbackInfo, ModifierMetadata> tapback : result.getTapbackModifiers()) ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.TapbackUpdate(tapback.getFirst(), tapback.getSecond(), true));
				for(Pair<TapbackInfo, ModifierMetadata> tapback : result.getTapbackRemovals()) ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.TapbackUpdate(tapback.getFirst(), tapback.getSecond(), false));
			}).subscribe();
		}
		
		@Override
		public void onFileRequestStart(short requestID, @Nullable String downloadFileName, @Nullable String downloadFileType, long fileLength, @Nullable Function<OutputStream, OutputStream> streamWrapper) {
			//Getting the request
			RequestSubject.Publish<ReduxEventAttachmentDownload, FileFetchRequest> subject = (RequestSubject.Publish<ReduxEventAttachmentDownload, FileFetchRequest>) idRequestSubjectMap.get(requestID);
			if(subject == null) return;
			
			//Initializing the request
			FileFetchRequest fileFetchRequest = subject.getRequestData();
			try {
				fileFetchRequest.initialize(getContext(), downloadFileName, downloadFileType, fileLength, streamWrapper);
			} catch(IOException exception) {
				subject.onError(new AMRequestException(AttachmentReqErrorCode.localIO));
				idRequestSubjectMap.remove(requestID);
				return;
			}
			
			//Sending an update
			subject.get().onNext(new ReduxEventAttachmentDownload.Start(fileLength));
		}
		
		@Override
		public void onFileRequestData(short requestID, int responseIndex, byte[] data) {
			//Getting the request
			RequestSubject.Publish<ReduxEventAttachmentDownload, FileFetchRequest> subject = (RequestSubject.Publish<ReduxEventAttachmentDownload, FileFetchRequest>) idRequestSubjectMap.get(requestID);
			if(subject == null) return;
			
			//Writing the data
			FileFetchRequest fileFetchRequest = subject.getRequestData();
			compositeDisposable.add(
					fileFetchRequest.writeChunk(responseIndex, data).subscribe((writtenLength) -> {
						//Getting the request
						RequestSubject.Publish<ReduxEventAttachmentDownload, FileFetchRequest> localSubject = (RequestSubject.Publish<ReduxEventAttachmentDownload, FileFetchRequest>) idRequestSubjectMap.get(requestID);
						if(localSubject == null) return;
						
						localSubject.get().onNext(new ReduxEventAttachmentDownload.Progress(writtenLength, fileFetchRequest.getTotalLength()));
					}, (error) -> {
						//Getting the request
						RequestSubject.Publish<ReduxEventAttachmentDownload, FileFetchRequest> localSubject = (RequestSubject.Publish<ReduxEventAttachmentDownload, FileFetchRequest>) idRequestSubjectMap.get(requestID);
						if(localSubject == null) return;
						
						if(error instanceof IOException) {
							localSubject.onError(new AMRequestException(AttachmentReqErrorCode.localIO, error));
						} else if(error instanceof IllegalArgumentException) {
							localSubject.onError(new AMRequestException(AttachmentReqErrorCode.localBadResponse, error));
						} else {
							localSubject.onError(new AMRequestException(AttachmentReqErrorCode.unknown, error));
						}
						idRequestSubjectMap.remove(requestID);
					})
			);
		}
		
		@Override
		public void onFileRequestComplete(short requestID) {
			//Getting the request
			RequestSubject.Publish<ReduxEventAttachmentDownload, FileFetchRequest> subject = (RequestSubject.Publish<ReduxEventAttachmentDownload, FileFetchRequest>) idRequestSubjectMap.get(requestID);
			if(subject == null) return;
			
			//Completing the request
			FileFetchRequest fileFetchRequest = subject.getRequestData();
			compositeDisposable.add(
					fileFetchRequest.complete(getContext()).subscribe((attachmentFile) -> {
						//Getting the request
						RequestSubject.Publish<ReduxEventAttachmentDownload, FileFetchRequest> localSubject = (RequestSubject.Publish<ReduxEventAttachmentDownload, FileFetchRequest>) idRequestSubjectMap.get(requestID);
						if(localSubject == null) return;
						
						localSubject.get().onNext(new ReduxEventAttachmentDownload.Complete(attachmentFile));
						localSubject.onComplete();
						ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.AttachmentFile(fileFetchRequest.getMessageID(), fileFetchRequest.getAttachmentID(), attachmentFile, fileFetchRequest.getDownloadFileName(), fileFetchRequest.getDownloadFileType()));
					}, (error) -> {
						//Getting the request
						RequestSubject.Publish<ReduxEventAttachmentDownload, FileFetchRequest> localSubject = (RequestSubject.Publish<ReduxEventAttachmentDownload, FileFetchRequest>) idRequestSubjectMap.get(requestID);
						if(localSubject == null) return;
						
						localSubject.onError(new AMRequestException(AttachmentReqErrorCode.localIO));
						idRequestSubjectMap.remove(requestID);
					})
			);
		}
		
		@Override
		public void onFileRequestFail(short requestID, @AttachmentReqErrorCode int errorCode) {
			//Getting the request
			RequestSubject.Publish<ReduxEventAttachmentDownload, FileFetchRequest> subject = (RequestSubject.Publish<ReduxEventAttachmentDownload, FileFetchRequest>) idRequestSubjectMap.get(requestID);
			if(subject == null) return;
			
			//Failing the request
			subject.onError(new AMRequestException(errorCode));
			idRequestSubjectMap.remove(requestID);
		}
		
		@Override
		public void onIDUpdate(long messageID) {
			SharedPreferencesManager.setLastServerMessageID(getContext(), messageID);
		}
		
		@Override
		public void onSendMessageSuccess(short requestID) {
			//Resolving the completable
			RequestSubject.EmptyCompletable<?, ?> subject = (RequestSubject.EmptyCompletable<?, ?>) idRequestSubjectMap.get(requestID);
			if(subject == null) return;
			
			subject.onComplete();
			
			idRequestSubjectMap.remove(requestID);
		}
		
		@Override
		public void onSendMessageFail(short requestID, CompoundErrorDetails.MessageSend error) {
			//Failing the completable
			RequestSubject<?, ?> subject = idRequestSubjectMap.get(requestID);
			if(subject == null) return;
			
			subject.onError(error.toException());
			
			idRequestSubjectMap.remove(requestID);
		}
		
		@Override
		public void onCreateChatSuccess(short requestID, String chatGUID) {
			//Resolving the completable
			RequestSubject.Single<String, ?> subject = (RequestSubject.Single<String, ?>) idRequestSubjectMap.get(requestID);
			if(subject == null) return;
			
			subject.get().onSuccess(chatGUID);
			
			idRequestSubjectMap.remove(requestID);
		}
		
		@Override
		public void onCreateChatError(short requestID, CompoundErrorDetails.ChatCreate error) {
			//Failing the completable
			RequestSubject<?, ?> subject = idRequestSubjectMap.get(requestID);
			if(subject == null) return;
			
			subject.onError(error.toException());
			
			idRequestSubjectMap.remove(requestID);
		}

		@Override
		public void onSoftwareUpdateListing(@Nullable ServerUpdateData updateData) {
			ReduxEmitterNetwork.getRemoteUpdateSubject().onNext(Optional.ofNullable(updateData));
		}

		@Override
		public void onSoftwareUpdateInstall(boolean installing) {
			ReduxEventRemoteUpdate event;
			if(installing) {
				event = ReduxEventRemoteUpdate.Initiate.INSTANCE;
			} else {
				event = new ReduxEventRemoteUpdate.Error(
						new AMRemoteUpdateException(AMRemoteUpdateException.errorCodeMismatch)
				);
			}

			ReduxEmitterNetwork.getRemoteUpdateProgressSubject().onNext(event);
		}

		@Override
		public void onSoftwareUpdateError(AMRemoteUpdateException exception) {
			ReduxEmitterNetwork.getRemoteUpdateProgressSubject().onNext(new ReduxEventRemoteUpdate.Error(exception));
		}

		@Override
		public void onFaceTimeNewLink(@Nullable String faceTimeLink) {
			//Ignoring if there is no pending request
			if(faceTimeLinkSubject == null) return;

			//Resolving the completable
			if(faceTimeLink == null) {
				faceTimeLinkSubject.onError(new AMRequestException(FaceTimeLinkErrorCode.external));
			} else {
				faceTimeLinkSubject.onSuccess(faceTimeLink);
			}
			
			faceTimeLinkSubject = null;
		}
		
		@Override
		public void onFaceTimeOutgoingCallInitiated(@FaceTimeInitiateCode int resultCode, @Nullable String errorDetails) {
			//Ignoring if there is no pending request
			if(faceTimeInitiateSubject == null) return;
			
			//Resolving the completable
			if(resultCode == FaceTimeInitiateCode.ok) {
				faceTimeInitiateSubject.onComplete();
			} else {
				faceTimeInitiateSubject.onError(new AMRequestException(resultCode, errorDetails));
			}
			
			faceTimeInitiateSubject = null;
		}
		
		@Override
		public void onFaceTimeOutgoingCallAccepted(@NonNull String faceTimeLink) {
			ReduxEmitterNetwork.getFaceTimeUpdateSubject()
					.onNext(new ReduxEventFaceTime.OutgoingAccepted(faceTimeLink));
		}
		
		@Override
		public void onFaceTimeOutgoingCallRejected() {
			ReduxEmitterNetwork.getFaceTimeUpdateSubject()
					.onNext(ReduxEventFaceTime.OutgoingRejected.INSTANCE);
		}
		
		@Override
		public void onFaceTimeOutgoingCallError(@Nullable String errorDetails) {
			ReduxEmitterNetwork.getFaceTimeUpdateSubject()
					.onNext(new ReduxEventFaceTime.OutgoingError(errorDetails));
		}
		
		@Override
		public void onFaceTimeIncomingCall(@Nullable String caller) {
			ReduxEmitterNetwork.getFaceTimeIncomingCallerSubject()
					.onNext(Optional.ofNullable(caller));
		}
		
		@Override
		public void onFaceTimeIncomingCallHandled(@NonNull String faceTimeLink) {
			ReduxEmitterNetwork.getFaceTimeUpdateSubject()
					.onNext(new ReduxEventFaceTime.IncomingHandled(faceTimeLink));
		}
		
		@Override
		public void onFaceTimeIncomingCallError(@Nullable String errorDetails) {
			ReduxEmitterNetwork.getFaceTimeUpdateSubject()
					.onNext(new ReduxEventFaceTime.IncomingHandleError(errorDetails));
		}
	};
	
	/**
	 * Finds an existing request
	 * @param category The category of the trackable request
	 * @param key The key of the trackable request
	 * @param <S> The result value of the request subject
	 * @param <R> The type of the request subject
	 * @param <K> The type of the key
	 * @return A matching request subject of type R, or NULL if not found
	 */
	@Nullable
	public <S, R extends RequestSubject<S, TrackableRequest<?>>, K> R findRequest(@TrackableRequestCategory int category, K key) {
		return (R) idRequestSubjectMap.values().stream().filter(requestSubject -> {
			//Ignoring if this subject isn't trackable
			if(!(requestSubject.getRequestData() instanceof TrackableRequest)) return false;
			
			//Ignoring if this trackable request is for a different category
			TrackableRequest<?> trackableRequest = (TrackableRequest<?>) requestSubject.getRequestData();
			if(trackableRequest.getCategory() != category) return false;
			
			//Ignoring if the keys don't match
			K trackableKey = (K) trackableRequest.getValue();
			if(!Objects.equals(key, trackableKey)) return false;
			
			return true;
		}).findAny().orElse(null);
	}
	
	private int getLastEmittedState() {
		ReduxEventConnection value = ReduxEmitterNetwork.getConnectionStateSubject().getValue();
		if(value == null) return -1;
		else return value.getState();
	}
	
	/**
	 * Sets the state to connecting
	 */
	private void emitStateConnecting() {
		if(getLastEmittedState() == ConnectionState.connecting) return;
		ReduxEmitterNetwork.getConnectionStateSubject().onNext(new ReduxEventConnection.Connecting());
	}
	
	/**
	 * Sets the state to connected
	 */
	private void emitStateConnected() {
		if(getLastEmittedState() == ConnectionState.connected) return;
		ReduxEmitterNetwork.getConnectionStateSubject().onNext(new ReduxEventConnection.Connected());
	}
	
	/**
	 * Sets the state to disconnected
	 * @param code The error code to notify listeners of
	 */
	private void emitStateDisconnected(@ConnectionErrorCode int code) {
		if(getLastEmittedState() == ConnectionState.disconnected) return;
		ReduxEmitterNetwork.getConnectionStateSubject().onNext(new ReduxEventConnection.Disconnected(code));
	}
	
	/**
	 * Gets the next request ID
	 */
	private short generateRequestID() {
		return ++currentRequestID;
	}
	
	/**
	 * Connects to the server in user mode
	 */
	public void connect() {
		//Cleaning up after the current mode
		stopCurrentMode();
		
		//Setting the current mode
		connMode = ConnectionMode.user;
		
		//If we're connected, latch on to that state
		if(connState == ConnectionState.connected) {
			emitStateConnected();
		} else {
			//Otherwise, initiate a new connection and update the state
			if(connState == ConnectionState.disconnected) {
				connectFromList(getContext(), 0);
			}
			emitStateConnecting();
		}
	}
	
	/**
	 * Starts or advances an immediate reconnection
	 * @return TRUE if the immediate reconnection was scheduled, or FALSE if none could be scheduled
	 */
	private boolean scheduleImmediateReconnect(boolean isFirst) {
		//Updating the mode
		connMode = ConnectionMode.immediate;
		
		//Checking if we aren't already doing immediate reconnect
		if(isFirst) {
			//Initialize state
			immediateReconnectIndex = 0;
		} else {
			//Failing if we are at the end of our attempts
			if(immediateReconnectIndex + 1 >= immediateReconnectDelayMillis.length) {
				return false;
			}
			
			//Incrementing the index
			immediateReconnectIndex++;
		}
		
		//Scheduling the immediate reconnection
		handler.postDelayed(immediateReconnectRunnable, immediateReconnectDelayMillis[immediateReconnectIndex] + random.nextInt(1000));
		
		return true;
	}
	
	/**
	 * Starts the background reconnection clock
	 */
	private void scheduleRepeatingBackgroundReconnect(Context context) {
		context.getSystemService(AlarmManager.class).setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
				SystemClock.elapsedRealtime() + backgroundReconnectFrequencyMillis / 2,
				backgroundReconnectFrequencyMillis,
				reconnectPendingIntent);
	}
	
	private void stopCurrentMode() {
		if(connMode == ConnectionMode.immediate) {
			handler.removeCallbacks(immediateReconnectRunnable);
		} else if(connMode == ConnectionMode.background) {
			getContext().getSystemService(AlarmManager.class).cancel(reconnectPendingIntent);
		}
	}
	
	/**
	 * Connects from the specified index down the priority list
	 * @return Whether the communications manager was started
	 */
	private boolean connectFromList(Context context, int index) {
		//Getting the parameters
		int proxyType = connectionOverride == null ? SharedPreferencesManager.getProxyType(context) : connectionOverride.getProxyType();
		ConnectionParams overrideValue = connectionOverride == null ? null : connectionOverride.getValue();
		
		//Creating and checking the communications manager
		communicationsManager = communicationsPriorityList.get(index).create(communicationsManagerListener, proxyType);
		if(!communicationsManager.isProxySupported(proxyType)) return false;
		
		//Updating the connection state
		connState = ConnectionState.connecting;
		
		//Recording the index
		currentCommunicationsIndex = index;
		
		//Starting the communications manager
		communicationsManager.connect(context, overrideValue);
		
		return true;
	}
	
	/**
	 * Disconnects from the server
	 */
	public void disconnect(@ConnectionErrorCode int code) {
		//Ignoring if we're not connected
		if(connState != ConnectionState.connected) return;
		
		if(communicationsManager != null) communicationsManager.disconnect(code);
	}
	
	/**
	 * Gets the current connection state
	 */
	@ConnectionState
	public int getState() {
		return connState;
	}
	
	/**
	 * Gets whether the current connection is a fallback connection
	 */
	public boolean isUsingFallback() {
		if(communicationsManager == null) return false;
		return communicationsManager.getDataProxy().isUsingFallback();
	}
	
	/**
	 * Gets if this connection manager is connected to the server (can send and receive messages)
	 */
	public boolean isConnected() {
		return connState == ConnectionState.connected;
	}
	
	/**
	 * Sends a FCM push token to AirMessage Cloud to receive push notifications
	 * @param token The token to send
	 * @return Whether the token was successfully sent
	 */
	public boolean sendPushToken(String token) {
		//Failing immediately if there is no network connection
		if(!isConnected()) return false;
		
		//Sending the token
		return communicationsManager.sendPushToken(token);
	}
	
	/**
	 * Sends a ping to the server and waits for a response
	 * The connection is closed if no response is received in time.
	 * This function is to be used when there is no network traffic present to validate the connection.
	 */
	public void testConnection() {
		//Sending the ping
		communicationsManager.sendPing();
		
		//Starting the ping timeout
		handler.postDelayed(pingExpiryRunnable, pingExpiryTime);
	}
	
	/**
	 * Resets all connection test timers, and schedules new timers for later
	 */
	public void resetConnectionTest(Context context) {
		//Cancelling the ping timeout
		handler.removeCallbacks(pingExpiryRunnable);
		
		//Resetting the ping timer
		context.getSystemService(AlarmManager.class).setWindow(AlarmManager.ELAPSED_REALTIME_WAKEUP,
				SystemClock.elapsedRealtime() + keepAliveMillis - keepAliveWindowMillis,
				keepAliveWindowMillis * 2,
				pingPendingIntent);
	}
	
	/**
	 * Cancels all connection test timers
	 */
	public void cancelConnectionTest(Context context) {
		//Cancelling the ping timeout
		handler.removeCallbacks(pingExpiryRunnable);
		
		//Cancelling the ping timer
		context.getSystemService(AlarmManager.class).cancel(pingPendingIntent);
	}
	
	/**
	 * Sends a text message to a conversation
	 * @param conversation The conversation to send to
	 * @param message The message to send
	 * @return An completable to track the state of the request, or an {@link AMRequestException} with a {@link MessageSendErrorCode}
	 */
	public Completable sendMessage(ConversationTarget conversation, String message) {
		final Throwable error = new AMRequestException(MessageSendErrorCode.localNetwork);
		
		//Failing immediately if there is no network connection
		if(!isConnected()) return Completable.error(error);
		
		//Getting the request ID
		short requestID = generateRequestID();
		
		//Sending the message
		boolean result = communicationsManager.sendMessage(requestID, conversation, message);
		if(!result) return Completable.error(error);
		
		//Adding the request
		return queueCompletableIDRequest(requestID, error);
	}
	
	/**
	 * Sends an attachment file to a conversation
	 * @param conversation The conversation to send to
	 * @param file The file to send
	 * @return An observable to track the progress of the upload, or an {@link AMRequestException} with a {@link MessageSendErrorCode}
	 */
	public Observable<ReduxEventAttachmentUpload> sendFile(ConversationTarget conversation, File file) {
		final Throwable error = new AMRequestException(MessageSendErrorCode.localNetwork);
		
		//Failing immediately if there is no network connection
		if(!isConnected()) return Observable.error(error);
		
		//Getting the request ID
		short requestID = generateRequestID();
		
		//Creating the subject
		PublishSubject<ReduxEventAttachmentUpload> subject = PublishSubject.create();
		
		//Adding the request to the list
		idRequestSubjectMap.put(requestID, new RequestSubject.Publish<>(subject, error, Unit.INSTANCE));
		
		//Sending the file (not passing completions to the subject, since we'll want to handle those when we receive a response instead)
		Observable.concat(communicationsManager.sendFile(requestID, conversation, file), Observable.never())
				.subscribeOn(uploadScheduler)
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(subject);
		
		//Adding a timeout
		return subject.compose(composeTimeoutIDObservable(requestID, error));
	}
	
	/**
	 * Fetches the data of an attachment
	 * @param messageLocalID the local ID of the attachment's message
	 * @param attachmentLocalID The local ID of the attachment
	 * @param attachmentGUID The GUID of the attachment
	 * @param attachmentName The name of the attachment file
	 * @return An observable to track the progress of the download, or an {@link AMRequestException} with an {@link AttachmentReqErrorCode}
	 */
	public Observable<ReduxEventAttachmentDownload> fetchAttachment(long messageLocalID, long attachmentLocalID, String attachmentGUID, String attachmentName) {
		final Throwable error = new AMRequestException(AttachmentReqErrorCode.localTimeout);
		
		//Failing immediately if there is no network connection
		if(!isConnected()) return Observable.error(error);
		
		//Getting the request ID
		short requestID = generateRequestID();
		
		//Sending the request
		boolean result = communicationsManager.requestAttachmentDownload(requestID, attachmentGUID);
		if(!result) return Observable.error(error);
		
		//Adding the request
		FileFetchRequest fileFetchRequest = new FileFetchRequest(messageLocalID, attachmentLocalID, attachmentName);
		return this.<ReduxEventAttachmentDownload, FileFetchRequest>queueObservableIDRequest(requestID, error, fileFetchRequest).doOnError((observableError) -> {
			//Cleaning up
			fileFetchRequest.cancel();
		});
	}
	
	/**
	 * Creates a chat
	 * @param members The addresses of the members of the chat
	 * @param service The service of the chat
	 * @return A single representing the GUID of the chat, or an {@link AMRequestException} with a {@link ChatCreateErrorCode}
	 */
	public Single<String> createChat(String[] members, String service) {
		final Throwable error = new AMRequestException(ChatCreateErrorCode.network);
		
		//Failing immediately if there is no network connection
		if(!isConnected()) return Single.error(error);
		
		//Getting the request ID
		short requestID = generateRequestID();
		
		//Sending the request
		boolean result = communicationsManager.requestChatCreation(requestID, members, service);
		if(!result) return Single.error(error);
		
		//Adding the request
		return queueSingleIDRequest(requestID, error);
	}
	
	/**
	 * Requests data for pending conversations from the server
	 */
	public void fetchPendingConversations() {
		//Ignoring if there is no network connection, or if there are no pending conversations
		if(!isConnected() || pendingConversations.isEmpty()) return;
		
		//Sending the request
		communicationsManager.requestConversationInfo(pendingConversations.keySet());
	}
	
	/**
	 * Requests messages between the time bounds from the server
	 * @param timeLower The lower time requirement in milliseconds
	 * @param timeUpper The upper time requirement in milliseconds
	 */
	public void requestMessagesTimeRange(long timeLower, long timeUpper) {
		//Failing immediately if there is no network connection
		if(!isConnected()) return;
		
		//Sending the request
		communicationsManager.requestRetrievalTime(timeLower, timeUpper);
	}
	
	/**
	 * Requests messages since above the specified ID from the server
	 * @param idLower The ID of the message to receive messages since
	 * @param timeLower The lower time requirement in milliseconds
	 * @param timeUpper The upper time requirement in milliseconds
	 */
	public void requestMessagesIDRange(long idLower, long timeLower, long timeUpper) {
		//Failing immediately if there is no network connection
		if(!isConnected()) return;
		
		//Sending the request
		communicationsManager.requestRetrievalID(idLower, timeLower, timeUpper);
	}

	/**
	 * Installs the server update with the specified ID
	 * @param updateID The ID of the update to install
	 */
	public void installSoftwareUpdate(int updateID) {
		//Failing immediately if there is no network connection
		if(!isConnected()) return;

		communicationsManager.installSoftwareUpdate(updateID);
	}

	/**
	 * Requests a FaceTime link from the server
	 * @return A single that resolves with the fetched FaceTime link
	 */
	public Single<String> requestFaceTimeLink() {
		//If there is already an active request, return that
		if(faceTimeLinkSubject != null) {
			return faceTimeLinkSubject;
		}

		final Throwable error = new AMRequestException(FaceTimeLinkErrorCode.network);

		//Failing immediately if there is no network connection
		if(!isConnected()) return Single.error(error);

		//Sending the request
		boolean result = communicationsManager.requestFaceTimeLink();
		if(!result) return Single.error(error);

		//Creating the subject
		faceTimeLinkSubject = SingleSubject.create();

		//Returning the subject with a timeout
		return faceTimeLinkSubject.timeout(requestTimeoutSeconds, TimeUnit.SECONDS, Single.error(error))
				.observeOn(AndroidSchedulers.mainThread())
				.doOnTerminate(() -> faceTimeLinkSubject = null);
	}
	
	/**
	 * Initiates a new outgoing FaceTime call with the specified addresses
	 * @param addresses The list of addresses to initiate the call with
	 * @return A single that resolves with whether the call was successfully initiated
	 */
	public Completable initiateFaceTimeCall(List<String> addresses) {
		//If there is already an active request, return that
		if(faceTimeInitiateSubject != null) {
			return faceTimeInitiateSubject;
		}
		
		final Throwable error = new AMRequestException(FaceTimeInitiateCode.network);
		
		//Failing immediately if there is no network connection
		if(!isConnected()) return Completable.error(error);
		
		//Sending the request
		boolean result = communicationsManager.initiateFaceTimeCall(addresses);
		if(!result) return Completable.error(error);
		
		//Creating the subject
		faceTimeInitiateSubject = CompletableSubject.create();
		
		//Returning the subject with a timeout
		return faceTimeInitiateSubject.timeout(requestTimeoutSeconds, TimeUnit.SECONDS, Completable.error(error))
				.observeOn(AndroidSchedulers.mainThread())
				.doOnTerminate(() -> faceTimeInitiateSubject = null);
	}
	
	/**
	 * Accepts or rejects a pending incoming FaceTime call
	 * @param caller The name of the caller to accept or reject the call of
	 * @param accept True to accept the call, or false to reject
	 * @return Whether the request was successfully sent
	 */
	public boolean rejectIncomingFaceTimeCall(@NonNull String caller, boolean accept) {
		//Failing immediately if there is no network connection
		if(!isConnected()) return false;
		
		//Sending the request
		return communicationsManager.handleIncomingFaceTimeCall(caller, accept);
	}
	
	/**
	 * Tells the server to leave the FaceTime call.
	 * This should be called after the client has connected to the call with the
	 * FaceTime link to avoid two of the same user connected.
	 * @return Whether the request was successfully sent
	 */
	public boolean dropFaceTimeCallServer() {
		//Failing immediately if there is no network connection
		if(!isConnected()) return false;
		
		//Sending the request
		return communicationsManager.dropFaceTimeCallServer();
	}
	
	/**
	 * Requests a mass message download from the server
	 * @param params The parameters to define what to download
	 */
	public Observable<ReduxEventMassRetrieval> fetchMassConversationData(MassRetrievalParams params) {
		final Throwable error = new Throwable("Mass retrieval error");
		
		//Failing immediately if there is already a mass retrieval in progress or there is no network connection
		if(isMassRetrievalInProgress || !isConnected()) return Observable.error(error);
		
		//Getting the request ID
		short requestID = generateRequestID();
		
		//Sending the request
		boolean result = communicationsManager.requestRetrievalAll(requestID, params);
		if(!result) return Observable.error(error);
		
		//Updating the mass retrieval state
		isMassRetrievalInProgress = true;
		
		//Adding the request
		MassRetrievalRequest massRetrievalRequest = new MassRetrievalRequest(requestID);
		return this.<ReduxEventMassRetrieval, MassRetrievalRequest>queueObservableIDRequest(requestID, error, massRetrievalRequest).doOnError((observableError) -> {
			//Getting the error code
			int errorCode;
			if(observableError instanceof AMRequestException) {
				errorCode = ((AMRequestException) observableError).getErrorCode();
			} else {
				errorCode = MassRetrievalErrorCode.unknown;
				FirebaseCrashlytics.getInstance().recordException(observableError);
			}
			
			//Cleaning up
			massRetrievalRequest.cancel();
			
			//Emitting an update
			ReduxEmitterNetwork.getMassRetrievalUpdateSubject().onNext(new ReduxEventMassRetrieval.Error(requestID, errorCode));
			Log.w(TAG, "Mass retrieval failed", observableError);
		}).doOnTerminate(() -> {
			//Updating the mass retrieval state
			isMassRetrievalInProgress = false;
		});
	}
	
	private CompletableTransformer composeTimeoutIDCompletable(short requestID, Throwable throwable) {
		return completable -> completable.timeout(requestTimeoutSeconds, TimeUnit.SECONDS, Completable.error(throwable))
				.observeOn(AndroidSchedulers.mainThread())
				.doOnTerminate(() -> idRequestSubjectMap.remove(requestID));
	}
	
	private <T> SingleTransformer<T, T> composeTimeoutIDSingle(short requestID, Throwable throwable) {
		return single -> single.timeout(requestTimeoutSeconds, TimeUnit.SECONDS, Single.error(throwable))
				.observeOn(AndroidSchedulers.mainThread())
				.doOnTerminate(() -> idRequestSubjectMap.remove(requestID));
	}
	
	private <T> ObservableTransformer<T, T> composeTimeoutIDObservable(short requestID, Throwable throwable) {
		return observable -> observable.timeout(requestTimeoutSeconds, TimeUnit.SECONDS, Observable.error(throwable))
				.observeOn(AndroidSchedulers.mainThread())
				.doOnTerminate(() -> idRequestSubjectMap.remove(requestID));
	}
	
	/**
	 * Adds a {@link CompletableSubject} to the map, and takes care of timeouts and cleanup
	 * @param requestID The ID of this request
	 * @param throwable The throwable to use in case of a timeout
	 * @return A completable representing the request
	 */
	private Completable queueCompletableIDRequest(short requestID, Throwable throwable) {
		//Creating the subject
		CompletableSubject subject = CompletableSubject.create();
		
		//Adding the request to the list
		idRequestSubjectMap.put(requestID, new RequestSubject.Completable(subject, throwable, Unit.INSTANCE));
		
		//Adding a timeout
		return subject.compose(composeTimeoutIDCompletable(requestID, throwable));
	}
	
	/**
	 * Adds a {@link SingleSubject} to the map, and takes care of timeouts and cleanup
	 * @param requestID The ID of this request
	 * @param throwable The throwable to use in case of a timeout
	 * @return A completable representing the request
	 */
	private <T> Single<T> queueSingleIDRequest(short requestID, Throwable throwable) {
		//Creating the subject
		SingleSubject<T> subject = SingleSubject.create();
		
		//Adding the request to the list
		idRequestSubjectMap.put(requestID, new RequestSubject.Single<>(subject, throwable, Unit.INSTANCE));
		
		//Adding a timeout
		return subject.compose(composeTimeoutIDSingle(requestID, throwable));
	}
	
	/**
	 * Adds a {@link PublishSubject} to the map, and takes care of timeouts and cleanup
	 * @param requestID The ID of this request
	 * @param throwable The throwable to use in case of a timeout
	 * @return A completable representing the request
	 */
	private <T> Observable<T> queueObservableIDRequest(short requestID, Throwable throwable) {
		return queueObservableIDRequest(requestID, throwable, null);
	}
	
	/**
	 * Adds a {@link PublishSubject} to the map, and takes care of timeouts and cleanup
	 * @param requestID The ID of this request
	 * @param throwable The throwable to use in case of a timeout
	 * @param data Additional data to keep track of during the request
	 * @return A completable representing the request
	 */
	private <S, T> Observable<S> queueObservableIDRequest(short requestID, Throwable throwable, T data) {
		//Creating the subject
		PublishSubject<S> subject = PublishSubject.create();
		
		//Adding the request to the list
		idRequestSubjectMap.put(requestID, new RequestSubject.Publish<>(subject, throwable, data));
		
		//Adding a timeout
		return subject.compose(composeTimeoutIDObservable(requestID, throwable));
	}
	
	/**
	 * Gets the human-readable version of the active communications protocol, or NULL if no protocol is active
	 */
	@Nullable
	public String getCommunicationsVersion() {
		if(communicationsManager != null) return communicationsManager.getCommunicationsVersion();
		else return null;
	}
	
	/**
	 * Gets if a mass retrieval is currently in progress
	 */
	public boolean isMassRetrievalInProgress() {
		return isMassRetrievalInProgress;
	}
	
	/**
	 * Gets if a sync is needed
	 */
	public boolean isPendingSync() {
		return isPendingSync;
	}
	
	/**
	 * Clears the pending sync state, for use after a sync has been initiated
	 */
	public void clearPendingSync() {
		isPendingSync = false;
	}
	
	/**
	 * Adds pending conversations to the list, and tries to fetch their details from the server
	 * @param conversations The list of conversations to register as pending conversations
	 */
	public void addPendingConversations(List<ConversationInfo> conversations) {
		//Adding the conversations
		pendingConversations.putAll(conversations.stream().collect(Collectors.toMap(ConversationInfo::getGUID, conversation -> conversation)));
		
		//Fetching pending conversations
		fetchPendingConversations();
	}
	
	/**
	 * Schedules the next keepalive ping
	 */
	private void schedulePing(Context context) {
		((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).setWindow(AlarmManager.ELAPSED_REALTIME_WAKEUP,
				SystemClock.elapsedRealtime() + keepAliveMillis - keepAliveWindowMillis,
				keepAliveWindowMillis * 2,
				pingPendingIntent);
	}
	
	/**
	 * Cancels the timer that sends keepalive pings
	 */
	void cancelSchedulePing(Context context) {
		((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).cancel(pingPendingIntent);
	}
	
	/**
	 * Gets the installation ID of the connected server, or NULL if unavailable
	 */
	@Nullable
	public String getServerInstallationID() {
		return serverInstallationID;
	}
	
	/**
	 * Gets the device name of the connected server, or NULL if unavailable
	 */
	@Nullable
	public String getServerDeviceName() {
		return serverDeviceName;
	}
	
	/**
	 * Gets the macOS system version of the connected server, or NULL if unavailable
	 */
	@Nullable
	public String getServerSystemVersion() {
		return serverSystemVersion;
	}
	
	/**
	 * Gets the AirMessage Server version of the connected server, or NULL if unavailable
	 */
	@Nullable
	public String getServerSoftwareVersion() {
		return serverSoftwareVersion;
	}
	
	/**
	 * Sets whether automatic reconnections should be disabled
	 */
	public void setDisableReconnections(boolean disableReconnections) {
		this.disableReconnections = disableReconnections;
		ReduxEmitterNetwork.getConnectionConfigurationSubject().onNext(disableReconnections);
	}
	
	/**
	 * Sets the override values for new connections
	 */
	public void setConnectionOverride(@Nullable ConnectionOverride<?> connectionOverride) {
		this.connectionOverride = connectionOverride;
	}
}