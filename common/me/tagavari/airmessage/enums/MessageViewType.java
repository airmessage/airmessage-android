package me.tagavari.airmessage.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({MessageViewType.message, MessageViewType.action})
public @interface MessageViewType {
	int message = 0;
	int action = 1;
}