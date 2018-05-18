package me.tagavari.airmessage.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class SharedValues {
	/* COMMUNICATIONS VERSION CHANGES
	*  1 - Original release
	*  2 - Serialization changes
	*  3 - Reworked without WS layer
	*/
	public static final int mmCommunicationsVersion = 4;
	
	public static final String headerCommVer = "MMS-Comm-Version";
	public static final String headerSoftVersion = "MMS-Soft-Version";
	public static final String headerSoftVersionCode = "MMS-Soft-Version-Code";
	public static final String headerPassword = "Password";
	
	public static final int resultBadRequest = 4000;
	public static final int resultClientOutdated = 4001;
	public static final int resultServerOutdated = 4002;
	public static final int resultUnauthorized = 4003;
	
	public static final byte wsFrameUpdate = 0;
	public static final byte wsFrameTimeRetrieval = 1;
	public static final byte wsFrameMassRetrieval = 2;
	public static final byte wsFrameChatInfo = 3;
	public static final byte wsFrameModifierUpdate = 4;
	public static final byte wsFrameAttachmentReq = 5;
	public static final byte wsFrameAttachmentReqConfirmed = 6;
	public static final byte wsFrameAttachmentReqFailed = 7;
	
	public static final byte wsFrameSendResult = 100;
	public static final byte wsFrameSendTextExisting = 101;
	public static final byte wsFrameSendTextNew = 102;
	public static final byte wsFrameSendFileExisting = 103;
	public static final byte wsFrameSendFileNew = 104;
	
	//NHT = Net Header Type
	public static final int nhtClose = -1;
	public static final int nhtPing = -2;
	public static final int nhtPong = -3;
	public static final int nhtInformation = -4;
	public static final int nhtAuthentication = 0;
	public static final int nhtMessageUpdate = 1;
	public static final int nhtTimeRetrieval = 2;
	public static final int nhtMassRetrieval = 3;
	public static final int nhtChatInfo = 4;
	public static final int nhtModifierUpdate = 5;
	public static final int nhtAttachmentReq = 6;
	public static final int nhtAttachmentReqConfirm = 7;
	public static final int nhtAttachmentReqFail = 8;
	
	public static final int nhtSendResult = 100;
	public static final int nhtSendTextExisting = 101;
	public static final int nhtSendTextNew = 102;
	public static final int nhtSendFileExisting = 103;
	public static final int nhtSendFileNew = 104;
	
	public static final int nhtAuthenticationOK = 0;
	public static final int nhtAuthenticationUnauthorized = 1;
	public static final int nhtAuthenticationBadRequest = 2;
	public static final int nhtAuthenticationVersionMismatch = 3;
	
	public static final String hashAlgorithm = "MD5";
	public static final String transmissionCheck = "4yAIlVK0Ce_Y7nv6at_hvgsFtaMq!lZYKipV40Fp5E%VSsLSML";
	
	public static class ConversationInfo implements Serializable {
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
	}
	
	public static abstract class ConversationItem implements Serializable, Cloneable {
		private static final long serialVersionUID = 101;
		
		public String guid;
		public String chatGuid;
		public long date;
		
		public ConversationItem(String guid, String chatGuid, long date) {
			this.guid = guid;
			this.chatGuid = chatGuid;
			this.date = date;
		}
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
	}
	
	public static class AttachmentInfo implements Serializable {
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
	}
	
	public static abstract class ModifierInfo implements Serializable {
		private static final long serialVersionUID = 106;
		
		public String message;
		
		public ModifierInfo(String message) {
			this.message = message;
		}
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
	
	public static class EncryptedData implements Serializable {
		//private static final long serialVersionUID = 0;
		public byte[] data;
		private transient String password = null;
		
		public EncryptedData(byte[] data, String password) {
			this.data = data;
			this.password = password;
		}
		
		private void readObject(ObjectInputStream stream) throws ClassNotFoundException, ClassCastException, IOException, GeneralSecurityException {
			//Reading the data
			byte[] salt = new byte[8];
			byte[] iv = new byte[12];
			
			stream.readFully(salt);
			stream.readFully(iv);
			
			int dataLength = stream.readInt();
			byte[] encryptedData = new byte[dataLength];
			stream.readFully(encryptedData);
			
			//Deciphering the data
			SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 10000, 128);
			SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
			SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getEncoded(), "AES");
			
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(iv));
			
			data = cipher.doFinal(data);
		}
		
		private void writeObject(ObjectOutputStream stream) throws IOException, GeneralSecurityException {
			//Creating a secure random
			SecureRandom random = new SecureRandom();
			
			//Generating a salt
			byte[] salt = new byte[8];
			random.nextBytes(salt);
			
			//Creating the key
			SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 10000, 128);
			SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
			SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getEncoded(), "AES");
			
			//Generating the IV
			byte[] iv = new byte[12];
			random.nextBytes(iv);
			IvParameterSpec ivSpec = new IvParameterSpec(iv);
			
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec);
			
			//Writing the data
			stream.write(salt);
			stream.write(iv);
			byte[] encryptedData = cipher.doFinal(data);
			stream.writeInt(encryptedData.length);
			stream.write(encryptedData);
		}
	}
}