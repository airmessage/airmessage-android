package me.tagavari.airmessage.compose.component

import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.provider.LocalConnectionManager
import me.tagavari.airmessage.compose.util.rememberAsyncLauncherForActivityResult
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxEventConnection

@Composable
fun StatusCardColumn() {
	val context = LocalContext.current
	
	val scope = rememberCoroutineScope()
	
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
					code = state.code
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
			val requestPermissionLauncher = rememberAsyncLauncherForActivityResult(ActivityResultContracts.RequestPermission())
			
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
							scope.launch {
								notificationPermissionGranted = requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
							}
						}) {
							Text(stringResource(R.string.action_enable))
						}
					}
				)
			}
		}
	}
}