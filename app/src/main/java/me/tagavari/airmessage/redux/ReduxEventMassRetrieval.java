package me.tagavari.airmessage.redux;

import java.util.Collection;

import me.tagavari.airmessage.enums.MassRetrievalErrorCode;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.ConversationItem;

//An event to represent the status of a mass retrieval
public abstract class ReduxEventMassRetrieval {
	private final short requestID;
	
	public ReduxEventMassRetrieval(short requestID) {
		this.requestID = requestID;
	}
	
	public short getRequestID() {
		return requestID;
	}
	
	public static class Start extends ReduxEventMassRetrieval {
		private final Collection<ConversationInfo> conversations;
		private final int messageCount;
		
		public Start(short requestID, Collection<ConversationInfo> conversations, int messageCount) {
			super(requestID);
			
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
		
		public Progress(short requestID, Collection<ConversationItem> items, int receivedItems, int totalItems) {
			super(requestID);
			
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
	
	public static class File extends ReduxEventMassRetrieval {
		public File(short requestID) {
			super(requestID);
		}
	}
	
	public static class Complete extends ReduxEventMassRetrieval {
		public Complete(short requestID) {
			super(requestID);
		}
	}
	
	public static class Error extends ReduxEventMassRetrieval {
		@MassRetrievalErrorCode int code;
		
		public Error(short requestID, int code) {
			super(requestID);
			
			this.code = code;
		}
		
		public int getCode() {
			return code;
		}
	}
}