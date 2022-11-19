package me.tagavari.airmessage.common.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({FaceTimeInitiateCode.ok, FaceTimeInitiateCode.network, FaceTimeInitiateCode.badMembers, FaceTimeInitiateCode.external})
public @interface FaceTimeInitiateCode {
    int ok = 0;
    int network = 1;
    int badMembers = 2;
    int external = 3;
}
