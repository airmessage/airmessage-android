package me.tagavari.airmessage.common;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import me.tagavari.airmessage.MainApplication;

public class Blocks {
	public interface Block {
		void writeObject(ObjectOutputStream stream) throws IOException;
	}
	
	public static class ConversationInfo implements Block {
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
		
		@Override
		public void writeObject(ObjectOutputStream stream) throws IOException {
			//Writing the fields
			stream.writeUTF(guid);
			stream.writeBoolean(available);
			if(available) {
				stream.writeUTF(service);
				stream.writeBoolean(name != null);
				if(name != null) stream.writeUTF(name);
				stream.writeInt(members.length);
				for(String member : members) stream.writeUTF(member);
			}
		}
	}
	
	public static abstract class ConversationItem implements Block {
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
		
		@Override
		public void writeObject(ObjectOutputStream stream) throws IOException {
			stream.writeInt(getItemType());
			
			stream.writeLong(serverID);
			stream.writeUTF(guid);
			stream.writeUTF(chatGuid);
			stream.writeLong(date);
		}
		
		abstract int getItemType();
	}
	
	public static class MessageInfo extends ConversationItem {
		private static final int itemType = 0;
		
		public static final int stateCodeGhost = 0;
		public static final int stateCodeIdle = 1;
		public static final int stateCodeSent = 2;
		public static final int stateCodeDelivered = 3;
		public static final int stateCodeRead = 4;
		
		public String text;
		public String sender;
		public List<AttachmentInfo> attachments;
		public List<StickerModifierInfo> stickers;
		public List<TapbackModifierInfo> tapbacks;
		public String sendEffect;
		public int stateCode;
		public int errorCode;
		public long dateRead;
		
		public MessageInfo(long serverID, String guid, String chatGuid, long date, String text, String sender, List<AttachmentInfo> attachments, List<StickerModifierInfo> stickers, List<TapbackModifierInfo> tapbacks, String sendEffect, int stateCode, int errorCode, long dateRead) {
			//Calling the super constructor
			super(serverID, guid, chatGuid, date);
			
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
		
		@Override
		public void writeObject(ObjectOutputStream stream) throws IOException {
			//Writing the fields
			super.writeObject(stream);
			
			stream.writeBoolean(text != null);
			if(text != null) stream.writeUTF(text);
			stream.writeBoolean(sender != null);
			if(sender != null) stream.writeUTF(sender);
			stream.writeInt(attachments.size());
			for(AttachmentInfo item : attachments) item.writeObject(stream);
			stream.writeInt(stickers.size());
			for(StickerModifierInfo item : stickers) item.writeObject(stream);
			stream.writeInt(tapbacks.size());
			for(TapbackModifierInfo item : tapbacks) item.writeObject(stream);
			stream.writeBoolean(sendEffect != null);
			if(sendEffect != null) stream.writeUTF(sendEffect);
			stream.writeInt(stateCode);
			stream.writeInt(errorCode);
			stream.writeLong(dateRead);
		}
		
		@Override
		int getItemType() {
			return itemType;
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
		
		@Override
		public void writeObject(ObjectOutputStream stream) throws IOException {
			//Writing the fields
			super.writeObject(stream);
			
			stream.writeBoolean(agent != null);
			if(agent != null) stream.writeUTF(agent);
			stream.writeBoolean(other != null);
			if(other != null) stream.writeUTF(other);
			stream.writeInt(groupActionType);
		}
		
		@Override
		int getItemType() {
			return itemType;
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
		
		@Override
		public void writeObject(ObjectOutputStream stream) throws IOException {
			//Writing the fields
			super.writeObject(stream);
			
			stream.writeBoolean(agent != null);
			if(agent != null) stream.writeUTF(agent);
			stream.writeBoolean(newChatName != null);
			if(newChatName != null) stream.writeUTF(newChatName);
		}
		
		@Override
		int getItemType() {
			return itemType;
		}
	}
	
	public static class AttachmentInfo implements Block {
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
		
		@Override
		public void writeObject(ObjectOutputStream stream) throws IOException {
			stream.writeUTF(guid);
			stream.writeUTF(name);
			stream.writeBoolean(type != null);
			if(type != null) stream.writeUTF(type);
			stream.writeLong(size);
			stream.writeBoolean(checksum != null);
			if(checksum != null) {
				stream.writeInt(checksum.length);
				stream.write(checksum);
			}
		}
	}
	
	public static abstract class ModifierInfo implements Block {
		public String message;
		
		public ModifierInfo(String message) {
			this.message = message;
		}
		
		@Override
		public void writeObject(ObjectOutputStream stream) throws IOException {
			stream.writeInt(getItemType());
			
			stream.writeUTF(message);
		}
		
		abstract int getItemType();
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
		
		public void writeObject(ObjectOutputStream stream) throws IOException {
			//Writing the fields
			super.writeObject(stream);
			
			stream.writeInt(state);
			stream.writeLong(dateRead);
		}
		
		@Override
		int getItemType() {
			return itemType;
		}
	}
	
	public static class StickerModifierInfo extends ModifierInfo {
		private static final int itemType = 1;
		
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
		
		@Override
		public void writeObject(ObjectOutputStream stream) throws IOException {
			//Writing the fields
			super.writeObject(stream);
			
			stream.writeInt(messageIndex);
			stream.writeUTF(fileGuid);
			stream.writeBoolean(sender != null);
			if(sender != null) stream.writeUTF(sender);
			stream.writeLong(date);
			stream.writeInt(image.length);
			stream.write(image);
		}
		
		@Override
		int getItemType() {
			return itemType;
		}
	}
	
	public static class TapbackModifierInfo extends ModifierInfo {
		private static final int itemType = 2;
		
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
		
		@Override
		public void writeObject(ObjectOutputStream stream) throws IOException {
			//Writing the fields
			super.writeObject(stream);
			
			stream.writeInt(messageIndex);
			stream.writeBoolean(sender != null);
			if(sender != null) stream.writeUTF(sender);
			stream.writeInt(code);
		}
		
		@Override
		int getItemType() {
			return itemType;
		}
	}
	
	public static class EncryptableData implements Block {
		//Creating the reference values
		private static final int saltLen = 8; //8 bytes
		private static final int ivLen = 12; //12 bytes (instead of 16 because of GCM)
		private static final String keyFactoryAlgorithm = "PBKDF2WithHmacSHA256";
		private static final String keyAlgorithm = "AES";
		private static final String cipherTransformation = "AES/GCM/NoPadding";
		private static final int keyIterationCount = 10000;
		private static final int keyLength = 128; //128 bits
		
		//private static final long serialVersionUID = 0;
		public byte[] salt;
		public byte[] iv;
		public byte[] data;
		private transient boolean dataEncrypted;
		
		public EncryptableData(byte[] data) {
			this.data = data;
			this.salt = null;
			this.iv = null;
			dataEncrypted = false;
		}
		
		public EncryptableData(byte[] salt, byte[] iv, byte[] data) {
			this.salt = salt;
			this.iv = iv;
			this.data = data;
			dataEncrypted = true;
		}
		
		public EncryptableData encrypt(String password) throws ClassCastException, GeneralSecurityException {
			//Returning if the data is already encrypted
			if(dataEncrypted) return this;
			
			//Creating a secure random
			SecureRandom random = new SecureRandom();
			
			//Generating a salt
			salt = new byte[saltLen];
			random.nextBytes(salt);
			
			//Creating the key
			SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(keyFactoryAlgorithm, MainApplication.getSecurityProvider());
			KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, keyIterationCount, keyLength);
			SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
			SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getEncoded(), keyAlgorithm);
			
			//Generating the IV
			iv = new byte[ivLen];
			random.nextBytes(iv);
			GCMParameterSpec gcmSpec = new GCMParameterSpec(keyLength, iv);
			
			Cipher cipher = Cipher.getInstance(cipherTransformation, MainApplication.getSecurityProvider());
			cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmSpec);
			
			//Encrypting the data
			data = cipher.doFinal(data);
			dataEncrypted = true;
			
			//Returning the object
			return this;
		}
		
