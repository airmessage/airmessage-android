package me.tagavari.airmessage.common.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({ConnectionMode.user, ConnectionMode.immediate, ConnectionMode.background})
public @interface ConnectionMode {
	int user = 0;
	int immediate = 1;
	int background = 2;
}