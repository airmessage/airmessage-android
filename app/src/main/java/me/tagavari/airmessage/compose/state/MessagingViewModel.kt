package me.tagavari.airmessage.compose.state

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.withContext
import me.tagavari.airmessage.data.DatabaseManager
import me.tagavari.airmessage.flavor.CrashlyticsBridge
import me.tagavari.airmessage.helper.AttachmentStorageHelper
import me.tagavari.airmessage.helper.AttachmentStorageHelper.deleteContentFile
import me.tagavari.airmessage.helper.ConversationBuildHelper
import me.tagavari.airmessage.helper.ConversationHelper
import me.tagavari.airmessage.messaging.*
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxEventMessaging
import me.tagavari.airmessage.redux.ReduxEventMessaging.ConversationDraftFileUpdate
import me.tagavari.airmessage.task.DraftActionTask
import me.tagavari.airmessage.util.ReplaceInsertResult
import java.io.File

class MessagingViewModel(
	application: Application,
	private val conversationID: Long
) : AndroidViewModel(application) {
	var conversation by mutableStateOf<ConversationInfo?>(null)
		private set
	var conversationTitle by mutableStateOf<String?>(null)
		private set
	private var lazyLoader: DatabaseManager.ConversationLazyLoader? = null
	var messages = mutableStateListOf<ConversationItem>()
	var queuedFiles = mutableStateListOf<QueuedFile>()
	
	init {
		//Load conversations
		loadConversation()
		
		//Listen to message updates
		viewModelScope.launch {
			ReduxEmitterNetwork.messageUpdateSubject.asFlow().collect(this@MessagingViewModel::applyMessageUpdate)
		}
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
			queuedFiles.addAll(loadedConversation.draftFiles.map { QueuedFile(it) })
			println("Loaded ${queuedFiles.size} files")
			
			//Load the conversation title
			launch {
				conversationTitle = ConversationBuildHelper.buildConversationTitleDirect(getApplication(), loadedConversation)
				conversationTitle = ConversationBuildHelper.buildConversationTitle(getApplication(), loadedConversation).await()
			}
			
			//Initialize the lazy loader
			val lazyLoader = DatabaseManager.ConversationLazyLoader(DatabaseManager.getInstance(), loadedConversation)
			this@MessagingViewModel.lazyLoader = lazyLoader
			
			//Load the initial messages
			withContext(Dispatchers.IO) {
				lazyLoader.loadNextChunk(getApplication())
			}.let { messages.addAll(it) }
		}
	}
	
	private fun applyMessageUpdate(event: ReduxEventMessaging) {
		when(event) {
			is ReduxEventMessaging.Message -> {
				//Filter results relevant to this conversation
				val resultList = event.conversationItems
					.filter { it.first.localID == conversationID }
					.flatMap { it.second }
				
				applyMessageUpdate(resultList)
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
				attachmentList[attachmentIndex] = attachmentList[attachmentIndex].clone().apply {
					file = event.file
					downloadFileName = event.downloadName
					downloadFileType = event.downloadType
				}
				
				//Update the message
				messages[messageIndex] = messageInfo.clone().apply {
					attachments = attachmentList
				}
			}
			else -> {}
		}
	}
	
	private fun applyMessageUpdate(resultList: List<ReplaceInsertResult>) {
		messages = messages.toMutableStateList().also { messages ->
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
	}
	
	private fun addQueuedFile(queuedFile: QueuedFile) {
		val conversation = conversation ?: return
		
		//Add the queued file to the list
		queuedFiles.add(queuedFile)
		
		val updateTime = System.currentTimeMillis()
		
		viewModelScope.launch {
			//Prepare the queued file
			val fileDraft: FileDraft = try {
				DraftActionTask.prepareLinkedToDraft(
					getApplication(),
					queuedFile.toFileLinked(),
					conversation.localID,
					conversation.fileCompressionTarget ?: -1,
					true,
					updateTime
				).await()
			} catch(exception: Throwable) {
				//Log the error
				Log.w(TAG, "Failed to queue draft", exception)
				CrashlyticsBridge.recordException(exception)
				
				//Dequeue the file
				queuedFiles.remove(queuedFile)
				
				return@launch
			}
			
			//Update the queue
			val index = queuedFiles.indexOf(queuedFile)
			if(index == -1) return@launch
			queuedFiles[index] = QueuedFile(fileDraft)
		}
	}
	
	/**
	 * Adds a local draft file as a queued file
	 */
	fun addQueuedFile(file: File) = addQueuedFile(QueuedFile(file))
	
	/**
	 * Adds a URI as a queued file
	 */
	fun addQueuedFile(uri: Uri) {
		viewModelScope.launch {
			val queuedFile = QueuedFile.fromURI(uri, getApplication())
			addQueuedFile(queuedFile)
		}
	}
	
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
		viewModelScope.launch {
			withContext(Dispatchers.IO) {
				//Delete the file
				deleteContentFile(AttachmentStorageHelper.dirNameDraft, file)
				
				//Remove the item from the database
				DatabaseManager.getInstance().removeDraftReference(localID, updateTime)
			}
		}
	}
	
	private companion object {
		val TAG = MessagingViewModel::class.simpleName
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
