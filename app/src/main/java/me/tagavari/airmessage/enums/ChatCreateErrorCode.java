package me.tagavari.airmessage.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({ChatCreateErrorCode.network, ChatCreateErrorCode.scriptError, ChatCreateErrorCode.badRequest, ChatCreateErrorCode.unauthorized, ChatCreateErrorCode.unknown})
public @interface ChatCreateErrorCode {
	int network = 0;
	int scriptError = 1;
	int badRequest = 2;
	int unauthorized = 3;
	int unknown = 4;
}