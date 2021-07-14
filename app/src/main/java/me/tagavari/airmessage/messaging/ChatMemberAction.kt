package me.tagavari.airmessage.messaging

import android.content.Context
import io.reactivex.rxjava3.core.Single
import me.tagavari.airmessage.R
import me.tagavari.airmessage.enums.ConversationItemType
import me.tagavari.airmessage.enums.GroupAction
import me.tagavari.airmessage.helper.ContactHelper
import java.util.*

class ChatMemberAction(
	localID: Long,
	serverID: Long,
	guid: String?,
	date: Long,
	@field:GroupAction @get:GroupAction @param:GroupAction val actionType: Int,
	val agent: String?,
	val other: String?
) : ConversationAction(localID, serverID, guid, date) {
	override val itemType = ConversationItemType.member
	
	override fun getMessageDirect(context: Context): String {
		return buildMessage(context, agent, other, actionType)
	}
	
	override val supportsBuildMessageAsync = true
	
	override fun buildMessageAsync(context: Context): Single<String> {
		return Single.zip(
			ContactHelper.getUserDisplayName(context, agent).map { Optional.of(it) }.defaultIfEmpty(Optional.empty()),
			ContactHelper.getUserDisplayName(context, other).map { Optional.of(it) }.defaultIfEmpty(Optional.empty())
		) { agentName, otherName -> buildMessage(context, agentName.orElse(null), otherName.orElse(null), actionType) }
	}
	
	override fun clone(): ChatMemberAction {
		return ChatMemberAction(localID, serverID, guid, date, actionType, agent, other)
	}
	
	companion object {
		/**
		 * Builds a summary message with the provided details
		 * @param context The context to use
		 * @param agent The name of the user who took this action, or NULL if the user is the local user
		 * @param other The name of the user who was acted upon, or NULL if the user is the local user
		 * @param actionType The type of action that was taken
		 * @return The message to display in the chat
		 */
		fun buildMessage(context: Context, agent: String?, other: String?, actionType: Int): String {
			//Returning the message based on the action type
			if(actionType == GroupAction.join) {
				return if(agent == other) {
					if(agent == null) context.getString(R.string.message_eventtype_join_you)
					else context.getString(R.string.message_eventtype_join, agent)
				} else {
					if(agent == null) context.getString(R.string.message_eventtype_invite_you_agent, other)
					else if(other == null) context.getString(R.string.message_eventtype_invite_you_object, agent)
					else context.getString(R.string.message_eventtype_invite, agent, other)
				}
			} else if(actionType == GroupAction.leave) {
				return if(agent == other) {
					if(agent == null) context.getString(R.string.message_eventtype_leave_you)
					else context.getString(R.string.message_eventtype_leave, agent)
				} else {
					if(agent == null) context.getString(R.string.message_eventtype_kick_you_agent, other)
					else if(other == null) context.getString(R.string.message_eventtype_kick_you_object, agent)
					else context.getString(R.string.message_eventtype_kick, agent, other)
				}
			}
			
			//Returning an unknown message
			return context.getString(R.string.message_eventtype_unknown)
		}
	}
}