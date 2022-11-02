package me.tagavari.airmessage.compose.component

import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import me.tagavari.airmessage.compose.R
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.connection.ConnectionManager
import me.tagavari.airmessage.constants.ExternalLinkConstants
import me.tagavari.airmessage.enums.ConnectionErrorCode
import me.tagavari.airmessage.helper.ConnectionServiceLaunchHelper
import me.tagavari.airmessage.helper.ErrorDetailsAction
import me.tagavari.airmessage.helper.ErrorDetailsHelper
import me.tagavari.airmessage.helper.IntentHelper

@Composable
fun ConnectionErrorCard(
	connectionManager: ConnectionManager?,
	@ConnectionErrorCode code: Int,
	onRequestChangePassword: () -> Unit
) {
	val errorDetails = remember(code) {
		ErrorDetailsHelper.getErrorDetails(code, false)
	}
	
	val context = LocalContext.current
	val recoverError = recoverError@{
		val button = errorDetails.button ?: return@recoverError
		when(button.action) {
			ErrorDetailsAction.RECONNECT -> {
				if(connectionManager == null) {
					ConnectionServiceLaunchHelper.launchAutomatic(context)
				} else {
					connectionManager.connect()
				}
			}
			ErrorDetailsAction.UPDATE_APP -> {
				Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
					.let { context.startActivity(it) }
			}
			ErrorDetailsAction.UPDATE_SERVER -> {
				IntentHelper.launchUri(context, ExternalLinkConstants.serverUpdateAddress)
			}
			ErrorDetailsAction.CHANGE_PASSWORD -> {
				onRequestChangePassword()
			}
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
			Text(stringResource(R.string.message_serverstatus_limitedfunctionality, stringResource(errorDetails.label)))
		},
		button = errorDetails.button?.let { button -> {
			TextButton(onClick = { recoverError() }) {
				Text(stringResource(button.label))
			}
		} }
	)
}

@Preview(widthDp = 384)
@Composable
private fun ConnectionErrorCardPreview() {
	AirMessageAndroidTheme {
		ConnectionErrorCard(
			connectionManager = null,
			code = ConnectionErrorCode.connection,
			onRequestChangePassword = {}
		)
	}
}