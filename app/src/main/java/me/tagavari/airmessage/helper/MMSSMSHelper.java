package me.tagavari.airmessage.helper;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.util.Pair;

import androidx.annotation.Nullable;

import com.google.android.mms.pdu_alt.EncodedStringValue;
import com.google.android.mms.pdu_alt.PduHeaders;
import com.google.android.mms.pdu_alt.PduPersister;
import com.klinker.android.send_message.Settings;
import com.klinker.android.send_message.Transaction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.reactivex.rxjava3.schedulers.Schedulers;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.activity.Messaging;
import me.tagavari.airmessage.activity.Preferences;
import me.tagavari.airmessage.constants.SMSReceiverConstants;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.enums.ConversationState;
import me.tagavari.airmessage.enums.MessageSendErrorCode;
import me.tagavari.airmessage.enums.MessageState;
import me.tagavari.airmessage.enums.ServiceHandler;
import me.tagavari.airmessage.enums.ServiceType;
import me.tagavari.airmessage.messaging.AttachmentInfo;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.MemberInfo;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.receiver.TextMMSSentReceiver;
import me.tagavari.airmessage.receiver.TextSMSDeliveredReceiver;
import me.tagavari.airmessage.receiver.TextSMSSentReceiver;
import me.tagavari.airmessage.redux.ReduxEmitterNetwork;
import me.tagavari.airmessage.redux.ReduxEventMessaging;
import me.tagavari.airmessage.util.ConversationValueUpdateResult;
import me.tagavari.airmessage.util.ReplaceInsertResult;

public class MMSSMSHelper {
	public static final String[] smsMixedColumnProjection = {Telephony.BaseMmsColumns._ID, Telephony.Mms.MESSAGE_BOX, Telephony.Sms.TYPE, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.ERROR_CODE, Telephony.Sms.STATUS, Telephony.Mms.SUBJECT};
	public static final String[] smsColumnProjection = {Telephony.Sms._ID, Telephony.Sms.TYPE, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.ERROR_CODE, Telephony.Sms.STATUS};
	public static final String[] mmsColumnProjection = {Telephony.Mms._ID, Telephony.Mms.DATE, Telephony.Mms.MESSAGE_BOX, Telephony.Mms.SUBJECT};
	public static final String[] mmsPartColumnProjection = {Telephony.Mms.Part._ID, Telephony.Mms.Part.CONTENT_TYPE, Telephony.Mms.Part.NAME, Telephony.Mms.Part._DATA, Telephony.Mms.Part.TEXT};
	
	/**
	 * Get the maximum size for an attachment to be sent over MMS
	 * @param context The context to use
	 * @return The maximum file size in bytes
	 */
	public static int getMaxMessageSize(Context context) {
		return 300 * 1024; //300 kB
		//return context.getSystemService(CarrierConfigManager.class).getConfig().getInt(CarrierConfigManager.KEY_MMS_MAX_MESSAGE_SIZE_INT);
	}
	
