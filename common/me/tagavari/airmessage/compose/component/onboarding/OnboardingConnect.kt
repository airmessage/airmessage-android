package me.tagavari.airmessage.compose.component.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.constants.ExternalLinkConstants
import me.tagavari.airmessage.enums.ConnectionErrorCode
import me.tagavari.airmessage.helper.ErrorDetailsAction
import me.tagavari.airmessage.helper.ErrorDetailsHelper
import me.tagavari.airmessage.helper.IntentHelper
import soup.compose.material.motion.MaterialSharedAxisY

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun OnboardingConnect(
	modifier: Modifier = Modifier,
	@ConnectionErrorCode errorCode: Int? = null,
	onEnterPassword: (String) -> Unit,
	onReconnect: () -> Unit,
	onCancel: () -> Unit
) {
	val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
	
	Scaffold(
		modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
		topBar = {
			TopAppBar(
				title = {},
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
		MaterialSharedAxisY(
			targetState = errorCode,
			forward = errorCode != null
		) { errorCode ->
			Column(
				modifier = modifier
					.fillMaxWidth()
					.verticalScroll(rememberScrollState())
					.padding(paddingValues)
					.padding(24.dp),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(20.dp)
			) {
				when(errorCode) {
					null -> {
						Icon(
							modifier = Modifier.size(48.dp),
							imageVector = Icons.Outlined.Language,
							contentDescription = null
						)
						
						Text(
							text = stringResource(R.string.progress_connectionverification),
							fontWeight = FontWeight.Bold
						)
						
						LinearProgressIndicator(
							modifier = Modifier
								.padding(horizontal = 40.dp)
								.fillMaxWidth()
						)
					}
					ConnectionErrorCode.unauthorized -> {
						Icon(
							modifier = Modifier.size(48.dp),
							imageVector = Icons.Outlined.Lock,
							contentDescription = null
						)
						
						Text(
							text = stringResource(R.string.message_passwordrequired),
							fontWeight = FontWeight.Bold
						)
						
						//Password
						var inputPassword by remember { mutableStateOf("") }
						var passwordHidden by remember { mutableStateOf(true) }
						TextField(
							modifier = Modifier.fillMaxWidth(),
							value = inputPassword,
							onValueChange = { inputPassword = it },
							singleLine = true,
							label = {
								Text(stringResource(R.string.message_setup_connect_password))
							},
							visualTransformation = if(passwordHidden) PasswordVisualTransformation() else VisualTransformation.None,
							keyboardOptions = KeyboardOptions(
								keyboardType = KeyboardType.Password
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
						
						Button(
							modifier = Modifier.align(Alignment.End),
							onClick = { onEnterPassword(inputPassword) },
							enabled = inputPassword.isNotBlank()
						) {
							Text(stringResource(id = R.string.action_continue))
						}
					}
					else -> {
						Icon(
							modifier = Modifier.size(48.dp),
							imageVector = Icons.Outlined.CloudOff,
							contentDescription = null
						)
						
						Text(
							text = stringResource(R.string.message_connecterror),
							fontWeight = FontWeight.Bold
						)
						
						val errorDetails = remember(errorCode) {
							ErrorDetailsHelper.getErrorDetails(errorCode, true)
						}
						
						val context = LocalContext.current
						fun recoverError() {
							val button = errorDetails.button ?: return
							when(button.action) {
								ErrorDetailsAction.RECONNECT -> {
									onReconnect()
								}
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
						
						Text(
							modifier = Modifier.align(Alignment.Start),
							text = stringResource(id = errorDetails.label)
						)
						
						Button(
							modifier = Modifier.align(Alignment.End),
							onClick = { recoverError() },
						) {
							Text(stringResource(id = errorDetails.button?.label ?: R.string.action_retry))
						}
					}
				}
			}
		}
	}
}

@Preview(name = "Connecting", widthDp = 384)
@Composable
private fun PreviewOnboardingWelcomeInitial() {
	AirMessageAndroidTheme {
		Surface(
			color = MaterialTheme.colorScheme.background
		) {
			OnboardingConnect(
				errorCode = null,
				onEnterPassword = {},
				onReconnect = {},
				onCancel = {}
			)
		}
	}
}

@Preview(name = "Password prompt", widthDp = 384)
@Composable
private fun PreviewOnboardingWelcomePassword() {
	AirMessageAndroidTheme {
		Surface(
			color = MaterialTheme.colorScheme.background
		) {
			OnboardingConnect(
				errorCode = ConnectionErrorCode.unauthorized,
				onEnterPassword = {},
				onReconnect = {},
				onCancel = {}
			)
		}
	}
}

@Preview(name = "Connection error", widthDp = 384)
@Composable
private fun PreviewOnboardingWelcomeError() {
	AirMessageAndroidTheme {
		Surface(
			color = MaterialTheme.colorScheme.background
		) {
			OnboardingConnect(
				errorCode = ConnectionErrorCode.connectNoGroup,
				onEnterPassword = {},
				onReconnect = {},
				onCancel = {}
			)
		}
	}
}
