package me.tagavari.airmessage.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({MessageState.ghost, MessageState.idle, MessageState.sent, MessageState.delivered, MessageState.read})
public @interface MessageState {
	int ghost = 0;
	int idle = 1;
	int sent = 2;
	int delivered = 3;
	int read = 4;
}