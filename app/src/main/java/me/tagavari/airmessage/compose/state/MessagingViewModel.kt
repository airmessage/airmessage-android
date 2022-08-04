package me.tagavari.airmessage.compose.state

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import me.tagavari.airmessage.helper.ConversationBuildHelper
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.messaging.ConversationItem
import me.tagavari.airmessage.messaging.MessageInfo
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxEventMessaging

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
	
	init {
		//Load conversations
		loadConversation()
		
		//Listen to message updates
		viewModelScope.launch {
			ReduxEmitterNetwork.messageUpdateSubject.asFlow().collect { event ->
				when(event) {
					is ReduxEventMessaging.AttachmentFile -> {
						//Find the message
						val messageIndex = messages.indexOfFirst { it.localID == event.messageID }
						if(messageIndex == -1) return@collect
						val messageInfo = messages[messageIndex] as? MessageInfo ?: return@collect
						
						//Find the attachment
						val attachmentList = messageInfo.attachments.toMutableList()
						val attachmentIndex = attachmentList.indexOfFirst { it.localID == event.attachmentID }
						if(attachmentIndex == -1) return@collect
						
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
