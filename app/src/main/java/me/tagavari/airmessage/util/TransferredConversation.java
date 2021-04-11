package me.tagavari.airmessage.util;

import java.util.List;

import me.tagavari.airmessage.messaging.ConversationInfo;

//Holds information for the transfer of a conversation, exclusively used by AM bridge to resolve client- and server-created conversations on the fly
public class TransferredConversation {
	private final ConversationInfo serverConversation; //The GUID of the conversation
	private final List<ReplaceInsertResult> serverConversationItems; //Items from the source conversation
	private final ConversationInfo clientConversation; //The target conversation
	
	/**
	 * Constructs a new transferred conversation instance
	 * @param serverConversation The server conversation that is being transferred from
	 * @param serverConversationItems Items from the server conversation
	 * @param clientConversation The target conversation that is being transferred to
	 */
	public TransferredConversation(ConversationInfo serverConversation, List<ReplaceInsertResult> serverConversationItems, ConversationInfo clientConversation) {
		this.serverConversation = serverConversation;
		this.serverConversationItems = serverConversationItems;
		this.clientConversation = clientConversation;
	}
	
	public ConversationInfo getServerConversation() {
		return serverConversation;
	}
	
	public List<ReplaceInsertResult> getServerConversationItems() {
		return serverConversationItems;
	}
	
	public ConversationInfo getClientConversation() {
		return clientConversation;
	}
}