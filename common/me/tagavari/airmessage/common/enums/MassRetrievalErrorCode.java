package me.tagavari.airmessage.common.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({MassRetrievalErrorCode.unknown, MassRetrievalErrorCode.localTimeout, MassRetrievalErrorCode.localBadResponse, MassRetrievalErrorCode.localIO})
public @interface MassRetrievalErrorCode {
	int unknown = -1;
	int localTimeout = 0; //Request timed out
	int localBadResponse = 1; //Bad response (packets out of order)
	int localIO = 2; //IO error
}