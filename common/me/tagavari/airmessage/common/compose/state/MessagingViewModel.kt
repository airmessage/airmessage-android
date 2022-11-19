package me.tagavari.airmessage.common.compose.state

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

class MessagingViewModel(
	application: Application,
	conversationID: Long
) : AndroidViewModel(application) {
	val data = MessagingViewModelData(application, conversationID, viewModelScope)
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
