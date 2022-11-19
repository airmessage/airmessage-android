package me.tagavari.airmessage.common.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({MessagePreviewState.notTried, MessagePreviewState.unavailable, MessagePreviewState.available})
public @interface MessagePreviewState {
	int notTried = 0;
	int unavailable = 1;
	int available = 2;
}