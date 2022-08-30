package me.tagavari.airmessage.helper

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import com.klinker.android.send_message.Message
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.withContext
import me.tagavari.airmessage.connection.ConnectionManager
import me.tagavari.airmessage.connection.exception.AMRequestException
import me.tagavari.airmessage.constants.RegexConstants
import me.tagavari.airmessage.data.DatabaseManager
import me.tagavari.airmessage.enums.MessageSendErrorCode
import me.tagavari.airmessage.enums.MessageState
import me.tagavari.airmessage.enums.ServiceHandler
import me.tagavari.airmessage.messaging.*
import me.tagavari.airmessage.redux.ReduxEventAttachmentUpload
import me.tagavari.airmessage.task.MessageActionTask
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream

object MessageSendHelperCoroutine {
	/**
	 * Writes a message body and files to disk and prepares messages to be sent
	 * @param context The context to use
	 * @param conversationInfo The conversation of the message
	 * @param messageText The message body
	 * @param fileList The list of attachment files to send
	 * @return An observable representing the messages to send
	 */
	suspend fun prepareMessage(context: Context, conversationInfo: ConversationInfo, messageText: String?, fileList: List<LocalFile>): List<MessageInfo> {
		return withContext(Dispatchers.IO) {
			//Convert local files to attachments
			val attachmentList = fileList.mapNotNull { localFile ->
				//Create a new target attachment file
				val targetFile = AttachmentStorageHelper.prepareContentFile(
					context,
					AttachmentStorageHelper.dirNameAttachment,
					localFile.fileName
				)
				
				//Move the file to attachments
				val moveResult = localFile.moveFile(targetFile)
				if(!moveResult) {
					localFile.deleteFile()
					return@mapNotNull null
				}
				
				//Return a new in-memory attachment representation
				AttachmentInfo(
					localID = -1,
					guid = null,
					fileName = localFile.fileName,
					contentType = localFile.fileType,
					fileSize = localFile.fileSize,
					file = targetFile,
					shouldAutoDownload = false
				)
			}
			
			//Prepare the message for its protocol
			val preparedMessageList = if(conversationInfo.serviceHandler == ServiceHandler.appleBridge) {
				prepareMessageApple(messageText, attachmentList)
			} else {
				prepareMessageStandard(messageText, attachmentList)
			}
			
			//Write the items to the database
			MessageActionTask.writeMessages(conversationInfo, preparedMessageList)
			
			return@withContext preparedMessageList
		}
	}
	
	/**
	 * Prepares a message to be sent over AirMessage Bridge
	 */
	private fun prepareMessageApple(messageText: String?, attachmentList: List<AttachmentInfo>): List<MessageInfo> {
		val messageList = mutableListOf<MessageInfo>()
		
		if(!messageText.isNullOrBlank()) {
			val messageTextList = mutableListOf<String>()
			
			//Trim the message text
			val cleanMessageText = messageText.trim()
			
			//Check for a straight line of pure URLs
			var matcher = RegexConstants.messageURLGroup.matcher(cleanMessageText)
			if(matcher.find()) {
				//Add the URLs
				messageTextList.addAll(cleanMessageText.split("\\s".toRegex()))
			} else {
				//Check for a single URL
				matcher = RegexConstants.messageURLSandwich.matcher(cleanMessageText)
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
						messageTextList.add(cleanMessageText)
					} else {
						//Add each message part separately
						if(prefixOK) messageTextList.add(prefix!!.trim { it <= ' ' })
						messageTextList.add(url)
						if(suffixOK) messageTextList.add(suffix!!.trim { it <= ' ' })
					}
				} else {
					messageTextList.add(cleanMessageText)
				}
			}
			
