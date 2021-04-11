package me.tagavari.airmessage.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({MessageEdgeType.anchored, MessageEdgeType.unanchored, MessageEdgeType.flat})
public @interface MessageEdgeType {
	int anchored = 0;
	int unanchored = 1;
	int flat = 2;
}