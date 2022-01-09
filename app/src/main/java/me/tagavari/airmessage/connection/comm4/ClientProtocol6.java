package me.tagavari.airmessage.connection.comm4;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import io.reactivex.rxjava3.core.Observable;
import me.tagavari.airmessage.common.Blocks;
import me.tagavari.airmessage.connection.DataProxy;
import me.tagavari.airmessage.connection.MassRetrievalParams;
import me.tagavari.airmessage.connection.exception.AMRequestException;
import me.tagavari.airmessage.constants.MIMEConstants;
import me.tagavari.airmessage.enums.*;
import me.tagavari.airmessage.helper.StandardCompressionHelper;
import me.tagavari.airmessage.helper.StringHelper;
import me.tagavari.airmessage.redux.ReduxEventAttachmentUpload;
import me.tagavari.airmessage.util.CompoundErrorDetails;
import me.tagavari.airmessage.util.ConversationTarget;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.URLConnection;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

//Improved error messages, chat creation requests
class ClientProtocol6 extends ProtocolManager<HeaderPacket> {
	private static final String hashAlgorithm = "MD5";
	private static final String stringCharset = "UTF-8";
	
	private static final int attachmentChunkSize = 1024 * 1024; //1 MB
	
	//Top-level net header type values
	private static final int nhtAuthentication = 1;
	private static final int nhtMessageUpdate = 2;
	private static final int nhtTimeRetrieval = 3;
	private static final int nhtMassRetrieval = 4;
	private static final int nhtConversationUpdate = 5;
	private static final int nhtModifierUpdate = 6;
	private static final int nhtAttachmentReq = 7;
	private static final int nhtAttachmentReqConfirm = 8;
	private static final int nhtAttachmentReqFail = 9;
	private static final int nhtMassRetrievalFinish = 10;
	private static final int nhtMassRetrievalFile = 11;
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
	private static final String transmissionCheck = "4yAIlVK0Ce_Y7nv6at_hvgsFtaMq!lZYKipV40Fp5E%VSsLSML";
	
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
	
	private short lastMassRetrievalRequestID = -1;
	
	//Encryption values
	private final SecureRandom random = new SecureRandom();
	
	public ClientProtocol6(ClientComm4 communicationsManager, DataProxy<HeaderPacket> dataProxy) {
		super(communicationsManager, dataProxy);
	}
	
	@Override
	boolean sendPing() {
		return dataProxy.send(new HeaderPacket(new byte[0], ClientComm4.nhtPing));
	}
	
