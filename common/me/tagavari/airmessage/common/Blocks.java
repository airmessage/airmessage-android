package me.tagavari.airmessage.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import me.tagavari.airmessage.enums.GroupAction;
import me.tagavari.airmessage.enums.MessageSendErrorCode;
import me.tagavari.airmessage.enums.MessageState;

import java.util.List;

/**
 * POJOs for messaging data received over the network
 */
public class Blocks {
	public static class ConversationInfo {
		public final String guid;
		public final boolean available;
		public final String service;
		@Nullable public final String name;
		@NonNull public final String[] members;
		
		//Conversation unavailable
		public ConversationInfo(String guid) {
			//Setting the values
			this.guid = guid;
			this.available = false;
			this.service = null;
			this.name = null;
			this.members = null;
		}
		
		//Conversation available
		public ConversationInfo(String guid, String service, @Nullable String name, @NonNull String[] members) {
			//Setting the values
			this.guid = guid;
			this.available = true;
			this.service = service;
			this.name = name;
			this.members = members;
		}
	}
	
	public static abstract class ConversationItem {
		public final long serverID;
		public final String guid;
		public final String chatGuid;
		public final long date;
		
		public ConversationItem(long serverID, String guid, String chatGuid, long date) {
			this.serverID = serverID;
			this.guid = guid;
			this.chatGuid = chatGuid;
			this.date = date;
		}
	}
	
	public static class MessageInfo extends ConversationItem {
		@Nullable public final String text;
		@Nullable public final String subject;
		@Nullable public final String sender;
		@NonNull public final List<AttachmentInfo> attachments;
		@NonNull public final List<StickerModifierInfo> stickers;
		@NonNull public final List<TapbackModifierInfo> tapbacks;
		@Nullable public final String sendEffect;
		@MessageState public final int stateCode;
		@MessageSendErrorCode public final int errorCode;
		public final long dateRead;
		
		public MessageInfo(long serverID, String guid, String chatGuid, long date, @Nullable String text, @Nullable String subject, @Nullable String sender, @NonNull List<AttachmentInfo> attachments, @NonNull List<StickerModifierInfo> stickers, @NonNull List<TapbackModifierInfo> tapbacks, @Nullable String sendEffect, @MessageState int stateCode, @MessageSendErrorCode int errorCode, long dateRead) {
			//Calling the super constructor
			super(serverID, guid, chatGuid, date);
			
			//Setting the variables
			this.text = text;
			this.subject = subject;
			this.sender = sender;
			this.attachments = attachments;
			this.stickers = stickers;
			this.tapbacks = tapbacks;
			this.sendEffect = sendEffect;
			this.stateCode = stateCode;
			this.errorCode = errorCode;
			this.dateRead = dateRead;
		}
	}
	
	public static class GroupActionInfo extends ConversationItem {
		@Nullable public final String agent;
		@Nullable public final String other;
		@GroupAction public final int groupActionType;
		
		public GroupActionInfo(long serverID, String guid, String chatGuid, long date, @Nullable String agent, @Nullable String other, @GroupAction int groupActionType) {
			//Calling the super constructor
			super(serverID, guid, chatGuid, date);
			
			//Setting the variables
			this.agent = agent;
			this.other = other;
			this.groupActionType = groupActionType;
		}
	}
	
	public static class ChatRenameActionInfo extends ConversationItem {
		@Nullable public final String agent;
		@NonNull public final String newChatName;
		
		public ChatRenameActionInfo(long serverID, String guid, String chatGuid, long date, @Nullable String agent, @NonNull String newChatName) {
			//Calling the super constructor
			super(serverID, guid, chatGuid, date);
			
			//Setting the variables
			this.agent = agent;
			this.newChatName = newChatName;
		}
	}
	
	public static class AttachmentInfo {
		@NonNull public final String guid;
		@NonNull public final String name;
		@NonNull public final String type;
		public final long size;
		@Nullable public final byte[] checksum;
		public final long sort;
		
		public AttachmentInfo(@NonNull String guid, @NonNull String name, @NonNull String type, long size, @Nullable byte[] checksum, long sort) {
			//Setting the variables
			this.guid = guid;
			this.name = name;
			this.type = type;
			this.size = size;
			this.checksum = checksum;
			this.sort = sort;
		}
	}
	
	public static abstract class ModifierInfo {
		public String message;
		
		public ModifierInfo(String message) {
			this.message = message;
		}
	}
	
	public static class ActivityStatusModifierInfo extends ModifierInfo {
		@MessageState public final int state;
		public final long dateRead;
		
		public ActivityStatusModifierInfo(String message, @MessageState int state, long dateRead) {
			//Calling the super constructor
			super(message);
			
			//Setting the values
			this.state = state;
			this.dateRead = dateRead;
		}
	}
	
	public static class StickerModifierInfo extends ModifierInfo {
		public final int messageIndex;
		public final String fileGuid;
		public final String sender;
		public final long date;
		public final byte[] data;
		public final String type;
		
		public StickerModifierInfo(String message, int messageIndex, String fileGuid, String sender, long date, byte[] data, String type) {
			//Calling the super constructor
			super(message);
			
			//Setting the values
			this.messageIndex = messageIndex;
			this.fileGuid = fileGuid;
			this.sender = sender;
			this.date = date;
			this.data = data;
			this.type = type;
		}
	}
	
	public static class TapbackModifierInfo extends ModifierInfo {
		public final int messageIndex;
		public final String sender;
		public final boolean isAddition;
		public final int tapbackType;
		
		public TapbackModifierInfo(String message, int messageIndex, String sender, boolean isAddition, int tapbackType) {
			//Calling the super constructor
			super(message);
			
			//Setting the values
			this.messageIndex = messageIndex;
			this.sender = sender;
			this.isAddition = isAddition;
			this.tapbackType = tapbackType;
		}
	}
}