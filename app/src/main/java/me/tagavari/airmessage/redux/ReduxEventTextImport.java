package me.tagavari.airmessage.redux;

import java.util.Collection;

import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.ConversationItem;

//An event to represent the status of a mass retrieval
public abstract class ReduxEventTextImport {
	public static class Start extends ReduxEventTextImport {
		private final int itemCount;
		
		public Start(int itemCount) {
			this.itemCount = itemCount;
		}
		
		public int getItemCount() {
			return itemCount;
		}
	}
	
	public static class Progress extends ReduxEventTextImport {
		private final int receivedItems, totalItems;
		
		public Progress(int receivedItems, int totalItems) {
			this.receivedItems = receivedItems;
			this.totalItems = totalItems;
		}
		
		public int getReceivedItems() {
			return receivedItems;
		}
		
		public int getTotalItems() {
			return totalItems;
		}
	}
	
	public static class Complete extends ReduxEventTextImport {
		private final Collection<ConversationInfo> conversations;
		
		public Complete(Collection<ConversationInfo> conversations) {
			this.conversations = conversations;
		}
		
		public Collection<ConversationInfo> getConversations() {
			return conversations;
		}
	}
	
	public static class Fail extends ReduxEventTextImport {
	}
}