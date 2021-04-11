package me.tagavari.airmessage.helper;

import android.content.Context;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.Nullable;

import com.klinker.android.send_message.Message;
import com.klinker.android.send_message.Transaction;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.CheckReturnValue;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.reactivex.rxjava3.schedulers.Schedulers;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.connection.exception.AMRequestException;
import me.tagavari.airmessage.constants.RegexConstants;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.enums.MessageSendErrorCode;
import me.tagavari.airmessage.enums.MessageState;
import me.tagavari.airmessage.enums.ServiceHandler;
import me.tagavari.airmessage.messaging.AttachmentInfo;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.FileDraft;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.redux.ReduxEventAttachmentUpload;
import me.tagavari.airmessage.task.MessageActionTask;

public class MessageSendHelper {
	/**
	 * Handles preparing a message, sending the message, and emitting relevant updates
	 * @param context The context to use
	 * @param conversationInfo The conversation of the message
	 * @param messageText The message body
	 * @param draftList The list of drafts to send
	 * @param connectionManager The connection manager to use, or NULL if unavailable
	 * @return An completable to represent this request
	 */
	@CheckReturnValue
	public static Completable prepareSendMessages(Context context, ConversationInfo conversationInfo, @Nullable String messageText, List<FileDraft> draftList, @Nullable ConnectionManager connectionManager) {
		return MessageSendHelper.prepareMessages(context, conversationInfo, messageText, draftList)
				.flatMapCompletable(message -> MessageSendHelper.sendMessage(context, conversationInfo, message, connectionManager));
	}
	
	/**
	 * Writes a message body and files to disk and prepares messages to be sent
	 * @param context The context to use
	 * @param conversationInfo The conversation of the message
	 * @param messageText The message body
	 * @param draftList The list of drafts to send
	 * @return An observable representing the messages to send
	 */
	@CheckReturnValue
	public static Observable<MessageInfo> prepareMessages(Context context, ConversationInfo conversationInfo, @Nullable String messageText, List<FileDraft> draftList) {
		return Observable.fromIterable(draftList)
				.observeOn(Schedulers.io())
				//Move the drafts to attachments
				.map(draft -> {
					File attachmentFile = moveDraftToAttachment(context, draft);
					if(attachmentFile == null) {
						//Delete the draft file and ignore
						AttachmentStorageHelper.deleteContentFile(AttachmentStorageHelper.dirNameDraft, draft.getFile());
						return Optional.<AttachmentInfo>empty();
					}
					
					return Optional.of(new AttachmentInfo(-1, null, draft.getFileName(), draft.getFileType(), draft.getFileSize(), -1, attachmentFile));
				}).filter(Optional::isPresent).map(Optional::get)
				.toList().flatMapObservable(attachmentList -> {
					if(conversationInfo.getServiceHandler() == ServiceHandler.appleBridge) {
						return prepareMessageApple(messageText, attachmentList);
					} else {
						return prepareMessageStandard(messageText, attachmentList).toObservable();
					}
				})
				.toList()
				//Write the items to the database and emit an update
				.flatMap(messages -> MessageActionTask.writeMessages(conversationInfo, messages))
				.flatMapObservable(Observable::fromIterable)
				.observeOn(AndroidSchedulers.mainThread());
	}
	
	/**
	 * Prepares a message to be sent over AirMessage Bridge
	 */
	@CheckReturnValue
	public static Observable<MessageInfo> prepareMessageApple(String messageText, List<AttachmentInfo> attachmentList) {
		return Observable.create((ObservableEmitter<MessageInfo> emitter) -> {
			if(messageText != null) {
				List<String> messageTextList = new ArrayList<>();
				
				//Checking for a straight line of pure URLS
				Matcher matcher = RegexConstants.messageURLGroup.matcher(messageText);
				if(matcher.find()) {
					//Adding the URLs
					messageTextList.addAll(Arrays.asList(messageText.split("\\s")));
				} else {
					//Checking for a single URL
					matcher = RegexConstants.messageURLSandwich.matcher(messageText);
					if(matcher.find()) {
						String prefix = matcher.group(1);
						if(prefix != null) prefix = prefix.trim();
						boolean prefixOK = !TextUtils.isEmpty(prefix);
						String url = matcher.group(2);
						String suffix = matcher.group(3);
						if(suffix != null) suffix = suffix.trim();
						boolean suffixOK = !TextUtils.isEmpty(suffix);
						
						if(prefixOK && suffixOK) {
							//Just add the entire message if there is both a prefix and a suffix, Apple Messages doesn't do anything special in this case
							messageTextList.add(messageText);
						} else {
							//Add each message part separately
							if(prefixOK) messageTextList.add(prefix.trim());
							messageTextList.add(url);
							if(suffixOK) messageTextList.add(suffix.trim());
						}
					} else {
						messageTextList.add(messageText);
					}
				}
				
				//Adding the text messages
				for(String message : messageTextList) emitter.onNext(MessageInfo.blankFromText(message));
			}
			
			//Adding the attachments
			for(AttachmentInfo attachment : attachmentList) {
				emitter.onNext(new MessageInfo(-1, -1, null, System.currentTimeMillis(), null, null, null, new ArrayList<>(Collections.singletonList(attachment)), null, false, -1, MessageState.ghost, MessageSendErrorCode.none, false));
			}
			
			emitter.onComplete();
		});
	}
	
	/**
	 * Prepares a message to be sent over a standard messaging protocol
	 */
	@CheckReturnValue
	public static Single<MessageInfo> prepareMessageStandard(String messageText, List<AttachmentInfo> attachmentList) {
		return Single.create((SingleEmitter<MessageInfo> emitter) -> {
			emitter.onSuccess(new MessageInfo(-1, -1, null, System.currentTimeMillis(), null, messageText, null, new ArrayList<>(attachmentList), null, false, -1, MessageState.ghost, MessageSendErrorCode.none, false));
		});
	}
	
