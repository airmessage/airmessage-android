package me.tagavari.airmessage.common.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({MessageSendErrorCode.none, MessageSendErrorCode.localUnknown, MessageSendErrorCode.localInvalidContent, MessageSendErrorCode.localFileTooLarge, MessageSendErrorCode.localIO, MessageSendErrorCode.localNetwork, MessageSendErrorCode.localExpired, MessageSendErrorCode.localReferences, MessageSendErrorCode.localInternal, MessageSendErrorCode.serverUnknown, MessageSendErrorCode.serverExternal, MessageSendErrorCode.serverBadRequest, MessageSendErrorCode.serverUnauthorized, MessageSendErrorCode.serverNoConversation, MessageSendErrorCode.serverRequestTimeout, MessageSendErrorCode.appleUnknown, MessageSendErrorCode.appleNetwork, MessageSendErrorCode.appleUnregistered})
public @interface MessageSendErrorCode {
	//No error
	int none = 0;
	
	//AirMessage app-provided error codes (if the app fails a request)
	int localUnknown = 100; //Unknown error (for example, a version upgrade where error codes change)
	int localInvalidContent = 101; //Invalid content
	int localFileTooLarge = 102; //Attachment too large
	int localIO = 103; //IO exception
	int localNetwork = 104; //Network exception
	int localExpired = 106; //Request expired
	int localReferences = 107; //References lost
	int localInternal = 108; //Internal exception
	
	//AirMessage server-provided error codes (if the server fails a request, or Apple Messages cannot properly handle it)
	int serverUnknown = 200; //An unknown response code was received from the server
	int serverExternal = 201; //The server received an external error
	int serverBadRequest = 202; //The server couldn't process the request
	int serverUnauthorized = 203; //The server doesn't have permission to send messages
	int serverNoConversation = 204; //The server couldn't find the requested conversation
	int serverRequestTimeout = 205; //The server timed out the client's request
	
	//Apple-provided error codes (converted, from the Messages database)
	int appleUnknown = 300; //An unknown error code
	int appleNetwork = 301; //Network error
	int appleUnregistered = 302; //Not registered with iMessage
}