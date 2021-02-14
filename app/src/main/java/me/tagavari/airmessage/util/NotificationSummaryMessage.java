package me.tagavari.airmessage.util;

//Represents an item in a summary notification
public class NotificationSummaryMessage {
	private final String conversation;
	private final String body;
	
	public NotificationSummaryMessage(String conversation, String body) {
		this.conversation = conversation;
		this.body = body;
	}
	
	public String getConversation() {
		return conversation;
	}
	
	public String getBody() {
		return body;
	}
}