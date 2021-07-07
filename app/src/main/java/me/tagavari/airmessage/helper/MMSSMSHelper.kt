package me.tagavari.airmessage.helper

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import com.google.android.mms.pdu_alt.EncodedStringValue
import com.google.android.mms.pdu_alt.PduHeaders
import com.google.android.mms.pdu_alt.PduPersister
import com.klinker.android.send_message.Settings
import com.klinker.android.send_message.Transaction
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter
import io.reactivex.rxjava3.schedulers.Schedulers
import me.tagavari.airmessage.R
import me.tagavari.airmessage.activity.Messaging
import me.tagavari.airmessage.activity.Preferences
import me.tagavari.airmessage.constants.SMSReceiverConstants
import me.tagavari.airmessage.data.DatabaseManager
import me.tagavari.airmessage.enums.*
import me.tagavari.airmessage.helper.AddressHelper.normalizeAddress
import me.tagavari.airmessage.helper.AttachmentStorageHelper.deleteContentFile
import me.tagavari.airmessage.helper.AttachmentStorageHelper.prepareContentFile
import me.tagavari.airmessage.helper.ConversationColorHelper.getColoredMembers
import me.tagavari.airmessage.helper.ConversationColorHelper.getDefaultConversationColor
import me.tagavari.airmessage.helper.ConversationHelper.updateConversationValues
import me.tagavari.airmessage.helper.DataStreamHelper.copyStream
import me.tagavari.airmessage.helper.FileHelper.cleanFileName
import me.tagavari.airmessage.messaging.*
import me.tagavari.airmessage.receiver.TextMMSSentReceiver
import me.tagavari.airmessage.receiver.TextSMSDeliveredReceiver
import me.tagavari.airmessage.receiver.TextSMSSentReceiver
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxEventMessaging
import me.tagavari.airmessage.redux.ReduxEventMessaging.ConversationUpdate
import me.tagavari.airmessage.util.ConversationValueUpdateResult
import me.tagavari.airmessage.util.ReplaceInsertResult
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

object MMSSMSHelper {
	@JvmField
	val smsMixedColumnProjection = arrayOf(Telephony.BaseMmsColumns._ID, Telephony.Mms.MESSAGE_BOX, Telephony.Sms.TYPE, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.ERROR_CODE, Telephony.Sms.STATUS, Telephony.Mms.SUBJECT)
	@JvmField
	val smsColumnProjection = arrayOf(Telephony.Sms._ID, Telephony.Sms.TYPE, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.ERROR_CODE, Telephony.Sms.STATUS)
	@JvmField
	val mmsColumnProjection = arrayOf(Telephony.Mms._ID, Telephony.Mms.DATE, Telephony.Mms.MESSAGE_BOX, Telephony.Mms.SUBJECT)
	val mmsPartColumnProjection = arrayOf(Telephony.Mms.Part._ID, Telephony.Mms.Part.CONTENT_TYPE, Telephony.Mms.Part.NAME, Telephony.Mms.Part._DATA, Telephony.Mms.Part.TEXT)
	
	/**
	 * Get the maximum size for an attachment to be sent over MMS
	 * @param context The context to use
	 * @return The maximum file size in bytes
	 */
	@JvmStatic
	fun getMaxMessageSize(context: Context): Int {
		return 300 * 1024 //300 kB
		//return context.getSystemService(CarrierConfigManager.class).getConfig().getInt(CarrierConfigManager.KEY_MMS_MAX_MESSAGE_SIZE_INT);
	}
	
