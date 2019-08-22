package me.tagavari.airmessage.connection.request;

import me.tagavari.airmessage.messaging.ConversationInfo;

public class ConversationInfoRequest {
	private final ConversationInfo conversationInfo;
	private final boolean sendNotifications;
	
	public ConversationInfoRequest(ConversationInfo conversationInfo, boolean sendNotifications) {
		this.conversationInfo = conversationInfo;
		this.sendNotifications = sendNotifications;
	}
	
	public ConversationInfo getConversationInfo() {
		return conversationInfo;
	}
	
	public boolean isSendNotifications() {
		return sendNotifications;
	}
}
