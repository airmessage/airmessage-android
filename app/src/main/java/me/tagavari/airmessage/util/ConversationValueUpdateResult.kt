package me.tagavari.airmessage.util

import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxEventMessaging
import me.tagavari.airmessage.redux.ReduxEventMessaging.ConversationArchive
import me.tagavari.airmessage.redux.ReduxEventMessaging.ConversationUnread

/**
 * Represents an update to a conversation due to an incoming message update
 * The conversation may be unarchived due to the incoming message, and will have its unread count incremented
 */
class ConversationValueUpdateResult(private val isUnarchived: Boolean, private val unreadIncrement: Int) {
	fun getEvents(conversationInfo: ConversationInfo): List<ReduxEventMessaging> {
		val events = mutableListOf<ReduxEventMessaging>()
		if(isUnarchived) events.add(ConversationArchive(conversationInfo, false))
		if(unreadIncrement > 0) events.add(ConversationUnread(conversationInfo, conversationInfo.unreadMessageCount + unreadIncrement))
		return events
	}
	
	fun emitUpdate(conversationInfo: ConversationInfo) {
		//Emitting updates
		if(isUnarchived) ReduxEmitterNetwork.messageUpdateSubject.onNext(ConversationArchive(conversationInfo, false))
		if(unreadIncrement > 0) ReduxEmitterNetwork.messageUpdateSubject.onNext(ConversationUnread(conversationInfo, conversationInfo.unreadMessageCount + unreadIncrement))
	}
}