package me.tagavari.airmessage.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({ConnectionErrorCode.user, ConnectionErrorCode.connection, ConnectionErrorCode.internet, ConnectionErrorCode.internalError, ConnectionErrorCode.externalError, ConnectionErrorCode.badRequest, ConnectionErrorCode.clientOutdated, ConnectionErrorCode.serverOutdated, ConnectionErrorCode.directUnauthorized, ConnectionErrorCode.connectNoGroup, ConnectionErrorCode.connectNoCapacity, ConnectionErrorCode.connectAccountValidation, ConnectionErrorCode.connectNoSubscription, ConnectionErrorCode.connectOtherLocation})
public @interface ConnectionErrorCode {
	//Standard result codes
	int user = 0; //The user cancelled the connection
	int connection = 1; //The connection between this device and AirMessage Server failed
	int internet = 2; //The connection between this device and the internet failed
	int internalError = 3;
	int externalError = 4;
	
	//Proxy shared
	int badRequest = 100;
	int clientOutdated = 101;
	int serverOutdated = 102;
	
	//Proxy direct
	int directUnauthorized = 200;
	
	//Proxy connect
	int connectNoGroup = 300;
	int connectNoCapacity = 301;
	int connectAccountValidation = 302;
	int connectNoSubscription = 303;
	int connectOtherLocation = 304;
}