package me.tagavari.airmessage.common.messaging

import android.app.PendingIntent
import android.graphics.drawable.Icon
import androidx.compose.runtime.Immutable

/**
 * Represents a suggested action that can be taken in a conversation
 *
 * Actions can either be reply actions that send a specified message,
 * or remote actions that launch an external intent
 */
@Immutable
class AMConversationAction private constructor(
	val isReplyAction: Boolean,
	val replyString: String?,
	val remoteAction: RemoteAction?
) {
	@Immutable
	data class RemoteAction(val icon: Icon?, val title: String, val actionIntent: PendingIntent)
	
	companion object {
		fun createReplyAction(replyString: String): AMConversationAction {
			return AMConversationAction(true, replyString, null)
		}
		
		fun createRemoteAction(remoteAction: RemoteAction): AMConversationAction {
			return AMConversationAction(false, null, remoteAction)
		}
	}
}