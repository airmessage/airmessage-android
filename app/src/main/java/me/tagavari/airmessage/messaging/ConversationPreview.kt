package me.tagavari.airmessage.messaging

import android.content.Context
import me.tagavari.airmessage.R
import me.tagavari.airmessage.helper.LanguageHelper.getNameFromContentType
import me.tagavari.airmessage.helper.SendStyleHelper
import java.io.Serializable

//A short preview of a conversation used as a summary of the conversation's latest activity
abstract class ConversationPreview(val date: Long) : Serializable {
	/**
	 * Builds a string to represent this preview
	 */
	abstract fun buildString(context: Context): String
	
	//The creation of a chat
	class ChatCreation(date: Long) : ConversationPreview(date) {
		override fun buildString(context: Context) = context.resources.getString(R.string.message_conversationcreated)
	}
	
	//A message in the conversation
	class Message(
		date: Long,
		val isOutgoing: Boolean,
		val message: String?,
		val subject: String?,
		val attachments: List<AttachmentPreview>,
		val sendStyle: String?,
		val isError: Boolean
	) : ConversationPreview(date) {
		override fun buildString(context: Context): String {
			val messageSummary =
				//Applying invisible ink
				if(SendStyleHelper.appleSendStyleBubbleInvisibleInk == sendStyle) {
					context.getString(R.string.message_messageeffect_invisibleink)
				} else if(message != null && subject != null) {
					//Both body and subject
					context.resources.getString(R.string.prefix_wild, subject.replace('\n', ' '), message.replace('\n', ' '))
				} else if(message != null) {
					//Only body
					message.replace('\n', ' ')
				} else if(subject != null) {
					//Only subject
					subject.replace('\n', ' ')
				}
				//Setting the attachments if there are attachments
				else if(attachments.isNotEmpty()) {
					attachmentPreviewListToString(context, attachments)
				}
				//Otherwise setting the message to "unknown"
				else {
					context.resources.getString(R.string.part_unknown)
				}
			
			//Returning the string with the message
			return if(isOutgoing) {
				context.getString(R.string.prefix_you, messageSummary)
			} else {
				messageSummary
			}
		}
		
		companion object {
			@kotlin.jvm.JvmStatic
			fun fromMessage(messageInfo: MessageInfo): Message {
				return Message(
					messageInfo.date,
					messageInfo.isOutgoing,
					messageInfo.messageText,
					messageInfo.messageSubject,
					messageInfo.attachments.map { AttachmentPreview(it.fileName, it.contentType) },
					messageInfo.sendStyle,
					messageInfo.hasError
				)
			}
		}
	}
	
	//A draft message or attachment
	class Draft(date: Long, val message: String?, val attachments: List<AttachmentPreview>) : ConversationPreview(date) {
		override fun buildString(context: Context): String {
			val messageSummary =
				//If we have a draft message, use that
				if(message != null) {
					message
				}
				//Setting the attachments if there are attachments
				else if(attachments.size > 0) {
					attachmentPreviewListToString(context, attachments)
				}
				//Otherwise setting the message to "unknown"
				else {
					context.resources.getString(R.string.part_unknown)
				}
			
			return context.getString(R.string.prefix_draft, messageSummary)
		}
	}
	
	companion object {
		/**
		 * Produces a human-readable summary of an array of attachment previews
		 */
		private fun attachmentPreviewListToString(context: Context, attachments: List<AttachmentPreview>): String {
			return when {
				attachments.size == 1 -> context.resources.getString(getNameFromContentType(attachments[0].type))
				attachments.size > 1 -> context.resources.getQuantityString(R.plurals.message_multipleattachments, attachments.size, attachments.size)
				else -> throw IllegalArgumentException("Empty attachments array received")
			}
		}
	}
}