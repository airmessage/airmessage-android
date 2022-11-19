package me.tagavari.airmessage.common.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({GroupAction.unknown, GroupAction.join, GroupAction.leave})
public @interface GroupAction {
	int unknown = 0;
	int join = 1;
	int leave = 2;
}