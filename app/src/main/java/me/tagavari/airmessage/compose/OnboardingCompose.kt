package me.tagavari.airmessage.compose

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.rxjava3.subscribeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow
import me.tagavari.airmessage.activity.Preferences
import me.tagavari.airmessage.compose.component.onboarding.OnboardingConnect
import me.tagavari.airmessage.compose.component.onboarding.OnboardingManual
import me.tagavari.airmessage.compose.component.onboarding.OnboardingNavigationPane
import me.tagavari.airmessage.compose.component.onboarding.OnboardingWelcome
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.connection.ConnectionManager
import me.tagavari.airmessage.connection.ConnectionOverride
import me.tagavari.airmessage.data.SharedPreferencesManager
import me.tagavari.airmessage.enums.ConnectionErrorCode
import me.tagavari.airmessage.enums.ProxyType
import me.tagavari.airmessage.flavor.FirebaseAuthBridge
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxEventConnection
import me.tagavari.airmessage.service.ConnectionService
import me.tagavari.airmessage.service.ConnectionService.ConnectionBinder
import me.tagavari.airmessage.util.ConnectionParams
import soup.compose.material.motion.MaterialSharedAxisX

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
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		WindowCompat.setDecorFitsSystemWindows(window, false)
		
		setContent {
			AirMessageAndroidTheme {
				OnboardingNavigationPane(
					connectionManager = connectionManager,
					onComplete = {
						startActivity(Intent(this, ConversationsCompose::class.java))
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
