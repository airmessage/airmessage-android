package me.tagavari.airmessage.connection.comm5;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.iid.FirebaseInstanceId;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePackException;
import org.msgpack.core.MessageUnpacker;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.common.Blocks;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.connection.MassRetrievalParams;
import me.tagavari.airmessage.connection.request.ChatCreationResponseManager;
import me.tagavari.airmessage.connection.request.ConversationInfoRequest;
import me.tagavari.airmessage.connection.request.FileDownloadRequest;
import me.tagavari.airmessage.connection.request.MessageResponseManager;
import me.tagavari.airmessage.util.Constants;

//https://trello.com/c/lRZ6cikc
class ClientProtocol1 extends ProtocolManager {
	private static final ConnectionManager.Packager protocolPackager = new ConnectionManager.PackagerGZIP();
	private static final String hashAlgorithm = "MD5";
	private static final String platformID = "android";
	
	//Top-level net header type values
	private static final int nhtClose = -1;
	private static final int nhtPing = -2;
	private static final int nhtPong = -3;
	
	private static final int nhtAuthentication = 1;
	private static final int nhtMessageUpdate = 2;
	private static final int nhtTimeRetrieval = 3;
	private static final int nhtMassRetrieval = 4;
	private static final int nhtMassRetrievalFile = 5;
	private static final int nhtMassRetrievalFinish = 6;
	private static final int nhtConversationUpdate = 7;
	private static final int nhtModifierUpdate = 8;
	private static final int nhtAttachmentReq = 9;
	private static final int nhtAttachmentReqConfirm = 10;
	private static final int nhtAttachmentReqFail = 11;
	private static final int nhtCreateChat = 12;
	
	private static final int nhtSendResult = 100;
	private static final int nhtSendTextExisting = 101;
	private static final int nhtSendTextNew = 102;
	private static final int nhtSendFileExisting = 103;
	private static final int nhtSendFileNew = 104;
	
	//Net subtype values
	private static final int nstAuthenticationOK = 0;
	private static final int nstAuthenticationUnauthorized = 1;
	private static final int nstAuthenticationBadRequest = 2;
	
	//Net state values
	private static final int nsMessageIdle = 0;
	private static final int nsMessageSent = 1;
	private static final int nsMessageDelivered = 2;
	private static final int nsMessageRead = 3;
	
	private static final int nsAppleErrorOK = 0;
	private static final int nsAppleErrorUnknown = 1; //Unknown error code
	private static final int nsAppleErrorNetwork = 2; //Network error
	private static final int nsAppleErrorUnregistered = 3; //Not registered with iMessage
	
	private static final int nsGroupActionSubtypeUnknown = 0;
	private static final int nsGroupActionSubtypeJoin = 1;
	private static final int nsGroupActionSubtypeLeave = 2;
	
	//Return codes
	private static final int nstSendResultOK = 0; //Message sent successfully
	private static final int nstSendResultScriptError = 1; //Some unknown AppleScript error
	private static final int nstSendResultBadRequest = 2; //Invalid data received
	private static final int nstSendResultUnauthorized = 3; //System rejected request to send message
	private static final int nstSendResultNoConversation = 4; //A valid conversation wasn't found
	private static final int nstSendResultRequestTimeout = 5; //File data blocks stopped being received
	
	private static final int nstAttachmentReqNotFound = 1; //File GUID not found
	private static final int nstAttachmentReqNotSaved = 2; //File (on disk) not found
	private static final int nstAttachmentReqUnreadable = 3; //No access to file
	private static final int nstAttachmentReqIO = 4; //IO error
	
	private static final int nstCreateChatOK = 0;
	private static final int nstCreateChatScriptError = 1; //Some unknown AppleScript error
	private static final int nstCreateChatBadRequest = 2; //Invalid data received
	private static final int nstCreateChatUnauthorized = 3; //System rejected request to send message
	
	//Item types
	private static final int conversationItemTypeMessage = 0;
	private static final int conversationItemTypeGroupAction = 1;
	private static final int conversationItemTypeChatRename = 2;
	
	private static final int modifierTypeActivity = 0;
	private static final int modifierTypeSticker = 1;
	private static final int modifierTypeTapback = 2;
	
	private static final int transmissionCheckLength = 32;
	
	ClientProtocol1(Context context, ConnectionManager connectionManager, ClientComm5 communicationsManager) {
		super(context, connectionManager, communicationsManager);
	}
	
