package me.tagavari.airmessage.common.messaging

import androidx.compose.runtime.Immutable
import me.tagavari.airmessage.common.enums.MessagePreviewType

/**
 * Represents a preview card for a message component
 */
@Immutable
class MessagePreviewInfo(
	@MessagePreviewType val type: Int,
	var localID: Long,
	val data: ByteArray?,
	val target: String,
	val title: String,
	val subtitle: String,
	val caption: String
)