	@Override
	void processData(byte[] data, int messageType) {
		switch(messageType) {
			case ClientComm4.nhtClose:
				communicationsManager.getHandler().post(() -> communicationsManager.disconnect(ConnectionErrorCode.connection));
				break;
			case ClientComm4.nhtPing:
				dataProxy.send(new HeaderPacket(new byte[0], ClientComm4.nhtPong));
				break;
			case nhtAuthentication: {
				//Stopping the authentication timer
				communicationsManager.stopTimeoutTimer();
				
				//Reading the result code
				ByteBuffer dataBuffer = ByteBuffer.wrap(data);
				int resultCode = dataBuffer.getInt();
				
				//Checking if the result as successful
				if(resultCode == nstAuthenticationOK) {
					//Finishing the connection establishment
					communicationsManager.getHandler().post(() -> communicationsManager.onHandshake(null, null, null, null, null, false));
				} else {
					//Translating the result code to a local value
					int localResultCode;
					switch(resultCode) {
						case nstAuthenticationUnauthorized:
							localResultCode = ConnectionErrorCode.unauthorized;
							break;
						case nstAuthenticationBadRequest:
							localResultCode = ConnectionErrorCode.badRequest;
							break;
						default:
							localResultCode = ConnectionErrorCode.connection;
							break;
					}
					
					//Closing the connection
					communicationsManager.getHandler().post(() -> communicationsManager.disconnect(localResultCode));
				}
				
				break;
			}
			case nhtMessageUpdate:
			case nhtTimeRetrieval: {
				//Reading the list
				List<Blocks.ConversationItem> list;
				try(ByteArrayInputStream src = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(src)) {
					//Reading the secure data
					try(ByteArrayInputStream srcSec = new ByteArrayInputStream(readEncrypted(in)); ObjectInputStream inSec = new ObjectInputStream(srcSec)) {
						list = deserializeConversationItems(inSec, inSec.readInt());
					}
				} catch(IOException | RuntimeException | GeneralSecurityException exception) {
					exception.printStackTrace();
					break;
				}
				
				//Processing the messages
				communicationsManager.runListener(listener -> listener.onMessageUpdate(list));
				
				break;
			}
			case nhtMassRetrieval: {
				try(ByteArrayInputStream src = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(src)) {
					//Reading the packet information
					short requestID = in.readShort();
					int packetIndex = in.readInt();
					
					//Reading the secure data
					try(ByteArrayInputStream srcSec = new ByteArrayInputStream(readEncrypted(in)); ObjectInputStream inSec = new ObjectInputStream(srcSec)) {
						//Checking if this is the first packet
						if(packetIndex == 0) {
							//Reading the conversation list
							List<Blocks.ConversationInfo> conversationList = deserializeConversations(inSec, inSec.readInt());
							
							//Reading the message count
							int messageCount = inSec.readInt();
							
							//Registering the mass retrieval manager
							communicationsManager.runListener(listener -> listener.onMassRetrievalStart(requestID, conversationList, messageCount));
							
							//Recording the request ID (to simulate a newer protocol version for future updates)
							lastMassRetrievalRequestID = requestID;
						} else {
							//Reading the item list
							List<Blocks.ConversationItem> listItems = deserializeConversationItems(inSec, inSec.readInt());
							
							//Processing the packet
							communicationsManager.runListener(listener -> listener.onMassRetrievalUpdate(requestID, packetIndex, listItems));
						}
					}
				} catch(IOException | RuntimeException | GeneralSecurityException exception) {
					//Logging the exception
					exception.printStackTrace();
				}
				
				break;
			}
			case nhtMassRetrievalFinish: {
				//Finishing the mass retrieval
				communicationsManager.runListener(listener -> listener.onMassRetrievalComplete(lastMassRetrievalRequestID));
				
				break;
			}
			case nhtMassRetrievalFile: {
				//Reading the data
				final short requestID;
				final int requestIndex;
				final String fileName;
				final boolean isLast;
				
				final String fileGUID;
				final byte[] fileData;
				
				try(ByteArrayInputStream bis = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(bis)) {
					requestID = in.readShort();
					requestIndex = in.readInt();
					if(requestIndex == 0) fileName = in.readUTF();
					else fileName = null;
					isLast = in.readBoolean();
					
					try(ByteArrayInputStream srcSec = new ByteArrayInputStream(readEncrypted(in)); ObjectInputStream inSec = new ObjectInputStream(srcSec)) {
						fileGUID = inSec.readUTF();
						int contentLen = inSec.readInt();
						byte[] compressedBytes = new byte[contentLen];
						inSec.readFully(compressedBytes);
						fileData = StandardCompressionHelper.decompressGZIP(compressedBytes);
					}
				} catch(IOException | RuntimeException | GeneralSecurityException exception) {
					exception.printStackTrace();
					break;
				}
				
				//Processing the data
				communicationsManager.runListener(listener -> {
					if(requestIndex == 0) listener.onMassRetrievalFileStart(requestID, fileGUID, fileName, null, null, null);
					listener.onMassRetrievalFileProgress(requestID, requestIndex, fileGUID, fileData);
					if(isLast) listener.onMassRetrievalFileComplete(requestID, fileGUID);
				});
				
				break;
			}
			case nhtConversationUpdate: {
				//Reading the list
				List<Blocks.ConversationInfo> list;
				try(ByteArrayInputStream src = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(src)) {
					//Reading the secure data
					try(ByteArrayInputStream srcSec = new ByteArrayInputStream(readEncrypted(in)); ObjectInputStream inSec = new ObjectInputStream(srcSec)) {
						list = deserializeConversations(inSec, inSec.readInt());
					}
				} catch(IOException | RuntimeException | GeneralSecurityException exception) {
					exception.printStackTrace();
					break;
				}
				
				//Processing the conversations
				communicationsManager.runListener(listener -> listener.onConversationUpdate(list));
				
				break;
			}
			case nhtModifierUpdate: {
				//Reading the list
				List<Blocks.ModifierInfo> list;
				try(ByteArrayInputStream bis = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(bis)) {
					//Reading the secure data
					try(ByteArrayInputStream srcSec = new ByteArrayInputStream(readEncrypted(in)); ObjectInputStream inSec = new ObjectInputStream(srcSec)) {
						list = deserializeModifiers(inSec, inSec.readInt());
					}
				} catch(IOException | RuntimeException | GeneralSecurityException exception) {
					exception.printStackTrace();
					break;
				}
				
				//Processing the conversations
				communicationsManager.runListener(listener -> listener.onModifierUpdate(list));
				
				break;
			}
			case nhtAttachmentReq: {
				//Reading the data
				final short requestID;
				final int requestIndex;
				final long fileSize;
				final boolean isLast;
				
				final String fileGUID;
				final byte[] decompressedBytes;
				
				try(ByteArrayInputStream bis = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(bis)) {
					requestID = in.readShort();
					requestIndex = in.readInt();
					if(requestIndex == 0) fileSize = in.readLong();
					else fileSize = -1;
					isLast = in.readBoolean();
					
					//Reading the secure data
					final byte[] compressedBytes;
					try(ByteArrayInputStream srcSec = new ByteArrayInputStream(readEncrypted(in)); ObjectInputStream inSec = new ObjectInputStream(srcSec)) {
						fileGUID = inSec.readUTF();
						compressedBytes = new byte[inSec.readInt()];
						inSec.readFully(compressedBytes);
					}
					
					//Decompressing the data
					decompressedBytes = StandardCompressionHelper.decompressGZIP(compressedBytes);
				} catch(IOException | RuntimeException | GeneralSecurityException exception) {
					exception.printStackTrace();
					break;
				}
				
				//Forwarding the data to the listeners
				communicationsManager.runListener(listener -> {
					if(requestIndex == 0) listener.onFileRequestStart(requestID, null, null, fileSize, null);
					listener.onFileRequestData(requestID, requestIndex, decompressedBytes);
					if(isLast) listener.onFileRequestComplete(requestID);
				});
				
				break;
			}
			case nhtAttachmentReqConfirm: {
				//Reading the data
				final short requestID = ByteBuffer.wrap(data).getShort();
				
				break;
			}
			case nhtAttachmentReqFail: {
				//Reading the data
				ByteBuffer byteBuffer = ByteBuffer.wrap(data);
				final short requestID = byteBuffer.getShort();
				final int errorCode = nstAttachmentReqCodeToLocalCode(byteBuffer.getInt());
				
				communicationsManager.runListener(listener -> listener.onFileRequestFail(requestID, errorCode));
				
				break;
			}
			case nhtSendResult: {
				short requestID;
				int resultCode;
				String details;
				
				try(ByteArrayInputStream src = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(src)) {
					requestID = in.readShort();
					
					//Reading the secure data
					try(ByteArrayInputStream srcSec = new ByteArrayInputStream(readEncrypted(in)); ObjectInputStream inSec = new ObjectInputStream(srcSec)) {
						resultCode = inSec.readInt();
						details = inSec.readBoolean() ? inSec.readUTF() : null;
					}
				} catch(IOException | RuntimeException | GeneralSecurityException exception) {
					//Logging the exception
					exception.printStackTrace();
					
					break;
				}
				
				if(resultCode == nstSendResultOK) {
					communicationsManager.runListener(listener -> listener.onSendMessageSuccess(requestID));
				} else {
					communicationsManager.runListener(listener -> listener.onSendMessageFail(requestID, new CompoundErrorDetails.MessageSend(nstSendCodeToErrorCode(resultCode), details)));
				}
				
				break;
			}
			case nhtCreateChat: {
				short requestID;
				int resultCode;
				String details;
				boolean resultOK;
				
				try(ByteArrayInputStream src = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(src)) {
					requestID = in.readShort();
					
					//Reading the secure data
					try(ByteArrayInputStream srcSec = new ByteArrayInputStream(readEncrypted(in)); ObjectInputStream inSec = new ObjectInputStream(srcSec)) {
						resultCode = inSec.readInt();
						details = (resultOK = resultCode == nstCreateChatOK) || inSec.readBoolean() ? inSec.readUTF() : null;
					}
				} catch(IOException | RuntimeException | GeneralSecurityException exception) {
					//Logging the exception
					exception.printStackTrace();
					
					break;
				}
				
				communicationsManager.runListener(listener -> {
					if(resultOK) listener.onCreateChatSuccess(requestID, details);
					else listener.onCreateChatError(requestID, new CompoundErrorDetails.ChatCreate(nstCreateChatCodeToErrorCode(resultCode), details));
				});
				
				break;
			}
		}
	}
	
