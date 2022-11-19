package me.tagavari.airmessage.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({TapbackType.unknown, TapbackType.heart, TapbackType.like, TapbackType.dislike, TapbackType.laugh, TapbackType.exclamation, TapbackType.question})
public @interface TapbackType {
	int unknown = -1;
	int heart = 0;
	int like = 1;
	int dislike = 2;
	int laugh = 3;
	int exclamation = 4;
	int question = 5;
}