package me.tagavari.airmessage.compose

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
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
			binder.connectionManager.setDisableReconnections(true)
			binder.connectionManager.disconnect(ConnectionErrorCode.user)
		}
		
		override fun onServiceDisconnected(name: ComponentName) {
			connectionManager = null
		}
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		WindowCompat.setDecorFitsSystemWindows(window, false)
		
		setContent {
			AirMessageAndroidTheme {
				DisposableEffect(connectionManager) {
					//Disable reconnections while configuring
					connectionManager?.setDisableReconnections(true)
					
					onDispose {
						val connectionManager = connectionManager ?: return@onDispose
						
						connectionManager.setConnectionOverride(null)
						connectionManager.setDisableReconnections(false)
					}
				}
				
				OnboardingManual(
					modifier = Modifier.fillMaxSize(),
					connectionManager = connectionManager,
					onCancel = ::finish,
					onFinish = { connectionParams ->
						//Save the connection data to shared preferences
						SharedPreferencesManager.setProxyType(this@ServerConfigStandaloneCompose, ProxyType.direct)
						SharedPreferencesManager.setDirectConnectionDetails(this@ServerConfigStandaloneCompose, connectionParams)
						SharedPreferencesManager.setConnectionConfigured(this@ServerConfigStandaloneCompose, true)
						
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
	}
	
	override fun onStop() {
		super.onStop()
		
		//Unbind from the connection service
		unbindService(serviceConnection)
	}
}