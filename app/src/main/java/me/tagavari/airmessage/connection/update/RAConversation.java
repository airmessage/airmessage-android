package me.tagavari.airmessage.connection.update;

/**
 * Represents a reactive update action concerning a conversation
 */
public abstract class RAConversation {
	public final long conversationID;
	
	public RAConversation(long conversationID) {
		this.conversationID = conversationID;
	}
}