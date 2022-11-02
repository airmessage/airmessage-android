package me.tagavari.airmessage.compose.component

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.compose.BuildConfig
import me.tagavari.airmessage.compose.R
import me.tagavari.airmessage.compose.provider.LocalConnectionManager
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.connection.ConnectionManager
import me.tagavari.airmessage.constants.ExternalLinkConstants
import me.tagavari.airmessage.constants.VersionConstants.latestCommVerString
import me.tagavari.airmessage.data.SharedPreferencesManager
import me.tagavari.airmessage.enums.ProxyType


@Composable
fun HelpPane(
	onDismissRequest: () -> Unit
) {
	val connectionManager = LocalConnectionManager.current
	val context = LocalContext.current
	
	ContentAlertDialog(
		onDismissRequest = onDismissRequest,
		title = { Text(stringResource(R.string.screen_helpandfeedback)) }
	) {
		Column(
			modifier = Modifier.padding(horizontal = 24.dp),
		) {
			//Message
			Text(stringResource(R.string.dialog_feedback_message))
			
			Spacer(modifier = Modifier.height(32.dp))
			
			//E-Mail button
			OutlinedButton(
				modifier = Modifier.fillMaxWidth(),
				onClick = { sendEmail(context, connectionManager) }
			) {
				Icon(
					Icons.Default.Mail,
					contentDescription = null
				)
				
				Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
				
				Text(stringResource(R.string.dialog_feedback_email))
			}
			
			//Discord button
			OutlinedButton(
				modifier = Modifier.fillMaxWidth(),
				onClick = { openDiscord(context) },
				colors = ButtonDefaults.textButtonColors(
					contentColor = Color(0xFF5865F2)
				)
			) {
				Icon(
					painterResource(id = R.drawable.discord),
					contentDescription = null
				)
				
				Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
				
				Text(stringResource(R.string.dialog_feedback_discord))
			}
			
			//Reddit button
			OutlinedButton(
				modifier = Modifier.fillMaxWidth(),
				onClick = { openReddit(context) },
				colors = ButtonDefaults.textButtonColors(
					contentColor = Color(0xFFFF5700)
				)
			) {
				Icon(
					painterResource(id = R.drawable.reddit),
					contentDescription = null
				)
				
				Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
				
				Text(stringResource(R.string.dialog_feedback_reddit))
			}
		}
	}
}

private fun sendEmail(context: Context, connectionManager: ConnectionManager?) {
	val currentCommunicationsVersion: String
	val serverSystemVersion: String
	val serverSoftwareVersion: String
	
	if(connectionManager != null) {
		currentCommunicationsVersion = connectionManager.communicationsVersion?.joinToString(".") ?: "(none)"
		serverSystemVersion = connectionManager.serverSystemVersion ?: "(none)"
		serverSoftwareVersion = connectionManager.serverSoftwareVersion ?: "(none)"
	} else {
		currentCommunicationsVersion = "(none)"
		serverSystemVersion = "(none)"
		serverSoftwareVersion = "(none)"
	}
	
	val proxyType = if(SharedPreferencesManager.isConnectionConfigured(context)) {
		if(SharedPreferencesManager.getProxyType(context) == ProxyType.direct) {
			"Direct"
		} else {
			"Connect"
		}
	} else {
		"(none)"
	}
	
	//Create the intent
	val intent = Intent(Intent.ACTION_SENDTO).apply {
		data = Uri.parse("mailto:")
		addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		putExtra(Intent.EXTRA_EMAIL, arrayOf(ExternalLinkConstants.feedbackEmail))
		putExtra(Intent.EXTRA_SUBJECT, "AirMessage feedback")
		putExtra(
			Intent.EXTRA_TEXT,
			"""
			---------- DEVICE INFORMATION ----------
			Device model: ${Build.MODEL}
			Android version: ${Build.VERSION.RELEASE}
			Client version: ${"BuildConfig.VERSION_NAME"}
			Build flavor: ${BuildConfig.FLAVOR}
			Communications version: $currentCommunicationsVersion (target $latestCommVerString)
			Proxy type: $proxyType
			Server system version: $serverSystemVersion
			Server software version: $serverSoftwareVersion
			""".trimIndent()
		)
	}
	
	//Launching the intent
	try {
		context.startActivity(intent)
	} catch(exception: ActivityNotFoundException) {
		Toast.makeText(context, R.string.message_intenterror_email, Toast.LENGTH_SHORT).show()
	}
}

private fun openDiscord(context: Context) {
	val intent = Intent(Intent.ACTION_VIEW, ExternalLinkConstants.discordAddress)
	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
	
	try {
		context.startActivity(intent)
	} catch(exception: ActivityNotFoundException) {
		Toast.makeText(context, R.string.message_intenterror_browser, Toast.LENGTH_SHORT).show()
	}
}

private fun openReddit(context: Context) {
	val intent = Intent(Intent.ACTION_VIEW, ExternalLinkConstants.redditAddress)
	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
	
	try {
		context.startActivity(intent)
	} catch(exception: ActivityNotFoundException) {
		Toast.makeText(context, R.string.message_intenterror_browser, Toast.LENGTH_SHORT).show()
	}
}

@Composable
@Preview
private fun HelpPanePreview() {
	AirMessageAndroidTheme {
		HelpPane(onDismissRequest = {})
	}
}
