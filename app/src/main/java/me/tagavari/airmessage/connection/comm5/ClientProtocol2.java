package me.tagavari.airmessage.connection.comm5;

import android.os.Build;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.security.DigestInputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.DeflaterInputStream;
import java.util.zip.InflaterOutputStream;

import io.reactivex.rxjava3.core.Observable;
import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.common.Blocks;
import me.tagavari.airmessage.connection.DataProxy;
import me.tagavari.airmessage.connection.MassRetrievalParams;
import me.tagavari.airmessage.connection.encryption.EncryptionAES;
import me.tagavari.airmessage.connection.exception.AMRequestException;
import me.tagavari.airmessage.constants.MIMEConstants;
import me.tagavari.airmessage.data.SharedPreferencesManager;
import me.tagavari.airmessage.enums.AttachmentReqErrorCode;
import me.tagavari.airmessage.enums.ChatCreateErrorCode;
import me.tagavari.airmessage.enums.ConnectionErrorCode;
import me.tagavari.airmessage.enums.ConnectionFeature;
import me.tagavari.airmessage.enums.GroupAction;
import me.tagavari.airmessage.enums.MessageSendErrorCode;
import me.tagavari.airmessage.enums.MessageState;
import me.tagavari.airmessage.enums.TapbackType;
import me.tagavari.airmessage.helper.LookAheadStreamIterator;
import me.tagavari.airmessage.helper.StandardCompressionHelper;
import me.tagavari.airmessage.helper.StringHelper;
import me.tagavari.airmessage.redux.ReduxEventAttachmentUpload;
import me.tagavari.airmessage.util.CompoundErrorDetails;
import me.tagavari.airmessage.util.ConversationTarget;

//https://trello.com/c/lRZ6cikc
class ClientProtocol2 extends ProtocolManager<EncryptedPacket> {
	private static final String hashAlgorithm = "MD5";
	private static final String platformID = "android";
	
	private static final int attachmentChunkSize = 1024 * 1024; //1 MB
	
	//Top-level net header type values
	private static final int nhtClose = 0;
	private static final int nhtPing = 1;
	private static final int nhtPong = 2;
	
	private static final int nhtAuthentication = 101;
	
	private static final int nhtMessageUpdate = 200;
	private static final int nhtTimeRetrieval = 201;
	private static final int nhtIDRetrieval = 202;
	private static final int nhtMassRetrieval = 203;
	private static final int nhtMassRetrievalFile = 204;
	private static final int nhtMassRetrievalFinish = 205;
	private static final int nhtConversationUpdate = 206;
	private static final int nhtModifierUpdate = 207;
	private static final int nhtAttachmentReq = 208;
	private static final int nhtAttachmentReqConfirm = 209;
	private static final int nhtAttachmentReqFail = 210;
	private static final int nhtIDUpdate = 211;
	
	private static final int nhtLiteConversationRetrieval = 300;
	private static final int nhtLiteThreadRetrieval = 301;
	
	private static final int nhtSendResult = 400;
	private static final int nhtSendTextExisting = 401;
	private static final int nhtSendTextNew = 402;
	private static final int nhtSendFileExisting = 403;
	private static final int nhtSendFileNew = 404;
	private static final int nhtCreateChat = 405;
	
	//Net return codes
	private static final int nrcSharedOK = 0;
	
	//Item sub-types
	private static final int nstMessageStateIdle = 0;
	private static final int nstMessageStateSent = 1;
	private static final int nstMessageStateDelivered = 2;
	private static final int nstMessageStateRead = 3;
	
	private static final int nstItemMessage = 0;
	private static final int nstItemGroupAction = 1;
	private static final int nstItemChatRename = 2;
	
	private static final int nstGroupActionUnknown = 0;
	private static final int nstGroupActionJoin = 1;
	private static final int nstGroupActionLeave = 2;
	
	private static final int nstModifierActivity = 0;
	private static final int nstModifierSticker = 1;
	private static final int nstModifierTapback = 2;
	
	private short lastMassRetrievalRequestID = -1;
	
