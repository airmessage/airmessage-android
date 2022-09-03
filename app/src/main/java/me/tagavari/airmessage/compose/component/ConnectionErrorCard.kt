package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.connection.ConnectionManager
import me.tagavari.airmessage.enums.ConnectionErrorCode
import me.tagavari.airmessage.helper.ConnectionServiceLaunchHelper
import me.tagavari.airmessage.helper.ErrorDetailsAction
import me.tagavari.airmessage.helper.ErrorDetailsHelper

@Composable
fun ConnectionErrorCard(
	connectionManager: ConnectionManager?,
	@ConnectionErrorCode code: Int
) {
	val errorDetails = remember(code) {
		ErrorDetailsHelper.getErrorDetails(code, false)
	}
	
	val context = LocalContext.current
	fun recoverError() {
		val button = errorDetails.button ?: return
		when(button.action) {
			ErrorDetailsAction.RECONNECT -> {
				if(connectionManager == null) {
					ConnectionServiceLaunchHelper.launchAutomatic(context)
				} else {
					connectionManager.connect()
				}
			}
			ErrorDetailsAction.UPDATE_APP -> {}
			ErrorDetailsAction.UPDATE_SERVER -> {}
			ErrorDetailsAction.CHANGE_PASSWORD -> {}
		}
	}
	
	Card(
		modifier = Modifier
			.fillMaxWidth()
			.padding(16.dp)
	) {
		Row(modifier = Modifier.padding(16.dp)) {
			Icon(
				imageVector = Icons.Outlined.CloudOff,
				contentDescription = null
			)
			
			Spacer(modifier = Modifier.width(16.dp))
			
			Column {
				Text(stringResource(R.string.message_serverstatus_limitedfunctionality, stringResource(errorDetails.label)))
				
				errorDetails.button?.let { button ->
					Spacer(modifier = Modifier.height(8.dp))
					
					TextButton(
						modifier = Modifier.align(Alignment.End),
						onClick = { recoverError() }
					) {
						Text(stringResource(button.label))
					}
				}
			}
		}
	}
}

@Preview(widthDp = 384)
@Composable
private fun ConnectionErrorCardPreview() {
	AirMessageAndroidTheme {
		ConnectionErrorCard(
			connectionManager = null,
			code = ConnectionErrorCode.connection
		)
	}
}