package me.tagavari.airmessage.messaging;

import android.content.Context;

import androidx.annotation.NonNull;

import io.reactivex.rxjava3.core.Single;
import me.tagavari.airmessage.enums.MessageViewType;

/**
 * Represents an action taken in a conversation
 */
public abstract class ConversationAction extends ConversationItem {
	public ConversationAction(long localID, long serverID, String guid, long date) {
		super(localID, serverID, guid, date);
	}
	
	@Override
	@MessageViewType
	public int getItemViewType() {
		return MessageViewType.action;
	}
	
	/**
	 * Builds the message of this conversation action synchronously, without extra detail such as user names
	 */
	public abstract String getMessageDirect(Context context);
	
	/**
	 * Gets whether this function supports building its message asynchronously.
	 *
	 * If this function returns FALSE, {@link #getMessageDirect(Context)} should be used as the final message.
	 */
	public abstract boolean supportsBuildMessageAsync();
	
	/**
	 * Builds the message of this conversation action asynchronously.
	 *
	 * If this function does not {@link #supportsBuildMessageAsync()}, do not call this method.
	 */
	public Single<String> buildMessageAsync(Context context) {
		throw new UnsupportedOperationException("This action does not support asynchronous message creation");
	}
}