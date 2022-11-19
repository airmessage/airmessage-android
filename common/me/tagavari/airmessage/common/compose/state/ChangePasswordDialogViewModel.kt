package me.tagavari.airmessage.common.compose.state

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow
import me.tagavari.airmessage.common.connection.ConnectionManager
import me.tagavari.airmessage.common.connection.ConnectionOverride
import me.tagavari.airmessage.common.data.SharedPreferencesManager
import me.tagavari.airmessage.common.enums.ConnectionState
import me.tagavari.airmessage.common.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.common.redux.ReduxEventConnection
import me.tagavari.airmessage.common.util.ConnectionParams

/**
 * A dialog that prompts the user to change their password
 */
class ChangePasswordDialogViewModel(application: Application) : AndroidViewModel(application) {
	var inputPassword by mutableStateOf("")
	var passwordHidden by mutableStateOf(true)
	
	var isLoading by mutableStateOf(false)
		private set
	var connectionError by mutableStateOf<Int?>(null)
		private set
	
	private val _isDone = MutableStateFlow(false)
	val isDone: StateFlow<Boolean> = _isDone
	
	fun submit(connectionManager: ConnectionManager?) {
		if(connectionManager == null) return
		
		viewModelScope.launch {
			//Reset the state
			connectionError = null
			isLoading = true
			
			//Start the connection process
			connectionManager.setConnectionOverride(ConnectionOverride(SharedPreferencesManager.getProxyType(getApplication()), ConnectionParams.Security(inputPassword)))
			connectionManager.connect()
			
			//Wait until we connect or fail to connect
			val connectionResult = ReduxEmitterNetwork.connectionStateSubject.asFlow()
				.filter { it.state == ConnectionState.connected || it.state == ConnectionState.disconnected }
				.first()
			
			//Apply the result
			if(connectionResult is ReduxEventConnection.Connected) {
				_isDone.emit(true)
			} else if(connectionResult is ReduxEventConnection.Disconnected) {
				connectionError = connectionResult.code
				isLoading = false
			}
		}
	}
}