package me.tagavari.airmessage.common.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({FaceTimeLinkErrorCode.network, FaceTimeLinkErrorCode.external})
public @interface FaceTimeLinkErrorCode {
    int network = 0;
    int external = 1;
}
