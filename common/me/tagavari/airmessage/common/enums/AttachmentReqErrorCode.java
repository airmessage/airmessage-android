package me.tagavari.airmessage.common.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({AttachmentReqErrorCode.unknown, AttachmentReqErrorCode.localCancelled, AttachmentReqErrorCode.localTimeout, AttachmentReqErrorCode.localBadResponse, AttachmentReqErrorCode.localReferencesLost, AttachmentReqErrorCode.localIO, AttachmentReqErrorCode.serverNotFound, AttachmentReqErrorCode.serverNotSaved, AttachmentReqErrorCode.serverUnreadable, AttachmentReqErrorCode.serverIO})
public @interface AttachmentReqErrorCode {
	int unknown = -1;
	int localCancelled = 0; //Request cancelled
	int localTimeout = 1; //Request timed out
	int localBadResponse = 2; //Bad response (packets out of order)
	int localReferencesLost = 3; //Reference to context lost
	int localIO = 4; //IO error
	
	int serverNotFound = 5; //Server file GUID not found
	int serverNotSaved = 6; //Server file (on disk) not found
	int serverUnreadable = 7; //Server no access to file
	int serverIO = 8; //Server IO error
}