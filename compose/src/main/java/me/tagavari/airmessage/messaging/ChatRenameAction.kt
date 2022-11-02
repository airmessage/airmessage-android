package me.tagavari.airmessage.messaging

import android.content.Context
import kotlinx.coroutines.rx3.awaitSingleOrNull
import me.tagavari.airmessage.compose.R
import me.tagavari.airmessage.enums.ConversationItemType
import me.tagavari.airmessage.helper.ContactHelper

class ChatRenameAction(
	localID: Long,
	serverID: Long,
	guid: String?,
	date: Long,
	val agent: String?,
	val title: String?
) : ConversationAction(localID, serverID, guid, date) {
	override val itemType = ConversationItemType.chatRename
	
	override fun getMessageDirect(context: Context) =
		buildMessage(context, agent, title)
	
	override val supportsBuildMessageAsync = true
	
	override suspend fun buildMessageAsync(context: Context): String {
		val agentName = ContactHelper.getUserDisplayName(context, agent).awaitSingleOrNull()
		return buildMessage(context, agentName, title)
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