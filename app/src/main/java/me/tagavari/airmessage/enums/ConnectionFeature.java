package me.tagavari.airmessage.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({ConnectionFeature.idBasedRetrieval, ConnectionFeature.pushNotifications})
public @interface ConnectionFeature {
	int idBasedRetrieval = 0;
	int pushNotifications = 1;
}