	/**
	 * Get a human-readable label for a contact
	 * @param resources The resources to use to retrieve the string
	 * @param mimeType The MIME type of this contact entry
	 * @param addressType The address type ID of this entry
	 * @return The label for this address type
	 */
	public static String getAddressLabel(Resources resources, String mimeType, int addressType) {
		if(mimeType.equals(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)) {
			int resourceID;
			switch(addressType) {
				case ContactsContract.CommonDataKinds.Email.TYPE_HOME:
					resourceID = R.string.label_email_home;
					break;
				case ContactsContract.CommonDataKinds.Email.TYPE_WORK:
					resourceID = R.string.label_email_work;
					break;
				case ContactsContract.CommonDataKinds.Email.TYPE_OTHER:
					resourceID = R.string.label_email_other;
					break;
				default:
					return null;
			}
			
			return resources.getString(resourceID);
		} else if(mimeType.equals(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
			int resourceID;
			switch(addressType) {
				case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
					resourceID = R.string.label_phone_mobile;
					break;
				case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
					resourceID = R.string.label_email_work;
					break;
				case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
					resourceID = R.string.label_phone_home;
					break;
				case ContactsContract.CommonDataKinds.Phone.TYPE_MAIN:
					resourceID = R.string.label_phone_main;
					break;
				case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK:
					resourceID = R.string.label_phone_workfax;
					break;
				case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME:
					resourceID = R.string.label_phone_homefax;
					break;
				case ContactsContract.CommonDataKinds.Phone.TYPE_PAGER:
					resourceID = R.string.label_phone_pager;
					break;
				case ContactsContract.CommonDataKinds.Phone.TYPE_OTHER:
					resourceID = R.string.label_phone_other;
					break;
				default:
					return null;
			}
			
			return resources.getString(resourceID);
		} else {
			return null;
		}
	}
	
	/**
	 * Creates or gets a system MMS/SMS conversation by participants
	 * @param context The context to use
	 * @param participants The participants of the conversation
	 * @return A pair of the conversation, and a boolean to indicate if this conversation is new
	 */
	public static Pair<ConversationInfo, Boolean> getOrCreateTextConversation(Context context, String[] participants) {
		//Getting the Android thread ID
		long threadID = Telephony.Threads.getOrCreateThreadId(context, new HashSet<>(Arrays.asList(participants)));
		
		//Trying to find a matching local conversation
		ConversationInfo conversationInfo = DatabaseManager.getInstance().findConversationByExternalID(context, threadID, ServiceHandler.systemMessaging, ServiceType.systemSMS);
		if(conversationInfo != null) {
			return new Pair<>(conversationInfo, false);
		}
		
		//Creating a new conversation if no existing conversation was found
		int conversationColor = ConversationColorHelper.getDefaultConversationColor(threadID);
		List<MemberInfo> coloredMembers = ConversationColorHelper.getColoredMembers(participants, conversationColor, threadID);
		conversationInfo = new ConversationInfo(-1, null, threadID, ConversationState.ready, ServiceHandler.systemMessaging, ServiceType.systemSMS, conversationColor, coloredMembers, null,0, false, false, null, null, new ArrayList<>(), -1);
		
		//Writing the conversation to disk
		boolean result = DatabaseManager.getInstance().addConversationInfo(conversationInfo);
		if(!result) return null;
		
		return new Pair<>(conversationInfo, true);
	}
	
	/**
	 * Creates or gets a system MMS/SMS conversation by its external thread ID
	 * @param context The context to use
	 * @param threadID The MMS/SMS ID of the thread
	 * @return A pair of the conversation, and a boolean to indicate if this conversation is new
	 */
	public static Pair<ConversationInfo, Boolean> getOrCreateTextConversation(Context context, long threadID) {
		//Trying to find a matching local conversation
		ConversationInfo conversationInfo = DatabaseManager.getInstance().findConversationByExternalID(context, threadID, ServiceHandler.systemMessaging, ServiceType.systemSMS);
		if(conversationInfo != null) return new Pair<>(conversationInfo, false);
		
		//Getting the conversation participants
		String recipientIDs;
		try(Cursor cursorConversation = context.getContentResolver().query(Uri.parse("content://mms-sms/conversations?simple=true"), new String[]{"*"},
				Telephony.Threads._ID + " = ?", new String[]{Long.toString(threadID)},
				null)) {
			if(cursorConversation == null || !cursorConversation.moveToFirst()) return null;
			
			recipientIDs = cursorConversation.getString(cursorConversation.getColumnIndexOrThrow(Telephony.Threads.RECIPIENT_IDS));
		}
		
		//Creating the conversation
		int conversationColor = ConversationColorHelper.getDefaultConversationColor(threadID);
		List<MemberInfo> coloredMembers = ConversationColorHelper.getColoredMembers(getAddressFromRecipientID(context, recipientIDs), conversationColor);
		conversationInfo = new ConversationInfo(-1, null, threadID, ConversationState.ready, ServiceHandler.systemMessaging, ServiceType.systemSMS, conversationColor, coloredMembers, null,0, false, false, null, null, new ArrayList<>(), -1);
		
		//Writing the conversation to disk
		boolean result = DatabaseManager.getInstance().addConversationInfo(conversationInfo);
		if(!result) return null;
		
		return new Pair<>(conversationInfo, true);
	}
	
	/**
	 * Handles the former half of a conversation-message insertion, getting the conversation from an array of participants
	 * @param context The context to use
	 * @param participants The conversation's participants
	 * @param newMessage The conversation's message
	 * @return A single for a pair of the new conversation and message
	 */
	public static Single<Pair<ConversationInfo, MessageInfo>> updateTextConversationMessage(Context context, String[] participants, MessageInfo newMessage) {
		return Single.fromCallable(() -> Objects.requireNonNull(getOrCreateTextConversation(context, participants)))
		.subscribeOn(Schedulers.single()).flatMap(pair -> updateTextConversationMessage(pair.first, pair.second, newMessage).map(message -> new Pair<>(pair.first, message)));
	}
	
	/**
	 * Handles the former half of a conversation-message insertion, getting the conversation from an array of participants
	 * @param context The context to use
	 * @param threadID The external MMS/SMS ID of this conversation
	 * @param newMessage The conversation's message
	 * @return A single for a pair of the new conversation and message
	 */
	public static Single<Pair<ConversationInfo, MessageInfo>> updateTextConversationMessage(Context context, long threadID, MessageInfo newMessage) {
		return Single.create((SingleEmitter<Pair<ConversationInfo, Boolean>> emitter) -> {
			emitter.onSuccess(getOrCreateTextConversation(context, threadID));
		}).subscribeOn(Schedulers.single())
				.flatMap(pair -> updateTextConversationMessage(pair.first, pair.second, newMessage).map(message -> new Pair<>(pair.first, message)));
	}
	
	/**
	 * Handles the latter half of a conversation-message insertion, writing the message to disk and sending the proper emitter update
	 * @param conversationInfo The conversation of the message
	 * @param conversationIsNew Whether the conversation is newly added
	 * @param newMessage The conversation's message
	 * @return A single for a pair of the new conversation and message
	 */
	private static Single<MessageInfo> updateTextConversationMessage(ConversationInfo conversationInfo, boolean conversationIsNew, MessageInfo newMessage) {
		//Triplet<Boolean: whether the conversation is freshly created, ConversationInfo: the conversation for the message, List<ConversationItem>: a list of newly added messages>
		return Single.create((SingleEmitter<MessageInfo> emitter) -> {
			//Writing the message to the database
			long messageID = DatabaseManager.getInstance().addConversationItem(conversationInfo.getLocalID(), newMessage, conversationInfo.getServiceHandler() == ServiceHandler.appleBridge);
			if(messageID == -1) throw new Exception("Failed to add message to database");
			
			//Updating the message
			MessageInfo localMessage = newMessage.clone();
			localMessage.setLocalID(messageID);
			
			emitter.onSuccess(localMessage);
		}).subscribeOn(Schedulers.single()).observeOn(AndroidSchedulers.mainThread()).flatMap(messageInfo -> {
			//Getting the values
			if(conversationIsNew) {
				//If we just created a new conversation, emit a conversation update
				ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.ConversationUpdate(
						Collections.singletonMap(conversationInfo, Collections.singletonList(messageInfo)),
						Collections.emptyList()
				));
			} else {
				//Otherwise, emit a message update
				ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.Message(
						Collections.singletonList(new Pair<>(conversationInfo, Collections.singletonList(ReplaceInsertResult.createAddition(messageInfo))))
				));
			}
			
			//Updating the conversation values in response to the added message
			List<Long> foregroundConversations = Messaging.getForegroundConversations();
			return Single.fromCallable(() -> ConversationHelper.updateConversationValues(foregroundConversations, conversationInfo, messageInfo.isOutgoing() ? 0 : 1))
					.subscribeOn(Schedulers.single())
					.observeOn(AndroidSchedulers.mainThread()).doOnSuccess(update -> update.emitUpdate(conversationInfo))
					.ignoreElement().andThen(Single.just(messageInfo));
		}).subscribeOn(AndroidSchedulers.mainThread());
	}
	
	/**
	 * Saves an MMS message to disk using its cursor, and returns a complete {@link MessageInfo}
	 * @param context The context to use
	 * @param cursorMMS The cursor to retrieve the data from
	 * @return The complete message information
	 */
	public static MessageInfo readMMSMessage(Context context, Cursor cursorMMS) {
		//Getting the message type
		int messageBox = cursorMMS.getInt(cursorMMS.getColumnIndexOrThrow(Telephony.Mms.MESSAGE_BOX));
		
		//Mapping the status code
		int messageState;
		int messageErrorCode = MessageSendErrorCode.none;
		boolean isOutgoing = true;
		switch(messageBox) {
			case Telephony.Mms.MESSAGE_BOX_INBOX:
				messageState = MessageState.sent;
				isOutgoing = false;
				break;
			case Telephony.Mms.MESSAGE_BOX_FAILED:
				messageState = MessageState.ghost;
				messageErrorCode = MessageSendErrorCode.localUnknown;
				break;
			case Telephony.Mms.MESSAGE_BOX_OUTBOX:
				messageState = MessageState.ghost; //Sending
				break;
			case Telephony.Mms.MESSAGE_BOX_SENT:
			default:
				messageState = MessageState.sent; //Sent
				break;
		}
		
		//Reading the common message data
		long messageID = cursorMMS.getLong(cursorMMS.getColumnIndexOrThrow(Telephony.Mms._ID));
		long date = cursorMMS.getLong(cursorMMS.getColumnIndexOrThrow(Telephony.Mms.DATE)) * 1000;
		String messageSubject = cursorMMS.getString(cursorMMS.getColumnIndexOrThrow(Telephony.Mms.SUBJECT));
		String sender = isOutgoing ? null : getMMSSender(context, messageID);
		//long threadID = cursorMMS.getLong(cursorMMS.getColumnIndexOrThrow(Telephony.Mms.THREAD_ID));
		
		StringBuilder messageTextSB = new StringBuilder();
		ArrayList<AttachmentInfo> messageAttachments = new ArrayList<>();
		
		
		//This URI has a constant at Telephony.Mms.Part.CONTENT_URI, but for some reason this is only available in Android Q, so we can't use it here
		try(Cursor cursorMMSData = context.getContentResolver().query(Uri.parse("content://mms/part"), mmsPartColumnProjection, Telephony.Mms.Part.MSG_ID + " = ?", new String[]{Long.toString(messageID)}, null)) {
			if(cursorMMSData == null || !cursorMMSData.moveToFirst()) return null;
			
			do {
				//Reading the part data
				long partID = cursorMMSData.getLong(cursorMMSData.getColumnIndex(Telephony.Mms.Part._ID));
				String contentType = cursorMMSData.getString(cursorMMSData.getColumnIndex(Telephony.Mms.Part.CONTENT_TYPE));
				String fileName = cursorMMSData.getString(cursorMMSData.getColumnIndex(Telephony.Mms.Part.NAME));
				if(fileName == null) fileName = "unnamed_attachment";
				else fileName = FileHelper.cleanFileName(fileName);
				
				//Checking if the part is text
				if("text/plain".equals(contentType)) {
					//Reading the text
					String data = cursorMMSData.getString(cursorMMSData.getColumnIndex(Telephony.Mms.Part._DATA));
					String body;
					if(data != null) {
						try {
							body = getMMSTextContent(context, partID);
						} catch(IOException exception) {
							exception.printStackTrace();
							body = null;
						}
					}
					else {
						body = cursorMMSData.getString(cursorMMSData.getColumnIndex(Telephony.Mms.Part.TEXT));
					}
					
					//Appending the text
					if(body != null) messageTextSB.append(body);
				}
				//Ignoring SMIL data
				else if(!"application/smil".equals(contentType)) {
					//Finding a target file
					File targetFile = AttachmentStorageHelper.prepareContentFile(context, AttachmentStorageHelper.dirNameAttachment, fileName);
					
					//Writing to the file
					long totalSize;
					try(InputStream inputStream = context.getContentResolver().openInputStream(ContentUris.withAppendedId(Uri.parse("content://mms/part/"), partID));
						FileOutputStream outputStream = new FileOutputStream(targetFile)) {
						if(inputStream == null) throw new IOException("Input stream is null");
						totalSize = DataStreamHelper.copyStream(inputStream, outputStream);
					} catch(IOException exception) {
						exception.printStackTrace();
						AttachmentStorageHelper.deleteContentFile(AttachmentStorageHelper.dirNameAttachment, targetFile);
						continue;
					}
					
					//Adding the attachment to the list
					messageAttachments.add(new AttachmentInfo(-1, null, FileHelper.cleanFileName(fileName), contentType, totalSize, -1, targetFile));
				}
			} while(cursorMMSData.moveToNext());
		}
		
		//Getting the message text
		String messageText = messageTextSB.length() > 0 ? messageTextSB.toString() : null;
		
		//Creating the message
		MessageInfo messageInfo = new MessageInfo(-1, -1, null, date, sender, messageText, messageSubject, messageAttachments, null, false, -1, messageState, messageErrorCode, false);
		
		//Returning the message
		return messageInfo;
	}
	
	/**
	 * Saves SMS message to disk using its cursor, and returns a complete {@link MessageInfo}
	 * @param cursorSMS The cursor to retrieve the data from
	 * @return The complete message information
	 */
	public static MessageInfo readSMSMessage(Cursor cursorSMS) {
		//Getting the message status information
		int type = cursorSMS.getInt(cursorSMS.getColumnIndexOrThrow(Telephony.Sms.TYPE));
		int statusCode = cursorSMS.getInt(cursorSMS.getColumnIndexOrThrow(Telephony.Sms.STATUS));
		
		//Figuring out the message state (thanks to Pulse SMS)
		int messageState;
		boolean isOutgoing = true;
		if(statusCode == Telephony.Sms.STATUS_NONE || type == Telephony.Sms.MESSAGE_TYPE_INBOX) {
			switch(type) {
				case Telephony.Sms.MESSAGE_TYPE_INBOX:
					isOutgoing = false;
					messageState = MessageState.sent;
					break;
				case Telephony.Sms.MESSAGE_TYPE_FAILED:
				case Telephony.Sms.MESSAGE_TYPE_OUTBOX:
					messageState = MessageState.ghost; //Sending
					break;
				case Telephony.Sms.MESSAGE_TYPE_SENT:
				default:
					messageState = MessageState.sent;
					break;
			}
		} else {
			switch(statusCode) {
				case Telephony.Sms.STATUS_COMPLETE:
					messageState = MessageState.delivered;
					break;
				case Telephony.Sms.STATUS_PENDING:
				default:
					messageState = MessageState.sent;
					break;
				case Telephony.Sms.STATUS_FAILED:
					messageState = MessageState.ghost;
					break;
			}
		}
		
		String sender = isOutgoing ? null : AddressHelper.normalizeAddress(cursorSMS.getString(cursorSMS.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)));
		String message = cursorSMS.getString(cursorSMS.getColumnIndexOrThrow(Telephony.Sms.BODY));
		long date = cursorSMS.getLong(cursorSMS.getColumnIndexOrThrow(Telephony.Sms.DATE));
		int errorCode = cursorSMS.getInt(cursorSMS.getColumnIndexOrThrow(Telephony.Sms.ERROR_CODE));
		
		//Mapping the error code
		int messageErrorCode = MessageSendErrorCode.none;
		if(messageState == MessageState.ghost) {
			messageErrorCode = MessageSendErrorCode.localUnknown;
		}
		
		//Creating the message
		MessageInfo messageInfo = new MessageInfo(-1, -1, null, date, sender, message, null, new ArrayList<>(), null, false, -1, messageState, messageErrorCode, false);
		if(messageErrorCode != MessageSendErrorCode.none) {
			messageInfo.setErrorDetails("SMS error code " + errorCode);
		}
		
		//Returning the message
		return messageInfo;
	}
	
	/**
	 * Gets the sender of an MMS message
	 * @param context The context to use
	 * @param messageID The database ID of the message to check
	 * @return The address of the sender of the MMS message (or NULL if failed)
	 */
	private static String getMMSSender(Context context, long messageID) {
		//Querying for the message information
		try(Cursor cursor = context.getContentResolver().query(
				Telephony.Mms.CONTENT_URI.buildUpon().appendPath(Long.toString(messageID)).appendPath("addr").build(),
				new String[]{Telephony.Mms.Addr.ADDRESS, Telephony.Mms.Addr.CHARSET},
				Telephony.Mms.Addr.TYPE + " = " + PduHeaders.FROM, null, null, null)) {
			//Returning immediately if the cursor couldn't be opened
			if(cursor == null || !cursor.moveToFirst()) return null;
			
			//Getting the raw sender
			String sender = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Mms.Addr.ADDRESS));
			if(sender == null || sender.isEmpty()) return null;
			
			//Re-encoding and returning the sender with the correct encoding
			byte[] senderBytes = PduPersister.getBytes(sender);
			int charset = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Mms.Addr.CHARSET));
			return AddressHelper.normalizeAddress(new EncodedStringValue(charset, senderBytes).getString());
		}
	}
	
	/**
	 * Gets the text content of an MMS message
	 * @param context The context to use
	 * @param partID The ID of the text part of the message
	 * @return The text content of the MMS message
	 */
	private static String getMMSTextContent(Context context, long partID) throws IOException {
		//Creating the string builder
		StringBuilder stringBuilder = new StringBuilder();
		
		//Opening the stream
		try(InputStream inputStream = context.getContentResolver().openInputStream(ContentUris.withAppendedId(Uri.parse("content://mms/part/"), partID))) {
			//Throwing an exception if the stream couldn't be opened
			if(inputStream == null) throw new IOException("Failed to open stream");
			
			try(InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
				//Reading to the string builder
				String buffer;
				do {
					buffer = bufferedReader.readLine();
					stringBuilder.append(buffer);
				} while(buffer != null);
			}
		}
		
		//Returning the string builder
		return stringBuilder.toString();
	}
	
	/**
	 * Fetches an array of addresses from a recipient ID string
	 * @param context The context to use
	 * @param recipientIDs The Android recipient ID string
	 * @return An array of addresses from the string
	 */
	public static String[] getAddressFromRecipientID(Context context, String recipientIDs) {
		//Getting the target URI
		Uri addressUri = Uri.parse("content://mms-sms/canonical-address");
		
		//Splitting the recipient IDs
		String[] recipientIDArray = recipientIDs.split(" ");
		
		//Creating the list
		String[] recipientAddressArray = new String[recipientIDArray.length];
		
		//Iterating over each recipient
		for(int i = 0; i < recipientIDArray.length; i++) {
			//Querying for the recipient data
			try(Cursor cursor = context.getContentResolver().query(ContentUris.withAppendedId(addressUri, Long.parseLong(recipientIDArray[i])), new String[]{Telephony.CanonicalAddressesColumns.ADDRESS}, null, null, null)) {
				//Ignoring invalid or empty results
				if(cursor == null || !cursor.moveToNext()) {
					recipientAddressArray[i] = "0";
					continue;
				}
				
				//Adding the address to the array
				String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.CanonicalAddressesColumns.ADDRESS));
				recipientAddressArray[i] = AddressHelper.normalizeAddress(address);
			} catch(RuntimeException exception) {
				recipientAddressArray[i] = "0";
				exception.printStackTrace();
			}
		}
		
		//Returning the array
		return recipientAddressArray;
	}
	
	/**
	 * Gets a transaction to be used when sending MMS / SMS messages
	 * @param context The context to use
	 * @param conversationLocalID The local ID of the message's conversation
	 * @param messageLocalID The local ID of the message
	 * @return The transaction to use when sending the message
	 */
	public static Transaction getMMSSMSTransaction(Context context, long conversationLocalID, long messageLocalID) {
		Settings settings = new Settings();
		settings.setDeliveryReports(Preferences.getPreferenceSMSDeliveryReports(context));
		Transaction transaction = new Transaction(context, settings);
		
		{
			Intent intent = new Intent(context, TextMMSSentReceiver.class);
			intent.putExtra(SMSReceiverConstants.conversationID, conversationLocalID);
			intent.putExtra(SMSReceiverConstants.messageID, messageLocalID);
			transaction.setExplicitBroadcastForSentMms(intent);
		}
		{
			Intent intent = new Intent(context, TextSMSSentReceiver.class);
			intent.putExtra(SMSReceiverConstants.conversationID, conversationLocalID);
			intent.putExtra(SMSReceiverConstants.messageID, messageLocalID);
			transaction.setExplicitBroadcastForSentSms(intent);
		}
		{
			Intent intent = new Intent(context, TextSMSDeliveredReceiver.class);
			intent.putExtra(SMSReceiverConstants.conversationID, conversationLocalID);
			intent.putExtra(SMSReceiverConstants.messageID, messageLocalID);
			transaction.setExplicitBroadcastForDeliveredSms(intent);
		}
		
		//Returning the transaction
		return transaction;
	}
	
	/**
	 * Gets if AirMessage is set as the system's default messaging app
	 */
	public static boolean isDefaultMessagingApp(Context context) {
		return context.getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(context));
	}
	
	/**
	 * Cleans the subject line of an MMS message before displaying it to the user
	 * @return The cleaned subject line, or NULL if no subject line should be displayed
	 */
	@Nullable
	public static String cleanMMSSubject(@Nullable String subject) {
		if(subject == null || subject.equals("") || subject.equals("no subject") || subject.equals("NoSubject")) {
			return null;
		} else {
			return subject;
		}
	}
}