	@Override
	boolean requestChatCreation(short requestID, String[] chatMembers, String service) {
		//Returning false if there is no connection thread
		if(!communicationsManager.isConnectionOpened()) return false;
		
		byte[] packetData;
		try(ByteArrayOutputStream trgt = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(trgt);
			ByteArrayOutputStream trgtSec = new ByteArrayOutputStream(); ObjectOutputStream outSec = new ObjectOutputStream(trgtSec)) {
			//Adding the data
			out.writeShort(requestID); //Request ID
			
			outSec.writeInt(chatMembers.length); //Members
			for(String item : chatMembers) outSec.writeUTF(item);
			outSec.writeUTF(service); //Service
			outSec.flush();
			
			writeEncrypted(trgtSec.toByteArray(), out); //Encrypted data
			
			out.flush();
			
			packetData = trgt.toByteArray();
		} catch(IOException | GeneralSecurityException exception) {
			//Printing the stack trace
			exception.printStackTrace();
			
			//Returning false
			return false;
		}
		
		//Sending the message
		dataProxy.send(new HeaderPacket(packetData, nhtCreateChat));
		
		//Returning true
		return true;
	}
	
	@Override
	boolean requestRetrievalAll(short requestID, MassRetrievalParams params) {
		//Returning false if there is no connection thread
		if(!communicationsManager.isConnectionOpened()) return false;
		
		//Building the data
		byte[] packetData;
		try(ByteArrayOutputStream trgt = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(trgt)) {
			//Adding the data
			out.writeShort(requestID); //Request ID
			//out.writeBoolean(params.isAdvanced); //Advanced retrieval
			
			out.writeBoolean(params.restrictMessages); //Whether or not to time-restrict messages
			if(params.restrictMessages) out.writeLong(params.timeSinceMessages); //Messages time since
			out.writeBoolean(params.downloadAttachments); //Whether or not to download attachments
			if(params.downloadAttachments) {
				out.writeBoolean(params.restrictAttachments); //Whether or not to time-restrict attachments
				if(params.restrictAttachments) out.writeLong(params.timeSinceAttachments); //Attachments time since
				out.writeBoolean(params.restrictAttachmentSizes); //Whether or not to size-restrict attachments
				if(params.restrictAttachmentSizes) out.writeLong(params.attachmentSizeLimit); //Attachment size limit
				
				out.writeInt(params.attachmentFilterWhitelist.size()); //Attachment type whitelist
				for(String filter : params.attachmentFilterWhitelist) out.writeUTF(filter);
				out.writeInt(params.attachmentFilterBlacklist.size()); //Attachment type blacklist
				for(String filter : params.attachmentFilterBlacklist) out.writeUTF(filter);
				out.writeBoolean(params.attachmentFilterDLOutside); //Whether or not to download "other" items
			}
			
			out.flush();
			
			packetData = trgt.toByteArray();
		} catch(IOException exception) {
			//Printing the stack trace
			exception.printStackTrace();
			
			//Returning false
			return false;
		}
		
		//Queuing the packet
		dataProxy.send(new HeaderPacket(packetData, nhtMassRetrieval));
		
		//Returning true
		return true;
	}
	
