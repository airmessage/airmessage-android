package me.tagavari.airmessage.connection.comm4;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.webkit.MimeTypeMap;

import com.google.android.exoplayer2.util.MimeTypes;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.common.Blocks;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.connection.MassRetrievalParams;
import me.tagavari.airmessage.connection.request.ChatCreationResponseManager;
import me.tagavari.airmessage.connection.request.ConversationInfoRequest;
import me.tagavari.airmessage.connection.request.FileDownloadRequest;
import me.tagavari.airmessage.connection.request.MessageResponseManager;
import me.tagavari.airmessage.messaging.TapbackInfo;
import me.tagavari.airmessage.util.Constants;

//Improved error messages, chat creation requests
class ClientProtocol6 extends ProtocolManager {
	final ConnectionManager.Packager protocolPackager = new ConnectionManager.PackagerGZIP();
	static final String hashAlgorithm = "MD5";
	static final String stringCharset = "UTF-8";
	
	//Top-level net header type values
	static final int nhtAuthentication = 1;
	static final int nhtMessageUpdate = 2;
	static final int nhtTimeRetrieval = 3;
	static final int nhtMassRetrieval = 4;
	static final int nhtConversationUpdate = 5;
	static final int nhtModifierUpdate = 6;
	static final int nhtAttachmentReq = 7;
	static final int nhtAttachmentReqConfirm = 8;
	static final int nhtAttachmentReqFail = 9;
	static final int nhtMassRetrievalFinish = 10;
	static final int nhtMassRetrievalFile = 11;
	static final int nhtCreateChat = 12;
	static final int nhtSendResult = 100;
	static final int nhtSendTextExisting = 101;
	static final int nhtSendTextNew = 102;
	static final int nhtSendFileExisting = 103;
	static final int nhtSendFileNew = 104;
	
	//Net subtype values
	static final int nstAuthenticationOK = 0;
	static final int nstAuthenticationUnauthorized = 1;
	static final int nstAuthenticationBadRequest = 2;
	static final String transmissionCheck = "4yAIlVK0Ce_Y7nv6at_hvgsFtaMq!lZYKipV40Fp5E%VSsLSML";
	
	//Net state values
	static final int nsMessageIdle = 0;
	static final int nsMessageSent = 1;
	static final int nsMessageDelivered = 2;
	static final int nsMessageRead = 3;
	
	static final int nsAppleErrorOK = 0;
	static final int nsAppleErrorUnknown = 1; //Unknown error code
	static final int nsAppleErrorNetwork = 2; //Network error
	static final int nsAppleErrorUnregistered = 3; //Not registered with iMessage
	
	static final int nsGroupActionSubtypeUnknown = 0;
	static final int nsGroupActionSubtypeJoin = 1;
	static final int nsGroupActionSubtypeLeave = 2;
	
	//Return codes
	static final int nstSendResultOK = 0; //Message sent successfully
	static final int nstSendResultScriptError = 1; //Some unknown AppleScript error
	static final int nstSendResultBadRequest = 2; //Invalid data received
	static final int nstSendResultUnauthorized = 3; //System rejected request to send message
	static final int nstSendResultNoConversation = 4; //A valid conversation wasn't found
	static final int nstSendResultRequestTimeout = 5; //File data blocks stopped being received
	
	static final int nstAttachmentReqNotFound = 1; //File GUID not found
	static final int nstAttachmentReqNotSaved = 2; //File (on disk) not found
	static final int nstAttachmentReqUnreadable = 3; //No access to file
	static final int nstAttachmentReqIO = 4; //IO error
	
	static final int nstCreateChatOK = 0;
	static final int nstCreateChatScriptError = 1; //Some unknown AppleScript error
	static final int nstCreateChatBadRequest = 2; //Invalid data received
	static final int nstCreateChatUnauthorized = 3; //System rejected request to send message
	
