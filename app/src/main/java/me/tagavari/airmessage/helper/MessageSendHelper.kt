package me.tagavari.airmessage.helper

import android.content.Context
import android.text.TextUtils
import android.util.Pair
import com.klinker.android.send_message.Message
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.annotations.CheckReturnValue
import io.reactivex.rxjava3.core.*
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import me.tagavari.airmessage.connection.ConnectionManager
import me.tagavari.airmessage.connection.exception.AMRequestException
import me.tagavari.airmessage.constants.RegexConstants
import me.tagavari.airmessage.data.DatabaseManager
import me.tagavari.airmessage.enums.MessageSendErrorCode
import me.tagavari.airmessage.enums.MessageState
import me.tagavari.airmessage.enums.ServiceHandler
import me.tagavari.airmessage.helper.AddressHelper.normalizeAddress
import me.tagavari.airmessage.helper.AttachmentStorageHelper.deleteContentFile
import me.tagavari.airmessage.helper.AttachmentStorageHelper.prepareContentFile
import me.tagavari.airmessage.helper.DataStreamHelper.copyStream
import me.tagavari.airmessage.messaging.*
import me.tagavari.airmessage.redux.ReduxEventAttachmentUpload
import me.tagavari.airmessage.task.MessageActionTask
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.util.*