	@MessageSendErrorCode
	private int nstSendCodeToErrorCode(int code) {
		switch(code) {
			case nstSendResultScriptError:
				return MessageSendErrorCode.serverExternal;
			case nstSendResultBadRequest:
				return MessageSendErrorCode.serverBadRequest;
			case nstSendResultUnauthorized:
				return MessageSendErrorCode.serverUnauthorized;
			case nstSendResultNoConversation:
				return MessageSendErrorCode.serverNoConversation;
			case nstSendResultRequestTimeout:
				return MessageSendErrorCode.serverRequestTimeout;
			default:
				return MessageSendErrorCode.serverUnknown;
		}
	}
	
	@AttachmentReqErrorCode
	private int nstAttachmentReqCodeToLocalCode(int code) {
		switch(code) {
			case nstAttachmentReqNotFound:
				return AttachmentReqErrorCode.serverNotFound;
			case nstAttachmentReqNotSaved:
				return AttachmentReqErrorCode.serverNotSaved;
			case nstAttachmentReqUnreadable:
				return AttachmentReqErrorCode.serverUnreadable;
			case nstAttachmentReqIO:
				return AttachmentReqErrorCode.serverIO;
			default:
				return AttachmentReqErrorCode.unknown;
		}
	}
	
	@MessageState
	private int convertCodeMessageState(int code) {
		switch(code) {
			default:
			case nsMessageIdle:
				return MessageState.idle;
			case nsMessageSent:
				return MessageState.sent;
			case nsMessageDelivered:
				return MessageState.delivered;
			case nsMessageRead:
				return MessageState.read;
		}
	}
	
	@MessageSendErrorCode
	private int convertCodeAppleError(int code) {
		switch(code) {
			case nsAppleErrorOK:
				return MessageSendErrorCode.none;
			case nsAppleErrorUnknown:
			default:
				return MessageSendErrorCode.serverUnknown;
			case nsAppleErrorNetwork:
				return MessageSendErrorCode.appleNetwork;
			case nsAppleErrorUnregistered:
				return MessageSendErrorCode.appleUnregistered;
		}
	}
	
	@ChatCreateErrorCode
	private static int nstCreateChatCodeToErrorCode(int code) {
		switch(code) {
			case nstCreateChatScriptError: //Some unknown AppleScript error
				return ChatCreateErrorCode.scriptError;
			case nstCreateChatBadRequest: //Invalid data received
				return ChatCreateErrorCode.badRequest;
			case nstCreateChatUnauthorized: //System rejected request
				return ChatCreateErrorCode.unauthorized;
			default:
				return ChatCreateErrorCode.unknown;
		}
	}
	
	@GroupAction
	private int convertCodeGroupActionSubtype(int code) {
		switch(code) {
			case nsGroupActionSubtypeUnknown:
			default:
				return GroupAction.unknown;
			case nsGroupActionSubtypeJoin:
				return GroupAction.join;
			case nsGroupActionSubtypeLeave:
				return GroupAction.leave;
		}
	}
	
	private List<Blocks.ConversationInfo> deserializeConversations(ObjectInputStream in, int count) throws IOException {
		//Creating the list
		List<Blocks.ConversationInfo> list = new ArrayList<>(count);
		
		//Iterating over the items
		for(int i = 0; i < count; i++) {
			String guid = in.readUTF();
			boolean available = in.readBoolean();
			if(available) {
				String service = in.readUTF();
				String name = in.readBoolean() ? in.readUTF() : null;
				String[] members = new String[in.readInt()];
				for(int m = 0; m < members.length; m++) members[m] = in.readUTF();
				list.add(new Blocks.ConversationInfo(guid, service, name, members));
			} else {
				list.add(new Blocks.ConversationInfo(guid));
			}
		}
		
		//Returning the list
		return list;
	}
	
