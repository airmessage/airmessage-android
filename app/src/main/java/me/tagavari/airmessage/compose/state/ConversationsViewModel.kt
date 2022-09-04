package me.tagavari.airmessage.compose.state

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow
import kotlinx.coroutines.withContext
import me.tagavari.airmessage.data.DatabaseManager
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxEventMessaging

class ConversationsViewModel(application: Application) : AndroidViewModel(application) {
	var conversations by mutableStateOf<Result<List<ConversationInfo>>?>(null)
		private set
	
	init {
		loadConversations()
	}
	
	/**
	 * Loads conversations into the conversations state
	 */
	fun loadConversations() {
		viewModelScope.launch {
			//Load conversations
			conversations = null
			conversations = withContext(Dispatchers.IO) {
				try {
					Result.success(DatabaseManager.getInstance().fetchSummaryConversations(getApplication(), false))
				} catch(throwable: Throwable) {
					Result.failure(throwable)
				}
			}
			
			//Subscribe to updates
			ReduxEmitterNetwork.messageUpdateSubject.asFlow().collect(this@ConversationsViewModel::applyMessageUpdate)
		}
	}
	
	private fun applyMessageUpdate(event: ReduxEventMessaging) {
		//Get the conversation list
		val conversationList = conversations?.getOrNull() ?: return
		
		when(event) {
			is ReduxEventMessaging.ConversationMute -> {
				conversations = Result.success(conversationList.toMutableList().also { list ->
					val index = list.indexOfFirst { it.localID == event.conversationInfo.localID }
					if(index == -1) return
					list[index] = list[index].clone().apply { isMuted = event.isMuted }
				})
			}
			is ReduxEventMessaging.ConversationArchive -> {
				conversations = Result.success(conversationList.toMutableList().also { list ->
					val index = list.indexOfFirst { it.localID == event.conversationInfo.localID }
					if(index == -1) return
					list[index] = list[index].clone().apply { isArchived = event.isArchived }
				})
			}
			is ReduxEventMessaging.ConversationDelete -> {
				conversations = Result.success(conversationList.filter { it.localID != event.conversationInfo.localID })
			}
			else -> {}
		}
	}
}