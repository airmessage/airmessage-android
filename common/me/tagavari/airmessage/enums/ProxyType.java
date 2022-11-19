package me.tagavari.airmessage.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({ProxyType.direct, ProxyType.connect})
public @interface ProxyType {
	int direct = 0;
	int connect = 1;
}