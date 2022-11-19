package me.tagavari.airmessage.common.util

import me.tagavari.airmessage.common.connection.exception.AMRequestException
import me.tagavari.airmessage.common.enums.ChatCreateErrorCode
import me.tagavari.airmessage.common.enums.MessageSendErrorCode

/**
 * Represents error information that has an error code and an optional error detail string
 * @param error The error code
 * @param detail The error detail string, if available
 */
sealed class CompoundErrorDetails(val error: Int, val detail: String?) {
	fun toException(): AMRequestException {
		return AMRequestException(error, detail)
	}
	
	class MessageSend(@MessageSendErrorCode error: Int, detail: String?) : CompoundErrorDetails(error, detail)
	class ChatCreate(@ChatCreateErrorCode error: Int, detail: String?) : CompoundErrorDetails(error, detail)
}