package me.tagavari.airmessage.messaging

import me.tagavari.airmessage.enums.ConversationItemType
import me.tagavari.airmessage.enums.MessageViewType

sealed class ConversationItem(open var localID: Long, var serverID: Long, var guid: String?, var date: Long) {
	@get:ConversationItemType
	abstract val itemType: Int
	
	@get:MessageViewType
	abstract val itemViewType: Int
	abstract fun clone(): ConversationItem
	
	companion object {
		const val viewTypeMessage = 0
		const val viewTypeAction = 1
	}
}