	/**
	 * Sends a message
	 * @param context The context to use
	 * @param conversationInfo The message's conversation
	 * @param messageInfo The message to send
	 * @param connectionManager The connection manager to use (or NULL if unavailable)
	 * @return A completable to represent this request
	 */
	@CheckReturnValue
	public static Completable sendMessage(Context context, ConversationInfo conversationInfo, MessageInfo messageInfo, @Nullable ConnectionManager connectionManager) {
		return Single.just(messageInfo).flatMapMaybe(message -> {
			if(conversationInfo.getServiceHandler() == ServiceHandler.appleBridge) {
				if(connectionManager != null) {
					//Send the messages over the connection
					return sendMessageAMBridge(conversationInfo, message, connectionManager)
							.onErrorReturn(error -> {
								if(error instanceof AMRequestException) {
									return new Pair<>(message, (AMRequestException) error);
								} else {
									return new Pair<>(message, new AMRequestException(MessageSendErrorCode.localUnknown, error));
								}
							});
				} else {
					//Fail immediately
					return Maybe.just(new Pair<>(message, new AMRequestException(MessageSendErrorCode.localNetwork)));
				}
			} else {
				//Send the messages over SMS / MMS
				return sendMessageMMSSMS(context, conversationInfo, message)
						.onErrorReturn(error -> new Pair<>(message, (AMRequestException) error));
			}
		}).flatMapCompletable(errorDetails -> {
			//Updating the message's state on fail
			AMRequestException requestException = errorDetails.second;
			return MessageActionTask.updateMessageErrorCode(conversationInfo, errorDetails.first, requestException.getErrorCode(), requestException.getErrorDetails()).onErrorComplete();
		});
	}
	
	/**
	 * Sends a message over AirMessage bridge
	 * @param conversationInfo The message's conversation
	 * @param messageInfo The message to send
	 * @param connectionManager The connection manager to use (or NULL if unavailable)
	 * @return A completable to represent this request
	 */
	@CheckReturnValue
	public static Completable sendMessageAMBridge(ConversationInfo conversationInfo, MessageInfo messageInfo, ConnectionManager connectionManager) {
		if(messageInfo.getMessageText() != null) {
			return connectionManager.sendMessage(conversationInfo.getConversationTarget(), messageInfo.getMessageText());
		} else if(messageInfo.getAttachments().size() == 1) {
			return connectionManager.sendFile(conversationInfo.getConversationTarget(), messageInfo.getAttachments().get(0).getFile())
					.flatMap(event -> {
						if(event instanceof ReduxEventAttachmentUpload.Complete) {
							//Updating the attachment's checksum on disk
							return Completable.fromAction(() -> {
								byte[] checksum = ((ReduxEventAttachmentUpload.Complete) event).getFileHash();
								DatabaseManager.getInstance().updateAttachmentChecksum(messageInfo.getAttachments().get(0).getLocalID(), checksum);
							}).subscribeOn(Schedulers.single()).observeOn(AndroidSchedulers.mainThread()).andThen(Observable.empty());
						} else {
							return Observable.empty();
						}
					}).ignoreElements();
		} else {
			return Completable.error(new IllegalArgumentException("Cannot send message of this type"));
		}
	}
	
	/**
	 * Sends a message over SMS / MMS
	 * @param context The context to use
	 * @param conversationInfo The message's conversation
	 * @param messageInfo The message to send
	 * @return A completable to represent this request
	 */
	@CheckReturnValue
	public static Completable sendMessageMMSSMS(Context context, ConversationInfo conversationInfo, MessageInfo messageInfo) {
		return Completable.fromAction(() -> {
			//Configuring the message settings
			Transaction transaction = MMSSMSHelper.getMMSSMSTransaction(context, conversationInfo.getLocalID(), messageInfo.getLocalID());
			
			//Creating the message
			Message message = new Message();
			
			//Setting the message recipients
			message.setAddresses(conversationInfo.getMembers().stream().map(member -> AddressHelper.normalizeAddress(member.getAddress())).toArray(String[]::new));
			
			//Setting the message text
			String messageText = messageInfo.getMessageText();
			if(messageText != null) message.setText(messageText);
			String messageSubject = messageInfo.getMessageSubject();
			if(messageSubject != null) message.setSubject(messageSubject);
			
			//Adding the file to the message
			for(AttachmentInfo attachment : messageInfo.getAttachments()) {
				try(InputStream inputStream = new BufferedInputStream(new FileInputStream(attachment.getFile())); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
					DataStreamHelper.copyStream(inputStream, outputStream);
					
					message.addMedia(outputStream.toByteArray(), attachment.getContentType(), attachment.getFileName());
				}
			}
			
			//Sending the message
			transaction.sendNewMessage(message, conversationInfo.getExternalID());
		});
	}
	
	/**
	 * Moves a draft file to the attachment directory
	 * @param context The context to use
	 * @param fileDraft The draft file to move
	 * @return The attachment file that the draft was moved to
	 */
	private static File moveDraftToAttachment(Context context, FileDraft fileDraft) {
		File draftFile = fileDraft.getFile();
		File targetFile = AttachmentStorageHelper.prepareContentFile(context, AttachmentStorageHelper.dirNameAttachment, fileDraft.getFileName());
		
		boolean result = draftFile.renameTo(targetFile);
		if(!result) {
			AttachmentStorageHelper.deleteContentFile(AttachmentStorageHelper.dirNameAttachment, targetFile);
			return null;
		}
		AttachmentStorageHelper.deleteContentFile(AttachmentStorageHelper.dirNameDraft, draftFile);
		
		return targetFile;
	}
}