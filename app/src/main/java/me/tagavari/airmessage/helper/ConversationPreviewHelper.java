package me.tagavari.airmessage.helper;

import androidx.annotation.Nullable;

import java.util.Collection;

import me.tagavari.airmessage.enums.ConversationItemType;
import me.tagavari.airmessage.messaging.ChatCreateAction;
import me.tagavari.airmessage.messaging.ConversationItem;
import me.tagavari.airmessage.messaging.ConversationPreview;
import me.tagavari.airmessage.messaging.MessageInfo;

public class ConversationPreviewHelper {
	/**
	 * Finds the latest valid item in the collection and returns it as a message preview
	 */
	@Nullable
	public static ConversationPreview latestItemToPreview(Collection<ConversationItem> items) {
		return items.stream()
				//Ignore non-applicable message types
				.filter(item -> item.getItemType() == ConversationItemType.message || item.getItemType() == ConversationItemType.chatCreate)
				//Find the most recent item
				.max((item1, item2) -> Long.compare(item1.getDate(), item2.getDate()))
				//Map each item to its preview counterpart
				.map(item -> {
					if(item.getItemType() == ConversationItemType.message) {
						return ConversationPreview.Message.fromMessage((MessageInfo) item);
					} else if(item.getItemType() == ConversationItemType.chatCreate) {
						return new ConversationPreview.ChatCreation(item.getDate());
					} else {
						throw new IllegalStateException("Illegal item type " + item.getItemType());
					}
				}).orElse(null);
	}
}