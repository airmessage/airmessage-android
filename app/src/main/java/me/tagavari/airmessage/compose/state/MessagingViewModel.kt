package me.tagavari.airmessage.compose.state

import android.app.Application
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.rx3.asFlow
import kotlinx.coroutines.rx3.await
import me.tagavari.airmessage.R
import me.tagavari.airmessage.activity.Preferences
import me.tagavari.airmessage.connection.ConnectionManager
import me.tagavari.airmessage.constants.FileNameConstants
import me.tagavari.airmessage.constants.MIMEConstants
import me.tagavari.airmessage.container.LocalFile
import me.tagavari.airmessage.container.ReadableBlob
import me.tagavari.airmessage.container.ReadableBlobLocalFile
import me.tagavari.airmessage.data.DatabaseManager
import me.tagavari.airmessage.data.ForegroundState
import me.tagavari.airmessage.enums.ServiceHandler
import me.tagavari.airmessage.enums.ServiceType
import me.tagavari.airmessage.flavor.CrashlyticsBridge
import me.tagavari.airmessage.helper.*
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.messaging.ConversationItem
import me.tagavari.airmessage.messaging.MessageInfo
import me.tagavari.airmessage.messaging.QueuedFile
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxEventMessaging
import me.tagavari.airmessage.redux.ReduxEventMessaging.ConversationDraftFileClear
import me.tagavari.airmessage.redux.ReduxEventMessaging.ConversationDraftFileUpdate
import me.tagavari.airmessage.task.ConversationActionTask
import me.tagavari.airmessage.task.DraftActionTaskCoroutine
import me.tagavari.airmessage.util.LatLngInfo
import me.tagavari.airmessage.util.ReplaceInsertResult
import java.io.IOException

