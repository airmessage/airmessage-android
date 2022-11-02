package me.tagavari.airmessage.messaging

import androidx.compose.runtime.Immutable
import me.tagavari.airmessage.enums.ConversationItemType
import me.tagavari.airmessage.enums.MessageViewType

@Immutable
sealed class ConversationItem(open var localID: Long, var serverID: Long, var guid: String?, var date: Long) {
	@get:ConversationItemType
	abstract val itemType: Int
	
	@get:MessageViewType
	abstract val itemViewType: Int
	abstract fun clone(): ConversationItem
}