	private boolean queueHeaderOnlyPacket(int header, boolean encrypt) {
		try(MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
			packer.packInt(header);
			
			communicationsManager.queuePacket(packer.toByteArray(), encrypt);
			
			return true;
		} catch(IOException exception) {
			exception.printStackTrace();
			
			return false;
		}
	}
	
	@Override
	boolean sendConnectionClose(Runnable sentRunnable) {
		try(MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
			packer.packInt(nhtClose);
			
			communicationsManager.queuePacket(packer.toByteArray(), false, sentRunnable);
			
			return true;
		} catch(IOException exception) {
			exception.printStackTrace();
			
			return false;
		}
	}
	
	@Override
	boolean sendPing() {
		return queueHeaderOnlyPacket(nhtPing, false);
	}
	
	@Override
	void processData(byte[] data, boolean wasEncrypted) {
		//Wrapping the data in a MessagePack unpacker
		MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data);
		try {
			//Reading the message type
			int messageType = unpacker.unpackInt();
			
			//Checking if this data is insecure
			//If there is no encryption manager, encryption is assumed to be handled by the network layer, and is to be ignored
			if(!wasEncrypted && communicationsManager.getEncryptionManager() != null) {
				processDataInsecure(messageType, unpacker);
			} else {
				//This data is always encrypted
				processDataSecure(messageType, unpacker);
			}
		} catch(MessagePackException | IOException exception) {
			exception.printStackTrace();
		}
	}
	
	/**
	 * Process data that doesn't need to be encrypted
	 * @param messageType The message header
	 * @param unpacker The message data
	 * @return TRUE if this message was consumed
	 */
	private boolean processDataInsecure(int messageType, MessageUnpacker unpacker) throws IOException {
		switch(messageType) {
			case nhtClose:
				//Closing the connection
				communicationsManager.disconnect(ConnectionManager.intentResultCodeConnection);
				break;
			case nhtPing:
				//Replying with a pong
				queueHeaderOnlyPacket(nhtPong, false);
				break;
			case nhtAuthentication:
				handleMessageAuthentication(unpacker);
				break;
			default:
				//Message not consumed
				return false;
		}
		
		return true;
	}
	
	/**
	 * Process data that does need to be encrypted
	 * This method also invokes {@link #processDataInsecure(int, MessageUnpacker)} method
	 * @param messageType The message header
	 * @param unpacker The message data
	 * @return TRUE if this message was consumed
	 */
	private boolean processDataSecure(int messageType, MessageUnpacker unpacker) throws IOException {
		if(processDataInsecure(messageType, unpacker)) return true;
		
		switch(messageType) {
			case nhtMessageUpdate:
			case nhtTimeRetrieval:
				handleMessageMessageUpdate(unpacker);
				break;
			case nhtMassRetrieval:
				handleMessageMassRetrieval(unpacker);
				break;
			case nhtMassRetrievalFinish:
				handleMessageMassRetrievalFinish(unpacker);
				break;
			case nhtMassRetrievalFile:
				handleMessageMassRetrievalFile(unpacker);
				break;
			case nhtConversationUpdate:
				handleMessageConversationUpdate(unpacker);
				break;
			case nhtModifierUpdate:
				handleMessageModifierUpdate(unpacker);
				break;
			case nhtAttachmentReq:
				handleMessageAttachmentRequest(unpacker);
				break;
			case nhtAttachmentReqConfirm:
				handleMessageAttachmentRequestConfirm(unpacker);
				break;
			case nhtAttachmentReqFail:
				handleMessageAttachmentRequestFail(unpacker);
				break;
			case nhtSendResult:
				handleMessageSendResult(unpacker);
				break;
			case nhtCreateChat:
				handleMessageCreateChat(unpacker);
				break;
			default:
				//Message not consumed
				return false;
		}
		
		return true;
	}
	
	private void handleMessageAuthentication(MessageUnpacker unpacker) throws IOException {
		//Stopping the authentication timer
		communicationsManager.handler.removeCallbacks(communicationsManager.handshakeExpiryRunnable);
		
		//Reading the result code
		int resultCode = unpacker.unpackInt();
		
		//Translating the result code to a local value
		switch(resultCode) {
			case nstAuthenticationOK:
				resultCode = ConnectionManager.intentResultCodeSuccess;
				break;
			case nstAuthenticationUnauthorized:
				resultCode = ConnectionManager.intentResultCodeUnauthorized;
				break;
			case nstAuthenticationBadRequest:
				resultCode = ConnectionManager.intentResultCodeBadRequest;
				break;
		}
		
		//Checking if the result was successful
		if(resultCode == ConnectionManager.intentResultCodeSuccess) {
			//Reading the server's information
			String installationID = unpacker.unpackString();
			String deviceName = unpacker.unpackString();
			String systemVersion = unpacker.unpackString();
			String softwareVersion = unpacker.unpackString();
			
			//Finishing the connection establishment
			communicationsManager.onHandshakeCompleted(installationID, deviceName, systemVersion, softwareVersion);
		} else {
			//Otherwise terminating the connection
			communicationsManager.disconnect(resultCode);
		}
	}
	
	private void handleMessageMessageUpdate(MessageUnpacker unpacker) throws IOException {
		//Reading the items
		List<Blocks.ConversationItem> conversationItems = unpackConversationItems(unpacker);
		
		//Processing the messages
		connectionManager.processMessageUpdate(conversationItems, true);
	}
	
	private void handleMessageMassRetrieval(MessageUnpacker unpacker) throws IOException {
		//Reading the packet information
		short requestID = unpacker.unpackShort();
		int packetIndex = unpacker.unpackInt();
		
		//Checking if this is the first packet
		if(packetIndex == 0) {
			//Reading the conversation list and message count
			List<Blocks.ConversationInfo> conversations = unpackConversations(unpacker);
			int messageCount = unpacker.unpackInt();
			
			//Registering the mass retrieval manager
			if(connectionManager.getMassRetrievalThread() != null) connectionManager.getMassRetrievalThread().registerInfo(MainApplication.getInstance(), requestID, conversations, messageCount, getPackager());
		} else {
			//Reading the item list
			List<Blocks.ConversationItem> conversationItems = unpackConversationItems(unpacker);
			
			//Processing the packet
			if(connectionManager.getMassRetrievalThread() != null) connectionManager.getMassRetrievalThread().addPacket(MainApplication.getInstance(), requestID, packetIndex, conversationItems);
		}
	}
	
	private void handleMessageMassRetrievalFinish(MessageUnpacker unpacker) throws IOException {
		//Finishing the mass retrieval
		connectionManager.finishMassRetrieval();
	}
	
	private void handleMessageMassRetrievalFile(MessageUnpacker unpacker) throws IOException {
		//Reading the data
		short requestID = unpacker.unpackShort();
		int requestIndex = unpacker.unpackInt();
		String fileName = requestIndex == 0 ? unpacker.unpackString() : null;
		boolean isLast = unpacker.unpackBoolean();
		
		String fileGUID = unpacker.unpackString();
		byte[] fileData = unpacker.readPayload(unpacker.unpackBinaryHeader());
		
		//Processing the data
		if(connectionManager.getMassRetrievalThread() != null) {
			if(requestIndex == 0) connectionManager.getMassRetrievalThread().startFileData(requestID, fileGUID, fileName);
			connectionManager.getMassRetrievalThread().appendFileData(requestID, requestIndex, fileGUID, fileData, isLast);
		}
	}
	
	private void handleMessageConversationUpdate(MessageUnpacker unpacker) throws IOException {
		//Reading the data
		List<Blocks.ConversationInfo> conversations = unpackConversations(unpacker);
		
		//Processing the conversations
		connectionManager.processChatInfoResponse(conversations);
	}
	
	private void handleMessageModifierUpdate(MessageUnpacker unpacker) throws IOException {
		//Reading the data
		List<Blocks.ModifierInfo> modifiers = unpackModifiers(unpacker);
		
		//Processing the conversations
		connectionManager.processModifierUpdate(modifiers, getPackager());
	}
	
	private void handleMessageAttachmentRequest(MessageUnpacker unpacker) throws IOException {
		//Reading the data
		short requestID = unpacker.unpackShort();
		int requestIndex = unpacker.unpackInt();
		long fileLength = requestIndex == 0 ? unpacker.unpackLong() : -1;
		boolean isLast = unpacker.unpackBoolean();
		
		String fileGUID = unpacker.unpackString();
		byte[] fileData = unpacker.readPayload(unpacker.unpackBinaryHeader());
		
		//Running on the UI thread
		new Handler(Looper.getMainLooper()).post(() -> {
			//Searching for a matching request
			for(FileDownloadRequest request : connectionManager.getFileDownloadRequests()) {
				if(request.getRequestID() != requestID || !request.getAttachmentGUID().equals(fileGUID)) continue;
				if(requestIndex == 0) request.setFileSize(fileLength);
				request.processFileFragment(context, fileData, requestIndex, isLast, getPackager());
				if(isLast) connectionManager.removeDownloadRequest(request);
				break;
			}
		});
	}
	
	private void handleMessageAttachmentRequestConfirm(MessageUnpacker unpacker) throws IOException {
		//Reading the request ID
		short requestID = unpacker.unpackShort();
		
		//Running on the UI thread
		new Handler(Looper.getMainLooper()).post(() -> {
			//Searching for a matching request
			for(FileDownloadRequest request : connectionManager.getFileDownloadRequests()) {
				if(request.getRequestID() != requestID) continue;
				request.stopTimer(true);
				request.onResponseReceived();
				break;
			}
		});
	}
	
	private void handleMessageAttachmentRequestFail(MessageUnpacker unpacker) throws IOException {
		//Reading the data
		short requestID = unpacker.unpackShort();
		int errorCode = unpacker.unpackInt();
		
		//Running on the UI thread
		new Handler(Looper.getMainLooper()).post(() -> {
			//Failing the download request
			connectionManager.failDownloadRequest(requestID, nstAttachmentReqCodeToLocalCode(errorCode));
		});
	}
	
	private void handleMessageSendResult(MessageUnpacker unpacker) throws IOException {
		//Reading the data
		short requestID = unpacker.unpackShort();
		int resultCode = unpacker.unpackInt();
		String details = unpackNilString(unpacker);
		
		//Getting the message response manager
		final MessageResponseManager messageResponseManager = connectionManager.getMessageSendRequestsList().get(requestID);
		if(messageResponseManager != null) {
			//Removing the request
			connectionManager.getMessageSendRequestsList().remove(requestID);
			messageResponseManager.stopTimer(false);
			
			//Mapping the result code
			int localResultCode = nstSendCodeToErrorCode(resultCode);
			
			//Running on the UI thread
			new Handler(Looper.getMainLooper()).post(() -> {
				//Telling the listener
				if(localResultCode == Constants.messageErrorCodeOK) messageResponseManager.onSuccess();
				else messageResponseManager.onFail(localResultCode, details);
			});
		}
	}
	
	private void handleMessageCreateChat(MessageUnpacker unpacker) throws IOException {
		//Reading the data
		short requestID = unpacker.unpackShort();
		int resultCode = unpacker.unpackInt();
		String details = unpackNilString(unpacker);
		
		//Running on the UI thread
		new Handler(Looper.getMainLooper()).post(() -> {
			//Getting the message response manager
			final ChatCreationResponseManager responseManager = connectionManager.getChatCreationRequests().get(requestID);
			if(responseManager == null) return;
			
			//Removing the request
			connectionManager.getChatCreationRequests().remove(requestID);
			responseManager.stopTimer();
			
			//Telling the listener
			if(resultCode == nstCreateChatOK) responseManager.onSuccess(details);
			else responseManager.onFail();
		});
	}
	
	@Override
	boolean requestChatCreation(short requestID, String[] chatMembers, String service) {
		//Returning false if there is no connection thread
		if(!communicationsManager.isConnectionOpened()) return false;
		
		//Sending the message
		try(MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
			packer.packInt(nhtCreateChat);
			
			packer.packShort(requestID);
			packer.packArrayHeader(chatMembers.length);
			for(String item : chatMembers) packer.packString(item);
			packer.packString(service);
			
			communicationsManager.queuePacket(packer.toByteArray(), true);
			return true;
		} catch(IOException exception) {
			exception.printStackTrace();
			Crashlytics.logException(exception);
			return false;
		}
	}
	
	@Override
	boolean requestRetrievalAll(short requestID, MassRetrievalParams params) {
		//Returning false if there is no connection thread
		if(!communicationsManager.isConnectionOpened()) return false;
		
		//Sending the message
		try(MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
			packer.packInt(nhtMassRetrieval);
			
			packer.packShort(requestID); //Request ID
			
			packer.packBoolean(params.restrictMessages); //Whether or not to time-restrict messages
			if(params.restrictMessages) packer.packLong(params.timeSinceMessages); //Messages time since
			packer.packBoolean(params.downloadAttachments); //Whether or not to download attachments
			if(params.downloadAttachments) {
				packer.packBoolean(params.restrictAttachments); //Whether or not to time-restrict attachments
				if(params.restrictAttachments) packer.packLong(params.timeSinceAttachments); //Attachments time since
				packer.packBoolean(params.restrictAttachmentSizes); //Whether or not to size-restrict attachments
				if(params.restrictAttachmentSizes) packer.packLong(params.attachmentSizeLimit); //Attachment size limit
				
				packer.packArrayHeader(params.attachmentFilterWhitelist.size()); //Attachment type whitelist
				for(String filter : params.attachmentFilterWhitelist) packer.packString(filter);
				packer.packArrayHeader(params.attachmentFilterBlacklist.size()); //Attachment type blacklist
				for(String filter : params.attachmentFilterBlacklist) packer.packString(filter);
				packer.packBoolean(params.attachmentFilterDLOutside); //Whether or not to download "other" items
			}
			
			communicationsManager.queuePacket(packer.toByteArray(), true);
			return true;
		} catch(IOException exception) {
			exception.printStackTrace();
			Crashlytics.logException(exception);
			return false;
		}
	}
	
	private int nstSendCodeToErrorCode(int code) {
		switch(code) {
			case nstSendResultOK:
				return Constants.messageErrorCodeOK;
			case nstSendResultScriptError:
				return Constants.messageErrorCodeServerExternal;
			case nstSendResultBadRequest:
				return Constants.messageErrorCodeServerBadRequest;
			case nstSendResultUnauthorized:
				return Constants.messageErrorCodeServerUnauthorized;
			case nstSendResultNoConversation:
				return Constants.messageErrorCodeServerNoConversation;
			case nstSendResultRequestTimeout:
				return Constants.messageErrorCodeServerRequestTimeout;
			default:
				return Constants.messageErrorCodeServerUnknown;
		}
	}
	
	private int nstAttachmentReqCodeToLocalCode(int code) {
		switch(code) {
			case nstAttachmentReqNotFound:
				return FileDownloadRequest.Callbacks.errorCodeServerNotFound;
			case nstAttachmentReqNotSaved:
				return FileDownloadRequest.Callbacks.errorCodeServerNotSaved;
			case nstAttachmentReqUnreadable:
				return FileDownloadRequest.Callbacks.errorCodeServerUnreadable;
			case nstAttachmentReqIO:
				return FileDownloadRequest.Callbacks.errorCodeServerIO;
			default:
				return FileDownloadRequest.Callbacks.errorCodeUnknown;
		}
	}
	
	private int convertCodeMessageState(int code) {
		switch(code) {
			default:
			case nsMessageIdle:
				return Constants.messageStateCodeIdle;
			case nsMessageSent:
				return Constants.messageStateCodeSent;
			case nsMessageDelivered:
				return Constants.messageStateCodeDelivered;
			case nsMessageRead:
				return Constants.messageStateCodeRead;
		}
	}
	
	private int convertCodeAppleError(int code) {
		switch(code) {
			case nsAppleErrorOK:
				return Constants.messageErrorCodeOK;
			case nsAppleErrorUnknown:
			default:
				return Constants.messageErrorCodeAppleUnknown;
			case nsAppleErrorNetwork:
				return Constants.messageErrorCodeAppleNetwork;
			case nsAppleErrorUnregistered:
				return Constants.messageErrorCodeAppleUnregistered;
		}
	}
	
	private int convertCodeGroupActionSubtype(int code) {
		switch(code) {
			case nsGroupActionSubtypeUnknown:
			default:
				return Constants.groupActionUnknown;
			case nsGroupActionSubtypeJoin:
				return Constants.groupActionJoin;
			case nsGroupActionSubtypeLeave:
				return Constants.groupActionLeave;
		}
	}
	
	private List<Blocks.ConversationInfo> unpackConversations(MessageUnpacker unpacker) throws IOException {
		//Reading the count
		int count = unpacker.unpackArrayHeader();
		
		//Creating the list
		List<Blocks.ConversationInfo> list = new ArrayList<>(count);
		
		//Iterating over the items
		for(int i = 0; i < count; i++) {
			String guid = unpacker.unpackString();
			boolean available = unpacker.unpackBoolean();
			if(available) {
				String service = unpacker.unpackString();
				String name = unpackNilString(unpacker);
				String[] members = new String[unpacker.unpackArrayHeader()];
				for(int m = 0; m < members.length; m++) members[m] = unpacker.unpackString();
				list.add(new Blocks.ConversationInfo(guid, service, name, members));
			} else {
				list.add(new Blocks.ConversationInfo(guid));
			}
		}
		
		//Returning the list
		return list;
	}
	
	private List<Blocks.ConversationItem> unpackConversationItems(MessageUnpacker unpacker) throws IOException, RuntimeException {
		//Reading the count
		int count = unpacker.unpackArrayHeader();
		
		//Creating the list
		List<Blocks.ConversationItem> list = new ArrayList<>(count);
		
		//Iterating over the items
		for(int i = 0; i < count; i++) {
			int type = unpacker.unpackInt();
			
			long serverID = unpacker.unpackLong();
			String guid = unpacker.unpackString();
			String chatGuid = unpacker.unpackString();
			long date = unpacker.unpackLong();
			
			switch(type) {
				default:
					throw new IOException("Invalid conversation item type: " + type);
				case conversationItemTypeMessage: {
					String text = unpackNilString(unpacker);
					String subject = unpackNilString(unpacker);
					String sender = unpackNilString(unpacker);
					List<Blocks.AttachmentInfo> attachments = unpackAttachments(unpacker);
					List<Blocks.StickerModifierInfo> stickers = (List<Blocks.StickerModifierInfo>) (List<?>) unpackModifiers(unpacker);
					List<Blocks.TapbackModifierInfo> tapbacks = (List<Blocks.TapbackModifierInfo>) (List<?>) unpackModifiers(unpacker);
					String sendEffect = unpackNilString(unpacker);
					int stateCode = convertCodeMessageState(unpacker.unpackInt());
					int errorCode = convertCodeAppleError(unpacker.unpackInt());
					long dateRead = unpacker.unpackLong();
					
					list.add(new Blocks.MessageInfo(serverID, guid, chatGuid, date, text, subject, sender, attachments, stickers, tapbacks, sendEffect, stateCode, errorCode, dateRead));
					break;
				}
				case conversationItemTypeGroupAction: {
					String agent = unpackNilString(unpacker);
					String other = unpackNilString(unpacker);
					int groupActionType = convertCodeGroupActionSubtype(unpacker.unpackInt());
					
					list.add(new Blocks.GroupActionInfo(serverID, guid, chatGuid, date, agent, other, groupActionType));
					break;
				}
				case conversationItemTypeChatRename: {
					String agent = unpackNilString(unpacker);
					String newChatName = unpackNilString(unpacker);
					
					list.add(new Blocks.ChatRenameActionInfo(serverID, guid, chatGuid, date, agent, newChatName));
					break;
				}
			}
		}
		
		//Returning the list
		return list;
	}
	
	private List<Blocks.AttachmentInfo> unpackAttachments(MessageUnpacker unpacker) throws IOException, RuntimeException {
		//Reading the count
		int count = unpacker.unpackArrayHeader();
		
		//Creating the list
		List<Blocks.AttachmentInfo> list = new ArrayList<>(count);
		
		//Iterating over the items
		for(int i = 0; i < count; i++) {
			String guid = unpacker.unpackString();
			String name = unpacker.unpackString();
			String type = unpackNilString(unpacker);
			long size = unpacker.unpackLong();
			byte[] checksum;
			if(unpacker.tryUnpackNil()) {
				checksum = null;
			} else {
				checksum = unpacker.readPayload(unpacker.unpackBinaryHeader());
			}
			
			list.add(new Blocks.AttachmentInfo(guid, name, type, size, checksum));
		}
		
		//Returning the list
		return list;
	}
	
	private List<Blocks.ModifierInfo> unpackModifiers(MessageUnpacker unpacker) throws IOException, RuntimeException {
		//Reading the count
		int count = unpacker.unpackArrayHeader();
		
		//Creating the list
		List<Blocks.ModifierInfo> list = new ArrayList<>(count);
		
		//Iterating over the items
		for(int i = 0; i < count; i++) {
			int type = unpacker.unpackInt();
			
			String message = unpacker.unpackString();
			
			switch(type) {
				default:
					throw new IOException("Invalid modifier type: " + type);
				case modifierTypeActivity: {
					int state = convertCodeMessageState(unpacker.unpackInt());
					long dateRead = unpacker.unpackLong();
					
					list.add(new Blocks.ActivityStatusModifierInfo(message, state, dateRead));
					break;
				}
				case modifierTypeSticker: {
					int messageIndex = unpacker.unpackInt();
					String fileGuid = unpacker.unpackString();
					String sender = unpackNilString(unpacker);
					long date = unpacker.unpackLong();
					byte[] image = unpacker.readPayload(unpacker.unpackBinaryHeader());
					
					list.add(new Blocks.StickerModifierInfo(message, messageIndex, fileGuid, sender, date, image));
					break;
				}
				case modifierTypeTapback: {
					int messageIndex = unpacker.unpackInt();
					String sender = unpackNilString(unpacker);
					int code = unpacker.unpackInt();
					
					list.add(new Blocks.TapbackModifierInfo(message, messageIndex, sender, code));
					break;
				}
			}
		}
		
		//Returning the list
		return list;
	}
	
	@Override
	boolean sendAuthenticationRequest(MessageUnpacker unpacker) throws IOException {
		//Returning false if there is no connection thread
		if(!communicationsManager.isConnectionOpened()) return false;
		
		//Reading the transmission check
		byte[] transmissionCheck;
		try {
			transmissionCheck = unpacker.readPayload(unpacker.unpackBinaryHeader());
		} catch(BufferUnderflowException exception) {
			exception.printStackTrace();
			return false;
		}
		
		//Getting the device information
		String installationID = MainApplication.getInstance().getInstallationID();
		String clientName = Build.MANUFACTURER + ' ' + Build.MODEL;
		String platformID = ClientProtocol1.platformID;
		
		//Writing back the transmission check and information about this device
		try(MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
			packer.packInt(nhtAuthentication);
			
			byte[] secureData;
			try(MessageBufferPacker securePacker = MessagePack.newDefaultBufferPacker()) {
				securePacker.addPayload(transmissionCheck); //Transmission check
				securePacker.packString(installationID); //Installation ID
				securePacker.packString(clientName); //Client name
				securePacker.packString(platformID); //Platform ID
				
				//Encrypting the data
				EncryptionManager encryptionManager = communicationsManager.getEncryptionManager();
				if(encryptionManager == null) return false;
				secureData = encryptionManager.encrypt(securePacker.toByteArray());
			}
			
			packer.packBinaryHeader(secureData.length);
			packer.addPayload(secureData);
			
			communicationsManager.queuePacket(packer.toByteArray(), false);
			return true;
		} catch(IOException | GeneralSecurityException exception) {
			exception.printStackTrace();
			Crashlytics.logException(exception);
			return false;
		}
	}
	
	@Override
	boolean sendMessage(short requestID, String chatGUID, String message) {
		//Returning false if there is no connection thread
		if(!communicationsManager.isConnectionOpened()) return false;
		
		try(MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
			packer.packInt(nhtSendTextExisting);
			
			packer.packShort(requestID); //Request ID
			
			packer.packString(chatGUID); //Chat GUID
			packer.packString(message); //Message
			
			communicationsManager.queuePacket(packer.toByteArray(), true);
			return true;
		} catch(IOException exception) {
			exception.printStackTrace();
			Crashlytics.logException(exception);
			return false;
		}
	}
	
	@Override
	boolean sendMessage(short requestID, String[] chatMembers, String message, String service) {
		//Returning false if there is no connection thread
		if(!communicationsManager.isConnectionOpened()) return false;
		
		try(MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
			packer.packInt(nhtSendTextNew);
			
			packer.packShort(requestID); //Request ID
			
			packer.packArrayHeader(chatMembers.length); //Members
			for(String item : chatMembers) packer.packString(item);
			packer.packString(service); //Service
			packer.packString(message); //Message
			
			communicationsManager.queuePacket(packer.toByteArray(), true);
			return true;
		} catch(IOException exception) {
			exception.printStackTrace();
			Crashlytics.logException(exception);
			return false;
		}
	}
	
	@Override
	boolean addDownloadRequest(short requestID, String attachmentGUID, Runnable sentRunnable) {
		//Returning false if there is no connection thread
		if(!communicationsManager.isConnectionOpened()) return false;
		
		try(MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
			packer.packInt(nhtAttachmentReq);
			
			packer.packShort(requestID); //Request ID
			packer.packInt(ConnectionManager.attachmentChunkSize); //Chunk size
			packer.packString(attachmentGUID); //File GUID
			
			communicationsManager.queuePacket(packer.toByteArray(), true);
			return true;
		} catch(IOException exception) {
			exception.printStackTrace();
			Crashlytics.logException(exception);
			return false;
		}
	}
	
	@Override
	boolean sendConversationInfoRequest(List<ConversationInfoRequest> list) {
		//Returning false if there is no connection thread
		if(!communicationsManager.isConnectionOpened()) return false;
		
		//Creating the guid list
		ArrayList<String> guidList;
		
		//Locking the pending conversations
		synchronized(list) {
			//Returning false if there are no pending conversations
			if(list.isEmpty()) return false;
			
			//Converting the conversation info list to a string list
			guidList = new ArrayList<>();
			for(ConversationInfoRequest conversationInfoRequest : list) {
				guidList.add(conversationInfoRequest.getConversationInfo().getGuid());
			}
		}
		
		try(MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
			packer.packInt(nhtConversationUpdate);
			
			packer.packArrayHeader(guidList.size());
			for(String item : guidList) packer.packString(item);
			
			communicationsManager.queuePacket(packer.toByteArray(), true);
			return true;
		} catch(IOException exception) {
			exception.printStackTrace();
			Crashlytics.logException(exception);
			return false;
		}
	}
	
	@Override
	boolean uploadFilePacket(short requestID, int requestIndex, String conversationGUID, byte[] data, String fileName, boolean isLast) {
		//Returning false if there is no connection thread
		if(!communicationsManager.isConnectionOpened()) return false;
		
		try(MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
			packer.packInt(nhtSendFileExisting);
			
			packer.packShort(requestID); //Request identifier
			packer.packInt(requestIndex); //Request index
			packer.packBoolean(isLast); //Is last message
			
			packer.packString(conversationGUID); //Chat GUID
			packer.packBinaryHeader(data.length); //File bytes
			packer.addPayload(data);
			if(requestIndex == 0) packer.packString(fileName); //File name
			
			communicationsManager.queuePacket(packer.toByteArray(), true);
			return true;
		} catch(IOException exception) {
			exception.printStackTrace();
			Crashlytics.logException(exception);
			return false;
		}
	}
	
	@Override
	boolean uploadFilePacket(short requestID, int requestIndex, String[] conversationMembers, byte[] data, String fileName, String service, boolean isLast) {
		//Returning false if there is no connection thread
		if(!communicationsManager.isConnectionOpened()) return false;
		
		try(MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
			packer.packInt(nhtSendFileNew);
			
			packer.packShort(requestID); //Request identifier
			packer.packInt(requestIndex); //Request index
			packer.packBoolean(isLast); //Is last message
			
			packer.packArrayHeader(conversationMembers.length); //Chat members
			for(String item : conversationMembers) packer.packString(item);
			packer.packBinaryHeader(data.length); //File bytes
			packer.addPayload(data);
			if(requestIndex == 0) {
				packer.packString(fileName); //File name
				packer.packString(service); //Service
			}
			
			communicationsManager.queuePacket(packer.toByteArray(), true);
			return true;
		} catch(IOException exception) {
			exception.printStackTrace();
			Crashlytics.logException(exception);
			return false;
		}
	}
	
	@Override
	boolean requestRetrievalTime(long timeLower, long timeUpper) {
		//Returning false if there is no connection thread
		if(!communicationsManager.isConnectionOpened()) return false;
		
		try(MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
			packer.packInt(nhtTimeRetrieval);
			
			packer.packLong(timeLower);
			packer.packLong(timeUpper);
			
			communicationsManager.queuePacket(packer.toByteArray(), true);
			return true;
		} catch(IOException exception) {
			exception.printStackTrace();
			Crashlytics.logException(exception);
			return false;
		}
	}
	
	@Override
	ConnectionManager.Packager getPackager() {
		return protocolPackager;
	}
	
	@Override
	String getHashAlgorithm() {
		return hashAlgorithm;
	}
	
	@Override
	boolean checkSupportsFeature(String feature) {
		return false;
	}
	
	private static String unpackNilString(MessageUnpacker unpacker) throws IOException {
		return unpacker.tryUnpackNil() ? null : unpacker.unpackString();
	}
}