	private List<Blocks.ConversationItem> deserializeConversationItems(ObjectInputStream in, int count) throws IOException, RuntimeException {
		//Creating the list
		List<Blocks.ConversationItem> list = new ArrayList<>(count);
		
		//Iterating over the items
		for(int i = 0; i < count; i++) {
			int type = in.readInt();
			
			long serverID = in.readLong();
			String guid = in.readUTF();
			String chatGuid = in.readUTF();
			long date = in.readLong();
			
			switch(type) {
				default:
					throw new IOException("Invalid conversation item type: " + type);
				case conversationItemTypeMessage: {
					String text = in.readBoolean() ? StringHelper.nullifyEmptyString(in.readUTF()) : null;
					String sender = in.readBoolean() ? StringHelper.nullifyEmptyString(in.readUTF()) : null;
					List<Blocks.AttachmentInfo> attachments = deserializeAttachments(in, in.readInt());
					List<Blocks.StickerModifierInfo> stickers = (List<Blocks.StickerModifierInfo>) (List<?>) deserializeModifiers(in, in.readInt());
					List<Blocks.TapbackModifierInfo> tapbacks = (List<Blocks.TapbackModifierInfo>) (List<?>) deserializeModifiers(in, in.readInt());
					String sendEffect = in.readBoolean() ? StringHelper.nullifyEmptyString(in.readUTF()) : null;
					int stateCode = convertCodeMessageState(in.readInt());
					int errorCode = convertCodeAppleError(in.readInt());
					long dateRead = in.readLong();
					
					list.add(new Blocks.MessageInfo(serverID, guid, chatGuid, date, text, null, sender, attachments, stickers, tapbacks, sendEffect, stateCode, errorCode, dateRead));
					break;
				}
				case conversationItemTypeGroupAction: {
					String agent = in.readBoolean() ? in.readUTF() : null;
					String other = in.readBoolean() ? in.readUTF() : null;
					int groupActionType = convertCodeGroupActionSubtype(in.readInt());
					
					list.add(new Blocks.GroupActionInfo(serverID, guid, chatGuid, date, agent, other, groupActionType));
					break;
				}
				case conversationItemTypeChatRename: {
					String agent = in.readBoolean() ? in.readUTF() : null;
					String newChatName = in.readBoolean() ? StringHelper.nullifyEmptyString(in.readUTF()) : null;
					
					list.add(new Blocks.ChatRenameActionInfo(serverID, guid, chatGuid, date, agent, newChatName));
					break;
				}
			}
		}
		
		//Returning the list
		return list;
	}
	
	private List<Blocks.AttachmentInfo> deserializeAttachments(ObjectInputStream in, int count) throws IOException, RuntimeException {
		//Creating the list
		List<Blocks.AttachmentInfo> list = new ArrayList<>(count);
		
		//Iterating over the items
		for(int i = 0; i < count; i++) {
			String guid = in.readUTF();
			String name = in.readUTF();
			String type = in.readBoolean() ? in.readUTF() : MIMEConstants.defaultMIMEType;
			long size = in.readLong();
			byte[] checksum;
			if(in.readBoolean()) {
				checksum = new byte[in.readInt()];
				in.readFully(checksum);
			} else {
				checksum = null;
			}
			
			list.add(new Blocks.AttachmentInfo(guid, name, type, size, checksum, -1));
		}
		
		//Returning the list
		return list;
	}
	
	private List<Blocks.ModifierInfo> deserializeModifiers(ObjectInputStream in, int count) throws IOException, RuntimeException {
		//Creating the list
		List<Blocks.ModifierInfo> list = new ArrayList<>(count);
		
		//Iterating over the items
		for(int i = 0; i < count; i++) {
			int type = in.readInt();
			
			String message = in.readUTF();
			
			switch(type) {
				default:
					throw new IOException("Invalid modifier type: " + type);
				case modifierTypeActivity: {
					int state = convertCodeMessageState(in.readInt());
					long dateRead = in.readLong();
					
					list.add(new Blocks.ActivityStatusModifierInfo(message, state, dateRead));
					break;
				}
				case modifierTypeSticker: {
					int messageIndex = in.readInt();
					String fileGuid = in.readUTF();
					String sender = in.readBoolean() ? in.readUTF() : null;
					long date = in.readLong();
					byte[] image = new byte[in.readInt()];
					in.readFully(image);
					String fileType = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(image));
					
					byte[] decompressedImage;
					try {
						decompressedImage = StandardCompressionHelper.decompressGZIP(image);
					} catch(IOException exception) {
						exception.printStackTrace();
						continue;
					}
					
					list.add(new Blocks.StickerModifierInfo(message, messageIndex, fileGuid, sender, date, decompressedImage, fileType));
					break;
				}
				case modifierTypeTapback: {
					int messageIndex = in.readInt();
					String sender = in.readBoolean() ? in.readUTF() : null;
					int associationType = in.readInt();
					
					//In protocol 5.1+, this data is provided by the server
					boolean tapbackAdded = associationType >= 2000 && associationType < 3000;
					int tapbackType = associationType % 1000;
					
					list.add(new Blocks.TapbackModifierInfo(message, messageIndex, sender, tapbackAdded, convertTapbackToPrivateCode(tapbackType)));
					break;
				}
			}
		}
		
