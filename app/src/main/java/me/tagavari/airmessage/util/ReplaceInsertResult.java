package me.tagavari.airmessage.util;

import java.util.Collections;
import java.util.List;

import me.tagavari.airmessage.messaging.ConversationItem;
import me.tagavari.airmessage.messaging.MessageInfo;

public class ReplaceInsertResult {
	private final ConversationItem targetItem;
	private final List<ConversationItem> newItems;
	private final List<MessageInfo> updatedItems;
	private final List<Long> deletedItems;
	
	/**
	 * Represents the result of a 'smart insert' where updated messages are merged into an existing conversation thread
	 * @param targetItem The main item that has been selected to represent the item to be merged
	 * @param newItems Any items that are new and couldn't be matched, to be added to the bottom of the chat
	 * @param updatedItems Any items that were updated as a result of the update
	 * @param deletedItems Any items that were deleted as a result of the update
	 */
	public ReplaceInsertResult(ConversationItem targetItem, List<ConversationItem> newItems, List<MessageInfo> updatedItems, List<Long> deletedItems) {
		this.targetItem = targetItem;
		this.newItems = newItems;
		this.updatedItems = updatedItems;
		this.deletedItems = deletedItems;
	}
	
	/**
	 * Creates a {@link ReplaceInsertResult} for the addition of a single conversation item
	 */
	public static ReplaceInsertResult createAddition(ConversationItem item) {
		return new ReplaceInsertResult(item, Collections.singletonList(item), Collections.emptyList(), Collections.emptyList());
	}
	
	public ConversationItem getTargetItem() {
		return targetItem;
	}
	
	public List<ConversationItem> getNewItems() {
		return newItems;
	}
	
	public List<MessageInfo> getUpdatedItems() {
		return updatedItems;
	}
	
	public List<Long> getDeletedItems() {
		return deletedItems;
	}
}