class MessagingViewModel(
	application: Application,
	private val conversationID: Long
) : AndroidViewModel(application) {
	//Input state
	var inputText by mutableStateOf("")
	
	//Screen state
	var collapseInputButtons by mutableStateOf(false)
	
	//Messaging state
	var conversation by mutableStateOf<ConversationInfo?>(null)
		private set
	var conversationTitle by mutableStateOf<String?>(null)
		private set
	private var lazyLoader: DatabaseManager.ConversationLazyLoader? = null
	var lazyLoadState by mutableStateOf(MessageLazyLoadState.IDLE)
		private set
	val messages = mutableStateListOf<ConversationItem>()
	val queuedFiles = mutableStateListOf<QueuedFile>()
	val messageSelectionState = MessageSelectionState()
	val conversationSuggestions = snapshotFlow { messages.toList() }
		.map { list ->
			//Make sure that we have reply suggestions enabled
			if(!Preferences.getPreferenceReplySuggestions(application)) {
				return@map listOf()
			}
			
			//Make sure that the last message is valid
			val lastMessage = list.lastOrNull() ?: return@map listOf()
			if(lastMessage !is MessageInfo
				|| lastMessage.isOutgoing
				|| lastMessage.sendStyle == SendStyleHelper.appleSendStyleBubbleInvisibleInk) {
				return@map listOf()
			}
			
			//Filter for messages
			val filteredMessages = list.takeLast(SmartReplyHelper.smartReplyHistoryLength)
				.filterIsInstance<MessageInfo>()
				.filter { it.messageText != null }
			
			if(filteredMessages.isEmpty()) {
				return@map listOf()
			}
			
			//Generate smart reply suggestions
			return@map SmartReplyHelper.generateResponses(application, filteredMessages).await()
		}
	
	private val _messageAdditionFlow = MutableSharedFlow<MessageAdditionEvent>()
	val messageAdditionFlow = _messageAdditionFlow.asSharedFlow()
	
	//Sound
	val soundPool = SoundHelper.getSoundPool()
	val soundIDMessageIncoming = soundPool.load(getApplication(), R.raw.message_in, 1)
	val soundIDMessageOutgoing = soundPool.load(getApplication(), R.raw.message_out, 1)
	
	init {
		//Load conversations
		loadConversation()
		
		//Update the conversation title
		viewModelScope.launch {
			snapshotFlow { conversation }
				.filterNotNull()
				.collectLatest { conversation ->
					//If we're generating the conversation title for the first time, display member addresses
					//until we manage to resolve the full title.
					//If we already have a title and are responding to a change in conversation metadata,
					//don't flicker back to the fallback title.
					if(conversationTitle == null) {
						conversationTitle = ConversationBuildHelper.buildConversationTitleDirect(getApplication(), conversation)
					}
					conversationTitle = ConversationBuildHelper.buildConversationTitle(getApplication(), conversation).await()
				}
		}
		
		//Listen to message updates
		viewModelScope.launch {
			ReduxEmitterNetwork.messageUpdateSubject.asFlow().collect(this@MessagingViewModel::applyMessageUpdate)
		}
		
		//Register this as a foreground conversation
		ForegroundState.loadedConversationIDs.add(conversationID)
	}
	
	override fun onCleared() {
		//Release sounds
		soundPool.release()
		
		//Unregister this as a foreground conversation
		ForegroundState.loadedConversationIDs.remove(conversationID)
	}
	
	/**
	 * Loads the conversation from its ID
	 */
	fun loadConversation() {
		viewModelScope.launch {
			//Load the conversation
			val loadedConversation = withContext(Dispatchers.IO) {
				DatabaseManager.getInstance().fetchConversationInfo(getApplication(), conversationID)
			}
			conversation = loadedConversation ?: return@launch
			loadedConversation.draftMessage?.let { inputText = it }
			queuedFiles.addAll(loadedConversation.draftFiles.map { QueuedFile(it) })
			
			//Initialize the lazy loader
			val lazyLoader = DatabaseManager.ConversationLazyLoader(DatabaseManager.getInstance(), loadedConversation)
			this@MessagingViewModel.lazyLoader = lazyLoader
			
			//Load the initial messages
			withContext(Dispatchers.IO) {
				lazyLoader.loadNextChunk(getApplication(), messageChunkSize)
			}.let { messages.addAll(it) }
		}
	}
	
	/**
	 * Loads past message history
	 */
	fun loadPastMessages() {
		//Ignore if we're still loading or we're not idle
		if(messages.isEmpty() || lazyLoadState != MessageLazyLoadState.IDLE) {
			return
		}
		
		//Get the lazy loader
		val lazyLoader = lazyLoader ?: return
		
		viewModelScope.launch {
			//Set the state to loading
			lazyLoadState = MessageLazyLoadState.LOADING
			
			//Fetch messages
			val loadedMessages = withContext(Dispatchers.IO) {
				lazyLoader.loadNextChunk(getApplication(), messageChunkSize)
			}
			
			//Handle the result
			lazyLoadState = if(loadedMessages.isNotEmpty()) {
				messages.addAll(0, loadedMessages)
				MessageLazyLoadState.COMPLETE
			} else {
				MessageLazyLoadState.IDLE
			}
		}
	}
	
	private suspend fun applyMessageUpdate(event: ReduxEventMessaging) {
		when(event) {
			is ReduxEventMessaging.Message -> {
				//Filter results relevant to this conversation
				val resultList = event.conversationItems
					.filter { it.first.localID == conversationID }
					.flatMap { it.second }
				
				//Apply the update
				applyMessageUpdate(resultList)
				
				//Notify listeners of new items
				if(resultList.any { result ->
						result.newItems.any { it is MessageInfo && it.isOutgoing }
					}) {
					_messageAdditionFlow.emit(MessageAdditionEvent.OUTGOING_MESSAGE)
				}
				
				if(resultList.any { result ->
						result.newItems.any { it is MessageInfo && !it.isOutgoing }
					}) {
					_messageAdditionFlow.emit(MessageAdditionEvent.INCOMING_MESSAGE)
				}
			}
			is ReduxEventMessaging.ConversationUpdate -> {
				//Ignore if we don't have a conversation loaded
				if(conversation == null) return
				
				val transferredConversation = event.transferredConversations.find {
					it.clientConversation.localID == conversationID
				} ?: return
				
				//Update the conversation details
				conversation = transferredConversation.serverConversation
				
				//Add transferred messages
				applyMessageUpdate(transferredConversation.serverConversationItems)
			}
			is ReduxEventMessaging.MessageState -> {
				//Find a matching message
				val messageIndex = messages.indexOfLast { it.localID == event.messageID }
				if(messageIndex == -1) return
				
				//Update the message
				messages[messageIndex] = messages[messageIndex].clone().apply {
					this as MessageInfo
					
					messageState = event.stateCode
					dateRead = event.dateRead
				}
			}
			is ReduxEventMessaging.MessageError -> {
				//Find a matching message
				val messageIndex = messages.indexOfLast { it.localID == event.messageInfo.localID }
				if(messageIndex == -1) return
				
				//Update the message
				messages[messageIndex] = messages[messageIndex].clone().apply {
					this as MessageInfo
					
					errorCode = event.errorCode
					errorDetailsAvailable = event.errorDetails != null
					errorDetails = event.errorDetails
				}
			}
			is ReduxEventMessaging.MessageDelete -> {
				//Find the message
				val messageIndex = messages.indexOfFirst { it.localID == event.messageInfo.localID }
				if(messageIndex == -1) return
				
				//Remove the message
				messages.removeAt(messageIndex)
			}
			is ReduxEventMessaging.AttachmentFile -> {
				//Find the message
				val messageIndex = messages.indexOfFirst { it.localID == event.messageID }
				if(messageIndex == -1) return
				val messageInfo = messages[messageIndex] as? MessageInfo ?: return
				
				//Find the attachment
				val attachmentList = messageInfo.attachments.toMutableList()
				val attachmentIndex = attachmentList.indexOfFirst { it.localID == event.attachmentID }
				if(attachmentIndex == -1) return
				
				//Update the attachment
				attachmentList[attachmentIndex] = attachmentList[attachmentIndex].copy(
					file = event.file,
					downloadFileName = event.downloadName,
					downloadFileType = event.downloadType
				)
				
				//Update the message
				messages[messageIndex] = messageInfo.clone().apply {
					attachments = attachmentList
				}
			}
			is ReduxEventMessaging.TapbackUpdate -> {
				//Find the message
				val messageIndex = messages.indexOfFirst { it.localID == event.metadata.messageID }
				if(messageIndex == -1) return
				val messageInfo = messages[messageIndex] as? MessageInfo ?: return
				
				//Find the component
				val component = messageInfo.getComponentOrNull(event.metadata.componentIndex) ?: return
				
				//Update the component's tapbacks
				val updatedComponent = if(event.isAddition) {
					component.copy(
						tapbacks = component.tapbacks.toMutableList().also { list ->
							val existingTapbackIndex = list.indexOfFirst { it.sender == event.tapbackInfo.sender }
							if(existingTapbackIndex == -1) {
								list.add(event.tapbackInfo)
							} else {
								list[existingTapbackIndex] = list[existingTapbackIndex].copy(code = event.tapbackInfo.code)
							}
						}
					)
				} else {
					component.copy(
						tapbacks = component.tapbacks.filter { it.sender != event.tapbackInfo.sender }.toMutableList()
					)
				}
				
				//Update the message
				messages[messageIndex] = messageInfo.clone().apply {
					updateComponent(event.metadata.componentIndex, updatedComponent)
				}
			}
			is ReduxEventMessaging.StickerAdd -> {
				//Find the message
				val messageIndex = messages.indexOfFirst { it.localID == event.metadata.messageID }
				if(messageIndex == -1) return
				val messageInfo = messages[messageIndex] as? MessageInfo ?: return
				
				//Find the component
				val component = messageInfo.getComponentOrNull(event.metadata.componentIndex) ?: return
				
				//Add the sticker
				val updatedComponent = component.copy(
					stickers = (component.stickers + event.stickerInfo).toMutableList()
				)
				
				//Update the message
				messages[messageIndex] = messageInfo.clone().apply {
					updateComponent(event.metadata.componentIndex, updatedComponent)
				}
			}
			is ReduxEventMessaging.ConversationMember -> {
				//Ignore if the event is not relevant to this conversation
				if(event.conversationInfo.localID != conversationID) return
				
				//Ignore if the conversation isn't loaded
				val loadedConversation = conversation ?: return
				
				//Update the members
				conversation = loadedConversation.copy(
					members = loadedConversation.members.toMutableList().also { list ->
						if(event.isJoin) {
							list.add(event.member)
						} else {
							list.removeAll { it.address != event.member.address }
						}
					}
				)
			}
			is ReduxEventMessaging.ConversationTitle -> {
				//Ignore if the event is not relevant to this conversation
				if(event.conversationInfo.localID != conversationID) return
				
				//Ignore if the conversation isn't loaded
				val loadedConversation = conversation ?: return
				
				//Update the conversation title
				conversation = loadedConversation.copy(
					title = event.title
				)
			}
			else -> {}
		}
	}
	
	private fun applyMessageUpdate(resultList: List<ReplaceInsertResult>) {
		for(result in resultList) {
			//Add new items
			for(newItem in result.newItems) {
				val messageBeforeIndex = messages.indexOfLast { item ->
					ConversationHelper.conversationItemComparator.compare(newItem, item) > 0
				}
				val insertIndex = messageBeforeIndex + 1
				
				messages.add(insertIndex, newItem)
			}
			
			//Apply updated items
			for(updatedItem in result.updatedItems) {
				val updatedItemIndex = messages.indexOfLast {
					it.localID == updatedItem.localID
				}
				if(updatedItemIndex != -1) {
					messages[updatedItemIndex] = updatedItem
				}
			}
		}
	}
	
	private fun addQueuedFiles(newQueuedFiles: List<QueuedFile>) {
		if(newQueuedFiles.isEmpty()) return
		
		val conversation = conversation ?: return
		
		//Add the queued file to the list
		queuedFiles.addAll(newQueuedFiles)
		
		val updateTime = System.currentTimeMillis()
		
		viewModelScope.launch {
			for(queuedFile in newQueuedFiles) {
				//Prepare the queued file
				val updatedQueuedFile = try {
					DraftActionTaskCoroutine.prepareLinkedToDraft(
						getApplication(),
						queuedFile,
						conversation.localID,
						conversation.fileCompressionTarget ?: -1,
						true,
						updateTime
					)
				} catch(exception: Exception) {
					//Log the error
					Log.w(TAG, "Failed to queue draft", exception)
					CrashlyticsBridge.recordException(exception)
					
					//Dequeue the file
					queuedFiles.remove(queuedFile)
					
					continue
				}
				
				//Update the queue
				val index = queuedFiles.indexOf(queuedFile)
				if(index == -1) return@launch
				queuedFiles[index] = updatedQueuedFile
			}
		}
	}
	
	/**
	 * Adds a list of readable blobs as queued files
	 */
	fun addQueuedFileBlobs(readableBlobList: List<ReadableBlob>) {
		viewModelScope.launch {
			addQueuedFiles(readableBlobList.map { QueuedFile.fromReadableBlob(it) })
		}
	}
	
	/**
	 * Adds a local draft file as a queued file
	 */
	fun addQueuedFile(file: LocalFile) = addQueuedFileBlobs(listOf(ReadableBlobLocalFile(file, deleteOnInvalidate = true)))
	
	/**
	 * Removes a queued file from the queue
	 */
	fun removeQueuedFile(queuedFile: QueuedFile) {
		//Get the conversation
		val conversation = conversation ?: return
		
		//Get the file
		val file = queuedFile.file.nullableB
			?: throw IllegalArgumentException("Tried to remove queued file with no file!")
		
		//Get the ID
		val localID = queuedFile.localID
			?: throw IllegalArgumentException("Tried to remove queued file with no ID!")
		
		val updateTime = System.currentTimeMillis()
		
		//Remove the file from memory
		queuedFiles.remove(queuedFile)
		ReduxEmitterNetwork.messageUpdateSubject.onNext(ConversationDraftFileUpdate(conversation, queuedFile.toFileDraft(), false, updateTime))
		
		//Remove the file from disk
		@OptIn(DelicateCoroutinesApi::class)
		GlobalScope.launch {
			withContext(Dispatchers.IO) {
				//Delete the file
				AttachmentStorageHelper.deleteContentFile(AttachmentStorageHelper.dirNameDraft, file)
				
				//Remove the item from the database
				DatabaseManager.getInstance().removeDraftReference(localID, updateTime)
			}
		}
	}
	
	/**
	 * Sends the current text and attachments
	 * @param connectionManager The connection manager to use
	 * @return Whether the message was successfully prepared and sent
	 */
	@OptIn(DelicateCoroutinesApi::class)
	fun submitInput(connectionManager: ConnectionManager?): Boolean {
		val conversation = conversation ?: return false
		
		//Sanitize input
		val cleanInputText = inputText.trim().ifEmpty { null }
		
		//Ignore if we have no content to send,
		//or if there is an attachment that hasn't been prepared
		if((cleanInputText == null && queuedFiles.isEmpty())
			|| queuedFiles.any { it.file.isA }) {
			return false
		}
		
		//Convert the queued files to local files
		val localFiles = queuedFiles.map { it.asLocalFile()!! }
		
		//Prepare and send the messages in the background
		GlobalScope.launch {
			MessageSendHelperCoroutine.prepareMessage(
				getApplication(),
				conversation,
				cleanInputText,
				localFiles
			).forEach { message ->
				MessageSendHelperCoroutine.sendMessage(
					getApplication(),
					connectionManager,
					conversation,
					message
				)
			}
		}
		
		//Clear references to draft files
		GlobalScope.launch {
			withContext(Dispatchers.IO) {
				DatabaseManager.getInstance().clearDraftReferences(conversation.localID)
				DatabaseManager.getInstance().updateConversationDraftMessage(conversation.localID, null, -1)
			}
			
			ReduxEmitterNetwork.messageUpdateSubject.onNext(ConversationDraftFileClear(conversation))
		}
		
		//Clear input
		inputText = ""
		queuedFiles.clear()
		
		//Play a sound
		SoundHelper.playSound(soundPool, soundIDMessageOutgoing)
		
		//If the conversation is archived, unarchive it
		if(conversation.isArchived) {
			GlobalScope.launch {
				ConversationActionTask.archiveConversations(
					setOf(conversation),
					false
				).await()
			}
		}
		
		return true
	}
	
	/**
	 * Sends a single file directory as an attachment
	 * @param connectionManager The connection manager to use
	 * @return Whether the message was successfully prepared and sent
	 */
	@OptIn(DelicateCoroutinesApi::class)
	fun submitFileDirect(connectionManager: ConnectionManager?, file: ReadableBlob): Boolean {
		val conversation = conversation ?: return false
		
		GlobalScope.launch {
			val fileData = file.getData()
			
			val targetFile = withContext(Dispatchers.IO) {
				//Find a target file
				val targetFile = AttachmentStorageHelper.prepareContentFile(
					context = getApplication(),
					directory = AttachmentStorageHelper.dirNameDraft,
					fileName = fileData.name ?: FileNameConstants.defaultFileName
				)
				
				//Copy and compress the draft file
				try {
					DraftActionTaskCoroutine.copyCompressAttachment(file, targetFile, conversation.fileCompressionTarget ?: -1)
				} catch(exception: Exception) {
					Log.w(TAG, "Failed to prepare direct file", exception)
					AttachmentStorageHelper.deleteContentFile(AttachmentStorageHelper.dirNameDraft, targetFile)
					return@withContext null
				} finally {
					//Clean up the file
					file.invalidate()
				}
				
				return@withContext LocalFile(
					file = targetFile,
					fileName = targetFile.name,
					fileType = fileData.type ?: MIMEConstants.defaultMIMEType,
					fileSize = fileData.size ?: -1,
					directoryID = AttachmentStorageHelper.dirNameDraft
				)
			} ?: return@launch
			
			//Send the message
			MessageSendHelperCoroutine.prepareMessage(
				getApplication(),
				conversation,
				null,
				listOf(targetFile)
			).forEach { message ->
				MessageSendHelperCoroutine.sendMessage(
					getApplication(),
					connectionManager,
					conversation,
					message
				)
			}
		}
		
		return true
	}
	
	/**
	 * Sends a text message directly
	 */
	@OptIn(DelicateCoroutinesApi::class)
	fun sendTextMessage(connectionManager: ConnectionManager?, message: String) {
		val conversation = conversation ?: return
		
		GlobalScope.launch {
			MessageSendHelperCoroutine.prepareMessage(
				getApplication(),
				conversation,
				message,
				listOf()
			).forEach { message ->
				MessageSendHelperCoroutine.sendMessage(
					getApplication(),
					connectionManager,
					conversation,
					message
				)
			}
		}
	}
	
	/**
	 * Sends location data
	 */
	@OptIn(DelicateCoroutinesApi::class)
	fun sendLocation(connectionManager: ConnectionManager?, location: LatLngInfo) {
		val conversation = conversation ?: return
		
		if(conversation.serviceHandler == ServiceHandler.appleBridge
			&& conversation.serviceType == ServiceType.appleMessage) {
			//If we're using iMessage, send a specialized location attachment
			GlobalScope.launch {
				val file = AttachmentStorageHelper.prepareContentFile(getApplication(), AttachmentStorageHelper.dirNameDraftPrepare, FileNameConstants.locationName)
				
				try {
					//Write the file
					MapLocationHelper.writeLocationVCard(location, file)
				} catch(exception: IOException) {
					exception.printStackTrace()
					
					//Clean up
					AttachmentStorageHelper.deleteContentFile(AttachmentStorageHelper.dirNameDraftPrepare, file)
				}
				
				//Send the file
				val localFile = LocalFile(file, file.name, MIMEConstants.mimeTypeVLocation, file.length(), AttachmentStorageHelper.dirNameDraftPrepare)
				submitFileDirect(connectionManager, ReadableBlobLocalFile(localFile))
			}
		} else {
			//Send a text message
			sendTextMessage(connectionManager, MapLocationHelper.getMapUri(location).toString())
		}
	}
	
	/**
	 * Saves the current input text as a draft to disk
	 */
	@OptIn(DelicateCoroutinesApi::class)
	fun saveInputDraft() {
		val conversation = conversation ?: return
		
		GlobalScope.launch {
			val text = inputText.ifBlank { null }
			ConversationActionTask.setConversationDraft(conversation, text, System.currentTimeMillis()).await()
		}
	}
	
	enum class MessageAdditionEvent {
		INCOMING_MESSAGE,
		OUTGOING_MESSAGE
	}
	
	private companion object {
		val TAG = MessagingViewModel::class.simpleName
		
		const val messageChunkSize = 20
	}
}