			//Add the text messages
			for(message in messageTextList) {
				messageList.add(MessageInfo.blankFromText(message))
			}
		}
		
		//Add the attachments
		for(attachment in attachmentList) {
			messageList.add(MessageInfo(-1, -1, null, System.currentTimeMillis(), null, null, null, mutableListOf(attachment), null, false, -1, MessageState.ghost, MessageSendErrorCode.none, false, null))
		}
		
		return messageList
	}
	
	/**
	 * Prepares a message to be sent over a standard messaging protocol
	 */
	private fun prepareMessageStandard(messageText: String?, attachmentList: List<AttachmentInfo>): List<MessageInfo> {
		return listOf(
			MessageInfo(-1, -1, null, System.currentTimeMillis(), null, messageText, null, attachmentList.toMutableList(), null, false, -1, MessageState.ghost, MessageSendErrorCode.none, false, null)
		)
	}
	
	@SuppressLint("WrongConstant")
	suspend fun sendMessage(context: Context, connectionManager: ConnectionManager?, conversationInfo: ConversationInfo, messageInfo: MessageInfo) {
		try {
			if(conversationInfo.serviceHandler == ServiceHandler.appleBridge) {
				//Send the messages over the connection
				val currentConnectionManager = connectionManager
					?: throw AMRequestException(MessageSendErrorCode.localNetwork)
				
				sendMessageAMBridge(currentConnectionManager, conversationInfo, messageInfo)
			} else {
				//Send the messages over SMS / MMS
				sendMessageMMSSMS(context, conversationInfo, messageInfo)
			}
		} catch(exception: AMRequestException) {
			//If the request failed, update the message state
			MessageActionTask.updateMessageErrorCode(
				conversationInfo,
				messageInfo,
				exception.errorCode,
				exception.errorDetails
			).await()
		}
	}
	
	/**
	 * Sends a message over AirMessage bridge
	 * @param connectionManager The connection manager to use
	 * @param conversationInfo The message's conversation
	 * @param messageInfo The message to send
	 * @return A completable to represent this request
	 */
	private suspend fun sendMessageAMBridge(connectionManager: ConnectionManager, conversationInfo: ConversationInfo, messageInfo: MessageInfo) {
		when {
			messageInfo.messageText != null -> {
				connectionManager.sendMessage(conversationInfo.conversationTarget, messageInfo.messageText).await()
			}
			messageInfo.attachments.size == 1 -> {
				connectionManager.sendFile(conversationInfo.conversationTarget, messageInfo.attachments[0].file)
					.flatMap { event: ReduxEventAttachmentUpload ->
						if(event is ReduxEventAttachmentUpload.Complete) {
							//Updating the attachment's checksum on disk
							return@flatMap Completable.fromAction {
								val checksum = event.fileHash
								DatabaseManager.getInstance().updateAttachmentChecksum(messageInfo.attachments[0].localID, checksum)
							}.subscribeOn(Schedulers.single()).observeOn(AndroidSchedulers.mainThread()).andThen(
								Observable.empty<Unit>())
						} else {
							return@flatMap Observable.empty<Unit>()
						}
					}.ignoreElements().await()
			}
			else -> {
				throw IllegalArgumentException("Cannot send message of this type")
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
	private suspend fun sendMessageMMSSMS(context: Context, conversationInfo: ConversationInfo, messageInfo: MessageInfo) {
		//Create the message
		@Suppress("BlockingMethodInNonBlockingContext")
		val message = withContext(Dispatchers.IO) {
			Message().apply {
				//Set addresses
				addresses = conversationInfo.members.map { member ->
					AddressHelper.normalizeAddress(member.address)
				}.toTypedArray()
				
				//Add text content
				text = messageInfo.messageText
				subject = messageInfo.messageSubject
				
				//Add attachments
				for(attachment in messageInfo.attachments) {
					BufferedInputStream(FileInputStream(attachment.file)).use { inputStream ->
						ByteArrayOutputStream().use { outputStream ->
							DataStreamHelper.copyStream(inputStream, outputStream)
							
							addMedia(outputStream.toByteArray(), attachment.contentType, attachment.fileName)
						}
					}
				}
			}
		}
		
		withContext(Dispatchers.Main) {
			//Send the message
			val transaction = MMSSMSHelper.getMMSSMSTransaction(context, conversationInfo.localID, messageInfo.localID)
			transaction.sendNewMessage(message, conversationInfo.externalID)
		}
	}
}