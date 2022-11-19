package me.tagavari.airmessage.compose.component.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.rx3.asFlow
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.component.AlertCard
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.connection.ConnectionManager
import me.tagavari.airmessage.connection.ConnectionOverride
import me.tagavari.airmessage.constants.ExternalLinkConstants
import me.tagavari.airmessage.constants.RegexConstants
import me.tagavari.airmessage.data.SharedPreferencesManager
import me.tagavari.airmessage.enums.ConnectionErrorCode
import me.tagavari.airmessage.enums.ConnectionState
import me.tagavari.airmessage.enums.ProxyType
import me.tagavari.airmessage.helper.ErrorDetailsAction
import me.tagavari.airmessage.helper.ErrorDetailsHelper
import me.tagavari.airmessage.helper.IntentHelper
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxEventConnection
import me.tagavari.airmessage.util.ConnectionParams
import me.tagavari.airmessage.util.DirectConnectionDetails
import soup.compose.material.motion.MaterialSharedAxisY

@Composable
fun OnboardingManual(
	modifier: Modifier = Modifier,
	connectionManager: ConnectionManager?,
	allowSkip: Boolean = false,
	onCancel: () -> Unit,
	onFinish: (ConnectionParams.Direct) -> Unit,
	onSkip: () -> Unit = {}
) {
	var connectionState by remember { mutableStateOf<OnboardingManualState>(OnboardingManualState.Idle) }
	val currentConnectionManager by rememberUpdatedState(connectionManager)
	
	LaunchedEffect(Unit) {
		ReduxEmitterNetwork.connectionStateSubject.asFlow().collect { update ->
			if(connectionState is OnboardingManualState.Connecting) {
				//Listen for state updates if we're connecting
				if(update.state == ConnectionState.connected) {
					val localConnectionManager = currentConnectionManager ?: return@collect
					
					connectionState = OnboardingManualState.Connected(
						deviceName = localConnectionManager.serverDeviceName,
						fallback = localConnectionManager.isUsingFallback
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
	
	OnboardingManualLayout(
		modifier = modifier,
		state = connectionState,
		allowSkip = allowSkip,
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
		onCancel = onCancel,
		onFinish = onFinish,
		onSkip = onSkip
	)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
private fun OnboardingManualLayout(
	modifier: Modifier = Modifier,
	state: OnboardingManualState,
	allowSkip: Boolean,
	onConnect: (ConnectionParams.Direct) -> Unit,
	onReset: () -> Unit,
	onCancel: () -> Unit,
	onFinish: (ConnectionParams.Direct) -> Unit,
	onSkip: () -> Unit
) {
	val context = LocalContext.current
	val initialDetails: DirectConnectionDetails = if(LocalInspectionMode.current) {
		remember { DirectConnectionDetails(address = null, fallbackAddress = null, password = null) }
	} else {
		remember { SharedPreferencesManager.getDirectConnectionDetails(context) }
	}
	
	var inputAddress by rememberSaveable { mutableStateOf(initialDetails.address ?: "") }
	var inputFallbackAddress by rememberSaveable { mutableStateOf(initialDetails.fallbackAddress ?: "") }
	var inputPassword by rememberSaveable { mutableStateOf(initialDetails.password ?: "") }
	
	val skipOK by remember {
		derivedStateOf {
			allowSkip && inputAddress == "skip!"
		}
	}
	
	val connectionParams by remember {
		derivedStateOf {
			//Return null if the user input isn't valid
			if(!RegexConstants.internetAddress.matcher(inputAddress).find()
				|| (inputFallbackAddress.isNotEmpty() && !RegexConstants.internetAddress.matcher(inputFallbackAddress).find())
				|| inputPassword.isEmpty()) return@derivedStateOf null
			
			ConnectionParams.Direct(
				address = inputAddress,
				fallbackAddress = inputFallbackAddress.ifBlank { null },
				password = inputPassword
			)
		}
	}
	
	val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
	
	val submitInput: () -> Unit = {
		if(skipOK) {
			onSkip()
		} else {
			connectionParams?.let(onConnect)
		}
	}
	
	//Whether input fields can be modified
	val isEditable = remember(state) {
		state is OnboardingManualState.Idle || state is OnboardingManualState.Error
	}
	
	Scaffold(
		modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.screen_manualconfiguration)) },
				navigationIcon = {
					IconButton(onClick = onCancel) {
						Icon(
							imageVector = Icons.Filled.Close,
							contentDescription = stringResource(id = R.string.action_close)
						)
					}
				},
				scrollBehavior = scrollBehavior
			)
		}
	) { paddingValues ->
		Column(
			modifier = Modifier
				.verticalScroll(rememberScrollState())
				.padding(paddingValues)
				.padding(24.dp),
			verticalArrangement = Arrangement.spacedBy(20.dp)
		) {
			//Server address
			Column(modifier = Modifier.fillMaxWidth()) {
				TextField(
					modifier = Modifier.fillMaxWidth(),
					value = inputAddress,
					onValueChange = { inputAddress = it },
					singleLine = true,
					enabled = isEditable,
					label = {
						Text(stringResource(R.string.message_setup_connect_address))
					},
					keyboardOptions = KeyboardOptions(
						keyboardType = KeyboardType.Uri,
						imeAction = ImeAction.Next,
						autoCorrect = false
					)
				)
				
				ConnectionInputSupportingText(
					modifier = Modifier.align(Alignment.End),
					show = state is OnboardingManualState.Connected,
					connected = state is OnboardingManualState.Connected && !state.fallback
				)
			}
			
			//Fallback address
			Column(modifier = Modifier.fillMaxWidth()) {
				TextField(
					modifier = Modifier.fillMaxWidth(),
					value = inputFallbackAddress,
					onValueChange = { inputFallbackAddress = it },
					singleLine = true,
					enabled = isEditable,
					label = {
						Text(stringResource(R.string.message_setup_connect_fallbackaddress))
					},
					keyboardOptions = KeyboardOptions(
						keyboardType = KeyboardType.Uri,
						imeAction = ImeAction.Next,
						autoCorrect = false
					)
				)
				
				ConnectionInputSupportingText(
					modifier = Modifier.align(Alignment.End),
					show = state is OnboardingManualState.Connected && state.fallback,
					connected = true
				)
			}
			
			//Password
			var passwordHidden by rememberSaveable { mutableStateOf(true) }
			TextField(
				modifier = Modifier.fillMaxWidth(),
				value = inputPassword,
				onValueChange = { inputPassword = it },
				singleLine = true,
				enabled = isEditable,
				label = {
					Text(stringResource(R.string.message_setup_connect_password))
				},
				visualTransformation = if(passwordHidden) PasswordVisualTransformation() else VisualTransformation.None,
				keyboardOptions = KeyboardOptions(
					keyboardType = KeyboardType.Password,
					imeAction = ImeAction.Go,
					autoCorrect = false
				),
				keyboardActions = KeyboardActions(
					onGo = { submitInput() }
				),
				trailingIcon = {
					IconButton(onClick = { passwordHidden = !passwordHidden }) {
						Icon(
							imageVector = if(passwordHidden) Icons.Filled.Visibility
							else Icons.Filled.VisibilityOff,
							contentDescription = stringResource(
								if(passwordHidden) R.string.action_showpassword
								else R.string.action_hidepassword
							)
						)
					}
				}
			)
			
			MaterialSharedAxisY(
				targetState = state,
				forward = state is OnboardingManualState.Connecting || state is OnboardingManualState.Connected
			) { state ->
				Column(
					modifier = Modifier
						.fillMaxWidth(),
					verticalArrangement = Arrangement.spacedBy(20.dp)
				) {
					when(state) {
						is OnboardingManualState.Idle, is OnboardingManualState.Error -> {
							if(state is OnboardingManualState.Error) {
								val errorDetails = remember(state.errorCode) {
									ErrorDetailsHelper.getErrorDetails(state.errorCode, true)
								}
								
								@Suppress("NAME_SHADOWING")
								val context = LocalContext.current
								val recoverError: () -> Unit = recoverError@{
									val button = errorDetails.button ?: return@recoverError
									when(button.action) {
										ErrorDetailsAction.UPDATE_APP -> {
											Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
												.let { context.startActivity(it) }
										}
										ErrorDetailsAction.UPDATE_SERVER -> {
											IntentHelper.launchUri(context, ExternalLinkConstants.serverUpdateAddress)
										}
										else -> {}
									}
								}
								
								AlertCard(
									icon = {
										Icon(
											imageVector = Icons.Outlined.CloudOff,
											contentDescription = null
										)
									},
									message = {
										Text(stringResource(errorDetails.label))
									},
									button = errorDetails.button?.let { button -> {
										TextButton(onClick = { recoverError() }) {
											Text(stringResource(button.label))
										}
									} }
								)
							}
							
							Row(
								modifier = Modifier.fillMaxWidth(),
								horizontalArrangement = Arrangement.End
							) {
								Button(
									onClick = submitInput,
									enabled = connectionParams != null || skipOK
								) {
									if(skipOK) {
										Text(stringResource(R.string.action_skip))
									} else {
										Text(stringResource(R.string.action_checkconnection))
									}
								}
							}
						}
						is OnboardingManualState.Connecting -> {
							Column {
								Text(
									text = stringResource(R.string.progress_connectionverification),
									color = MaterialTheme.colorScheme.onSurfaceVariant
								)
								
								Spacer(modifier = Modifier.height(8.dp))
								
								LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
							}
						}
						is OnboardingManualState.Connected -> {
							Card(modifier = Modifier.fillMaxWidth()) {
								Row(modifier = Modifier.padding(16.dp)) {
									Icon(
										imageVector = Icons.Outlined.CheckCircleOutline,
										contentDescription = null
									)
									
									Spacer(modifier = Modifier.width(8.dp))
									
									Text(
										modifier = modifier.padding(top = 1.dp),
										text = state.deviceName?.let { name -> stringResource(R.string.message_connection_connectedcomputer, name) }
											?: stringResource(R.string.message_connection_connected)
									)
								}
							}
							
							Row(
								modifier = Modifier.fillMaxWidth(),
								horizontalArrangement = Arrangement.End
							) {
								TextButton(onClick = onReset) {
									Text(stringResource(R.string.action_back))
								}
								
								Spacer(modifier = Modifier.width(20.dp))
								
								Button(onClick = {
									connectionParams?.let(onFinish)
								}) {
									Text(stringResource(R.string.action_done))
								}
							}
						}
					}
				}
			}
		}
	}
}

sealed class OnboardingManualState {
	object Idle : OnboardingManualState()
	object Connecting : OnboardingManualState()
	data class Error(@ConnectionErrorCode val errorCode: Int) : OnboardingManualState()
	data class Connected(val deviceName: String?, val fallback: Boolean) : OnboardingManualState()
}

@Preview(name = "Initial state", widthDp = 384)
@Composable
private fun PreviewOnboardingWelcomeInitial() {
	AirMessageAndroidTheme {
		Surface(
			color = MaterialTheme.colorScheme.background
		) {
			OnboardingManualLayout(
				state = OnboardingManualState.Idle,
				allowSkip = false,
				onConnect = {},
				onReset = {},
				onCancel = {},
				onFinish = {},
				onSkip = {}
			)
		}
	}
}

@Preview(name = "Connecting", widthDp = 384)
@Composable
private fun PreviewOnboardingWelcomeConnecting() {
	AirMessageAndroidTheme {
		Surface(
			color = MaterialTheme.colorScheme.background
		) {
			OnboardingManualLayout(
				state = OnboardingManualState.Connecting,
				allowSkip = false,
				onConnect = {},
				onReset = {},
				onCancel = {},
				onFinish = {},
				onSkip = {}
			)
		}
	}
}

@Preview(name = "Connected", widthDp = 384)
@Composable
private fun PreviewOnboardingWelcomeConnected() {
	AirMessageAndroidTheme {
		Surface(
			color = MaterialTheme.colorScheme.background
		) {
			OnboardingManualLayout(
				state = OnboardingManualState.Connected(deviceName = "My Computer", fallback = true),
				allowSkip = false,
				onConnect = {},
				onReset = {},
				onCancel = {},
				onFinish = {},
				onSkip = {}
			)
		}
	}
}

@Preview(name = "Error", widthDp = 384)
@Composable
private fun PreviewOnboardingWelcomeError() {
	AirMessageAndroidTheme {
		Surface(
			color = MaterialTheme.colorScheme.background
		) {
			OnboardingManualLayout(
				state = OnboardingManualState.Error(errorCode = ConnectionErrorCode.unauthorized),
				allowSkip = false,
				onConnect = {},
				onReset = {},
				onCancel = {},
				onFinish = {},
				onSkip = {}
			)
		}
	}
}
