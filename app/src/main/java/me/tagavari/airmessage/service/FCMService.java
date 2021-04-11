package me.tagavari.airmessage.service;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.helper.ConnectionServiceLaunchHelper;

public class FCMService extends FirebaseMessagingService {
	@Override
	public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
		//Only starting the service if it isn't already running (for example, if it's bound to an activity, we'll want to leave it that way)
		if(ConnectionService.getInstance() == null) {
			ConnectionServiceLaunchHelper.launchTemporary(this);
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
}