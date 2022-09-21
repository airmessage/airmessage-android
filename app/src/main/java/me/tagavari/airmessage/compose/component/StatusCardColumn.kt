package me.tagavari.airmessage.compose.component

import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationImportant
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.rxjava3.subscribeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.provider.LocalConnectionManager
import me.tagavari.airmessage.data.SharedPreferencesManager
import me.tagavari.airmessage.flavor.CrashlyticsBridge
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxEventConnection
import java.io.IOException
import java.security.GeneralSecurityException

@Composable
fun StatusCardColumn() {
	val context = LocalContext.current
	
	var showPasswordDialog by rememberSaveable { mutableStateOf(false) }
	
	Column(
		modifier = Modifier.padding(16.dp),
		verticalArrangement = Arrangement.spacedBy(16.dp)
	) {
		//Connection state
		val connectionState by ReduxEmitterNetwork.connectionStateSubject.subscribeAsState(initial = null)
		connectionState.let { state ->
			if(state is ReduxEventConnection.Disconnected) {
				ConnectionErrorCard(
					connectionManager = LocalConnectionManager.current,
					code = state.code,
					onRequestChangePassword = { showPasswordDialog = true }
				)
			}
		}
		
		//Notifications
		if(Build.VERSION.SDK_INT >= 33) {
			var notificationPermissionGranted by remember {
				mutableStateOf(
					ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
				)
			}
			val requestPermissionLauncher = rememberLauncherForActivityResult(
				ActivityResultContracts.RequestPermission()
			) { notificationPermissionGranted = it}
			
			if(!notificationPermissionGranted) {
				AlertCard(
					icon = {
						Icon(
							imageVector = Icons.Outlined.NotificationImportant,
							contentDescription = null
						)
					},
					message = {
						Text(stringResource(R.string.message_permissiondetails_notifications))
					},
					button = {
						TextButton(onClick = {
							requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
						}) {
							Text(stringResource(R.string.action_enable))
						}
					}
				)
			}
		}
	}
	
	if(showPasswordDialog) {
		ChangePasswordDialog(
			onDismissRequest = { showPasswordDialog = false },
			onComplete = { password ->
				//Save the new password to disk
				try {
					SharedPreferencesManager.setDirectConnectionPassword(context, password)
				} catch(exception: GeneralSecurityException) {
					exception.printStackTrace()
					CrashlyticsBridge.recordException(exception)
				} catch(exception: IOException) {
					exception.printStackTrace()
					CrashlyticsBridge.recordException(exception)
				}
				
				//Show a confirmation toast
				Toast.makeText(context, R.string.message_passwordupdated, Toast.LENGTH_SHORT).show()
				
				//Dismiss the dialog
				showPasswordDialog = false
			}
		)
	}
}