		//Returning the list
		return list;
	}
	
	@Override
	boolean sendAuthenticationRequest() {
		//Returning false if there is no connection
		if(!communicationsManager.isConnectionOpened()) return false;
		
		byte[] packetData;
		try(ByteArrayOutputStream trgt = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(trgt)) {
			writeEncrypted(transmissionCheck.getBytes(stringCharset), out);
			out.flush();
			
			packetData = trgt.toByteArray();
		} catch(IOException | GeneralSecurityException exception) {
			//Logging the error
			exception.printStackTrace();
			FirebaseCrashlytics.getInstance().recordException(exception);
			
			//Closing the connection
			communicationsManager.getHandler().post(() -> communicationsManager.disconnect(ConnectionErrorCode.internalError));
			
			//Returning false
			return false;
		}
		
		//Sending the message
		dataProxy.send(new HeaderPacket(packetData, nhtAuthentication));
		
		//Returning true
		return true;
	}
	
	@Override
	boolean sendMessage(short requestID, ConversationTarget conversation, String message) {
		if(conversation instanceof ConversationTarget.AppleLinked) {
			return sendMessage(requestID, ((ConversationTarget.AppleLinked) conversation).getGuid(), message);
		} else if(conversation instanceof ConversationTarget.AppleUnlinked) {
			ConversationTarget.AppleUnlinked unlinkedTarget = (ConversationTarget.AppleUnlinked) conversation;
			return sendMessage(requestID, unlinkedTarget.getMembers(), message, unlinkedTarget.getService());
		} else {
			return false;
		}
	}
	
	private boolean sendMessage(short requestID, String chatGUID, String message) {
		//Returning false if there is no connection
		if(!communicationsManager.isConnectionOpened()) return false;
		
		byte[] packetData;
		try(ByteArrayOutputStream trgt = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(trgt);
			ByteArrayOutputStream trgtSec = new ByteArrayOutputStream(); ObjectOutputStream outSec = new ObjectOutputStream(trgtSec)) {
			//Adding the data
			out.writeShort(requestID); //Request ID
			
			outSec.writeUTF(chatGUID); //Chat GUID
			outSec.writeUTF(message); //Message
			outSec.flush();
			
			writeEncrypted(trgtSec.toByteArray(), out); //Encrypted data
			
			out.flush();
			
			packetData = trgt.toByteArray();
		} catch(IOException | GeneralSecurityException exception) {
			//Printing the stack trace
			exception.printStackTrace();
			FirebaseCrashlytics.getInstance().recordException(exception);
			
			//Returning false
			return false;
		}
		
		//Sending the message
		dataProxy.send(new HeaderPacket(packetData, nhtSendTextExisting));
		
		//Returning true
		return true;
	}
	
	private boolean sendMessage(short requestID, List<String> chatMembers, String message, String service) {
		//Returning false if there is no connection thread
		if(!communicationsManager.isConnectionOpened()) return false;
		
		byte[] packetData;
		try(ByteArrayOutputStream trgt = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(trgt);
			ByteArrayOutputStream trgtSec = new ByteArrayOutputStream(); ObjectOutputStream outSec = new ObjectOutputStream(trgtSec)) {
			//Adding the data
			out.writeShort(requestID); //Request ID
			
			outSec.writeInt(chatMembers.size()); //Members
			for(String item : chatMembers) outSec.writeUTF(item);
			outSec.writeUTF(message); //Message
			outSec.writeUTF(service); //Service
			outSec.flush();
			
			writeEncrypted(trgtSec.toByteArray(), out); //Encrypted data
			
			out.flush();
			
			packetData = trgt.toByteArray();
		} catch(IOException | GeneralSecurityException exception) {
			//Printing the stack trace
			exception.printStackTrace();
			
			//Returning false
			return false;
		}
		
		//Sending the message
		dataProxy.send(new HeaderPacket(packetData, nhtSendTextNew));
		
		//Returning true
		return true;
	}
	
	@Override
	public Observable<ReduxEventAttachmentUpload> sendFile(short requestID, ConversationTarget conversation, File file) {
		return Observable.create((emitter) -> {
			try {
				MessageDigest messageDigest = MessageDigest.getInstance(hashAlgorithm);
				try(InputStream inputStream = new DigestInputStream(new BufferedInputStream(new FileInputStream(file)), messageDigest)) {
					long totalLength = inputStream.available();
					byte[] buffer = new byte[attachmentChunkSize];
					int bytesRead;
					long totalBytesRead = 0;
					int requestIndex = 0;
					
					//Looping while there is data to read
					while((bytesRead = inputStream.read(buffer)) != -1) {
						//Adding to the total bytes read
						totalBytesRead += bytesRead;
						
						//Compressing the data in chunks
						byte[] preparedData;
						try {
							preparedData = StandardCompressionHelper.compressGZIP(buffer, bytesRead);
						} catch(IOException exception) {
							exception.printStackTrace();
							FirebaseCrashlytics.getInstance().recordException(exception);
							throw new AMRequestException(MessageSendErrorCode.localInternal, exception);
						}
						
						//Uploading the file part
						boolean isLast = totalBytesRead >= totalLength;
						boolean uploadResult;
						if(conversation instanceof ConversationTarget.AppleLinked) {
							uploadResult = uploadFilePacket(requestID, requestIndex, ((ConversationTarget.AppleLinked) conversation).getGuid(), preparedData, file.getName(), isLast);
						} else if(conversation instanceof ConversationTarget.AppleUnlinked) {
							ConversationTarget.AppleUnlinked unlinkedConversation = (ConversationTarget.AppleUnlinked) conversation;
							uploadResult = uploadFilePacket(requestID, requestIndex, unlinkedConversation.getMembers(), preparedData, file.getName(), unlinkedConversation.getService(), isLast);
						} else {
							uploadResult = false;
						}
						
						//Throwing an error if the file part wasn't uploaded
						if(!uploadResult) {
							throw new AMRequestException(MessageSendErrorCode.localNetwork);
						}
						
						//Incrementing the request index
						requestIndex++;
						
						//Updating the progress
						emitter.onNext(new ReduxEventAttachmentUpload.Progress(totalBytesRead, totalLength));
					}
				}
				
				//Finishing
				emitter.onNext(new ReduxEventAttachmentUpload.Complete(messageDigest.digest()));
				emitter.onComplete();
			} catch(IOException exception) {
				exception.printStackTrace();
				throw new AMRequestException(MessageSendErrorCode.localIO, exception);
			} catch(NoSuchAlgorithmException | BufferOverflowException exception) {
				exception.printStackTrace();
				FirebaseCrashlytics.getInstance().recordException(exception);
				throw new AMRequestException(MessageSendErrorCode.localInternal, exception);
			}
		});
	}
	
	private boolean uploadFilePacket(short requestID, int requestIndex, String conversationGUID, byte[] data, String fileName, boolean isLast) {
		//Returning false if there is no connection thread
		if(!communicationsManager.isConnectionOpened()) return false;
		
		//Adding the data
		byte[] packetData;
		try(ByteArrayOutputStream trgt = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(trgt);
			ByteArrayOutputStream trgtSec = new ByteArrayOutputStream(); ObjectOutputStream outSec = new ObjectOutputStream(trgtSec)) {
			out.writeShort(requestID); //Request identifier
			out.writeInt(requestIndex); //Request index
			out.writeBoolean(isLast); //Is last message
			
			outSec.writeUTF(conversationGUID); //Chat GUID
			outSec.writeInt(data.length); //File bytes
			outSec.write(data);
			if(requestIndex == 0) outSec.writeUTF(fileName); //File name
			outSec.flush();
			
			writeEncrypted(trgtSec.toByteArray(), out); //Encrypted data
			
			out.flush();
			
			packetData = trgt.toByteArray();
		} catch(IOException | GeneralSecurityException exception) {
			//Logging the exception
			exception.printStackTrace();
			FirebaseCrashlytics.getInstance().recordException(exception);
			
			//Returning false
			return false;
		}
		
		//Sending the message
		dataProxy.send(new HeaderPacket(packetData, nhtSendFileExisting));
		
		//Returning true
		return true;
	}
	
	private boolean uploadFilePacket(short requestID, int requestIndex, List<String> conversationMembers, byte[] data, String fileName, String service, boolean isLast) {
		//Returning false if there is no connection thread
		if(!communicationsManager.isConnectionOpened()) return false;
		
		//Adding the data
		byte[] packetData;
		try(ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos);
			ByteArrayOutputStream trgtSec = new ByteArrayOutputStream(); ObjectOutputStream outSec = new ObjectOutputStream(trgtSec)) {
			out.writeShort(requestID); //Request identifier
			out.writeInt(requestIndex); //Request index
			out.writeBoolean(isLast); //Is last message
			
			outSec.writeInt(conversationMembers.size()); //Chat members
			for(String item : conversationMembers) outSec.writeUTF(item);
			outSec.writeInt(data.length); //File bytes
			outSec.write(data);
			if(requestIndex == 0) {
				outSec.writeUTF(fileName); //File name
				outSec.writeUTF(service); //Service
			}
			outSec.flush();
			
			writeEncrypted(trgtSec.toByteArray(), out); //Encrypted data
			
			out.flush();
			
			packetData = bos.toByteArray();
		} catch(IOException | GeneralSecurityException exception) {
			//Logging the exception
			exception.printStackTrace();
			FirebaseCrashlytics.getInstance().recordException(exception);
			
			//Returning false
			return false;
		}
		
		//Sending the message
		dataProxy.send(new HeaderPacket(packetData, nhtSendFileNew));
		
		//Returning true
		return true;
	}
	
	@Override
	public boolean requestAttachmentDownload(short requestID, String attachmentGUID) {
		//Preparing to serialize the request
		try(ByteArrayOutputStream trgt = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(trgt);
			ByteArrayOutputStream trgtSec = new ByteArrayOutputStream(); ObjectOutputStream outSec = new ObjectOutputStream(trgtSec)) {
			//Adding the data
			out.writeShort(requestID); //Request ID
			out.writeInt(attachmentChunkSize); //Chunk size
			
			outSec.writeUTF(attachmentGUID); //File GUID
			outSec.flush();
			
			writeEncrypted(trgtSec.toByteArray(), out); //Encrypted data
			
			out.flush();
			
			//Sending the message
			return dataProxy.send(new HeaderPacket(trgt.toByteArray(), nhtAttachmentReq));
		} catch(IOException | GeneralSecurityException exception) {
			//Printing the stack trace
			exception.printStackTrace();
			FirebaseCrashlytics.getInstance().recordException(exception);
			
			//Returning false
			return false;
		}
	}
	
	@Override
	public boolean requestConversationInfo(Collection<String> conversations) {
		//Returning false if there is no connection thread
		if(!communicationsManager.isConnectionOpened()) return false;
		
		//Requesting information on new conversations
		try(ByteArrayOutputStream trgt = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(trgt);
			ByteArrayOutputStream trgtSec = new ByteArrayOutputStream(); ObjectOutputStream outSec = new ObjectOutputStream(trgtSec)) {
			outSec.writeInt(conversations.size());
			for(String item : conversations) outSec.writeUTF(item);
			outSec.flush();
			
			writeEncrypted(trgtSec.toByteArray(), out); //Encrypted data
			
			out.flush();
			
			//Sending the message
			dataProxy.send(new HeaderPacket(trgt.toByteArray(), nhtConversationUpdate));
		} catch(IOException | GeneralSecurityException exception) {
			//Logging the exception
			exception.printStackTrace();
			FirebaseCrashlytics.getInstance().recordException(exception);
			
			//Returning false
			return false;
		}
		
		//Returning true
		return true;
	}
	
	@Override
	boolean requestRetrievalTime(long timeLower, long timeUpper) {
		//Returning false if there is no connection thread
		if(!communicationsManager.isConnectionOpened()) return false;
		
		//Sending the message
		dataProxy.send(new HeaderPacket(ByteBuffer.allocate(Long.SIZE / 8 * 2).putLong(timeLower).putLong(timeUpper).array(), nhtTimeRetrieval));
		
		//Returning true
		return true;
	}
	
	private static final int encryptionSaltLen = 8;
	private static final int encryptionIvLen = 12; //12 bytes (instead of 16 because of GCM)
	private static final String encryptionKeyFactoryAlgorithm = "PBKDF2WithHmacSHA256";
	private static final String encryptionKeyAlgorithm = "AES";
	private static final String encryptionCipherTransformation = "AES/GCM/NoPadding";
	private static final int encryptionKeyIterationCount = 10000;
	private static final int encryptionKeyLength = 128; //128 bits
	
	byte[] readEncrypted(ObjectInputStream stream) throws IOException, GeneralSecurityException {
		if(communicationsManager.getPassword() == null) throw new GeneralSecurityException("No password available");
		
		//Reading the data
		byte[] salt = new byte[encryptionSaltLen];
		stream.readFully(salt);
		
		byte[] iv = new byte[encryptionIvLen];
		stream.readFully(iv);
		
		byte[] data = new byte[stream.readInt()];
		stream.readFully(data);
		
		//Creating the key
		SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(encryptionKeyFactoryAlgorithm);
		KeySpec keySpec = new PBEKeySpec(communicationsManager.getPassword().toCharArray(), salt, encryptionKeyIterationCount, encryptionKeyLength);
		SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
		SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getEncoded(), encryptionKeyAlgorithm);
		
		//Creating the IV
		GCMParameterSpec gcmSpec = new GCMParameterSpec(encryptionKeyLength, iv);
		
		//Creating the cipher
		Cipher cipher = Cipher.getInstance(encryptionCipherTransformation);
		cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmSpec);
		
		//Deciphering the data
		byte[] block = cipher.doFinal(data);
		return block;
	}
	
	void writeEncrypted(byte[] block, ObjectOutputStream stream) throws IOException, GeneralSecurityException {
		if(communicationsManager.getPassword() == null) throw new GeneralSecurityException("No password available");
		
		//Generating a salt
		byte[] salt = new byte[encryptionSaltLen];
		random.nextBytes(salt);
		
		//Creating the key
		SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(encryptionKeyFactoryAlgorithm);
		KeySpec keySpec = new PBEKeySpec(communicationsManager.getPassword().toCharArray(), salt, encryptionKeyIterationCount, encryptionKeyLength);
		SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
		SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getEncoded(), encryptionKeyAlgorithm);
		
		//Generating the IV
		byte[] iv = new byte[encryptionIvLen];
		random.nextBytes(iv);
		GCMParameterSpec gcmSpec = new GCMParameterSpec(encryptionKeyLength, iv);
		
		Cipher cipher = Cipher.getInstance(encryptionCipherTransformation);
		cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmSpec);
		
		//Encrypting the data
		byte[] data = cipher.doFinal(block);
		
		//Writing the data
		stream.write(salt);
		stream.write(iv);
		stream.writeInt(data.length);
		stream.write(data);
	}
	
	@TapbackType
	private static int convertTapbackToPrivateCode(int publicCode) {
		//Returning the associated version
		switch(publicCode) {
			case 0:
				return TapbackType.heart;
			case 1:
				return TapbackType.like;
			case 2:
				return TapbackType.dislike;
			case 3:
				return TapbackType.laugh;
			case 4:
				return TapbackType.exclamation;
			case 5:
				return TapbackType.question;
			default:
				return TapbackType.unknown;
		}
	}
}