	/**
	 * Get a human-readable label for a contact
	 * @param resources The resources to use to retrieve the string
	 * @param mimeType The MIME type of this contact entry
	 * @param addressType The address type ID of this entry
	 * @return The label for this address type
	 */
	@JvmStatic
	fun getAddressLabel(resources: Resources, mimeType: String, addressType: Int): String? {
		return when(mimeType) {
			ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
				resources.getString(when(addressType) {
					ContactsContract.CommonDataKinds.Email.TYPE_HOME -> R.string.label_email_home
					ContactsContract.CommonDataKinds.Email.TYPE_WORK -> R.string.label_email_work
					ContactsContract.CommonDataKinds.Email.TYPE_OTHER -> R.string.label_email_other
					else -> return null
				})
			}
			ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
				resources.getString(when(addressType) {
					ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> R.string.label_phone_mobile
					ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> R.string.label_email_work
					ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> R.string.label_phone_home
					ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> R.string.label_phone_main
					ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK -> R.string.label_phone_workfax
					ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME -> R.string.label_phone_homefax
					ContactsContract.CommonDataKinds.Phone.TYPE_PAGER -> R.string.label_phone_pager
					ContactsContract.CommonDataKinds.Phone.TYPE_OTHER -> R.string.label_phone_other
					else -> return null
				})
			}
			else -> {
				null
			}
		}
	}
	
	/**
	 * Creates or gets a system MMS/SMS conversation by participants
	 * @param context The context to use
	 * @param participants The participants of the conversation
	 * @return A pair of the conversation, and a boolean to indicate if this conversation is new
	 */
	@JvmStatic
	fun getOrCreateTextConversation(context: Context, participants: List<String>): Pair<ConversationInfo, Boolean>? {
		//Getting the Android thread ID
		val threadID = Telephony.Threads.getOrCreateThreadId(context, participants.toSet())
		
		//Trying to find a matching local conversation
		var conversationInfo = DatabaseManager.getInstance().findConversationByExternalID(context, threadID, ServiceHandler.systemMessaging, ServiceType.systemSMS)
		if(conversationInfo != null) {
			return Pair(conversationInfo, false)
		}
		
		//Creating a new conversation if no existing conversation was found
		val conversationColor = getDefaultConversationColor(threadID)
		val coloredMembers = getColoredMembers(participants, conversationColor, threadID)
		conversationInfo = ConversationInfo(-1, null, threadID, ConversationState.ready, ServiceHandler.systemMessaging, ServiceType.systemSMS, conversationColor, coloredMembers, null, 0, false, false, null, null, mutableListOf(), -1)
		
		//Writing the conversation to disk
		val result = DatabaseManager.getInstance().addConversationInfo(conversationInfo)
		return if(!result) null else Pair(conversationInfo, true)
	}
	
	/**
	 * Creates or gets a system MMS/SMS conversation by its external thread ID
	 * @param context The context to use
	 * @param threadID The MMS/SMS ID of the thread
	 * @return A pair of the conversation, and a boolean to indicate if this conversation is new
	 */
	@JvmStatic
	fun getOrCreateTextConversation(context: Context, threadID: Long): Pair<ConversationInfo, Boolean>? {
		//Trying to find a matching local conversation
		var conversationInfo = DatabaseManager.getInstance().findConversationByExternalID(context, threadID, ServiceHandler.systemMessaging, ServiceType.systemSMS)
		if(conversationInfo != null) return Pair(conversationInfo, false)
		
		//Getting the conversation participants
		var recipientIDs: String
		context.contentResolver.query(Uri.parse("content://mms-sms/conversations?simple=true"), arrayOf("*"),
				Telephony.Threads._ID + " = ?", arrayOf(threadID.toString()),
				null).use { cursorConversation ->
			if(cursorConversation == null || !cursorConversation.moveToFirst()) return null
			recipientIDs = cursorConversation.getString(cursorConversation.getColumnIndexOrThrow(Telephony.Threads.RECIPIENT_IDS))
		}
		
		//Creating the conversation
		val conversationColor = getDefaultConversationColor(threadID)
		val coloredMembers = getColoredMembers(getAddressFromRecipientID(context, recipientIDs), conversationColor)
		conversationInfo = ConversationInfo(-1, null, threadID, ConversationState.ready, ServiceHandler.systemMessaging, ServiceType.systemSMS, conversationColor, coloredMembers, null, 0, false, false, null, null, ArrayList(), -1)
		
		//Writing the conversation to disk
		val result = DatabaseManager.getInstance().addConversationInfo(conversationInfo)
		return if(!result) null else Pair(conversationInfo, true)
	}
	
	/**
	 * Handles the former half of a conversation-message insertion, getting the conversation from an array of participants
	 * @param context The context to use
	 * @param participants The conversation's participants
	 * @param newMessage The conversation's message
	 * @return A single for a pair of the new conversation and message
	 */
	@JvmStatic
	fun updateTextConversationMessage(context: Context, participants: List<String>, newMessage: MessageInfo): Single<Pair<ConversationInfo, MessageInfo>> {
		return Single.fromCallable {
			getOrCreateTextConversation(context, participants) ?: throw RuntimeException("Failed to create conversation")
		}.subscribeOn(Schedulers.single()).flatMap { (conversation, isNew) ->
			updateTextConversationMessage(conversation, isNew, newMessage).map { message ->
				Pair(conversation, message)
			}
		}
	}
	
	/**
	 * Handles the former half of a conversation-message insertion, getting the conversation from an array of participants
	 * @param context The context to use
	 * @param threadID The external MMS/SMS ID of this conversation
	 * @param newMessage The conversation's message
	 * @return A single for a pair of the new conversation and message
	 */
	@JvmStatic
	fun updateTextConversationMessage(context: Context, threadID: Long, newMessage: MessageInfo): Single<Pair<ConversationInfo, MessageInfo>> {
		return Single.fromCallable {
			getOrCreateTextConversation(context, threadID) ?: throw RuntimeException("Failed to create conversation")
		}.subscribeOn(Schedulers.single()).flatMap { (conversation, isNew) ->
			updateTextConversationMessage(conversation, isNew, newMessage).map { message ->
				Pair(conversation, message)
			}
		}
	}
	
	/**
	 * Handles the latter half of a conversation-message insertion, writing the message to disk and sending the proper emitter update
	 * @param conversationInfo The conversation of the message
	 * @param conversationIsNew Whether the conversation is newly added
	 * @param newMessage The conversation's message
	 * @return A single for a pair of the new conversation and message
	 */
	private fun updateTextConversationMessage(conversationInfo: ConversationInfo, conversationIsNew: Boolean, newMessage: MessageInfo): Single<MessageInfo> {
		return Single.fromCallable {
			//Writing the message to the database
			val messageID = DatabaseManager.getInstance().addConversationItem(conversationInfo.localID, newMessage, conversationInfo.serviceHandler == ServiceHandler.appleBridge)
			if(messageID == -1L) throw Exception("Failed to add message to database")
			
			//Updating the message
			val localMessage = newMessage.clone()
			localMessage.localID = messageID
			localMessage
		}.subscribeOn(Schedulers.single()).observeOn(AndroidSchedulers.mainThread()).flatMap { messageInfo ->
			//Getting the values
			if(conversationIsNew) {
				//If we just created a new conversation, emit a conversation update
				ReduxEmitterNetwork.messageUpdateSubject.onNext(ConversationUpdate(mapOf(conversationInfo to listOf(messageInfo)), emptyList()))
			} else {
				//Otherwise, emit a message update
				ReduxEmitterNetwork.messageUpdateSubject.onNext(ReduxEventMessaging.Message(listOf(Pair(conversationInfo, listOf(ReplaceInsertResult.createAddition(messageInfo))))))
			}
			
			//Updating the conversation values in response to the added message
			val foregroundConversations = Messaging.getForegroundConversations()
			return@flatMap Single.fromCallable { updateConversationValues(foregroundConversations, conversationInfo, if(messageInfo.isOutgoing) 0 else 1) }
					.subscribeOn(Schedulers.single())
					.observeOn(AndroidSchedulers.mainThread())
					.doOnSuccess { it.emitUpdate(conversationInfo) }
					.ignoreElement().andThen(Single.just(messageInfo))
		}.subscribeOn(AndroidSchedulers.mainThread())
	}
	
	/**
	 * Saves an MMS message to disk using its cursor, and returns a complete [MessageInfo]
	 * @param context The context to use
	 * @param cursorMMS The cursor to retrieve the data from
	 * @return The complete message information
	 */
	@JvmStatic
	fun readMMSMessage(context: Context, cursorMMS: Cursor): MessageInfo? {
		//Getting the message type
		val messageBox = cursorMMS.getInt(cursorMMS.getColumnIndexOrThrow(Telephony.Mms.MESSAGE_BOX))
		
		//Mapping the status code
		val messageState: Int
		var messageErrorCode = MessageSendErrorCode.none
		var isOutgoing = true
		when(messageBox) {
			Telephony.Mms.MESSAGE_BOX_INBOX -> {
				messageState = MessageState.sent
				isOutgoing = false
			}
			Telephony.Mms.MESSAGE_BOX_FAILED -> {
				messageState = MessageState.ghost
				messageErrorCode = MessageSendErrorCode.localUnknown
			}
			Telephony.Mms.MESSAGE_BOX_OUTBOX -> messageState = MessageState.ghost //Sending
			Telephony.Mms.MESSAGE_BOX_SENT -> messageState = MessageState.sent //Sent
			else -> messageState = MessageState.sent
		}
		
		//Reading the common message data
		val messageID = cursorMMS.getLong(cursorMMS.getColumnIndexOrThrow(Telephony.Mms._ID))
		val date = cursorMMS.getLong(cursorMMS.getColumnIndexOrThrow(Telephony.Mms.DATE)) * 1000
		val messageSubject = cleanMMSSubject(cursorMMS.getString(cursorMMS.getColumnIndexOrThrow(Telephony.Mms.SUBJECT)))
		val sender = if(isOutgoing) null else getMMSSender(context, messageID)
		//long threadID = cursorMMS.getLong(cursorMMS.getColumnIndexOrThrow(Telephony.Mms.THREAD_ID));
		val messageTextSB = StringBuilder()
		val messageAttachments = ArrayList<AttachmentInfo>()
		context.contentResolver.query(Uri.parse("content://mms/part"), mmsPartColumnProjection, Telephony.Mms.Part.MSG_ID + " = ?", arrayOf(java.lang.Long.toString(messageID)), null).use { cursorMMSData ->
			if(cursorMMSData == null || !cursorMMSData.moveToFirst()) return null
			do {
				//Reading the part data
				val partID = cursorMMSData.getLong(cursorMMSData.getColumnIndex(Telephony.Mms.Part._ID))
				val contentType = cursorMMSData.getString(cursorMMSData.getColumnIndex(Telephony.Mms.Part.CONTENT_TYPE))
				var fileName = cursorMMSData.getString(cursorMMSData.getColumnIndex(Telephony.Mms.Part.NAME))
				fileName = if(fileName == null) "unnamed_attachment" else cleanFileName(fileName)
				
				//Checking if the part is text
				if("text/plain" == contentType) {
					//Reading the text
					val data = cursorMMSData.getString(cursorMMSData.getColumnIndex(Telephony.Mms.Part._DATA))
					val body: String? = if(data != null) {
						try {
							getMMSTextContent(context, partID)
						} catch(exception: IOException) {
							exception.printStackTrace()
							null
						}
					} else {
						cursorMMSData.getString(cursorMMSData.getColumnIndex(Telephony.Mms.Part.TEXT))
					}
					
					//Appending the text
					if(body != null) messageTextSB.append(body)
				} else if("application/smil" != contentType) {
					//Finding a target file
					val targetFile = prepareContentFile(context, AttachmentStorageHelper.dirNameAttachment, fileName)
					
					//Writing to the file
					var totalSize: Long
					try {
						context.contentResolver.openInputStream(ContentUris.withAppendedId(Uri.parse("content://mms/part/"), partID)).use { inputStream ->
							FileOutputStream(targetFile).use { outputStream ->
								if(inputStream == null) throw IOException("Input stream is null")
								totalSize = copyStream(inputStream, outputStream)
							}
						}
					} catch(exception: IOException) {
						exception.printStackTrace()
						deleteContentFile(AttachmentStorageHelper.dirNameAttachment, targetFile)
						continue
					}
					
					//Adding the attachment to the list
					messageAttachments.add(AttachmentInfo(-1, null, cleanFileName(fileName), contentType, totalSize, -1, targetFile, shouldAutoDownload = false))
				}
			} while(cursorMMSData.moveToNext())
		}
		
		//Getting the message text
		val messageText = if(messageTextSB.isNotEmpty()) messageTextSB.toString() else null
		
		//Returning the message
		return MessageInfo(-1, -1, null, date, sender, messageText, messageSubject, messageAttachments, null, false, -1, messageState, messageErrorCode, false, null)
	}
	
	/**
	 * Saves SMS message to disk using its cursor, and returns a complete [MessageInfo]
	 * @param cursorSMS The cursor to retrieve the data from
	 * @return The complete message information
	 */
	@JvmStatic
	fun readSMSMessage(cursorSMS: Cursor): MessageInfo {
		//Getting the message status information
		val type = cursorSMS.getInt(cursorSMS.getColumnIndexOrThrow(Telephony.Sms.TYPE))
		val statusCode = cursorSMS.getInt(cursorSMS.getColumnIndexOrThrow(Telephony.Sms.STATUS))
		
		//Figuring out the message state (thanks to Pulse SMS)
		val messageState: Int
		var isOutgoing = true
		if(statusCode == Telephony.Sms.STATUS_NONE || type == Telephony.Sms.MESSAGE_TYPE_INBOX) {
			when(type) {
				Telephony.Sms.MESSAGE_TYPE_INBOX -> {
					isOutgoing = false
					messageState = MessageState.sent
				}
				Telephony.Sms.MESSAGE_TYPE_FAILED, Telephony.Sms.MESSAGE_TYPE_OUTBOX -> messageState = MessageState.ghost //Sending
				Telephony.Sms.MESSAGE_TYPE_SENT -> messageState = MessageState.sent
				else -> messageState = MessageState.sent
			}
		} else {
			messageState = when(statusCode) {
				Telephony.Sms.STATUS_COMPLETE -> MessageState.delivered
				Telephony.Sms.STATUS_PENDING -> MessageState.sent
				Telephony.Sms.STATUS_FAILED -> MessageState.ghost
				else -> MessageState.sent
			}
		}
		val sender = if(isOutgoing) null else normalizeAddress(cursorSMS.getString(cursorSMS.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)))
		val message = cursorSMS.getString(cursorSMS.getColumnIndexOrThrow(Telephony.Sms.BODY))
		val date = cursorSMS.getLong(cursorSMS.getColumnIndexOrThrow(Telephony.Sms.DATE))
		val errorCode = cursorSMS.getInt(cursorSMS.getColumnIndexOrThrow(Telephony.Sms.ERROR_CODE))
		
		//Mapping the error code
		var messageErrorCode = MessageSendErrorCode.none
		if(messageState == MessageState.ghost) {
			messageErrorCode = MessageSendErrorCode.localUnknown
		}
		
		//Creating the message
		val messageInfo = MessageInfo(-1, -1, null, date, sender, message, null, mutableListOf(), null, false, -1, messageState, messageErrorCode, false, null)
		if(messageErrorCode != MessageSendErrorCode.none) {
			messageInfo.errorDetails = "SMS error code $errorCode"
		}
		
		//Returning the message
		return messageInfo
	}
	
	/**
	 * Gets the sender of an MMS message
	 * @param context The context to use
	 * @param messageID The database ID of the message to check
	 * @return The address of the sender of the MMS message (or NULL if failed)
	 */
	private fun getMMSSender(context: Context, messageID: Long): String? {
		//Querying for the message information
		context.contentResolver.query(
				Telephony.Mms.CONTENT_URI.buildUpon().appendPath(messageID.toString()).appendPath("addr").build(), arrayOf(Telephony.Mms.Addr.ADDRESS, Telephony.Mms.Addr.CHARSET),
				Telephony.Mms.Addr.TYPE + " = " + PduHeaders.FROM, null, null, null).use { cursor ->
			//Returning immediately if the cursor couldn't be opened
			if(cursor == null || !cursor.moveToFirst()) return null
			
			//Getting the raw sender
			val sender = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Mms.Addr.ADDRESS))
			if(sender == null || sender.isEmpty()) return null
			
			//Re-encoding and returning the sender with the correct encoding
			val senderBytes = PduPersister.getBytes(sender)
			val charset = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Mms.Addr.CHARSET))
			return normalizeAddress(EncodedStringValue(charset, senderBytes).string)
		}
	}
	
	/**
	 * Gets the text content of an MMS message
	 * @param context The context to use
	 * @param partID The ID of the text part of the message
	 * @return The text content of the MMS message
	 */
	@Throws(IOException::class)
	private fun getMMSTextContent(context: Context, partID: Long): String {
		context.contentResolver.openInputStream(ContentUris.withAppendedId(Uri.parse("content://mms/part/"), partID)).use { inputStream ->
			//Throwing an exception if the stream couldn't be opened
			if(inputStream == null) throw IOException("Failed to open stream")
			return inputStream.bufferedReader().readLines().joinToString("\n")
		}
	}
	
	/**
	 * Fetches an array of addresses from a recipient ID string
	 * @param context The context to use
	 * @param recipientIDs The Android recipient ID string
	 * @return An array of addresses from the string
	 */
	@JvmStatic
	fun getAddressFromRecipientID(context: Context, recipientIDs: String): List<String> {
		//Getting the target URI
		val addressUri = Uri.parse("content://mms-sms/canonical-address")
		
		//Splitting the recipient IDs
		val recipientIDList = recipientIDs.split(" ".toRegex())
		
		return recipientIDList.mapNotNull { recipientID ->
			//Querying for the recipient data
			try {
				context.contentResolver.query(ContentUris.withAppendedId(addressUri, recipientID.toLong()), arrayOf(Telephony.CanonicalAddressesColumns.ADDRESS), null, null, null).use { cursor ->
					//Ignoring invalid or empty results
					if(cursor == null || !cursor.moveToNext()) {
						return@mapNotNull null
					}
					
					//Adding the address to the array
					val address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.CanonicalAddressesColumns.ADDRESS))
					return@mapNotNull normalizeAddress(address)
				}
			} catch(exception: RuntimeException) {
				exception.printStackTrace()
				return@mapNotNull null
			}
		}
	}
	
	/**
	 * Gets a transaction to be used when sending MMS / SMS messages
	 * @param context The context to use
	 * @param conversationLocalID The local ID of the message's conversation
	 * @param messageLocalID The local ID of the message
	 * @return The transaction to use when sending the message
	 */
	@JvmStatic
	fun getMMSSMSTransaction(context: Context?, conversationLocalID: Long, messageLocalID: Long): Transaction {
		val settings = Settings()
		settings.deliveryReports = Preferences.getPreferenceSMSDeliveryReports(context)
		
		val transaction = Transaction(context, settings)
		transaction.setExplicitBroadcastForSentMms(
				Intent(context, TextMMSSentReceiver::class.java).apply {
					putExtra(SMSReceiverConstants.conversationID, conversationLocalID)
					putExtra(SMSReceiverConstants.messageID, messageLocalID)
				})
		transaction.setExplicitBroadcastForSentSms(
				Intent(context, TextSMSSentReceiver::class.java).apply {
					putExtra(SMSReceiverConstants.conversationID, conversationLocalID)
					putExtra(SMSReceiverConstants.messageID, messageLocalID)
				})
		transaction.setExplicitBroadcastForDeliveredSms(
				Intent(context, TextSMSDeliveredReceiver::class.java).apply {
					putExtra(SMSReceiverConstants.conversationID, conversationLocalID)
					putExtra(SMSReceiverConstants.messageID, messageLocalID)
				})
		
		//Returning the transaction
		return transaction
	}
	
	/**
	 * Gets if AirMessage is set as the system's default messaging app
	 */
	@JvmStatic
	fun isDefaultMessagingApp(context: Context): Boolean {
		return context.packageName == Telephony.Sms.getDefaultSmsPackage(context)
	}
	
	/**
	 * Cleans the subject line of an MMS message before displaying it to the user
	 * @return The cleaned subject line, or NULL if no subject line should be displayed
	 */
	@JvmStatic
	fun cleanMMSSubject(subject: String?): String? {
		return if(subject == null || subject == "" || subject == "no subject" || subject == "NoSubject") {
			null
		} else {
			subject
		}
	}
}