package me.tagavari.airmessage.util

import me.tagavari.airmessage.connection.exception.AMRequestException
import me.tagavari.airmessage.enums.ChatCreateErrorCode
import me.tagavari.airmessage.enums.MessageSendErrorCode

/**
 * Represents error information that has an error code and an optional error detail string
 * @param error The error code
 * @param detail The error detail string, if available
 */
abstract class CompoundErrorDetails(val error: Int, val detail: String?) {
	fun toException(): AMRequestException {
		return AMRequestException(error, detail)
	}
	
	class MessageSend(@MessageSendErrorCode error: Int, detail: String?) : CompoundErrorDetails(error, detail)
	class ChatCreate(@ChatCreateErrorCode error: Int, detail: String?) : CompoundErrorDetails(error, detail)
}