package me.tagavari.airmessage.common.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({TrackableRequestCategory.messageUpload, TrackableRequestCategory.attachmentUpload})
public @interface TrackableRequestCategory {
	int messageUpload = 0;
	int attachmentUpload = 1;
}