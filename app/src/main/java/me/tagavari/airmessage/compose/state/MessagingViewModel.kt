package me.tagavari.airmessage.compose.state

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.withContext
import me.tagavari.airmessage.connection.ConnectionManager
import me.tagavari.airmessage.data.DatabaseManager
import me.tagavari.airmessage.helper.ConversationBuildHelper
import me.tagavari.airmessage.messaging.AttachmentInfo
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.messaging.ConversationItem
import me.tagavari.airmessage.messaging.MessageInfo
import me.tagavari.airmessage.redux.ReduxEventAttachmentDownload

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
	var attachmentRequests = mutableStateMapOf<String, Flow<ReduxEventAttachmentDownload?>>()
	
	init {
		loadConversation()
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
	
	/**
	 * Downloads an attachment, and adds it to the request map
	 */
	fun downloadAttachment(connectionManager: ConnectionManager, message: MessageInfo, attachment: AttachmentInfo): Boolean {
		val attachmentGUID = attachment.guid ?: return false
		val attachmentName = attachment.fileName ?: return false
		
		//Make the request
		val responseFlow = connectionManager.fetchAttachment(
			message.localID,
			attachment.localID,
			attachmentGUID,
			attachmentName
		).asFlow().stateIn(
			scope = viewModelScope,
			started = SharingStarted.Eagerly,
			initialValue = null
		)
		
		//Record the request
		attachmentRequests[attachmentGUID] = responseFlow
		
		return true
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
