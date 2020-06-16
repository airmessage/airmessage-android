package me.tagavari.airmessage.common;

import java.util.List;

public class Blocks {
	public static class ConversationInfo {
		public String guid;
		public boolean available;
		public String service;
		public String name;
		public String[] members;
		
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
		public ConversationInfo(String guid, String service, String name, String[] members) {
			//Setting the values
			this.guid = guid;
			this.available = true;
			this.service = service;
			this.name = name;
			this.members = members;
		}
	}
	
	public static abstract class ConversationItem {
		public long serverID;
		public String guid;
		public String chatGuid;
		public long date;
		
		public ConversationItem(long serverID, String guid, String chatGuid, long date) {
			this.serverID = serverID;
			this.guid = guid;
			this.chatGuid = chatGuid;
			this.date = date;
		}
	}
	
	public static class MessageInfo extends ConversationItem {
		private static final int itemType = 0;
		
		public static final int stateCodeGhost = 0;
		public static final int stateCodeIdle = 1;
		public static final int stateCodeSent = 2;
		public static final int stateCodeDelivered = 3;
		public static final int stateCodeRead = 4;
		
		public String text;
		public String subject;
		public String sender;
		public List<AttachmentInfo> attachments;
		public List<StickerModifierInfo> stickers;
		public List<TapbackModifierInfo> tapbacks;
		public String sendEffect;
		public int stateCode;
		public int errorCode;
		public long dateRead;
		
		public MessageInfo(long serverID, String guid, String chatGuid, long date, String text, String subject, String sender, List<AttachmentInfo> attachments, List<StickerModifierInfo> stickers, List<TapbackModifierInfo> tapbacks, String sendEffect, int stateCode, int errorCode, long dateRead) {
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
		private static final int itemType = 1;
		
		public String agent;
		public String other;
		public int groupActionType;
		
		public GroupActionInfo(long serverID, String guid, String chatGuid, long date, String agent, String other, int groupActionType) {
			//Calling the super constructor
			super(serverID, guid, chatGuid, date);
			
			//Setting the variables
			this.agent = agent;
			this.other = other;
			this.groupActionType = groupActionType;
		}
	}
	
	public static class ChatRenameActionInfo extends ConversationItem {
		private static final int itemType = 2;
		
		public String agent;
		public String newChatName;
		
		public ChatRenameActionInfo(long serverID, String guid, String chatGuid, long date, String agent, String newChatName) {
			//Calling the super constructor
			super(serverID, guid, chatGuid, date);
			
			//Setting the variables
			this.agent = agent;
			this.newChatName = newChatName;
		}
	}
	
	public static class AttachmentInfo {
		public String guid;
		public String name;
		public String type;
		public long size;
		public byte[] checksum;
		
		public AttachmentInfo(String guid, String name, String type, long size, byte[] checksum) {
			//Setting the variables
			this.guid = guid;
			this.name = name;
			this.type = type;
			this.size = size;
			this.checksum = checksum;
		}
	}
	
	public static abstract class ModifierInfo {
		public String message;
		
		public ModifierInfo(String message) {
			this.message = message;
		}
	}
	
	public static class ActivityStatusModifierInfo extends ModifierInfo {
		private static final int itemType = 0;
		
		public int state;
		public long dateRead;
		
		public ActivityStatusModifierInfo(String message, int state, long dateRead) {
			//Calling the super constructor
			super(message);
			
			//Setting the values
			this.state = state;
			this.dateRead = dateRead;
		}
	}
	
	public static class StickerModifierInfo extends ModifierInfo {
		private static final int itemType = 1;
		
		public int messageIndex;
		public String fileGuid;
		public String sender;
		public long date;
		public byte[] data;
		public String type;
		
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
		private static final int itemType = 2;
		
		//Creating the reference values
		public static final int tapbackLove = 0;
		public static final int tapbackLike = 1;
		public static final int tapbackDislike = 2;
		public static final int tapbackLaugh = 3;
		public static final int tapbackEmphasis = 4;
		public static final int tapbackQuestion = 5;
		
		public int messageIndex;
		public String sender;
		public boolean isAddition;
		public int tapbackType;
		
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