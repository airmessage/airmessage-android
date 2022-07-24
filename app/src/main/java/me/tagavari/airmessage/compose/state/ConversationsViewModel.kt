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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.tagavari.airmessage.data.DatabaseManager
import me.tagavari.airmessage.messaging.ConversationInfo

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
			conversations = withContext(Dispatchers.IO) {
				try {
					Result.success(DatabaseManager.getInstance().fetchSummaryConversations(getApplication(), false))
				} catch(throwable: Throwable) {
					Result.failure(throwable)
				}
			}
		}
	}
}