		public EncryptableData decrypt(String password) throws ClassCastException, GeneralSecurityException {
			//Returning if the data is not encrypted
			if(!dataEncrypted) return this;
			
			//Creating the key
			SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(keyFactoryAlgorithm, MainApplication.getSecurityProvider());
			KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, keyIterationCount, keyLength);
			SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
			SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getEncoded(), keyAlgorithm);
			
			//Creating the IV
			GCMParameterSpec gcmSpec = new GCMParameterSpec(keyLength, iv);
			
			//Creating the cipher
			Cipher cipher = Cipher.getInstance(cipherTransformation, MainApplication.getSecurityProvider());
			cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmSpec);
			
			//Deciphering the data
			data = cipher.doFinal(data);
			dataEncrypted = false;
			
			//Invalidating the encryption information
			salt = null;
			iv = null;
			
			//Returning the object
			return this;
		}
		
		@Override
		public void writeObject(ObjectOutputStream stream) throws IOException {
			//Throwing an exception if the data isn't encrypted
			if(!dataEncrypted) throw new RuntimeException("Data serialization attempt before encryption!");
			
			//Writing the data
			stream.write(salt);
			stream.write(iv);
			stream.writeInt(data.length);
			stream.write(data);
		}
		
		/* private void readObject(ObjectInputStream stream) throws IOException {
			//Reading the data
			salt = new byte[saltLen];
			stream.readFully(salt);
			
			iv = new byte[ivLen];
			stream.readFully(iv);
			
			data = new byte[stream.readInt()];
			stream.readFully(data);
		} */
		
		public static EncryptableData readObject(ObjectInputStream stream) throws IOException {
			//Reading the data
			byte[] salt = new byte[saltLen];
			stream.readFully(salt);
			
			byte[] iv = new byte[ivLen];
			stream.readFully(iv);
			
			byte[] data = new byte[stream.readInt()];
			stream.readFully(data);
			
			//Returning the data
			return new EncryptableData(salt, iv, data);
		}
		
		public boolean isEncrypted() {
			return dataEncrypted;
		}
	}
}