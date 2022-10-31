package me.tagavari.airmessage.compose.component.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.rxjava3.subscribeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow
import me.tagavari.airmessage.activity.Preferences
import me.tagavari.airmessage.connection.ConnectionManager
import me.tagavari.airmessage.connection.ConnectionOverride
import me.tagavari.airmessage.data.SharedPreferencesManager
import me.tagavari.airmessage.enums.ConnectionErrorCode
import me.tagavari.airmessage.enums.ProxyType
import me.tagavari.airmessage.flavor.FirebaseAuthBridge
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxEventConnection
import me.tagavari.airmessage.util.ConnectionParams
import soup.compose.material.motion.MaterialSharedAxisX

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingNavigationPane(
	connectionManager: ConnectionManager?,
	onComplete: () -> Unit
) {
	val context = LocalContext.current
	val scope = rememberCoroutineScope()
	
	LaunchedEffect(Unit) {
		//Prevent the connection service from launching on boot
		Preferences.updateConnectionServiceBootEnabled(context, false)
	}
	
	var screen by rememberSaveable { mutableStateOf(OnboardingComposeScreen.WELCOME) }
	
	DisposableEffect(connectionManager) {
		//Disable reconnections while configuring
		connectionManager?.setDisableReconnections(true)
		
		onDispose {
			if(connectionManager == null) return@onDispose
			
			connectionManager.setConnectionOverride(null)
			connectionManager.setDisableReconnections(false)
		}
	}
	
	//Navigates back to the welcome screen from a child screen
	fun navigateWelcome() {
		screen = OnboardingComposeScreen.WELCOME
		connectionManager?.disconnect(ConnectionErrorCode.user)
		scope.launch {
			FirebaseAuthBridge.signOut(context)
		}
	}
	
	//Navigate backwards when back is pressed
	BackHandler(enabled = screen != OnboardingComposeScreen.WELCOME) {
		navigateWelcome()
	}
	
	MaterialSharedAxisX(
		modifier = Modifier.background(MaterialTheme.colorScheme.background),
		targetState = screen,
		forward = screen != OnboardingComposeScreen.WELCOME,
	) { animatedScreen ->
		when(animatedScreen) {
			OnboardingComposeScreen.WELCOME -> {
				OnboardingWelcome(
					modifier = Modifier.fillMaxSize(),
					showGoogle = FirebaseAuthBridge.isSupported,
					onClickGoogle = {
						//Start connecting
						connectionManager?.let { connectionManager ->
							screen = OnboardingComposeScreen.CONNECT
							connectionManager.setConnectionOverride(ConnectionOverride(ProxyType.connect, ConnectionParams.Security(null)))
							connectionManager.connect()
						}
					},
					onClickManual = {
						screen = OnboardingComposeScreen.MANUAL
					}
				)
			}
			OnboardingComposeScreen.CONNECT -> {
				val connectionState by ReduxEmitterNetwork.connectionStateSubject.subscribeAsState(initial = null)
				var appliedPassword by remember { mutableStateOf<String?>(null) }
				
				LaunchedEffect(Unit) {
					//Wait for when we become connected
					ReduxEmitterNetwork.connectionStateSubject.asFlow()
						.filterIsInstance<ReduxEventConnection.Connected>()
						.first()
					
					//Save the connection data to shared preferences
					SharedPreferencesManager.setProxyType(context, ProxyType.connect)
					@Suppress("BlockingMethodInNonBlockingContext")
					SharedPreferencesManager.setDirectConnectionPassword(context, appliedPassword)
					SharedPreferencesManager.setConnectionConfigured(context, true)
					
					//Disable the connection on boot
					Preferences.updateConnectionServiceBootEnabled(context, false)
					
					//Start the conversations activity
					onComplete()
				}
				
				OnboardingConnect(
					modifier = Modifier.fillMaxSize(),
					errorCode = (connectionState as? ReduxEventConnection.Disconnected)?.code,
					onEnterPassword = { password ->
						connectionManager?.let { connectionManager ->
							connectionManager.setConnectionOverride(ConnectionOverride(ProxyType.connect, ConnectionParams.Security(password)))
							connectionManager.connect()
							appliedPassword = password
						}
					},
					onReconnect = {
						connectionManager?.connect()
					},
					onCancel = ::navigateWelcome
				)
			}
			OnboardingComposeScreen.MANUAL -> {
				val submitConnectionParams = { connectionParams: ConnectionParams.Direct ->
					//Save the connection data to shared preferences
					SharedPreferencesManager.setProxyType(context, ProxyType.direct)
					SharedPreferencesManager.setDirectConnectionDetails(context, connectionParams)
					SharedPreferencesManager.setConnectionConfigured(context, true)
					
					//Enable connection on boot
					Preferences.updateConnectionServiceBootEnabled(context, Preferences.getPreferenceStartOnBoot(context))
					
					//Start the conversations activity
					onComplete()
				}
				
				OnboardingManual(
					modifier = Modifier.fillMaxSize(),
					connectionManager = connectionManager,
					allowSkip = true,
					onCancel = ::navigateWelcome,
					onFinish = submitConnectionParams,
					onSkip = {
						//Save invalid connection values
						val connectionParams = ConnectionParams.Direct("127.0.0.1", null, "password")
						submitConnectionParams(connectionParams)
					}
				)
			}
		}
	}
}

private enum class OnboardingComposeScreen {
	WELCOME,
	MANUAL,
	CONNECT
}
