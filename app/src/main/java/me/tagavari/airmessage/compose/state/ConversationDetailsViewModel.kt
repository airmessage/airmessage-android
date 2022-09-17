package me.tagavari.airmessage.compose.state

import android.app.Activity
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow
import kotlinx.coroutines.withContext
import me.tagavari.airmessage.data.DatabaseManager
import me.tagavari.airmessage.flavor.LocationBridge
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxEventMessaging
import me.tagavari.airmessage.util.LatLngInfo

class ConversationDetailsViewModel(
	application: Application,
	private val conversationID: Long
) : AndroidViewModel(application) {
	var conversation by mutableStateOf<ConversationInfo?>(null)
		private set
	
	var isLoadingCurrentLocation by mutableStateOf(false)
		private set
	private val _currentLocation = MutableStateFlow<Result<LatLngInfo>?>(null)
	val currentLocation = _currentLocation.asStateFlow()
	
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
				conversation = conversation?.copy(
					isMuted = event.isMuted
				)
			}
			is ReduxEventMessaging.ConversationArchive -> {
				conversation = conversation?.copy(
					isArchived = event.isArchived
				)
			}
			is ReduxEventMessaging.ConversationTitle -> {
				conversation = conversation?.copy(
					title = event.title
				)
			}
			is ReduxEventMessaging.ConversationMember -> {
				conversation = conversation?.let { conversation ->
					conversation.copy(
						members = conversation.members.toMutableList().apply {
							if(event.isJoin) {
								add(event.member.clone())
							} else {
								filter { it.address != event.member.address }
							}
						}
					)
				}
			}
			else -> {}
		}
	}
	
	fun loadCurrentLocation(activity: Activity) {
		viewModelScope.launch {
			isLoadingCurrentLocation = true
			
			val location = runCatching {
				LocationBridge.getLocation(activity)
			}
			_currentLocation.emit(location)
			
			isLoadingCurrentLocation = false
		}
	}
	
	fun clearCurrentLocation() {
		viewModelScope.launch {
			_currentLocation.emit(null)
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