package me.tagavari.airmessage.service;

import android.content.Intent;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import me.tagavari.airmessage.connection.ConnectionManager;

public class FCMService extends FirebaseMessagingService {
	@Override
	public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
		startService(new Intent(this, ConnectionService.class)
				.setAction(ConnectionService.selfIntentActionConnect)
				.putExtra(ConnectionService.selfIntentExtraTemporary, true));
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