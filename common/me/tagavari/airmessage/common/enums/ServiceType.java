package me.tagavari.airmessage.common.enums;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@StringDef({ServiceType.appleMessage, ServiceType.appleSMS, ServiceType.systemSMS, ServiceType.systemRCS})
public @interface ServiceType {
	String appleMessage = "iMessage";
	String appleSMS = "SMS"; //Text message forwarding
	
	String systemSMS = "MMSSMS"; //MMS and SMS
	String systemRCS = "RCS"; //Rich communication services
}