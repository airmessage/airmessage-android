package me.tagavari.airmessage.helper;

import android.content.Context;
import android.util.Pair;

import me.tagavari.airmessage.R;
import me.tagavari.airmessage.enums.MessageSendErrorCode;
import me.tagavari.airmessage.messaging.ConversationInfo;

public class ErrorLanguageHelper {
	/**
	 * Gets error details to display to the user from a constant error code
	 * @param context The context to use
	 * @param conversationInfo The conversation relevant to the error
	 * @param errorCode The error code
	 * @return A pair, containing a String for the error message, and a Boolean for whether the error is recoverable
	 */
	public static Pair<String, Boolean> getErrorDisplay(Context context, ConversationInfo conversationInfo, @MessageSendErrorCode int errorCode) {
		String message;
		boolean showRetryButton;
		
		switch(errorCode) {
			case MessageSendErrorCode.localUnknown:
			default:
				//Setting the message
				message = context.getResources().getString(R.string.message_unknownerror);
				
				//Enabling the retry button
				showRetryButton = true;
				
				break;
			
			case MessageSendErrorCode.localInvalidContent:
				//Setting the message
				message = context.getResources().getString(R.string.message_messageerror_desc_air_invalidcontent);
				
				//Disabling the retry button
				showRetryButton = false;
				
				break;
			case MessageSendErrorCode.localFileTooLarge:
				//Setting the message
				message = context.getResources().getString(R.string.message_messageerror_desc_air_filetoolarge);
				
				//Disabling the retry button
				showRetryButton = false;
				
				break;
			case MessageSendErrorCode.localIO:
				//Setting the message
				message = context.getResources().getString(R.string.message_messageerror_desc_air_io);
				
				//Enabling the retry button
				showRetryButton = true;
				
				break;
			case MessageSendErrorCode.localNetwork:
				//Setting the message
				message = context.getResources().getString(R.string.message_messageerror_desc_air_network);
				
				//Enabling the retry button
				showRetryButton = true;
				
				break;
			case MessageSendErrorCode.serverExternal:
				//Setting the message
				message = context.getResources().getString(R.string.message_messageerror_desc_air_external);
				
				//Enabling the retry button
				showRetryButton = true;
				
				break;
			case MessageSendErrorCode.localExpired:
				//Setting the message
				message = context.getResources().getString(R.string.message_messageerror_desc_air_expired);
				
				//Enabling the retry button
				showRetryButton = true;
				
				break;
			case MessageSendErrorCode.localReferences:
				//Setting the message
				message = context.getResources().getString(R.string.message_messageerror_desc_air_references);
				
				//Enabling the retry button
				showRetryButton = true;
				
				break;
			case MessageSendErrorCode.localInternal:
				//Setting the message
				message = context.getResources().getString(R.string.message_messageerror_desc_air_internal);
				
				//Enabling the retry button
				showRetryButton = true;
				
				break;
			case MessageSendErrorCode.serverBadRequest:
				//Setting the message
				message = context.getResources().getString(R.string.message_messageerror_desc_air_badrequest);
				
				//Enabling the retry button
				showRetryButton = true;
				
				break;
			case MessageSendErrorCode.serverUnauthorized:
				//Setting the message
				message = context.getResources().getString(R.string.message_messageerror_desc_air_unauthorized);
				
				//Enabling the retry button
				showRetryButton = true;
				
				break;
			case MessageSendErrorCode.serverNoConversation:
				//Setting the message
				message = context.getResources().getString(R.string.message_messageerror_desc_air_noconversation);
				
				//Disabling the retry button
				showRetryButton = false;
				
				break;
			case MessageSendErrorCode.serverRequestTimeout:
				//Setting the message
				message = context.getResources().getString(R.string.message_messageerror_desc_air_serverexpired);
				
				//Disabling the retry button
				showRetryButton = true;
				
				break;
			case MessageSendErrorCode.serverUnknown:
				//Setting the message
				message = context.getResources().getString(R.string.message_messageerror_desc_air_externalunknown);
				
				//Disabling the retry button
				showRetryButton = true;
				
				break;
			case MessageSendErrorCode.appleNetwork:
				//Setting the message
				message = context.getResources().getString(R.string.message_messageerror_desc_apple_network);
				
				//Disabling the retry button
				showRetryButton = false;
				
				break;
			case MessageSendErrorCode.appleUnregistered:
				//Setting the message
				message = conversationInfo.getMembers().isEmpty() ?
						context.getResources().getString(R.string.message_messageerror_desc_apple_unregistered_generic) :
						context.getResources().getString(R.string.message_messageerror_desc_apple_unregistered, conversationInfo.getMembers().get(0).getAddress());
				
				//Disabling the retry button
				showRetryButton = false;
				
				break;
		}
		
		//Returning the information
		return new Pair<>(message, showRetryButton);
	}
}