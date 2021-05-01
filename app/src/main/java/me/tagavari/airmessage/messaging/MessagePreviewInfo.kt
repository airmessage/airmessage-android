package me.tagavari.airmessage.messaging

import me.tagavari.airmessage.enums.MessagePreviewType

/**
 * Represents a preview card for a message component
 */
class MessagePreviewInfo(
	@field:MessagePreviewType @get:MessagePreviewType
	@param:MessagePreviewType val type: Int,
	val localID: Long,
	val data: ByteArray?,
	val target: String,
	val title: String,
	val subtitle: String,
	val caption: String
)