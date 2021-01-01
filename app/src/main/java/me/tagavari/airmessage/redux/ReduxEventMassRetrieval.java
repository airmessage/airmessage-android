package me.tagavari.airmessage.redux;

import java.util.Collection;

import me.tagavari.airmessage.enums.MassRetrievalErrorCode;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.ConversationItem;

//An event to represent the status of a mass retrieval
public abstract class ReduxEventMassRetrieval {
	public static class Start extends ReduxEventMassRetrieval {
		private final Collection<ConversationInfo> conversations;
		private final int messageCount;
		
		public Start(Collection<ConversationInfo> conversations, int messageCount) {
			this.conversations = conversations;
			this.messageCount = messageCount;
		}
		
		public Collection<ConversationInfo> getConversations() {
			return conversations;
		}
		
		public int getMessageCount() {
			return messageCount;
		}
	}
	
	public static class Progress extends ReduxEventMassRetrieval {
		private final Collection<ConversationItem> items;
		private final int receivedItems, totalItems;
		
		public Progress(Collection<ConversationItem> items, int receivedItems, int totalItems) {
			this.items = items;
			this.receivedItems = receivedItems;
			this.totalItems = totalItems;
		}
		
		public Collection<ConversationItem> getItems() {
			return items;
		}
		
		public int getReceivedItems() {
			return receivedItems;
		}
		
		public int getTotalItems() {
			return totalItems;
		}
	}
	
	public static class Complete extends ReduxEventMassRetrieval {
		
	}
	
	public static class Error extends ReduxEventMassRetrieval {
		@MassRetrievalErrorCode int code;
		
		public Error(int code) {
			this.code = code;
		}
		
		public int getCode() {
			return code;
		}
	}
}