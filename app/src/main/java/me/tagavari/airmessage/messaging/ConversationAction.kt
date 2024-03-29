package me.tagavari.airmessage.messaging

import android.content.Context
import io.reactivex.rxjava3.core.Single
import me.tagavari.airmessage.enums.MessageViewType

/**
 * Represents an action taken in a conversation
 */
abstract class ConversationAction(localID: Long, serverID: Long, guid: String?, date: Long) :
	ConversationItem(localID, serverID, guid, date) {
	@get:MessageViewType
	override val itemViewType = MessageViewType.action
	
	/**
	 * Builds the message of this conversation action synchronously, without extra detail such as user names
	 */
	abstract fun getMessageDirect(context: Context): String
	
	/**
	 * Gets whether this function supports building its message asynchronously.
	 *
	 * If this function returns FALSE, [.getMessageDirect] should be used as the final message.
	 */
	abstract val supportsBuildMessageAsync: Boolean
	
	/**
	 * Builds the message of this conversation action asynchronously.
	 *
	 * If this function does not [.supportsBuildMessageAsync], do not call this method.
	 */
	open fun buildMessageAsync(context: Context): Single<String> {
		throw UnsupportedOperationException("This action does not support asynchronous message creation")
	}
}