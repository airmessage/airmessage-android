package me.tagavari.airmessage.common.helper

import android.content.Context
import me.tagavari.airmessage.R
import me.tagavari.airmessage.common.enums.MessageSendErrorCode
import me.tagavari.airmessage.common.messaging.ConversationInfo

object ErrorLanguageHelper {
	/**
	 * Gets error details to display to the user from a constant error code
	 * @param context The context to use
	 * @param conversationInfo The conversation relevant to the error
	 * @param errorCode The error code
	 * @return A pair, containing a String for the error message, and a Boolean for whether the error is recoverable
	 */
	@JvmStatic
	fun getErrorDisplay(context: Context, conversationInfo: ConversationInfo, @MessageSendErrorCode errorCode: Int): ErrorDisplay {
		val message: String
		val showRetryButton: Boolean
		when(errorCode) {
			MessageSendErrorCode.localUnknown -> {
				//Setting the message
				message = context.resources.getString(R.string.message_unknownerror)
				
				//Enabling the retry button
				showRetryButton = true
			}
			MessageSendErrorCode.localInvalidContent -> {
				//Setting the message
				message = context.resources.getString(R.string.message_messageerror_desc_air_invalidcontent)
				
				//Disabling the retry button
				showRetryButton = false
			}
			MessageSendErrorCode.localFileTooLarge -> {
				//Setting the message
				message = context.resources.getString(R.string.message_messageerror_desc_air_filetoolarge)
				
				//Disabling the retry button
				showRetryButton = false
			}
			MessageSendErrorCode.localIO -> {
				//Setting the message
				message = context.resources.getString(R.string.message_messageerror_desc_air_io)
				
				//Enabling the retry button
				showRetryButton = true
			}
			MessageSendErrorCode.localNetwork -> {
				//Setting the message
				message = context.resources.getString(R.string.message_messageerror_desc_air_network)
				
				//Enabling the retry button
				showRetryButton = true
			}
			MessageSendErrorCode.serverExternal -> {
				//Setting the message
				message = context.resources.getString(R.string.message_messageerror_desc_air_external)
				
				//Enabling the retry button
				showRetryButton = true
			}
			MessageSendErrorCode.localExpired -> {
				//Setting the message
				message = context.resources.getString(R.string.message_messageerror_desc_air_expired)
				
				//Enabling the retry button
				showRetryButton = true
			}
			MessageSendErrorCode.localReferences -> {
				//Setting the message
				message = context.resources.getString(R.string.message_messageerror_desc_air_references)
				
				//Enabling the retry button
				showRetryButton = true
			}
			MessageSendErrorCode.localInternal -> {
				//Setting the message
				message = context.resources.getString(R.string.message_messageerror_desc_air_internal)
				
				//Enabling the retry button
				showRetryButton = true
			}
			MessageSendErrorCode.serverBadRequest -> {
				//Setting the message
				message = context.resources.getString(R.string.message_messageerror_desc_air_badrequest)
				
				//Enabling the retry button
				showRetryButton = true
			}
			MessageSendErrorCode.serverUnauthorized -> {
				//Setting the message
				message = context.resources.getString(R.string.message_messageerror_desc_air_unauthorized)
				
				//Enabling the retry button
				showRetryButton = true
			}
			MessageSendErrorCode.serverNoConversation -> {
				//Setting the message
				message = context.resources.getString(R.string.message_messageerror_desc_air_noconversation)
				
				//Disabling the retry button
				showRetryButton = false
			}
			MessageSendErrorCode.serverRequestTimeout -> {
				//Setting the message
				message = context.resources.getString(R.string.message_messageerror_desc_air_serverexpired)
				
				//Disabling the retry button
				showRetryButton = true
			}
			MessageSendErrorCode.serverUnknown -> {
				//Setting the message
				message = context.resources.getString(R.string.message_messageerror_desc_air_externalunknown)
				
				//Disabling the retry button
				showRetryButton = true
			}
			MessageSendErrorCode.appleNetwork -> {
				//Setting the message
				message = context.resources.getString(R.string.message_messageerror_desc_apple_network)
				
				//Disabling the retry button
				showRetryButton = false
			}
			MessageSendErrorCode.appleUnregistered -> {
				//Setting the message
				message = if(conversationInfo.members.isEmpty()) {
					context.resources.getString(R.string.message_messageerror_desc_apple_unregistered_generic)
				} else {
					context.resources.getString(R.string.message_messageerror_desc_apple_unregistered, conversationInfo.members[0].address)
				}
				
				//Disabling the retry button
				showRetryButton = false
			}
			else -> {
				message = context.resources.getString(R.string.message_unknownerror)
				showRetryButton = true
			}
		}
		
		//Returning the information
		return ErrorDisplay(message, showRetryButton)
	}
	
	data class ErrorDisplay(val message: String, val isRecoverable: Boolean)
}