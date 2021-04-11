package me.tagavari.airmessage.helper

import me.tagavari.airmessage.data.DatabaseManager
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.messaging.ConversationItem
import me.tagavari.airmessage.util.ConversationValueUpdateResult

object ConversationHelper {
	//Sorts conversations in descending order by their preview date
	@JvmField
	val conversationComparator: Comparator<ConversationInfo> = Comparator { conversation1, conversation2 ->
		//Getting the last conversation item times
		val dynamicPreview1 = conversation1.dynamicPreview
		val dynamicPreview2 = conversation2.dynamicPreview
		
		val lastTime1 = dynamicPreview1?.date ?: Long.MIN_VALUE
		val lastTime2 = dynamicPreview2?.date ?: Long.MIN_VALUE
		
		lastTime2.compareTo(lastTime1)
	}
	
	@JvmField
	val conversationItemComparator: Comparator<ConversationItem> = Comparator { item1, item2 ->
		when {
			item1.serverID != -1L && item2.serverID != -1L -> item1.serverID.compareTo(item2.serverID)
			item1.localID != -1L && item2.localID != -1L -> item1.localID.compareTo(item2.localID)
			item1.localID == -1L && item2.localID == -1L -> item1.date.compareTo(item2.date)
			item1.localID == -1L -> 1
			else -> -1 //Item 2's local ID is -1
		}
	}
	
	/**
	 * Finds the index to insert the conversation into a list based on its preview, in descending chronological order
	 * @param conversationInfo The conversation to check
	 * @param list The list of conversations to compare against
	 * @return The index to insert the conversation at to retain the list in chronological order
	 */
	@JvmStatic
	fun findInsertionIndex(conversationInfo: ConversationInfo, list: List<ConversationInfo>): Int {
		//Getting the conversation's dynamic preview
		val dynamicPreview = conversationInfo.dynamicPreview
		
		//If the conversation has no dynamic preview, add the item to the end of the list
		if(dynamicPreview == null) {
			return list.size
		}
		
		//Finding the correct insertion position for this conversation
		list.forEachIndexed { i, listedConversation ->
			val listedPreview = listedConversation.dynamicPreview
			if(listedPreview != null && listedPreview.date < dynamicPreview.date) {
				return i
			}
		}
		
		//No matches; add this conversation to the end of the list
		return list.size
	}
	
	/**
	 * Finds the index to re-insert the conversation into a list after its removal based on its preview, in descending chronological order
	 * @param conversationInfo The conversation to check
	 * @param list The list of conversations to compare against
	 * @return The index to insert the conversation at to retain the list in chronological order
	 */
	@JvmStatic
	fun findReinsertionIndex(conversationInfo: ConversationInfo, list: List<ConversationInfo>): Int {
		//Getting the conversation's dynamic preview
		val dynamicPreview = conversationInfo.dynamicPreview
		
		//If the conversation has no dynamic preview, add the item to the end of the list
		if(dynamicPreview == null) {
			return list.size
		}
		
		//Finding the correct insertion position for this conversation
		var pastOriginalIndex = false
		list.forEachIndexed { i, listedConversation ->
			//If we hit the original item, mark it down and continue to the next item
			if(conversationInfo.localID == listedConversation.localID) {
				pastOriginalIndex = true
				return@forEachIndexed
			}
			
			val listedPreview = listedConversation.dynamicPreview
			if(listedPreview != null && listedPreview.date < dynamicPreview.date) {
				//If we're past the point where this conversation will be removed, compensate by going one index down
				return if(pastOriginalIndex) i - 1 else i
			}
		}
		
		//No matches; add this conversation to the end of the list
		return list.size - 1
	}
	
	/**
	 * Updates the state of a conversation in response to a message update
	 * @param foregroundConversationIDs A list of the IDs of conversations currently loaded in the foreground
	 * @param conversationInfo The conversation to update
	 * @param incomingMessageCount The amount of incoming messages
	 */
	@JvmStatic
	fun updateConversationValues(foregroundConversationIDs: Collection<Long>, conversationInfo: ConversationInfo, incomingMessageCount: Int): ConversationValueUpdateResult {
		//Unarchiving the conversation if it is archived
		if(conversationInfo.isArchived) {
			DatabaseManager.getInstance().updateConversationArchived(conversationInfo.localID, false)
		}
		
		//Incrementing the unread count if the conversation is not part of a conversation that is currently in the foreground, and we have been requested to do so
		val updateUnread = incomingMessageCount > 0 && !foregroundConversationIDs.contains(conversationInfo.localID)
		if(updateUnread) {
			DatabaseManager.getInstance().incrementUnreadMessageCount(conversationInfo.localID)
		}
		
		return ConversationValueUpdateResult(conversationInfo.isArchived, if(updateUnread) incomingMessageCount else 0)
	}
}