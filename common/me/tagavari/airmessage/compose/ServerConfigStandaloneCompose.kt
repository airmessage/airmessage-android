package me.tagavari.airmessage.compose

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import me.tagavari.airmessage.compose.component.onboarding.OnboardingManual
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.connection.ConnectionManager
import me.tagavari.airmessage.data.SharedPreferencesManager
import me.tagavari.airmessage.enums.ConnectionErrorCode
import me.tagavari.airmessage.enums.ProxyType
import me.tagavari.airmessage.service.ConnectionService

class ServerConfigStandaloneCompose : ComponentActivity() {
	//Service bindings
	private var connectionManager by mutableStateOf<ConnectionManager?>(null)
	private val serviceConnection = object : ServiceConnection {
		override fun onServiceConnected(name: ComponentName, service: IBinder) {
			val binder = service as ConnectionService.ConnectionBinder
			connectionManager = binder.connectionManager
			
			//Disconnect and disable reconnections
			prepareConnectionManager(binder.connectionManager)
		}
		
		override fun onServiceDisconnected(name: ComponentName) {
			//Restore the previous connection state
			connectionManager?.let { restoreConnectionManager(it, isComplete = false) }
			
			connectionManager = null
		}
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		WindowCompat.setDecorFitsSystemWindows(window, false)
		
		setContent {
			AirMessageAndroidTheme {
				OnboardingManual(
					modifier = Modifier.fillMaxSize(),
					connectionManager = connectionManager,
					onCancel = ::finish,
					onFinish = { connectionParams ->
						//Save the connection data to shared preferences
						SharedPreferencesManager.setProxyType(this@ServerConfigStandaloneCompose, ProxyType.direct)
						SharedPreferencesManager.setDirectConnectionDetails(this@ServerConfigStandaloneCompose, connectionParams)
						SharedPreferencesManager.setConnectionConfigured(this@ServerConfigStandaloneCompose, true)
						
						//Reset the connection manager
						connectionManager?.let { restoreConnectionManager(it, isComplete = true) }
						
						//Finish the activity
						finish()
					}
				)
			}
		}
	}
	
	override fun onStart() {
		super.onStart()
		
		//Bind to the connection service
		bindService(
			Intent(this, ConnectionService::class.java),
			serviceConnection,
			BIND_AUTO_CREATE
		)
		
		//Update the connection manager
		connectionManager?.let { prepareConnectionManager(it) }
	}
	
	override fun onStop() {
		super.onStop()
		
		//Unbind from the connection service
		unbindService(serviceConnection)
		
		//Reset the connection manager
		connectionManager?.let { restoreConnectionManager(it, isComplete = false) }
	}
	
	/**
	 * Prepares the connection manager for manual setup
	 * @param connectionManager The connection manager to configure
	 */
	private fun prepareConnectionManager(connectionManager: ConnectionManager) {
		//Disconnect and disable reconnections
		connectionManager.setDisableReconnections(true)
		connectionManager.disconnect(ConnectionErrorCode.user)
	}
	
	/**
	 * Resets the connection manager from manual setup
	 * @param connectionManager The connection manager to configure
	 * @param isComplete Whether the connection configuration was successful
	 */
	private fun restoreConnectionManager(connectionManager: ConnectionManager, isComplete: Boolean) {
		if(!isComplete) {
			//Restore the service state
			connectionManager.disconnect(ConnectionErrorCode.user)
			connectionManager.connect()
		}
		
		//Disable overrides
		connectionManager.setConnectionOverride(null)
		
		//Re-enable reconnections
		connectionManager.setDisableReconnections(false)
	}
}
