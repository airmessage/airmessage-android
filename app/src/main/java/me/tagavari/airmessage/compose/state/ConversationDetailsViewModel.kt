package me.tagavari.airmessage.compose.state

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow
import kotlinx.coroutines.withContext
import me.tagavari.airmessage.data.DatabaseManager
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxEventMessaging

class ConversationDetailsViewModel(
	application: Application,
	private val conversationID: Long
) : AndroidViewModel(application) {
	var conversation by mutableStateOf<ConversationInfo?>(null)
		private set
	
	init {
		viewModelScope.launch {
			//Load the conversation
			conversation = withContext(Dispatchers.IO) {
				DatabaseManager.getInstance().fetchConversationInfo(getApplication(), conversationID)
			}
			
			ReduxEmitterNetwork.messageUpdateSubject.asFlow().collect(this@ConversationDetailsViewModel::applyMessageUpdate)
		}
	}
	
	private fun applyMessageUpdate(event: ReduxEventMessaging) {
		when(event) {
			is ReduxEventMessaging.ConversationMute -> {
				conversation = conversation?.clone()?.apply {
					isMuted = event.isMuted
				}
			}
			is ReduxEventMessaging.ConversationArchive -> {
				conversation = conversation?.clone()?.apply {
					isArchived = event.isArchived
				}
			}
			is ReduxEventMessaging.ConversationTitle -> {
				conversation = conversation?.clone()?.apply {
					title = event.title
				}
			}
			is ReduxEventMessaging.ConversationMember -> {
				conversation = conversation?.clone()?.apply {
					members = members.toMutableList().apply {
						if(event.isJoin) {
							add(event.member.clone())
						} else {
							filter { it.address != event.member.address }
						}
					}
				}
			}
			else -> {}
		}
	}
}

@Suppress("UNCHECKED_CAST")
class ConversationDetailsViewModelFactory(
	private val application: Application,
	private val conversationID: Long
) : ViewModelProvider.NewInstanceFactory() {
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		return ConversationDetailsViewModel(application, conversationID) as T
	}
}