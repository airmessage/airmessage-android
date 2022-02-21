package me.tagavari.airmessage.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({ConnectionFeature.idBasedRetrieval, ConnectionFeature.payloadPushNotifications, ConnectionFeature.remoteUpdates, ConnectionFeature.faceTime})
public @interface ConnectionFeature {
	int idBasedRetrieval = 0; //Retrieve messages using an ID range
	int payloadPushNotifications = 1; //Receive FCM messages with message content
	int remoteUpdates = 2; //Initiate server updates from clients
	int faceTime = 3; //Initiate and receive FaceTime calls
}