package me.tagavari.airmessage.messaging

import android.app.PendingIntent
import android.graphics.drawable.Icon

/**
 * Represents a suggested action that can be taken in a conversation
 *
 * Actions can either be reply actions that send a specified message,
 * or remote actions that launch an external intent
 */
class AMConversationAction private constructor(
	val isReplyAction: Boolean,
	val replyString: CharSequence?,
	val remoteAction: RemoteAction?
) {
	data class RemoteAction(val icon: Icon?, val title: CharSequence, val actionIntent: PendingIntent)
	
	companion object {
		fun createReplyAction(replyString: CharSequence): AMConversationAction {
			return AMConversationAction(true, replyString, null)
		}
		
		fun createRemoteAction(remoteAction: RemoteAction): AMConversationAction {
			return AMConversationAction(false, null, remoteAction)
		}
	}
}