@Suppress("UNCHECKED_CAST")
class MessagingViewModelFactory(
	private val application: Application,
	private val conversationID: Long
) : ViewModelProvider.NewInstanceFactory() {
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		return MessagingViewModel(application, conversationID) as T
	}
}

enum class MessageLazyLoadState {
	//New messages can be loaded
	IDLE,
	//New messages are being loaded
	LOADING,
	//There are no new messages to load
	COMPLETE
}

class MessageSelectionState {
	var selectedMessageIDs by mutableStateOf(setOf<Long>())
		private set
	var selectedAttachmentIDs by mutableStateOf(setOf<Long>())
		private set
	
	/**
	 * Gets whether no items are selected
	 */
	fun isEmpty() = selectedMessageIDs.isEmpty() && selectedAttachmentIDs.isEmpty()
	
	/**
	 * Gets the number of selected items
	 */
	val size: Int
		get() = selectedMessageIDs.size + selectedAttachmentIDs.size
	
	/**
	 * Sets whether the provided message ID should be selected
	 * @param id The ID of the message to apply the change to
	 * @param selected True to add the selection, false to remove
	 */
	fun setSelectionMessageID(id: Long, selected: Boolean) {
		selectedMessageIDs = selectedMessageIDs.toMutableSet().apply {
			if(selected) {
				add(id)
			} else {
				remove(id)
			}
		}
	}
	
	/**
	 * Sets whether the provided attachment ID should be selected
	 * @param id The ID of the attachment to apply the change to
	 * @param selected True to add the selection, false to remove
	 */
	fun setSelectionAttachmentID(id: Long, selected: Boolean) {
		selectedAttachmentIDs = selectedAttachmentIDs.toMutableSet().apply {
			if(selected) {
				add(id)
			} else {
				remove(id)
			}
		}
	}
	
	/**
	 * Clears all selected items
	 */
	fun clear() {
		selectedMessageIDs = setOf()
		selectedAttachmentIDs = setOf()
	}
}