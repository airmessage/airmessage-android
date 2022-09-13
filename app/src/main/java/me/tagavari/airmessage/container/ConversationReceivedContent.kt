package me.tagavari.airmessage.container

import android.net.Uri

data class ConversationReceivedContent(
	val conversationID: Long,
	val text: String?,
	val attachments: List<Uri>
)
