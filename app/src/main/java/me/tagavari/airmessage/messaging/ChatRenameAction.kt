package me.tagavari.airmessage.messaging

import android.content.Context
import io.reactivex.rxjava3.core.Single
import me.tagavari.airmessage.R
import me.tagavari.airmessage.enums.ConversationItemType
import me.tagavari.airmessage.helper.ContactHelper.getUserDisplayName
import java.util.*

class ChatRenameAction(
	localID: Long,
	serverID: Long,
	guid: String?,
	date: Long,
	val agent: String,
	val title: String
) : ConversationAction(localID, serverID, guid, date) {
	override val itemType = ConversationItemType.chatRename
	
	override fun getMessageDirect(context: Context): String {
		return buildMessage(context, agent, title)
	}
	
	override val supportsBuildMessageAsync = true
	
	override fun buildMessageAsync(context: Context): Single<String> {
		return getUserDisplayName(context, agent)
			.map { Optional.of(it) }
			.defaultIfEmpty(Optional.empty())
			.map { userName: Optional<String> -> buildMessage(context, userName.orElse(null), title) }
	}
	
	override fun clone(): ChatRenameAction {
		return ChatRenameAction(localID, serverID, guid, date, agent, title)
	}
	
	companion object {
		/**
		 * Builds a summary message with the provided details
		 * @param context The context to use
		 * @param agent The name of the user who took this action, or NULL if the user is the local user
		 * @param title The title of the conversation, or NULL if the title is removed
		 * @return The message to display in the chat
		 */
		private fun buildMessage(context: Context, agent: String?, title: String?): String {
			return if(agent == null) {
				if(title == null) context.getString(R.string.message_eventtype_chatrename_remove_you)
				else context.getString(R.string.message_eventtype_chatrename_change_you, title)
			} else {
				if(title == null) context.getString(R.string.message_eventtype_chatrename_remove, agent)
				else context.getString(R.string.message_eventtype_chatrename_change, agent, title)
			}
		}
	}
}