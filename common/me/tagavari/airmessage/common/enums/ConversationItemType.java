package me.tagavari.airmessage.common.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({ConversationItemType.message, ConversationItemType.member, ConversationItemType.chatRename, ConversationItemType.chatCreate})
public @interface ConversationItemType {
	int message = 0;
	int member = 1;
	int chatRename = 2;
	int chatCreate = 3;
}