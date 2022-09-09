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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import kotlinx.coroutines.rx3.asFlow
import me.tagavari.airmessage.activity.Conversations
import me.tagavari.airmessage.activity.Preferences
import me.tagavari.airmessage.compose.component.onboarding.OnboardingConnect
import me.tagavari.airmessage.compose.component.onboarding.OnboardingManual
import me.tagavari.airmessage.compose.component.onboarding.OnboardingManualState
import me.tagavari.airmessage.compose.component.onboarding.OnboardingWelcome
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.connection.ConnectionManager
import me.tagavari.airmessage.connection.ConnectionOverride
import me.tagavari.airmessage.connection.comm5.ProxyConnect
import me.tagavari.airmessage.data.SharedPreferencesManager
import me.tagavari.airmessage.enums.ConnectionErrorCode
import me.tagavari.airmessage.enums.ConnectionState
import me.tagavari.airmessage.enums.ProxyType
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxEventConnection
import me.tagavari.airmessage.service.ConnectionService
import me.tagavari.airmessage.service.ConnectionService.ConnectionBinder
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
	
	@OptIn(ExperimentalAnimationApi::class)
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		WindowCompat.setDecorFitsSystemWindows(window, false)
		
		setContent {
			AirMessageAndroidTheme {
				LaunchedEffect(Unit) {
					//Prevent the connection service from launching on boot
					Preferences.updateConnectionServiceBootEnabled(this@OnboardingCompose, false)
				}
				
				var screen by rememberSaveable { mutableStateOf(OnboardingComposeScreen.WELCOME) }
				
				DisposableEffect(connectionManager) {
					//Disable reconnections while configuring
					connectionManager?.setDisableReconnections(true)
					
					onDispose {
						val connectionManager = connectionManager ?: return@onDispose
						
						connectionManager.setConnectionOverride(null)
						connectionManager.setDisableReconnections(false)
					}
				}
				
				//Navigate backwards when back is pressed
				BackHandler(enabled = screen != OnboardingComposeScreen.WELCOME) {
					screen = OnboardingComposeScreen.WELCOME
				}
				
				MaterialSharedAxisX(
					targetState = screen,
					forward = screen != OnboardingComposeScreen.WELCOME,
				) { animatedScreen ->
					when(animatedScreen) {
						OnboardingComposeScreen.WELCOME -> {
							OnboardingWelcome(
								modifier = Modifier.fillMaxSize(),
								showGoogle = ProxyConnect.isAvailable,
								onClickGoogle = { screen = OnboardingComposeScreen.CONNECT },
								onClickManual = { screen = OnboardingComposeScreen.MANUAL }
							)
						}
						OnboardingComposeScreen.CONNECT -> {
							OnboardingConnect(
								modifier = Modifier.fillMaxSize(),
								onEnterPassword = {},
								onReconnect = {}
							)
						}
						OnboardingComposeScreen.MANUAL -> {
							var connectionState by remember { mutableStateOf<OnboardingManualState>(OnboardingManualState.Idle) }
							
							LaunchedEffect(Unit) {
								ReduxEmitterNetwork.connectionStateSubject.asFlow().collect { update ->
									if(connectionState is OnboardingManualState.Connecting) {
										//Listen for state updates if we're connecting
										if(update.state == ConnectionState.connected) {
											val connectionManager = connectionManager ?: return@collect
											
											connectionState = OnboardingManualState.Connected(
												deviceName = connectionManager.serverDeviceName,
												fallback = connectionManager.isUsingFallback
											)
										} else if(update is ReduxEventConnection.Disconnected) {
											connectionState = OnboardingManualState.Error(
												errorCode = update.code
											)
										}
									} else if(connectionState is OnboardingManualState.Connected) {
										//If the user disconnects after connecting, discard the state
										if(update is ReduxEventConnection.Disconnected) {
											connectionState = OnboardingManualState.Idle
										}
									}
								}
							}
							
							OnboardingManual(
								modifier = Modifier.fillMaxSize(),
								state = connectionState,
								onConnect = { connectionParams ->
									connectionManager?.let { connectionManager ->
										connectionState = OnboardingManualState.Connecting
										connectionManager.setConnectionOverride(ConnectionOverride(ProxyType.direct, connectionParams))
										connectionManager.connect()
									}
								},
								onReset = {
									connectionManager?.disconnect(ConnectionErrorCode.user)
								},
								onCancel = {
									screen = OnboardingComposeScreen.WELCOME
								},
								onFinish = { connectionParams ->
									//Save the connection data to shared preferences
									SharedPreferencesManager.setProxyType(this@OnboardingCompose, ProxyType.direct)
									SharedPreferencesManager.setDirectConnectionDetails(this@OnboardingCompose, connectionParams)
									SharedPreferencesManager.setConnectionConfigured(this@OnboardingCompose, true)
									
									//Enable connection on boot
									Preferences.updateConnectionServiceBootEnabled(this@OnboardingCompose, Preferences.getPreferenceStartOnBoot(this@OnboardingCompose))
									
									//Start the conversations activity
									startActivity(Intent(this@OnboardingCompose, Conversations::class.java))
									finish()
								}
							)
						}
					}
				}
			}
		}
	}
	
	override fun onStart() {
		super.onStart()
		
		//Binding to the connection service
		bindService(
			Intent(this, ConnectionService::class.java),
			serviceConnection,
			BIND_AUTO_CREATE
		)
	}
	
	override fun onStop() {
		super.onStop()
		
		//Unbinding from the connection service
		unbindService(serviceConnection)
	}
}

private enum class OnboardingComposeScreen {
	WELCOME,
	MANUAL,
	CONNECT
}