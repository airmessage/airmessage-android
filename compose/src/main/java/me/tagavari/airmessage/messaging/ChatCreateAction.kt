package me.tagavari.airmessage.messaging

import android.content.Context
import androidx.compose.ui.text.AnnotatedString
import me.tagavari.airmessage.compose.R
import me.tagavari.airmessage.enums.ConversationItemType

/**
 * A message used to signify the creation of a new chat
 */
class ChatCreateAction(localID: Long, date: Long) : ConversationAction(localID, -1, null, date) {
	@get:ConversationItemType
	override val itemType: Int
		get() = ConversationItemType.chatCreate
	
	override fun getMessageDirect(context: Context) =
		context.resources.getString(R.string.message_conversationcreated)
	
	override val supportsBuildMessageAsync = false
	
	override fun clone(): ChatCreateAction {
		return ChatCreateAction(localID, date)
	}
}