	//Item types
	static final int conversationItemTypeMessage = 0;
	static final int conversationItemTypeGroupAction = 1;
	static final int conversationItemTypeChatRename = 2;
	
	private static final int modifierTypeActivity = 0;
	private static final int modifierTypeSticker = 1;
	private static final int modifierTypeTapback = 2;
	
	ClientProtocol6(Context context, ConnectionManager connectionManager, ClientComm4 communicationsManager) {
		super(context, connectionManager, communicationsManager);
	}
	
	@Override
	boolean sendPing() {
		return communicationsManager.queuePacket(new PacketStructOut(ClientComm4.nhtPing, new byte[0]));
	}
	
	@Override
	void processData(int messageType, byte[] data) {
		switch(messageType) {
			case ClientComm4.nhtClose:
				communicationsManager.disconnect(ConnectionManager.connResultConnection);
				break;
			case ClientComm4.nhtPing:
				communicationsManager.queuePacket(new PacketStructOut(ClientComm4.nhtPong, new byte[0]));
				break;
			case nhtAuthentication: {
				//Stopping the authentication timer
				communicationsManager.handler.removeCallbacks(communicationsManager.handshakeExpiryRunnable);
				
				//Reading the result code
				ByteBuffer dataBuffer = ByteBuffer.wrap(data);
				int resultCode = dataBuffer.getInt();
				
				//Translating the result code to a local value
				switch(resultCode) {
					case nstAuthenticationOK:
						resultCode = ConnectionManager.connResultSuccess;
						break;
					case nstAuthenticationUnauthorized:
						resultCode = ConnectionManager.connResultDirectUnauthorized;
						break;
					case nstAuthenticationBadRequest:
						resultCode = ConnectionManager.connResultBadRequest;
						break;
							/* case nhtAuthenticationVersionMismatch:
								if(SharedValues.mmCommunicationsVersion > communicationsVersion) result = intentResultCodeServerOutdated;
								else result = intentResultCodeClientOutdated;
								break; */
				}
				
				//Finishing the connection establishment if the handshake was successful
				if(resultCode == ConnectionManager.connResultSuccess) communicationsManager.onHandshakeCompleted(null, null, null, null);
					//Otherwise terminating the connection
				else communicationsManager.disconnect(resultCode);
				
				break;
			}
			case nhtMessageUpdate:
			case nhtTimeRetrieval: {
				//Reading the list
				List<Blocks.ConversationItem> list;
				try(ByteArrayInputStream src = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(src)) {
					//Reading the secure data
					try(ByteArrayInputStream srcSec = new ByteArrayInputStream(readEncrypted(in, ConnectionManager.password)); ObjectInputStream inSec = new ObjectInputStream(srcSec)) {
						list = deserializeConversationItems(inSec, inSec.readInt());
					}
				} catch(IOException | RuntimeException | GeneralSecurityException exception) {
					exception.printStackTrace();
					break;
				}
				
				//Processing the messages
				connectionManager.processMessageUpdate(list, true);
				
				break;
			}
			case nhtMassRetrieval: {
				try(ByteArrayInputStream src = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(src)) {
					//Reading the packet information
					short requestID = in.readShort();
					int packetIndex = in.readInt();
					
					//Reading the secure data
					try(ByteArrayInputStream srcSec = new ByteArrayInputStream(readEncrypted(in, ConnectionManager.password)); ObjectInputStream inSec = new ObjectInputStream(srcSec)) {
						//Checking if this is the first packet
						if(packetIndex == 0) {
							//Reading the conversation list
							List<Blocks.ConversationInfo> conversationList = deserializeConversations(inSec, inSec.readInt());
							
							//Reading the message count
							int messageCount = inSec.readInt();
							
							//Registering the mass retrieval manager
							if(connectionManager.getMassRetrievalThread() != null) connectionManager.getMassRetrievalThread().registerInfo(MainApplication.getInstance(), requestID, conversationList, messageCount, getPackager());
						} else {
							//Reading the item list
							List<Blocks.ConversationItem> listItems = deserializeConversationItems(inSec, inSec.readInt());
							
							//Processing the packet
							if(connectionManager.getMassRetrievalThread() != null) connectionManager.getMassRetrievalThread().addPacket(MainApplication.getInstance(), requestID, packetIndex, listItems);
						}
					}
				} catch(IOException | RuntimeException | GeneralSecurityException exception) {
					//Logging the exception
					exception.printStackTrace();
					
					//Cancelling the mass retrieval process
					connectionManager.cancelMassRetrieval(MainApplication.getInstance());
				}
				
				break;
			}
			case nhtMassRetrievalFinish: {
				//Finishing the mass retrieval
				connectionManager.finishMassRetrieval();
				
				break;
			}
			case nhtMassRetrievalFile: {
				//Reading the data
				final short requestID;
				final int requestIndex;
				final String fileName;
				final boolean isLast;
				
				final String fileGUID;
				final byte[] compressedBytes;
				
				try(ByteArrayInputStream bis = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(bis)) {
					requestID = in.readShort();
					requestIndex = in.readInt();
					if(requestIndex == 0) fileName = in.readUTF();
					else fileName = null;
					isLast = in.readBoolean();
					
					try(ByteArrayInputStream srcSec = new ByteArrayInputStream(readEncrypted(in, ConnectionManager.password)); ObjectInputStream inSec = new ObjectInputStream(srcSec)) {
						fileGUID = inSec.readUTF();
						int contentLen = inSec.readInt();
						if(contentLen > ConnectionManager.maxPacketAllocation) {
							//Logging the error
							Logger.getGlobal().log(Level.WARNING, "Rejecting large byte chunk (type: " + messageType + " - size: " + contentLen + ")");
							
							//Closing the connection
							communicationsManager.disconnect(ConnectionManager.connResultConnection);
							break;
						}
						compressedBytes = new byte[contentLen];
						inSec.readFully(compressedBytes);
					}
				} catch(IOException | RuntimeException | GeneralSecurityException exception) {
					exception.printStackTrace();
					break;
				}
				
				//Processing the data
				if(connectionManager.getMassRetrievalThread() != null) {
					if(requestIndex == 0) connectionManager.getMassRetrievalThread().startFileData(requestID, fileGUID, fileName);
					connectionManager.getMassRetrievalThread().appendFileData(requestID, requestIndex, fileGUID, compressedBytes, isLast);
				}
				break;
			}
			case nhtConversationUpdate: {
				//Reading the list
				List<Blocks.ConversationInfo> list;
				try(ByteArrayInputStream src = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(src)) {
					//Reading the secure data
					try(ByteArrayInputStream srcSec = new ByteArrayInputStream(readEncrypted(in, ConnectionManager.password)); ObjectInputStream inSec = new ObjectInputStream(srcSec)) {
						list = deserializeConversations(inSec, inSec.readInt());
					}
				} catch(IOException | RuntimeException | GeneralSecurityException exception) {
					exception.printStackTrace();
					break;
				}
				
				//Processing the conversations
				connectionManager.processChatInfoResponse(list);
				
				break;
			}
			case nhtModifierUpdate: {
				//Reading the list
				List<Blocks.ModifierInfo> list;
				try(ByteArrayInputStream bis = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(bis)) {
					//Reading the secure data
					try(ByteArrayInputStream srcSec = new ByteArrayInputStream(readEncrypted(in, ConnectionManager.password)); ObjectInputStream inSec = new ObjectInputStream(srcSec)) {
						list = deserializeModifiers(inSec, inSec.readInt());
					}
				} catch(IOException | RuntimeException | GeneralSecurityException exception) {
					exception.printStackTrace();
					break;
				}
				
				//Processing the conversations
				connectionManager.processModifierUpdate(list, getPackager());
				
				break;
			}
			case nhtAttachmentReq: {
				//Reading the data
				final short requestID;
				final int requestIndex;
				final long fileSize;
				final boolean isLast;
				
				final String fileGUID;
				final byte[] compressedBytes;
				
				try(ByteArrayInputStream bis = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(bis)) {
					requestID = in.readShort();
					requestIndex = in.readInt();
					if(requestIndex == 0) fileSize = in.readLong();
					else fileSize = -1;
					isLast = in.readBoolean();
					
					//Reading the secure data
					try(ByteArrayInputStream srcSec = new ByteArrayInputStream(readEncrypted(in, ConnectionManager.password)); ObjectInputStream inSec = new ObjectInputStream(srcSec)) {
						fileGUID = inSec.readUTF();
						compressedBytes = new byte[inSec.readInt()];
						inSec.readFully(compressedBytes);
					}
				} catch(IOException | RuntimeException | GeneralSecurityException exception) {
					exception.printStackTrace();
					break;
				}
				
				//Running on the UI thread
				new Handler(Looper.getMainLooper()).post(() -> {
					//Searching for a matching request
					for(FileDownloadRequest request : connectionManager.getFileDownloadRequests()) {
						if(request.getRequestID() != requestID || !request.getAttachmentGUID().equals(fileGUID)) continue;
						if(requestIndex == 0) request.setFileSize(fileSize);
						request.processFileFragment(context, compressedBytes, requestIndex, isLast, getPackager());
						if(isLast) connectionManager.removeDownloadRequest(request);
						break;
					}
				});
				
				break;
			}
			case nhtAttachmentReqConfirm: {
				//Reading the data
				final short requestID = ByteBuffer.wrap(data).getShort();
				
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
				
				break;
			}
			case nhtAttachmentReqFail: {
				//Reading the data
				ByteBuffer byteBuffer = ByteBuffer.wrap(data);
				final short requestID = byteBuffer.getShort();
				final int errorCode = byteBuffer.getInt();
				
				//Running on the UI thread
				new Handler(Looper.getMainLooper()).post(() -> {
					//Failing the download request
					connectionManager.failDownloadRequest(requestID, nstAttachmentReqCodeToLocalCode(errorCode));
				});
				
				break;
			}
			case nhtSendResult: {
				short requestID;
				int resultCode;
				String details;
				
				try(ByteArrayInputStream src = new ByteArrayInputStream(data); ObjectInputStream in = new ObjectInputStream(src)) {
					requestID = in.readShort();
					
					//Reading the secure data
					try(ByteArrayInputStream srcSec = new ByteArrayInputStream(readEncrypted(in, ConnectionManager.password)); ObjectInputStream inSec = new ObjectInputStream(srcSec)) {
						resultCode = inSec.readInt();
						details = inSec.readBoolean() ? inSec.readUTF() : null;
					}
				} catch(IOException | RuntimeException | GeneralSecurityException exception) {
					//Logging the exception
					exception.printStackTrace();
					
					break;
				}
				
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
					try(ByteArrayInputStream srcSec = new ByteArrayInputStream(readEncrypted(in, ConnectionManager.password)); ObjectInputStream inSec = new ObjectInputStream(srcSec)) {
						resultCode = inSec.readInt();
						details = (resultOK = resultCode == nstCreateChatOK) || inSec.readBoolean() ? inSec.readUTF() : null;
					}
				} catch(IOException | RuntimeException | GeneralSecurityException exception) {
					//Logging the exception
					exception.printStackTrace();
					
					break;
				}
				
				//Running on the UI thread
				new Handler(Looper.getMainLooper()).post(() -> {
					//Getting the message response manager
					final ChatCreationResponseManager responseManager = connectionManager.getChatCreationRequests().get(requestID);
					if(responseManager == null) return;
					
					//Removing the request
					connectionManager.getChatCreationRequests().remove(requestID);
					responseManager.stopTimer();
					
					//Telling the listener
					if(resultOK) responseManager.onSuccess(details);
					else responseManager.onFail();
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
			
			writeEncrypted(trgtSec.toByteArray(), out, ConnectionManager.password); //Encrypted data
			
			out.flush();
			
			packetData = trgt.toByteArray();
		} catch(IOException | GeneralSecurityException exception) {
			//Printing the stack trace
			exception.printStackTrace();
			
			//Returning false
			return false;
		}
		
		//Sending the message
		communicationsManager.queuePacket(new PacketStructOut(nhtCreateChat, packetData));
		
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
		communicationsManager.queuePacket(new PacketStructOut(nhtMassRetrieval, packetData));
		
		//Returning true
		return true;
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
					String text = in.readBoolean() ? in.readUTF() : null;
					String sender = in.readBoolean() ? in.readUTF() : null;
					List<Blocks.AttachmentInfo> attachments = deserializeAttachments(in, in.readInt());
					List<Blocks.StickerModifierInfo> stickers = (List<Blocks.StickerModifierInfo>) (List<?>) deserializeModifiers(in, in.readInt());
					List<Blocks.TapbackModifierInfo> tapbacks = (List<Blocks.TapbackModifierInfo>) (List<?>) deserializeModifiers(in, in.readInt());
					String sendEffect = in.readBoolean() ? in.readUTF() : null;
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
					String newChatName = in.readBoolean() ? in.readUTF() : null;
					
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
			String type = in.readBoolean() ? in.readUTF() : null;
			long size = in.readLong();
			byte[] checksum;
			if(in.readBoolean()) {
				checksum = new byte[in.readInt()];
				in.readFully(checksum);
			} else {
				checksum = null;
			}
			
			list.add(new Blocks.AttachmentInfo(guid, name, type, size, checksum));
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
					list.add(new Blocks.StickerModifierInfo(message, messageIndex, fileGuid, sender, date, image, fileType));
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
		//Returning false if there is no connection thread
		if(!communicationsManager.isConnectionOpened()) return false;
		
		byte[] packetData;
		try(ByteArrayOutputStream trgt = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(trgt)) {
			writeEncrypted(transmissionCheck.getBytes(stringCharset), out, ConnectionManager.password);
			out.flush();
			
			packetData = trgt.toByteArray();
		} catch(IOException | GeneralSecurityException exception) {
			//Logging the error
			exception.printStackTrace();
			FirebaseCrashlytics.getInstance().recordException(exception);
			
			//Closing the connection
			communicationsManager.disconnect(ConnectionManager.connResultInternalError);
			
			//Returning false
			return false;
		}
		
		//Sending the message
		communicationsManager.queuePacket(new PacketStructOut(nhtAuthentication, packetData));
		
		//Returning true
		return true;
	}
	
	@Override
	boolean sendMessage(short requestID, String chatGUID, String message) {
		//Returning false if there is no connection thread
		if(!communicationsManager.isConnectionOpened()) return false;
		
		byte[] packetData;
		try(ByteArrayOutputStream trgt = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(trgt);
			ByteArrayOutputStream trgtSec = new ByteArrayOutputStream(); ObjectOutputStream outSec = new ObjectOutputStream(trgtSec)) {
			//Adding the data
			out.writeShort(requestID); //Request ID
			
			outSec.writeUTF(chatGUID); //Chat GUID
			outSec.writeUTF(message); //Message
			outSec.flush();
			
			writeEncrypted(trgtSec.toByteArray(), out, ConnectionManager.password); //Encrypted data
			
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
		communicationsManager.queuePacket(new PacketStructOut(nhtSendTextExisting, packetData));
		
		//Returning true
		return true;
	}
	
	@Override
	boolean sendMessage(short requestID, String[] chatMembers, String message, String service) {
		//Returning false if there is no connection thread
		if(!communicationsManager.isConnectionOpened()) return false;
		
		byte[] packetData;
		try(ByteArrayOutputStream trgt = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(trgt);
			ByteArrayOutputStream trgtSec = new ByteArrayOutputStream(); ObjectOutputStream outSec = new ObjectOutputStream(trgtSec)) {
			//Adding the data
			out.writeShort(requestID); //Request ID
			
			outSec.writeInt(chatMembers.length); //Members
			for(String item : chatMembers) outSec.writeUTF(item);
			outSec.writeUTF(message); //Message
			outSec.writeUTF(service); //Service
			outSec.flush();
			
			writeEncrypted(trgtSec.toByteArray(), out, ConnectionManager.password); //Encrypted data
			
			out.flush();
			
			packetData = trgt.toByteArray();
		} catch(IOException | GeneralSecurityException exception) {
			//Printing the stack trace
			exception.printStackTrace();
			
			//Returning false
			return false;
		}
		
		//Sending the message
		communicationsManager.queuePacket(new PacketStructOut(nhtSendTextNew, packetData));
		
		//Returning true
		return true;
	}
	
	@Override
	boolean addDownloadRequest(short requestID, String attachmentGUID, Runnable sentRunnable) {
		//Preparing to serialize the request
		try(ByteArrayOutputStream trgt = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(trgt);
			ByteArrayOutputStream trgtSec = new ByteArrayOutputStream(); ObjectOutputStream outSec = new ObjectOutputStream(trgtSec)) {
			//Adding the data
			out.writeShort(requestID); //Request ID
			out.writeInt(ConnectionManager.attachmentChunkSize); //Chunk size
			
			outSec.writeUTF(attachmentGUID); //File GUID
			outSec.flush();
			
			writeEncrypted(trgtSec.toByteArray(), out, ConnectionManager.password); //Encrypted data
			
			out.flush();
			
			//Sending the message
			return communicationsManager.queuePacket(new PacketStructOut(nhtAttachmentReq, trgt.toByteArray(), sentRunnable));
		} catch(IOException | GeneralSecurityException exception) {
			//Printing the stack trace
			exception.printStackTrace();
			FirebaseCrashlytics.getInstance().recordException(exception);
			
			//Returning false
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
			for(ConversationInfoRequest conversationInfoRequest : list)
				guidList.add(conversationInfoRequest.getConversationInfo().getGuid());
		}
		
		//Requesting information on new conversations
		try(ByteArrayOutputStream trgt = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(trgt);
			ByteArrayOutputStream trgtSec = new ByteArrayOutputStream(); ObjectOutputStream outSec = new ObjectOutputStream(trgtSec)) {
			outSec.writeInt(guidList.size());
			for(String item : guidList) outSec.writeUTF(item);
			outSec.flush();
			
			writeEncrypted(trgtSec.toByteArray(), out, ConnectionManager.password); //Encrypted data
			
			out.flush();
			
			//Sending the message
			communicationsManager.queuePacket(new PacketStructOut(nhtConversationUpdate, trgt.toByteArray()));
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
	boolean uploadFilePacket(short requestID, int requestIndex, String conversationGUID, byte[] data, String fileName, boolean isLast) {
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
			
			writeEncrypted(trgtSec.toByteArray(), out, ConnectionManager.password); //Encrypted data
			
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
		communicationsManager.queuePacket(new PacketStructOut(nhtSendFileExisting, packetData));
		
		//Returning true
		return true;
	}
	
	@Override
	boolean uploadFilePacket(short requestID, int requestIndex, String[] conversationMembers, byte[] data, String fileName, String service, boolean isLast) {
		//Returning false if there is no connection thread
		if(!communicationsManager.isConnectionOpened()) return false;
		
		//Adding the data
		byte[] packetData;
		try(ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos);
			ByteArrayOutputStream trgtSec = new ByteArrayOutputStream(); ObjectOutputStream outSec = new ObjectOutputStream(trgtSec)) {
			out.writeShort(requestID); //Request identifier
			out.writeInt(requestIndex); //Request index
			out.writeBoolean(isLast); //Is last message
			
			outSec.writeInt(conversationMembers.length); //Chat members
			for(String item : conversationMembers) outSec.writeUTF(item);
			outSec.writeInt(data.length); //File bytes
			outSec.write(data);
			if(requestIndex == 0) {
				outSec.writeUTF(fileName); //File name
				outSec.writeUTF(service); //Service
			}
			outSec.flush();
			
			writeEncrypted(trgtSec.toByteArray(), out, ConnectionManager.password); //Encrypted data
			
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
		communicationsManager.queuePacket(new PacketStructOut(nhtSendFileNew, packetData));
		
		//Returning true
		return true;
	}
	
	@Override
	boolean requestRetrievalTime(long timeLower, long timeUpper) {
		//Returning false if there is no connection thread
		if(!communicationsManager.isConnectionOpened()) return false;
		
		//Sending the message
		communicationsManager.queuePacket(new PacketStructOut(nhtTimeRetrieval, ByteBuffer.allocate(Long.SIZE / 8 * 2).putLong(timeLower).putLong(timeUpper).array()));
		
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
	
	byte[] readEncrypted(ObjectInputStream stream, String password) throws IOException, GeneralSecurityException {
		//Reading the data
		byte[] salt = new byte[encryptionSaltLen];
		stream.readFully(salt);
		
		byte[] iv = new byte[encryptionIvLen];
		stream.readFully(iv);
		
		byte[] data = new byte[stream.readInt()];
		stream.readFully(data);
		
		//Creating the key
		SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(encryptionKeyFactoryAlgorithm, MainApplication.getSecurityProvider());
		KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, encryptionKeyIterationCount, encryptionKeyLength);
		SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
		SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getEncoded(), encryptionKeyAlgorithm);
		
		//Creating the IV
		GCMParameterSpec gcmSpec = new GCMParameterSpec(encryptionKeyLength, iv);
		
		//Creating the cipher
		Cipher cipher = Cipher.getInstance(encryptionCipherTransformation, MainApplication.getSecurityProvider());
		cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmSpec);
		
		//Deciphering the data
		byte[] block = cipher.doFinal(data);
		return block;
	}
	
	void writeEncrypted(byte[] block, ObjectOutputStream stream, String password) throws IOException, GeneralSecurityException {
		SecureRandom random = ConnectionManager.getSecureRandom();
		
		//Generating a salt
		byte[] salt = new byte[encryptionSaltLen];
		random.nextBytes(salt);
		
		//Creating the key
		SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(encryptionKeyFactoryAlgorithm, MainApplication.getSecurityProvider());
		KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, encryptionKeyIterationCount, encryptionKeyLength);
		SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
		SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getEncoded(), encryptionKeyAlgorithm);
		
		//Generating the IV
		byte[] iv = new byte[encryptionIvLen];
		random.nextBytes(iv);
		GCMParameterSpec gcmSpec = new GCMParameterSpec(encryptionKeyLength, iv);
		
		Cipher cipher = Cipher.getInstance(encryptionCipherTransformation, MainApplication.getSecurityProvider());
		cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmSpec);
		
		//Encrypting the data
		byte[] data = cipher.doFinal(block);
		
		//Writing the data
		stream.write(salt);
		stream.write(iv);
		stream.writeInt(data.length);
		stream.write(data);
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
	
	private static int convertTapbackToPrivateCode(int publicCode) {
		//Returning the associated version
		switch(publicCode) {
			case 0:
				return TapbackInfo.tapbackHeart;
			case 1:
				return TapbackInfo.tapbackLike;
			case 2:
				return TapbackInfo.tapbackDislike;
			case 3:
				return TapbackInfo.tapbackLaugh;
			case 4:
				return TapbackInfo.tapbackExclamation;
			case 5:
				return TapbackInfo.tapbackQuestion;
			default:
				return -1;
		}
	}
}