object MessageSendHelper {
	/**
	 * Handles preparing a message, sending the message, and emitting relevant updates
	 * @param context The context to use
	 * @param conversationInfo The conversation of the message
	 * @param messageText The message body
	 * @param draftList The list of drafts to send
	 * @param connectionManager The connection manager to use, or NULL if unavailable
	 * @return An completable to represent this request
	 */
	@JvmStatic
	@CheckReturnValue
	fun prepareSendMessages(context: Context, conversationInfo: ConversationInfo, messageText: String?, draftList: List<FileDraft>, connectionManager: ConnectionManager?): Completable {
		return prepareMessages(context, conversationInfo, messageText, draftList)
				.flatMapCompletable { message: MessageInfo -> sendMessage(context, conversationInfo, message, connectionManager) }
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
	fun prepareMessages(context: Context, conversationInfo: ConversationInfo, messageText: String?, draftList: List<FileDraft>): Observable<MessageInfo> {
		return Observable.fromIterable(draftList)
				.observeOn(Schedulers.io())
				//Move the drafts to attachments
				.map { draft: FileDraft ->
					val attachmentFile = moveDraftToAttachment(context, draft)
					if(attachmentFile == null) {
						//If the operation failed, delete the draft file and ignore
						deleteContentFile(AttachmentStorageHelper.dirNameDraft, draft.file)
						return@map Optional.empty<AttachmentInfo>()
					}
					
					return@map Optional.of(AttachmentInfo(-1, null, draft.fileName, draft.fileType, draft.fileSize, -1, attachmentFile))
				}
				.filter { it.isPresent }
				.map { it.get() }
				.toList().flatMapObservable { attachmentList: List<AttachmentInfo> ->
					if(conversationInfo.serviceHandler == ServiceHandler.appleBridge) {
						return@flatMapObservable prepareMessageApple(messageText, attachmentList)
					} else {
						return@flatMapObservable prepareMessageStandard(messageText, attachmentList).toObservable()
					}
				}
				.toList()
				//Write the items to the database and emit an update
				.flatMap { messages -> MessageActionTask.writeMessages(conversationInfo, messages) }
				.flatMapObservable { Observable.fromIterable(it) }
				.observeOn(AndroidSchedulers.mainThread())
	}
	
	/**
	 * Prepares a message to be sent over AirMessage Bridge
	 */
	@CheckReturnValue
	fun prepareMessageApple(messageText: String?, attachmentList: List<AttachmentInfo>): Observable<MessageInfo?> {
		return Observable.create { emitter: ObservableEmitter<MessageInfo> ->
			if(messageText != null) {
				val messageTextList = mutableListOf<String>()
				
				//Checking for a straight line of pure URLs
				var matcher = RegexConstants.messageURLGroup.matcher(messageText)
				if(matcher.find()) {
					//Adding the URLs
					messageTextList.addAll(messageText.split("\\s".toRegex()))
				} else {
					//Checking for a single URL
					matcher = RegexConstants.messageURLSandwich.matcher(messageText)
					if(matcher.find()) {
						var prefix = matcher.group(1)
						if(prefix != null) prefix = prefix.trim { it <= ' ' }
						val prefixOK = !TextUtils.isEmpty(prefix)
						val url = matcher.group(2)!!
						var suffix = matcher.group(3)
						if(suffix != null) suffix = suffix.trim { it <= ' ' }
						val suffixOK = !TextUtils.isEmpty(suffix)
						if(prefixOK && suffixOK) {
							//Just add the entire message if there is both a prefix and a suffix, Apple Messages doesn't do anything special in this case
							messageTextList.add(messageText)
						} else {
							//Add each message part separately
							if(prefixOK) messageTextList.add(prefix!!.trim { it <= ' ' })
							messageTextList.add(url)
							if(suffixOK) messageTextList.add(suffix!!.trim { it <= ' ' })
						}
					} else {
						messageTextList.add(messageText)
					}
				}
				
				//Adding the text messages
				for(message in messageTextList) {
					emitter.onNext(MessageInfo.blankFromText(message))
				}
			}
			
			//Adding the attachments
			for(attachment in attachmentList) {
				emitter.onNext(MessageInfo(-1, -1, null, System.currentTimeMillis(), null, null, null, ArrayList(listOf(attachment)), null, false, -1, MessageState.ghost, MessageSendErrorCode.none, false))
			}
			emitter.onComplete()
		}
	}
	
	/**
	 * Prepares a message to be sent over a standard messaging protocol
	 */
	@CheckReturnValue
	fun prepareMessageStandard(messageText: String?, attachmentList: List<AttachmentInfo>?): Single<MessageInfo> {
		return Single.create { emitter: SingleEmitter<MessageInfo> ->
			emitter.onSuccess(MessageInfo(-1, -1, null, System.currentTimeMillis(), null, messageText, null, ArrayList(attachmentList), null, false, -1, MessageState.ghost, MessageSendErrorCode.none, false))
		}
	}
	
	/**
	 * Sends a message
	 * @param context The context to use
	 * @param conversationInfo The message's conversation
	 * @param messageInfo The message to send
	 * @param connectionManager The connection manager to use (or NULL if unavailable)
	 * @return A completable to represent this request
	 */
	@JvmStatic
	@CheckReturnValue
	fun sendMessage(context: Context, conversationInfo: ConversationInfo, messageInfo: MessageInfo, connectionManager: ConnectionManager?): Completable {
		return Single.just(messageInfo).flatMapMaybe<Pair<MessageInfo, AMRequestException>> { message: MessageInfo ->
			if(conversationInfo.serviceHandler == ServiceHandler.appleBridge) {
				if(connectionManager != null) {
					//Send the messages over the connection
					return@flatMapMaybe sendMessageAMBridge(conversationInfo, message, connectionManager)
							.onErrorReturn { error ->
								if(error is AMRequestException) {
									return@onErrorReturn Pair(message, error)
								} else {
									return@onErrorReturn Pair(message, AMRequestException(MessageSendErrorCode.localUnknown, error))
								}
							}
				} else {
					//Fail immediately
					return@flatMapMaybe Maybe.just(Pair(message, AMRequestException(MessageSendErrorCode.localNetwork)))
				}
			} else {
				//Send the messages over SMS / MMS
				return@flatMapMaybe sendMessageMMSSMS(context, conversationInfo, message)
						.onErrorReturn { error -> Pair(message, error as AMRequestException) }
			}
		}.flatMapCompletable { errorDetails: Pair<MessageInfo, AMRequestException> ->
			//Updating the message's state on fail
			val requestException = errorDetails.second
			return@flatMapCompletable MessageActionTask.updateMessageErrorCode(conversationInfo, errorDetails.first, requestException.errorCode, requestException.errorDetails).onErrorComplete()
		}
	}
	
	/**
	 * Sends a message over AirMessage bridge
	 * @param conversationInfo The message's conversation
	 * @param messageInfo The message to send
	 * @param connectionManager The connection manager to use (or NULL if unavailable)
	 * @return A completable to represent this request
	 */
	@CheckReturnValue
	fun sendMessageAMBridge(conversationInfo: ConversationInfo, messageInfo: MessageInfo, connectionManager: ConnectionManager): Completable {
		return when {
			messageInfo.messageText != null -> {
				connectionManager.sendMessage(conversationInfo.conversationTarget, messageInfo.messageText)
			}
			messageInfo.attachments.size == 1 -> {
				connectionManager.sendFile(conversationInfo.conversationTarget, messageInfo.attachments[0].file)
						.flatMap { event: ReduxEventAttachmentUpload ->
							if(event is ReduxEventAttachmentUpload.Complete) {
								//Updating the attachment's checksum on disk
								return@flatMap Completable.fromAction {
									val checksum = event.fileHash
									DatabaseManager.getInstance().updateAttachmentChecksum(messageInfo.attachments[0].localID, checksum)
								}.subscribeOn(Schedulers.single()).observeOn(AndroidSchedulers.mainThread()).andThen(Observable.empty<Unit>())
							} else {
								return@flatMap Observable.empty<Unit>()
							}
						}.ignoreElements()
			}
			else -> {
				Completable.error(IllegalArgumentException("Cannot send message of this type"))
			}
		}
	}
	
	/**
	 * Sends a message over SMS / MMS
	 * @param context The context to use
	 * @param conversationInfo The message's conversation
	 * @param messageInfo The message to send
	 * @return A completable to represent this request
	 */
	@JvmStatic
	@CheckReturnValue
	fun sendMessageMMSSMS(context: Context?, conversationInfo: ConversationInfo, messageInfo: MessageInfo): Completable {
		return Completable.fromAction {
			//Configuring the message settings
			val transaction = MMSSMSHelper.getMMSSMSTransaction(context, conversationInfo.localID, messageInfo.localID)
			
			//Creating the message
			val message = Message().apply {
				addresses = conversationInfo.members.map { member -> normalizeAddress(member.address) }.toTypedArray()
				
				text = messageInfo.messageText
				subject = messageInfo.messageSubject
				
				for(attachment in messageInfo.attachments) {
					BufferedInputStream(FileInputStream(attachment.file)).use { inputStream ->
						ByteArrayOutputStream().use { outputStream ->
							copyStream(inputStream, outputStream)
							this.addMedia(outputStream.toByteArray(), attachment.contentType, attachment.fileName)
						}
					}
				}
			}
			
			//Sending the message
			transaction.sendNewMessage(message, conversationInfo.externalID)
		}
	}
	
	/**
	 * Moves a draft file to the attachment directory
	 * @param context The context to use
	 * @param fileDraft The draft file to move
	 * @return The attachment file that the draft was moved to, or NULL if the operation failed
	 */
	private fun moveDraftToAttachment(context: Context, fileDraft: FileDraft): File? {
		val draftFile = fileDraft.file
		val targetFile = prepareContentFile(context, AttachmentStorageHelper.dirNameAttachment, fileDraft.fileName)
		val result = draftFile.renameTo(targetFile)
		if(!result) {
			deleteContentFile(AttachmentStorageHelper.dirNameAttachment, targetFile)
			return null
		}
		deleteContentFile(AttachmentStorageHelper.dirNameDraft, draftFile)
		return targetFile
	}
}