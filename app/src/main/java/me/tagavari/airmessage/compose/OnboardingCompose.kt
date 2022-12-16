package me.tagavari.airmessage.compose

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import me.tagavari.airmessage.compose.component.onboarding.OnboardingPane
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.connection.ConnectionManager
import me.tagavari.airmessage.enums.ConnectionErrorCode
import me.tagavari.airmessage.helper.PlatformHelper
import me.tagavari.airmessage.service.ConnectionService
import me.tagavari.airmessage.service.ConnectionService.ConnectionBinder

class OnboardingCompose : ComponentActivity() {
	//Service bindings
	private var connectionManager by mutableStateOf<ConnectionManager?>(null)
	private val serviceConnection = object : ServiceConnection {
		override fun onServiceConnected(name: ComponentName, service: IBinder) {
			val binder = service as ConnectionBinder
			connectionManager = binder.connectionManager
			
			//Disconnect and disable reconnections
			binder.connectionManager.setDisableReconnections(true)
			binder.connectionManager.disconnect(ConnectionErrorCode.user)
		}
		
		override fun onServiceDisconnected(name: ComponentName) {
			connectionManager = null
		}
	}
	
	@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		WindowCompat.setDecorFitsSystemWindows(window, false)
		
		setContent {
			AirMessageAndroidTheme {
				PlatformHelper.updateChromeOSTopBarCompose(this)
				
				OnboardingPane(
					connectionManager = connectionManager,
					onComplete = {
						startActivity(Intent(this, ConversationsCompose::class.java))
						finish()
					},
					windowSizeClass = calculateWindowSizeClass(this)
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
