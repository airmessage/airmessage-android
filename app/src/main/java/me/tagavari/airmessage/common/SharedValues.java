package me.tagavari.airmessage.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class SharedValues {
	public interface BlockAccess {
		Blocks.Block toBlock();
	}
	
	public static class ConversationInfo implements Serializable, BlockAccess {
		private static final long serialVersionUID = 100;
		
		public String guid;
		public boolean available;
		public String service;
		public String name;
		public ArrayList<String> members;
		
		//Conversation unavailable
		public ConversationInfo(String guid) {
			//Setting the values
			this.guid = guid;
			this.available = false;
			this.name = null;
			this.members = null;
		}
		
		//Conversation available
		public ConversationInfo(String guid, String service, String name, ArrayList<String> members) {
			//Setting the values
			this.guid = guid;
			this.available = true;
			this.service = service;
			this.name = name;
			this.members = members;
		}
		
		private void readObject(ObjectInputStream stream) throws ClassNotFoundException, ClassCastException, IOException {
			//Reading the data
			guid = stream.readUTF();
			available = stream.readBoolean();
			service = stream.readUTF();
			name = (String) stream.readObject();
			members = (ArrayList<String>) stream.readObject();
		}
		
		@Override
		public Blocks.ConversationInfo toBlock() {
			if(available) return new Blocks.ConversationInfo(guid, service, name, members.toArray(new String[0]));
			else return new Blocks.ConversationInfo(guid);
		}
	}
	
	public static abstract class ConversationItem implements Serializable, Cloneable, BlockAccess {
		private static final long serialVersionUID = 101;
		
		public String guid;
		public String chatGuid;
		public long date;
		
		public ConversationItem(String guid, String chatGuid, long date) {
			this.guid = guid;
			this.chatGuid = chatGuid;
			this.date = date;
		}
		
		@Override
		public abstract Blocks.ConversationItem toBlock();
	}
	
	public static class MessageInfo extends ConversationItem {
		private static final long serialVersionUID = 102;
		
		public static final int stateCodeGhost = 0;
		public static final int stateCodeIdle = 1;
		public static final int stateCodeSent = 2;
		public static final int stateCodeDelivered = 3;
		public static final int stateCodeRead = 4;
		
		public String text;
		public String sendEffect;
		public String sender;
		public ArrayList<AttachmentInfo> attachments;
		public ArrayList<StickerModifierInfo> stickers;
		public ArrayList<TapbackModifierInfo> tapbacks;
		public int stateCode;
		public int errorCode;
		public long dateRead;
		
		public MessageInfo(String guid, String chatGuid, long date, String text, String sender, ArrayList<AttachmentInfo> attachments, ArrayList<StickerModifierInfo> stickers, ArrayList<TapbackModifierInfo> tapbacks, String sendEffect, int stateCode, int errorCode, long dateRead) {
			//Calling the super constructor
			super(guid, chatGuid, date);
			
			//Setting the variables
			this.text = text;
			this.sender = sender;
			this.attachments = attachments;
			this.stickers = stickers;
			this.tapbacks = tapbacks;
			this.sendEffect = sendEffect;
			this.stateCode = stateCode;
			this.errorCode = errorCode;
			this.dateRead = dateRead;
		}
		
		private void readObject(ObjectInputStream stream) throws ClassNotFoundException, ClassCastException, IOException, NullPointerException {
			//Reading the data
			guid = stream.readUTF();
			chatGuid = stream.readUTF();
			date = stream.readLong();
			
			text = (String) stream.readObject();
			sender = (String) stream.readObject();
			attachments = (ArrayList<AttachmentInfo>) stream.readObject();
			stickers = (ArrayList<StickerModifierInfo>) stream.readObject();
			tapbacks = (ArrayList<TapbackModifierInfo>) stream.readObject();
			sendEffect = (String) stream.readObject();
			stateCode = stream.readInt();
			errorCode = stream.readInt();
			dateRead = stream.readLong();
			
			//Throwing an exception if any of the lists are invalid
			if(attachments == null) throw new NullPointerException("Attachment list is null");
			if(stickers == null) throw new NullPointerException("Sticker list is null");
			if(tapbacks == null) throw new NullPointerException("Tapback list is null");
		}
		
		@Override
		public Blocks.MessageInfo toBlock() {
			List<Blocks.AttachmentInfo> blockAttachments = new ArrayList<>(attachments.size());
			for(AttachmentInfo item : attachments) blockAttachments.add(item.toBlock());
			List<Blocks.StickerModifierInfo> blockStickers = new ArrayList<>(stickers.size());
			for(StickerModifierInfo item : stickers) blockStickers.add(item.toBlock());
			List<Blocks.TapbackModifierInfo> blockTapbacks = new ArrayList<>(tapbacks.size());
			for(TapbackModifierInfo item : tapbacks) blockTapbacks.add(item.toBlock());
			
			return new Blocks.MessageInfo(-1, guid, chatGuid, date, text, sender, blockAttachments, blockStickers, blockTapbacks, sendEffect, stateCode, errorCode, dateRead);
		}
	}
	
	public static class AttachmentInfo implements Serializable, BlockAccess {
		private static final long serialVersionUID = 103;
		public String guid;
		public String name;
		public String type;
		public byte[] checksum;
		
		
		public AttachmentInfo(String guid, String name, String type, byte[] checksum) {
			//Setting the values
			this.guid = guid;
			this.name = name;
			this.type = type;
			this.checksum = checksum;
		}
		
		private void readObject(ObjectInputStream stream) throws ClassNotFoundException, ClassCastException, IOException {
			guid = stream.readUTF();
			name = (String) stream.readObject();
			type = (String) stream.readObject();
			checksum = (byte[]) stream.readObject();
		}
		
		@Override
		public Blocks.AttachmentInfo toBlock() {
			return new Blocks.AttachmentInfo(guid, name, type, -1, checksum);
		}
	}
	
	public static class GroupActionInfo extends ConversationItem {
		private static final long serialVersionUID = 104;
		
		public String agent;
		public String other;
		public int groupActionType;
		
		public GroupActionInfo(String guid, String chatGuid, long date, String agent, String other, int groupActionType) {
			//Calling the super constructor
			super(guid, chatGuid, date);
			
			//Setting the values
			this.agent = agent;
			this.other = other;
			this.groupActionType = groupActionType;
		}
		
		private void readObject(ObjectInputStream stream) throws ClassNotFoundException, ClassCastException, IOException {
			//Reading the data
			guid = stream.readUTF();
			chatGuid = stream.readUTF();
			date = stream.readLong();
			
			agent = (String) stream.readObject();
			other = (String) stream.readObject();
			groupActionType = stream.readInt();
		}
		
		@Override
		public Blocks.GroupActionInfo toBlock() {
			return new Blocks.GroupActionInfo(-1, guid, chatGuid, date, agent, other, groupActionType);
		}
	}
	
	public static class ChatRenameActionInfo extends ConversationItem {
		private static final long serialVersionUID = 105;
		
		public String agent;
		public String newChatName;
		
		public ChatRenameActionInfo(String guid, String chatGuid, long date, String agent, String newChatName) {
			//Calling the super constructor
			super(guid, chatGuid, date);
			
			//Setting the values
			this.agent = agent;
			this.newChatName = newChatName;
		}
		
		private void readObject(ObjectInputStream stream) throws ClassNotFoundException, ClassCastException, IOException {
			//Reading the data
			guid = stream.readUTF();
			chatGuid = stream.readUTF();
			date = stream.readLong();
			
			agent = (String) stream.readObject();
			newChatName = (String) stream.readObject();
		}
		
		@Override
		public Blocks.ChatRenameActionInfo toBlock() {
			return new Blocks.ChatRenameActionInfo(-1, guid, chatGuid, date, agent, newChatName);
		}
	}
	
	public static abstract class ModifierInfo implements Serializable, BlockAccess {
		private static final long serialVersionUID = 106;
		
		public String message;
		
		public ModifierInfo(String message) {
			this.message = message;
		}
		
		@Override
		public abstract Blocks.ModifierInfo toBlock();
	}
	
	public static class ActivityStatusModifierInfo extends ModifierInfo {
		private static final long serialVersionUID = 107;
		
		public int state;
		public long dateRead;
		
		public ActivityStatusModifierInfo(String guid, int state, long dateRead) {
			//Calling the super constructor
			super(guid);
			
			//Setting the values
			this.state = state;
			this.dateRead = dateRead;
		}
		
		private void readObject(ObjectInputStream stream) throws ClassNotFoundException, ClassCastException, IOException {
			//Reading the data
			message = stream.readUTF();
			state = stream.readInt();
			dateRead = stream.readLong();
		}
		
		@Override
		public Blocks.ActivityStatusModifierInfo toBlock() {
			return new Blocks.ActivityStatusModifierInfo(message, state, dateRead);
		}
	}
	
	public static class StickerModifierInfo extends ModifierInfo {
		private static final long serialVersionUID = 108;
		
		public int messageIndex;
		public String fileGuid;
		public String sender;
		public long date;
		public byte[] image;
		
		public StickerModifierInfo(String message, int messageIndex, String fileGuid, String sender, long date, byte[] image) {
			//Calling the super constructor
			super(message);
			
			//Setting the values
			this.messageIndex = messageIndex;
			this.fileGuid = fileGuid;
			this.sender = sender;
			this.date = date;
			this.image = image;
		}
		
		private void readObject(ObjectInputStream stream) throws ClassNotFoundException, ClassCastException, IOException {
			//Reading the data
			message = stream.readUTF();
			
			messageIndex = stream.readInt();
			fileGuid = stream.readUTF();
			sender = (String) stream.readObject();
			date = stream.readLong();
			image = (byte[]) stream.readObject();
		}
		
		@Override
		public Blocks.StickerModifierInfo toBlock() {
			return new Blocks.StickerModifierInfo(message, messageIndex, fileGuid, sender, date, image);
		}
	}
	
	public static class TapbackModifierInfo extends ModifierInfo {
		private static final long serialVersionUID = 109;
		
		//Creating the reference values
		public static final int tapbackBaseAdd = 2000;
		public static final int tapbackBaseRemove = 3000;
		public static final int tapbackLove = 0;
		public static final int tapbackLike = 1;
		public static final int tapbackDislike = 2;
		public static final int tapbackLaugh = 3;
		public static final int tapbackEmphasis = 4;
		public static final int tapbackQuestion = 5;
		
		public int messageIndex;
		public String sender;
		public int code;
		
		public TapbackModifierInfo(String message, int messageIndex, String sender, int code) {
			//Calling the super constructor
			super(message);
			
			//Setting the values
			this.messageIndex = messageIndex;
			this.sender = sender;
			this.code = code;
		}
		
		private void readObject(ObjectInputStream stream) throws ClassNotFoundException, ClassCastException, IOException {
			//Reading the data
			message = stream.readUTF();
			
			messageIndex = stream.readInt();
			sender = (String) stream.readObject();
			code = stream.readInt();
		}
		
		@Override
		public Blocks.TapbackModifierInfo toBlock() {
			return new Blocks.TapbackModifierInfo(message, messageIndex, sender, code);
		}
	}
	
	/* private static void serializeString(ObjectOutputStream stream, String string) throws IOException {
		stream.writeBoolean(string != null);
		if(string != null) stream.writeUTF(string);
	}
	
	private static String deserializeString(ObjectInputStream stream) throws IOException {
		if(!stream.readBoolean()) return null;
		return stream.readUTF();
	} */
	
	public static byte[] compressLegacyV2(byte[] data, int length) throws IOException {
		Deflater deflater = new Deflater();
		deflater.setInput(data, 0, length);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(length);
		deflater.finish();
		byte[] buffer = new byte[1024];
		while (!deflater.finished()) {
			int count = deflater.deflate(buffer); // returns the generated code... index
			outputStream.write(buffer, 0, count);
		}
		outputStream.close();
		return outputStream.toByteArray();
	}
	
	public static byte[] compress(byte[] data, int length) {
		Deflater compressor = new Deflater();
		compressor.setInput(data, 0, length);
		compressor.finish();
		byte[] compressedData = new byte[length];
		int compressedLen = compressor.deflate(compressedData);
		compressor.end();
		return Arrays.copyOf(compressedData, compressedLen);
	}
	
	public static byte[] decompressLegacyV2(byte[] data) throws IOException, DataFormatException {
		Inflater inflater = new Inflater();
		inflater.setInput(data);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
		byte[] buffer = new byte[1024];
		while (!inflater.finished()) {
			int count = inflater.inflate(buffer);
			outputStream.write(buffer, 0, count);
		}
		outputStream.close();
		return outputStream.toByteArray();
	}
}