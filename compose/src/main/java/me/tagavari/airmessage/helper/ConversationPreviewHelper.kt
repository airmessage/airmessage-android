package me.tagavari.airmessage.helper

import me.tagavari.airmessage.enums.ConversationItemType
import me.tagavari.airmessage.messaging.ConversationItem
import me.tagavari.airmessage.messaging.ConversationPreview
import me.tagavari.airmessage.messaging.ConversationPreview.ChatCreation
import me.tagavari.airmessage.messaging.MessageInfo

object ConversationPreviewHelper {
	/**
	 * Finds the latest valid item in the collection and returns it as a message preview
	 */
	@JvmStatic
	fun latestItemToPreview(items: Collection<ConversationItem>): ConversationPreview? {
		return items
				//Ignore non-applicable message types
				.filter { item -> item.itemType == ConversationItemType.message || item.itemType == ConversationItemType.chatCreate }
				//Find the most recent item
				.maxByOrNull { it.date }
				//Map the item to its preview counterpart
				?.let { item ->
					return when(item.itemType) {
						ConversationItemType.message -> ConversationPreview.Message.fromMessage(item as MessageInfo)
						ConversationItemType.chatCreate -> ChatCreation(item.date)
						else -> throw IllegalStateException("Illegal item type " + item.itemType)
					}
				}
	}
}