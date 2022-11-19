package me.tagavari.airmessage.common.enums;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({ConversationState.ready, ConversationState.incompleteServer, ConversationState.incompleteClient})
public @interface ConversationState {
	int ready = 0; //The conversation is in sync with the server
	int incompleteServer = 1; //The conversation is a result of a message from the server, but is missing info
	int incompleteClient = 2; //The conversation was created on the client, but isn't linked to the server
}