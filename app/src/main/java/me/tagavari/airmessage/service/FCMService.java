package me.tagavari.airmessage.service;

import android.util.Base64;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import kotlin.Pair;
import me.tagavari.airmessage.activity.Messaging;
import me.tagavari.airmessage.common.Blocks;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.connection.comm5.AirUnpacker;
import me.tagavari.airmessage.connection.comm5.ClientProtocol3;
import me.tagavari.airmessage.connection.encryption.EncryptionAES;
import me.tagavari.airmessage.connection.exception.LargeAllocationException;
import me.tagavari.airmessage.connection.task.MessageUpdateTask;
import me.tagavari.airmessage.connection.task.ModifierUpdateTask;
import me.tagavari.airmessage.data.SharedPreferencesManager;
import me.tagavari.airmessage.helper.ConnectionServiceLaunchHelper;
import me.tagavari.airmessage.helper.NotificationHelper;
import me.tagavari.airmessage.messaging.StickerInfo;
import me.tagavari.airmessage.messaging.TapbackInfo;
import me.tagavari.airmessage.redux.ReduxEmitterNetwork;
import me.tagavari.airmessage.redux.ReduxEventMessaging;
import me.tagavari.airmessage.util.ActivityStatusUpdate;
import me.tagavari.airmessage.util.ModifierMetadata;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FCMService extends FirebaseMessagingService {
	private static final String TAG = FCMService.class.getSimpleName();
	
	@Override
	public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
		Map<String, String> remoteMessageData = remoteMessage.getData();
		if(remoteMessageData.isEmpty()) {
			startConnectionService();
			return;
		}
		
		//Reading the data
		String payloadVersionString = remoteMessageData.get("payload_version");
		if(payloadVersionString == null) {
			Log.w(TAG, "No version string for FCM data");
			return;
		}
		int payloadVersion;
		try {
			payloadVersion = Integer.parseInt(payloadVersionString);
		} catch(NumberFormatException exception) {
			exception.printStackTrace();
			return;
		}
		
		if(payloadVersion == 1 || payloadVersion == 2) {
			//Reading the protocol version
			String protocolVersionString = remoteMessageData.get("protocol_version");
			if(protocolVersionString == null) {
				Log.w(TAG, "No payload for FCM data version " + payloadVersion);
				return;
			}
			int[] protocolVersion;
			try {
				protocolVersion = Arrays.stream(protocolVersionString.split("\\."))
						.mapToInt(Integer::parseInt)
						.toArray();
			} catch(NumberFormatException exception) {
				exception.printStackTrace();
				return;
			}
			
			//Reading the payload
			String payload = remoteMessageData.get("payload");
			if(payload == null) {
				Log.w(TAG, "No payload for FCM data version " + payloadVersion);
			}
			
			//Decoding the data
			byte[] data = Base64.decode(payload, Base64.DEFAULT);
			
			//If the payload version is 2, the first byte represents whether the data is encrypted, and the data is included as a payload
			if(payloadVersion == 2) {
				AirUnpacker airUnpacker = new AirUnpacker(data);
				boolean isEncrypted = airUnpacker.unpackBoolean();
				
				if(isEncrypted) {
					try {
						String password = SharedPreferencesManager.getDirectConnectionPassword(this);
						if(password == null) throw new GeneralSecurityException("No password available");
						
						EncryptionAES encryptionAES = new EncryptionAES(password);
						data = encryptionAES.decrypt(airUnpacker.unpackPayload());
					} catch(GeneralSecurityException | IOException exception) {
						exception.printStackTrace();
						
						//Notify the user
						NotificationHelper.sendDecryptErrorNotification(this);
						return;
					}
				} else {
					data = airUnpacker.unpackPayload();
				}
			}
			
			List<Blocks.ConversationItem> conversationItems = null;
			List<Blocks.ModifierInfo> modifiers = null;
			boolean dataLoaded = false;
			
			//Protocol version 5
			if(protocolVersion.length == 2 && protocolVersion[0] == 5) {
				//Protocol 5.3
				if(protocolVersion[1] == 3) {
					AirUnpacker airUnpacker = new AirUnpacker(data);
					try {
						conversationItems = ClientProtocol3.unpackConversationItems(airUnpacker);
						modifiers = ClientProtocol3.unpackModifiers(airUnpacker);
						dataLoaded = true;
					} catch(BufferUnderflowException | LargeAllocationException | IllegalArgumentException exception) {
						exception.printStackTrace();
						return;
					}
				}
			}
			
			if(dataLoaded) {
				//Loading the foreground conversations (needs to be done on the main thread)
				List<Blocks.ConversationItem> finalConversationItems = conversationItems;
				Single.fromCallable(Messaging::getForegroundConversations)
						.subscribeOn(AndroidSchedulers.mainThread())
						.flatMap(foregroundConversations -> MessageUpdateTask.create(this, foregroundConversations, finalConversationItems, false))
						.observeOn(AndroidSchedulers.mainThread())
						.doOnSuccess(response -> {
							//Emitting any generated events
							for(ReduxEventMessaging event : response.getEvents()) {
								ReduxEmitterNetwork.getMessageUpdateSubject().onNext(event);
							}
							
							//If we have incomplete conversations, query the server to complete them
							if(!response.getIncompleteServerConversations().isEmpty()) {
								ConnectionManager connectionManager = ConnectionService.getConnectionManager();
								if(connectionManager != null) {
									connectionManager.addPendingConversations(response.getIncompleteServerConversations());
								} else {
									startConnectionService();
								}
							}
						}).subscribe();
				
				//Writing modifiers to disk
				ModifierUpdateTask.create(this, modifiers).doOnSuccess(result -> {
					//Pushing emitter updates
					for(ActivityStatusUpdate statusUpdate : result.getActivityStatusUpdates()) {
						ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.MessageState(statusUpdate.getMessageID(), statusUpdate.getMessageState(), statusUpdate.getDateRead()));
					}
					for(Pair<StickerInfo, ModifierMetadata> sticker : result.getStickerModifiers()) ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.StickerAdd(sticker.getFirst(), sticker.getSecond()));
					for(Pair<TapbackInfo, ModifierMetadata> tapback : result.getTapbackModifiers()) ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.TapbackUpdate(tapback.getFirst(), tapback.getSecond(), true));
					for(Pair<TapbackInfo, ModifierMetadata> tapback : result.getTapbackRemovals()) ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.TapbackUpdate(tapback.getFirst(), tapback.getSecond(), false));
				}).subscribe();
			} else {
				//Fetch messages from the connection service
				startConnectionService();
			}
		}
	}
	
	@Override
	public void onDeletedMessages() {
	
	}
	
	@Override
	public void onNewToken(@NonNull String token) {
		//Updating Connect servers with the new token
		ConnectionManager connectionManager = ConnectionService.getConnectionManager();
		if(connectionManager == null) return;
		
		connectionManager.sendPushToken(token);
	}
	
	private void startConnectionService() {
		//Only starting the service if it isn't already running (for example, if it's bound to an activity, we'll want to leave it that way)
		if(ConnectionService.getInstance() == null) {
			ConnectionServiceLaunchHelper.launchTemporary(this);
		}
	}
}