package me.tagavari.airmessage.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({ServiceHandler.appleBridge, ServiceHandler.systemMessaging})
public @interface ServiceHandler {
	int appleBridge = 0; //AirMessage bridge (iMessage, text message forwarding)
	int systemMessaging = 1; //System messaging (SMS, MMS, RCS)
}