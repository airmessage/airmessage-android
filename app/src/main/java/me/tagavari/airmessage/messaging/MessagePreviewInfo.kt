package me.tagavari.airmessage.messaging

import me.tagavari.airmessage.enums.MessagePreviewType

/**
 * Represents a preview card for a message component
 */
class MessagePreviewInfo(
	@MessagePreviewType val type: Int,
	var localID: Long,
	val data: ByteArray?,
	val target: String,
	val title: String,
	val subtitle: String,
	val caption: String
)