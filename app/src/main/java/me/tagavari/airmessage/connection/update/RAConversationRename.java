package me.tagavari.airmessage.connection.update;

public class RAConversationRename extends RAConversation {
	public final String title;
	
	public RAConversationRename(long conversationID, String title) {
		super(conversationID);
		this.title = title;
	}
}