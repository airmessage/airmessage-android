package me.tagavari.airmessage.messaging;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;

import me.tagavari.airmessage.R;
import me.tagavari.airmessage.helper.LanguageHelper;
import me.tagavari.airmessage.helper.SendStyleHelper;

//A short preview of a conversation used as a summary of the conversation's latest activity
public abstract class ConversationPreview implements Serializable {
	private final long date;
	
	public ConversationPreview(long date) {
		this.date = date;
	}
	
	public long getDate() {
		return date;
	}
	
	/**
	 * Builds a string to represent this preview
	 */
	public abstract String buildString(Context context);
	
	//The creation of a chat
	public static final class ChatCreation extends ConversationPreview {
		public ChatCreation(long date) {
			super(date);
		}
		
		@Override
		public String buildString(Context context) {
			return context.getResources().getString(R.string.message_conversationcreated);
		}
	}
	
	//A message in the conversation
	public static final class Message extends ConversationPreview {
		private final boolean isOutgoing;
		@Nullable private final String message;
		@Nullable private final String subject;
		private final AttachmentPreview[] attachments;
		@Nullable private final String sendStyle;
		private final boolean isError;
		
		public Message(long date, boolean isOutgoing, @Nullable String message, @Nullable String subject, AttachmentPreview[] attachments, @Nullable String sendStyle, boolean isError) {
			super(date);
			this.isOutgoing = isOutgoing;
			this.message = message;
			this.subject = subject;
			this.attachments = attachments;
			this.sendStyle = sendStyle;
			this.isError = isError;
		}
		
		public boolean isOutgoing() {
			return isOutgoing;
		}
		
		@Nullable
		public String getMessage() {
			return message;
		}
		
		@Nullable
		public String getSubject() {
			return subject;
		}
		
		public AttachmentPreview[] getAttachments() {
			return attachments;
		}
		
		@Nullable
		public String getSendStyle() {
			return sendStyle;
		}
		
		public boolean isError() {
			return isError;
		}
		
		@Override
		public String buildString(Context context) {
			//Creating the message variable
			String messageSummary;
			
			//Applying invisible ink
			if(SendStyleHelper.appleSendStyleBubbleInvisibleInk.equals(sendStyle)) messageSummary = context.getString(R.string.message_messageeffect_invisibleink);
				//Otherwise assigning the message to the message text (without line breaks)
			else if(message != null || subject != null) {
				//Only text
				if(message != null) {
					messageSummary = message.replace('\n', ' ');
				}
				//Only subject
				else if(subject != null) {
					messageSummary = subject.replace('\n', ' ');
				}
				//Both text and subject
				else {
					messageSummary = context.getResources().getString(R.string.prefix_wild, subject.replace('\n', ' '), message.replace('\n', ' '));
				}
			}
			//Setting the attachments if there are attachments
			else if(attachments.length > 0) {
				messageSummary = attachmentPreviewArrayToString(context, attachments);
			}
			//Otherwise setting the message to "unknown"
			else {
				messageSummary = context.getResources().getString(R.string.part_unknown);
			}
			
			//Returning the string with the message
			if(isOutgoing) return context.getString(R.string.prefix_you, messageSummary);
			else return messageSummary;
		}
		
		public static ConversationPreview.Message fromMessage(MessageInfo messageInfo) {
			return new Message(
					messageInfo.getDate(),
					messageInfo.isOutgoing(),
					messageInfo.getMessageText(),
					messageInfo.getMessageSubject(),
					messageInfo.getAttachments().stream().map(attachment -> new AttachmentPreview(attachment.getFileName(), attachment.getContentType())).toArray(AttachmentPreview[]::new),
					messageInfo.getSendStyle(),
					messageInfo.hasError()
			);
		}
	}
	
	//A draft message or attachment
	public static final class Draft extends ConversationPreview {
		@Nullable private final String message;
		@NonNull private final AttachmentPreview[] attachments;
		
		public Draft(long date, @Nullable String message, @NonNull AttachmentPreview[] attachments) {
			super(date);
			this.message = message;
			this.attachments = attachments;
		}
		
		@Nullable
		public String getMessage() {
			return message;
		}
		
		@NonNull
		public AttachmentPreview[] getAttachments() {
			return attachments;
		}
		
		@Override
		public String buildString(Context context) {
			String messageSummary;
			
			//If we have a draft message, use that
			if(message != null) {
				messageSummary = message;
			}
			//Setting the attachments if there are attachments
			else if(attachments.length > 0) {
				messageSummary = attachmentPreviewArrayToString(context, attachments);
			}
			//Otherwise setting the message to "unknown"
			else {
				messageSummary = context.getResources().getString(R.string.part_unknown);
			}
			
			return context.getString(R.string.prefix_draft, messageSummary);
		}
	}
	
	/**
	 * Produces a human-readable summary of an array of attachment previews
	 */
	private static String attachmentPreviewArrayToString(Context context, AttachmentPreview[] attachments) {
		if(attachments.length == 1) {
			AttachmentPreview attachmentPreview = attachments[0];
			return context.getResources().getString(LanguageHelper.getNameFromContentType(attachmentPreview.getType()));
		} else if(attachments.length > 1) {
			return context.getResources().getQuantityString(R.plurals.message_multipleattachments, attachments.length, attachments.length);
		} else {
			throw new IllegalArgumentException("Empty attachments array received");
		}
	}
}