	ClientProtocol2(ClientComm5 communicationsManager, DataProxy<EncryptedPacket> dataProxy) {
		super(communicationsManager, dataProxy);
	}
	
	private boolean queueHeaderOnlyPacket(int header, boolean shouldEncrypt) {
		try(AirPacker packer = AirPacker.get()) {
			packer.packInt(header);
			
			dataProxy.send(new EncryptedPacket(packer.toByteArray(), shouldEncrypt));
			
			return true;
		} catch(BufferOverflowException exception) {
			exception.printStackTrace();
			
			return false;
		}
	}
	
	@Override
	void processData(byte[] data, boolean wasEncrypted) {
		//Wrapping the data in an unpacker
		AirUnpacker unpacker = new AirUnpacker(data);
		try {
			//Reading the message type
			int messageType = unpacker.unpackInt();
			
			//Checking if this data is insecure
			if(!wasEncrypted) {
				processDataInsecure(messageType, unpacker);
			} else {
				//This data is always encrypted
				processDataSecure(messageType, unpacker);
			}
		} catch(BufferUnderflowException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	boolean sendPing() {
		return queueHeaderOnlyPacket(nhtPing, false);
	}
	
	/**
	 * Process data that doesn't need to be encrypted
	 * @param messageType The message header
	 * @param unpacker The message data
	 * @return TRUE if this message was consumed
	 */
	private boolean processDataInsecure(int messageType, AirUnpacker unpacker) {
		switch(messageType) {
			case nhtClose:
				//Closing the connection
				communicationsManager.getHandler().post(() -> communicationsManager.disconnect(ConnectionErrorCode.connection));
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
	 * This method also invokes {@link #processDataInsecure(int, AirUnpacker)} method
	 * @param messageType The message header
	 * @param unpacker The message data
	 * @return TRUE if this message was consumed
	 */
	private boolean processDataSecure(int messageType, AirUnpacker unpacker) {
		if(processDataInsecure(messageType, unpacker)) return true;
		
		switch(messageType) {
			case nhtMessageUpdate:
			case nhtTimeRetrieval:
			case nhtIDRetrieval:
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
			case nhtIDUpdate:
				handleMessageIDUpdate(unpacker);
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
	
	private void handleMessageAuthentication(AirUnpacker unpacker) throws BufferUnderflowException {
		//Stopping the authentication timer
		communicationsManager.stopTimeoutTimer();
		
		//Reading the result code
		int resultCode = unpacker.unpackInt();
		
		//Checking if the result was successful
		if(resultCode == nrcSharedOK) {
			//Reading the server's information
			String installationID = unpacker.unpackString();
			String deviceName = unpacker.unpackString();
			String systemVersion = unpacker.unpackString();
			String softwareVersion = unpacker.unpackString();
			
			//Finishing the connection establishment
			communicationsManager.getHandler().post(() -> communicationsManager.onHandshake(installationID, deviceName, systemVersion, softwareVersion));
		} else {
			//Otherwise terminating the connection
			communicationsManager.getHandler().post(() -> communicationsManager.disconnect(mapNRCAuthenticationCode(resultCode)));
		}
	}
	
	private void handleMessageMessageUpdate(AirUnpacker unpacker) throws BufferUnderflowException {
		List<Blocks.ConversationItem> conversationItems = unpackConversationItems(unpacker);
		communicationsManager.runListener(listener -> listener.onMessageUpdate(conversationItems));
	}
	
	private void handleMessageMassRetrieval(AirUnpacker unpacker) {
		//Reading the packet information
		short requestID = unpacker.unpackShort();
		int packetIndex = unpacker.unpackInt();
		
		//Checking if this is the first packet
		if(packetIndex == 0) {
			//Reading the conversation list and message count
			List<Blocks.ConversationInfo> conversations = unpackConversations(unpacker);
			int messageCount = unpacker.unpackInt();
			
			//Registering the mass retrieval manager
			communicationsManager.runListener(listener -> listener.onMassRetrievalStart(requestID, conversations, messageCount));
			
			//Recording the request ID (to simulate a newer protocol version for future updates)
			lastMassRetrievalRequestID = requestID;
		} else {
			//Reading the item list
			List<Blocks.ConversationItem> conversationItems = unpackConversationItems(unpacker);
			
			//Processing the packet
			communicationsManager.runListener(listener -> listener.onMassRetrievalUpdate(requestID, packetIndex, conversationItems));
		}
	}
	
	private void handleMessageMassRetrievalFinish(AirUnpacker unpacker) {
		//Finishing the mass retrieval
		communicationsManager.runListener(listener -> listener.onMassRetrievalComplete(lastMassRetrievalRequestID));
	}
	
	private void handleMessageMassRetrievalFile(AirUnpacker unpacker) {
		//Reading the data
		short requestID = unpacker.unpackShort();
		int requestIndex = unpacker.unpackInt();
		String fileName = requestIndex == 0 ? unpacker.unpackString() : null;
		boolean isLast = unpacker.unpackBoolean();
		
		String fileGUID = unpacker.unpackString();
		byte[] fileData = unpacker.unpackPayload();
		
		//Processing the data
		communicationsManager.runListener(listener -> {
			if(requestIndex == 0) listener.onMassRetrievalFileStart(requestID, fileGUID, fileName, InflaterOutputStream::new);
			listener.onMassRetrievalFileProgress(requestID, requestIndex, fileGUID, fileData);
			if(isLast) listener.onMassRetrievalFileComplete(requestID, fileGUID);
		});
	}
	
	private void handleMessageConversationUpdate(AirUnpacker unpacker) {
		//Reading the data
		List<Blocks.ConversationInfo> conversations = unpackConversations(unpacker);
		
		//Processing the conversations
		communicationsManager.runListener(listener -> listener.onConversationUpdate(conversations));
	}
	
	private void handleMessageModifierUpdate(AirUnpacker unpacker) {
		//Reading the data
		List<Blocks.ModifierInfo> modifiers = unpackModifiers(unpacker);
		
		//Processing the conversations
		communicationsManager.runListener(listener -> listener.onModifierUpdate(modifiers));
	}
	
	private void handleMessageAttachmentRequest(AirUnpacker unpacker) {
		//Reading the data
		short requestID = unpacker.unpackShort();
		int requestIndex = unpacker.unpackInt();
		long fileLength = requestIndex == 0 ? unpacker.unpackLong() : -1;
		boolean isLast = unpacker.unpackBoolean();
		
		String fileGUID = unpacker.unpackString();
		byte[] fileData = unpacker.unpackPayload();
		
		//Forwarding the data to the listeners
		communicationsManager.runListener(listener -> {
			if(requestIndex == 0) listener.onFileRequestStart(requestID, fileLength, InflaterOutputStream::new);
			listener.onFileRequestData(requestID, requestIndex, fileData);
			if(isLast) listener.onFileRequestComplete(requestID);
		});
	}
	
	private void handleMessageAttachmentRequestConfirm(AirUnpacker unpacker) {
		//Reading the request ID
		short requestID = unpacker.unpackShort();
	}
	
	private void handleMessageAttachmentRequestFail(AirUnpacker unpacker) {
		//Reading the data
		short requestID = unpacker.unpackShort();
		int errorCode = mapNRCAttachmentReqCode(unpacker.unpackInt());
		
		communicationsManager.runListener(listener -> listener.onFileRequestFail(requestID, errorCode));
	}
	
	private void handleMessageIDUpdate(AirUnpacker unpacker) {
		//Reading the data
		long messageID = unpacker.unpackLong();
		
		communicationsManager.runListener(listener -> listener.onIDUpdate(messageID));
	}
	
	private void handleMessageSendResult(AirUnpacker unpacker) {
		//Reading the data
		short requestID = unpacker.unpackShort();
		int resultCode = unpacker.unpackInt();
		String details = unpacker.unpackNullableString();
		
		if(resultCode == nrcSharedOK) {
			communicationsManager.runListener(listener -> listener.onSendMessageSuccess(requestID));
		} else {
			communicationsManager.runListener(listener -> listener.onSendMessageFail(requestID, new CompoundErrorDetails.MessageSend(mapNRCSendCode(resultCode), details)));
		}
	}
	
	private void handleMessageCreateChat(AirUnpacker unpacker) {
		//Reading the data
		short requestID = unpacker.unpackShort();
		int resultCode = unpacker.unpackInt();
		String details = unpacker.unpackNullableString();
		
		communicationsManager.runListener(listener -> {
			if(details == null) {
				//This shouldn't happen
				listener.onCreateChatError(requestID, new CompoundErrorDetails.ChatCreate(ChatCreateErrorCode.network, null));
			} if(resultCode == nrcSharedOK) {
				listener.onCreateChatSuccess(requestID, details);
			} else {
				listener.onCreateChatError(requestID, new CompoundErrorDetails.ChatCreate(mapNRCCreateChatCode(resultCode), details));
			}
		});
	}
	
	@Override
	boolean requestChatCreation(short requestID, String[] chatMembers, String service) {
		//Returning false if there is no connection thread
		if(!communicationsManager.isConnectionOpened()) return false;
		
		//Sending the message
		try(AirPacker packer = AirPacker.get()) {
			packer.packInt(nhtCreateChat);
			
			packer.packShort(requestID);
			packer.packArrayHeader(chatMembers.length);
			for(String item : chatMembers) packer.packString(item);
			packer.packString(service);
			
			dataProxy.send(new EncryptedPacket(packer.toByteArray(), true));
			return true;
		} catch(BufferOverflowException exception) {
			exception.printStackTrace();
			FirebaseCrashlytics.getInstance().recordException(exception);
			return false;
		}
	}
	
	@Override
	boolean requestRetrievalAll(short requestID, MassRetrievalParams params) {
		//Returning false if there is no connection thread
		if(!communicationsManager.isConnectionOpened()) return false;
		
		//Sending the message
		try(AirPacker packer = AirPacker.get()) {
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
			
			dataProxy.send(new EncryptedPacket(packer.toByteArray(), true));
			return true;
		} catch(BufferOverflowException exception) {
			exception.printStackTrace();
			FirebaseCrashlytics.getInstance().recordException(exception);
			return false;
		}
	}
	
	@Override
	boolean sendAuthenticationRequest(AirUnpacker unpacker) {
		//Returning false if there is no connection thread
		if(!communicationsManager.isConnectionOpened()) return false;
		
		//Assembling the device information
		String installationID = SharedPreferencesManager.getInstallationID(MainApplication.getInstance());
		String clientName = Build.MANUFACTURER + ' ' + Build.MODEL;
		String platformID = ClientProtocol2.platformID;
		
		//Checking if the current protocol requires authentication
		if(unpacker.unpackBoolean()) {
			//Reading the transmission check
			byte[] transmissionCheck;
			try {
				transmissionCheck = unpacker.unpackPayload();
			} catch(BufferUnderflowException exception) {
				exception.printStackTrace();
				return false;
			}
			
			//Writing back the transmission check and information about this device
			try(AirPacker packer = AirPacker.get()) {
				packer.packInt(nhtAuthentication);
				
				byte[] secureData;
				{
					AirPacker securePacker = new AirPacker(1024);
					securePacker.packPayload(transmissionCheck); //Transmission check
					securePacker.packString(installationID); //Installation ID
					securePacker.packString(clientName); //Client name
					securePacker.packString(platformID); //Platform ID
					
					//Encrypting the data
					secureData = new EncryptionAES(communicationsManager.getPassword()).encrypt(securePacker.toByteArray());
				}
				
				packer.packPayload(secureData);
				
				dataProxy.send(new EncryptedPacket(packer.toByteArray(), false));
				return true;
			} catch(BufferOverflowException | GeneralSecurityException exception) {
				exception.printStackTrace();
				FirebaseCrashlytics.getInstance().recordException(exception);
				return false;
			}
		} else {
			//Writing back the device information
			try(AirPacker packer = AirPacker.get()) {
				packer.packInt(nhtAuthentication);
				packer.packString(installationID); //Installation ID
				packer.packString(clientName); //Client name
				packer.packString(platformID); //Platform ID
				
				dataProxy.send(new EncryptedPacket(packer.toByteArray(), false));
				return true;
			} catch(BufferOverflowException exception) {
				exception.printStackTrace();
				FirebaseCrashlytics.getInstance().recordException(exception);
				return false;
			}
		}
	}
	
	@Override
	boolean sendMessage(short requestID, ConversationTarget conversation, String message) {
		//Returning false if there is no open connection
		if(!communicationsManager.isConnectionOpened()) return false;
		
		try(AirPacker packer = AirPacker.get()) {
			//Message type
			if(conversation instanceof ConversationTarget.AppleLinked) {
				packer.packInt(nhtSendTextExisting);
			} else if(conversation instanceof ConversationTarget.AppleUnlinked) {
				packer.packInt(nhtSendTextNew);
			}
			
			packer.packShort(requestID); //Request ID
			
			if(conversation instanceof ConversationTarget.AppleLinked) {
				packer.packString(((ConversationTarget.AppleLinked) conversation).getGuid()); //Chat GUID
			} else if(conversation instanceof ConversationTarget.AppleUnlinked) {
				ConversationTarget.AppleUnlinked unlinked = ((ConversationTarget.AppleUnlinked) conversation);
				packer.packArrayHeader(unlinked.getMembers().size()); //Members
				for(String item : unlinked.getMembers()) packer.packString(item);
				packer.packString(unlinked.getService()); //Service
			}
			
			packer.packString(message); //Message
			
			dataProxy.send(new EncryptedPacket(packer.toByteArray(), true));
			return true;
		} catch(BufferOverflowException exception) {
			exception.printStackTrace();
			FirebaseCrashlytics.getInstance().recordException(exception);
			return false;
		}
	}
	
	@Override
	public Observable<ReduxEventAttachmentUpload> sendFile(short requestID, ConversationTarget conversation, File file) {
		return Observable.create((emitter) -> {
			try {
				MessageDigest messageDigest = MessageDigest.getInstance(hashAlgorithm);
				try(InputStream inputStream = new DeflaterInputStream(new DigestInputStream(new BufferedInputStream(new FileInputStream(file)), messageDigest))) {
					long totalLength = inputStream.available();
					long totalBytesRead = 0;
					int requestIndex = 0;
					
					for(LookAheadStreamIterator iterator = new LookAheadStreamIterator(attachmentChunkSize, inputStream); iterator.hasNext();) {
						LookAheadStreamIterator.ForwardsStreamData data = iterator.next();
						
						//Adding to the total bytes read
						totalBytesRead += data.getLength();
						
						//Uploading the file part
						try(AirPacker packer = AirPacker.get()) {
							//Message type
							if(conversation instanceof ConversationTarget.AppleLinked) {
								packer.packInt(nhtSendFileExisting);
							} else if(conversation instanceof ConversationTarget.AppleUnlinked) {
								packer.packInt(nhtSendFileNew);
							}
							
							packer.packShort(requestID); //Request identifier
							packer.packInt(requestIndex); //Request index
							packer.packBoolean(data.isLast()); //Is last message
							
							if(conversation instanceof ConversationTarget.AppleLinked) {
								packer.packString(((ConversationTarget.AppleLinked) conversation).getGuid()); //Chat GUID
							} else if(conversation instanceof ConversationTarget.AppleUnlinked) {
								ConversationTarget.AppleUnlinked unlinked = ((ConversationTarget.AppleUnlinked) conversation);
								packer.packArrayHeader(unlinked.getMembers().size()); //Members
								for(String item : unlinked.getMembers()) packer.packString(item);
							}
							
							packer.packPayload(data.getData(), data.getLength()); //File bytes
							if(requestIndex == 0) {
								packer.packString(file.getName()); //File name
								if(conversation instanceof ConversationTarget.AppleUnlinked) {
									packer.packString(((ConversationTarget.AppleUnlinked) conversation).getService()); //Service
								}
							}
							
							dataProxy.send(new EncryptedPacket(packer.toByteArray(), true));
						}
						
						//Incrementing the index
						requestIndex++;
						
						//Updating the progress
						emitter.onNext(new ReduxEventAttachmentUpload.Progress(totalBytesRead, totalLength));
					}
				}
				
				//Finishing
				byte[] checksum = messageDigest.digest();
				emitter.onNext(new ReduxEventAttachmentUpload.Complete(checksum));
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
	
	@Override
	public boolean requestAttachmentDownload(short requestID, String attachmentGUID) {
		//Returning false if there is no open connection
		if(!communicationsManager.isConnectionOpened()) return false;
		
		try(AirPacker packer = AirPacker.get()) {
			packer.packInt(nhtAttachmentReq);
			
			packer.packShort(requestID); //Request ID
			packer.packInt(attachmentChunkSize); //Chunk size
			packer.packString(attachmentGUID); //File GUID
			
			dataProxy.send(new EncryptedPacket(packer.toByteArray(), true));
			return true;
		} catch(BufferOverflowException exception) {
			exception.printStackTrace();
			FirebaseCrashlytics.getInstance().recordException(exception);
			return false;
		}
	}
	
	@Override
	public boolean requestConversationInfo(Collection<String> conversations) {
		//Returning false if there is no connection thread
		if(!communicationsManager.isConnectionOpened()) return false;
		
		try(AirPacker packer = AirPacker.get()) {
			packer.packInt(nhtConversationUpdate);
			
			packer.packArrayHeader(conversations.size());
			for(String item : conversations) packer.packString(item);
			
			dataProxy.send(new EncryptedPacket(packer.toByteArray(), true));
			return true;
		} catch(BufferOverflowException exception) {
			exception.printStackTrace();
			FirebaseCrashlytics.getInstance().recordException(exception);
			return false;
		}
	}
	
	@Override
	boolean requestRetrievalTime(long timeLower, long timeUpper) {
		//Returning false if there is no connection thread
		if(!communicationsManager.isConnectionOpened()) return false;
		
		try(AirPacker packer = AirPacker.get()) {
			packer.packInt(nhtTimeRetrieval);
			
			packer.packLong(timeLower);
			packer.packLong(timeUpper);
			
			dataProxy.send(new EncryptedPacket(packer.toByteArray(), true));
			return true;
		} catch(BufferOverflowException exception) {
			exception.printStackTrace();
			FirebaseCrashlytics.getInstance().recordException(exception);
			return false;
		}
	}
	
	@Override
	boolean requestRetrievalID(long idSince, long timeLower, long timeUpper) {
		//Returning false if there is no connection thread
		if(!communicationsManager.isConnectionOpened()) return false;
		
		try(AirPacker packer = AirPacker.get()) {
			packer.packInt(nhtIDRetrieval);
			
			packer.packLong(idSince);
			
			dataProxy.send(new EncryptedPacket(packer.toByteArray(), true));
			return true;
		} catch(BufferOverflowException exception) {
			exception.printStackTrace();
			FirebaseCrashlytics.getInstance().recordException(exception);
			return false;
		}
	}
	
	@Override
	boolean isFeatureSupported(@ConnectionFeature int featureID) {
		return featureID == ConnectionFeature.idBasedRetrieval;
	}
	
	/**
	 * Maps an NRC authentication code to a local result code
	 */
	@ConnectionErrorCode
	private static int mapNRCAuthenticationCode(int code) {
		switch(code) {
			case 1:
				return ConnectionErrorCode.directUnauthorized;
			case 2:
				return ConnectionErrorCode.badRequest;
			default:
				return ConnectionErrorCode.connection;
		}
	}
	
	/**
	 * Maps an NRC send code to a local error code
	 */
	@MessageSendErrorCode
	private static int mapNRCSendCode(int code) {
		switch(code) {
			case 1: //Some unknown AppleScript error
				return MessageSendErrorCode.serverExternal;
			case 2: //Invalid data received
				return MessageSendErrorCode.serverBadRequest;
			case 3: //System rejected request to send message
				return MessageSendErrorCode.serverUnauthorized;
			case 4: //A valid conversation wasn't found
				return MessageSendErrorCode.serverNoConversation;
			case 5: //Request timed out
				return MessageSendErrorCode.serverRequestTimeout;
			default:
				return MessageSendErrorCode.serverUnknown;
		}
	}
	
	/**
	 * Maps an NRC attachment request code to a local error code
	 */
	@AttachmentReqErrorCode
	private static int mapNRCAttachmentReqCode(int code) {
		switch(code) {
			case 1: //File GUID not found
				return AttachmentReqErrorCode.serverNotFound;
			case 2: //File (on disk) not found
				return AttachmentReqErrorCode.serverNotSaved;
			case 3: //No access to file
				return AttachmentReqErrorCode.serverUnreadable;
			case 4: //I/O error
				return AttachmentReqErrorCode.serverIO;
			default:
				return AttachmentReqErrorCode.unknown;
		}
	}
	
	/**
	 * Maps an NRC chat creation code to a local error code
	 */
	@ChatCreateErrorCode
	private static int mapNRCCreateChatCode(int code) {
		switch(code) {
			case 1: //Some unknown AppleScript error
				return ChatCreateErrorCode.scriptError;
			case 2: //Invalid data received
				return ChatCreateErrorCode.badRequest;
			case 3: //System rejected request
				return ChatCreateErrorCode.unauthorized;
			default:
				return ChatCreateErrorCode.unknown;
		}
	}
	
	/**
	 * Maps an NRC Apple message error code to a local error code
	 */
	@MessageSendErrorCode
	private static int mapNRCAppleErrorCode(int code) {
		switch(code) {
			case 0:
				return MessageSendErrorCode.none;
			case 1: //Unknown error code
			default:
				return MessageSendErrorCode.appleUnknown;
			case 2: //Network error
				return MessageSendErrorCode.appleNetwork;
			case 3: //Not registered with iMessage
				return MessageSendErrorCode.appleUnregistered;
		}
	}
	
	/**
	 * Maps an NST message state code to a local code
	 */
	@MessageState
	private static int mapNSTMessageState(int code) {
		switch(code) {
			default:
			case nstMessageStateIdle:
				return MessageState.idle;
			case nstMessageStateSent:
				return MessageState.sent;
			case nstMessageStateDelivered:
				return MessageState.delivered;
			case nstMessageStateRead:
				return MessageState.read;
		}
	}
	
	/**
	 * Maps an NST group action code to a local code
	 */
	@GroupAction
	private static int mapNSTGroupAction(int code) {
		switch(code) {
			case nstGroupActionUnknown:
			default:
				return GroupAction.unknown;
			case nstGroupActionJoin:
				return GroupAction.join;
			case nstGroupActionLeave:
				return GroupAction.leave;
		}
	}
	
	/**
	 * Maps an Apple tapback code to a local code
	 */
	@TapbackType
	private static int mapTapbackType(int appleCode) {
		//Returning the associated version
		switch(appleCode) {
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
	
	/**
	 * Unpacks a list of conversations
	 */
	private static List<Blocks.ConversationInfo> unpackConversations(AirUnpacker unpacker) {
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
				String name = unpacker.unpackNullableString();
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
	
	/**
	 * Unpacks a list of conversation items
	 */
	private static List<Blocks.ConversationItem> unpackConversationItems(AirUnpacker unpacker) {
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
					throw new IllegalArgumentException("Invalid conversation item type: " + type);
				case nstItemMessage: {
					String text = StringHelper.nullifyEmptyString(unpacker.unpackNullableString());
					String subject = StringHelper.nullifyEmptyString(unpacker.unpackNullableString());
					String sender = StringHelper.nullifyEmptyString(unpacker.unpackNullableString());
					List<Blocks.AttachmentInfo> attachments = unpackAttachments(unpacker);
					List<Blocks.StickerModifierInfo> stickers = (List<Blocks.StickerModifierInfo>) (List<?>) unpackModifiers(unpacker);
					List<Blocks.TapbackModifierInfo> tapbacks = (List<Blocks.TapbackModifierInfo>) (List<?>) unpackModifiers(unpacker);
					String sendEffect = StringHelper.nullifyEmptyString(unpacker.unpackNullableString());
					@MessageState int stateCode = mapNSTMessageState(unpacker.unpackInt());
					@MessageSendErrorCode int errorCode = mapNRCAppleErrorCode(unpacker.unpackInt());
					long dateRead = unpacker.unpackLong();
					
					list.add(new Blocks.MessageInfo(serverID, guid, chatGuid, date, text, subject, sender, attachments, stickers, tapbacks, sendEffect, stateCode, errorCode, dateRead));
					break;
				}
				case nstItemGroupAction: {
					String agent = unpacker.unpackNullableString();
					String other = unpacker.unpackNullableString();
					int groupActionType = mapNSTGroupAction(unpacker.unpackInt());
					
					list.add(new Blocks.GroupActionInfo(serverID, guid, chatGuid, date, agent, other, groupActionType));
					break;
				}
				case nstItemChatRename: {
					String agent = unpacker.unpackNullableString();
					String newChatName = StringHelper.nullifyEmptyString(unpacker.unpackNullableString());
					
					list.add(new Blocks.ChatRenameActionInfo(serverID, guid, chatGuid, date, agent, newChatName));
					break;
				}
			}
		}
		
		//Returning the list
		return list;
	}
	
	/**
	 * Unpacks a list of attachments
	 */
	private static List<Blocks.AttachmentInfo> unpackAttachments(AirUnpacker unpacker) {
		//Reading the count
		int count = unpacker.unpackArrayHeader();
		
		//Creating the list
		List<Blocks.AttachmentInfo> list = new ArrayList<>(count);
		
		//Iterating over the items
		for(int i = 0; i < count; i++) {
			String guid = unpacker.unpackString();
			String name = unpacker.unpackString();
			String type = StringHelper.defaultEmptyString(unpacker.unpackNullableString(), MIMEConstants.defaultMIMEType);
			long size = unpacker.unpackLong();
			byte[] checksum = unpacker.unpackNullablePayload();
			long sort = unpacker.unpackLong();
			
			list.add(new Blocks.AttachmentInfo(guid, name, type, size, checksum, sort));
		}
		
		//Returning the list
		return list;
	}
	
	/**
	 * Unpacks a list of modifiers
	 */
	private static List<Blocks.ModifierInfo> unpackModifiers(AirUnpacker unpacker) {
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
					throw new IllegalArgumentException("Invalid modifier type: " + type);
				case nstModifierActivity: {
					@MessageState int state = mapNSTMessageState(unpacker.unpackInt());
					long dateRead = unpacker.unpackLong();
					
					list.add(new Blocks.ActivityStatusModifierInfo(message, state, dateRead));
					break;
				}
				case nstModifierSticker: {
					int messageIndex = unpacker.unpackInt();
					String fileGuid = unpacker.unpackString();
					String sender = unpacker.unpackNullableString();
					long date = unpacker.unpackLong();
					byte[] data = unpacker.unpackPayload();
					String fileType = unpacker.unpackString();
					
					byte[] decompressedData;
					try {
						decompressedData = StandardCompressionHelper.decompressDeflate(data);
					} catch(IOException exception) {
						exception.printStackTrace();
						
						continue;
					}
					
					list.add(new Blocks.StickerModifierInfo(message, messageIndex, fileGuid, sender, date, decompressedData, fileType));
					break;
				}
				case nstModifierTapback: {
					int messageIndex = unpacker.unpackInt();
					String sender = unpacker.unpackNullableString();
					boolean isAddition = unpacker.unpackBoolean();
					int tapbackType = unpacker.unpackInt();
					
					list.add(new Blocks.TapbackModifierInfo(message, messageIndex, sender, isAddition, mapTapbackType(tapbackType)));
					break;
				}
			}
		}
		
		//Returning the list
		return list;
	}
}