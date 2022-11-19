package me.tagavari.airmessage.common.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({AttachmentType.media, AttachmentType.document, AttachmentType.audio, AttachmentType.contact, AttachmentType.location})
public @interface AttachmentType {
	int document = 0;
	int media = 1;
	int audio = 2;
	int contact = 3;
	int location = 4;
}