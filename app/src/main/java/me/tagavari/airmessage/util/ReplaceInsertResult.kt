package me.tagavari.airmessage.util

import me.tagavari.airmessage.messaging.ConversationItem
import me.tagavari.airmessage.messaging.MessageInfo
import me.tagavari.airmessage.util.ReplaceInsertResult

/**
 * Represents the result of a 'smart insert' where updated messages are merged into an existing conversation thread
 * @param targetItem The main item that has been selected to represent the item to be merged
 * @param newItems Any items that are new and couldn't be matched, to be added to the bottom of the chat
 * @param updatedItems Any items that were updated as a result of the update
 * @param deletedItems Any items that were deleted as a result of the update
 */
class ReplaceInsertResult(
	val targetItem: ConversationItem,
	val newItems: List<ConversationItem>,
	val updatedItems: List<MessageInfo>,
	val deletedItems: List<Long>) {
	
	companion object {
		/**
		 * Creates a [ReplaceInsertResult] for the addition of a single conversation item
		 */
		@JvmStatic
		fun createAddition(item: ConversationItem): ReplaceInsertResult {
			return ReplaceInsertResult(item, listOf(item), emptyList(), emptyList())
		}
	}
}