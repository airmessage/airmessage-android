package me.tagavari.airmessage.container

import android.net.Uri

data class ConversationReceivedContent(
	val text: String? = null,
	val attachments: List<Uri> = listOf()
)

data class PendingConversationReceivedContent(
	val conversationID: Long?,
	val content: ConversationReceivedContent
)
