package me.tagavari.airmessage.common.util

import me.tagavari.airmessage.common.messaging.ConversationInfo

/**
 * Holds information for the transfer of a conversation, exclusively used by AM bridge to resolve client- and server-created conversations on the fly
 * @param serverConversation The server conversation that is being transferred from
 * @param serverConversationItems Items from the server conversation
 * @param clientConversation The target conversation that is being transferred to
 */
data class TransferredConversation(
	val serverConversation: ConversationInfo,
	val serverConversationItems: List<ReplaceInsertResult>,
	val clientConversation: ConversationInfo
)