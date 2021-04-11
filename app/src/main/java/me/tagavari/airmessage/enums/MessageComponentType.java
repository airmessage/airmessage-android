package me.tagavari.airmessage.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({MessageComponentType.text, MessageComponentType.attachmentDocument, MessageComponentType.attachmentVisual, MessageComponentType.attachmentAudio, MessageComponentType.attachmentContact, MessageComponentType.attachmentLocation})
public @interface MessageComponentType {
	int text = 0;
	int attachmentDocument = 1;
	int attachmentVisual = 2;
	int attachmentAudio = 3;
	int attachmentContact = 4;
	int attachmentLocation = 5;
}