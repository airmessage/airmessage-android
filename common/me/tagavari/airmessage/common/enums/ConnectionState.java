package me.tagavari.airmessage.common.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({ConnectionState.disconnected, ConnectionState.connecting, ConnectionState.connected})
public @interface ConnectionState {
	int disconnected = 0;
	int connecting = 1;
	int connected = 2;
}