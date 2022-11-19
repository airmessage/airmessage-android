package me.tagavari.airmessage.compose.component

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.NotificationImportant
import androidx.compose.material.icons.outlined.Update
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
import me.tagavari.airmessage.MainApplication
import me.tagavari.airmessage.R
import me.tagavari.airmessage.activity.ServerUpdate
import me.tagavari.airmessage.compose.provider.LocalConnectionManager
import me.tagavari.airmessage.compose.remember.deriveCachedValue
import me.tagavari.airmessage.connection.ConnectionManager
import me.tagavari.airmessage.data.SharedPreferencesManager
import me.tagavari.airmessage.enums.ConnectionErrorCode
import me.tagavari.airmessage.flavor.CrashlyticsBridge
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxEventConnection
import me.tagavari.airmessage.util.ServerUpdateData
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.*
import kotlin.jvm.optionals.getOrNull

private val cardSpacing = 16.dp

@OptIn(ExperimentalStdlibApi::class)
@Composable
fun StatusCardColumn() {
	val context = LocalContext.current
	
	var showPasswordDialog by rememberSaveable { mutableStateOf(false) }
	
	Column(
		modifier = Modifier.padding(start = cardSpacing, end = cardSpacing, top = cardSpacing)
	) {
		//Connection state
		val connectionState by ReduxEmitterNetwork.connectionStateSubject.subscribeAsState(
			initial = ReduxEmitterNetwork.connectionStateSubject.value
		)
		val connectionErrorCode by deriveCachedValue(
			(connectionState as? ReduxEventConnection.Disconnected)?.code
				?: ConnectionErrorCode.user
		)
		
		ConnectionErrorCard(
			modifier = Modifier.padding(bottom = cardSpacing),
			show = connectionState is ReduxEventConnection.Disconnected,
			connectionManager = LocalConnectionManager.current,
			code = connectionErrorCode,
			onRequestChangePassword = { showPasswordDialog = true }
		)
		
		//Notifications
		if(Build.VERSION.SDK_INT >= 33) {
			var notificationPermissionGranted by remember {
				mutableStateOf(
					ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
				)
			}
			val requestPermissionLauncher = rememberLauncherForActivityResult(
				ActivityResultContracts.RequestPermission()
			) { notificationPermissionGranted = it }
			
			AlertCard(
				modifier = Modifier.padding(bottom = cardSpacing),
				show = !notificationPermissionGranted,
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
		
		//Contacts
		run {
			var contactsPermissionGranted by remember {
				mutableStateOf(
					ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
				)
			}
			val requestPermissionLauncher = rememberLauncherForActivityResult(
				ActivityResultContracts.RequestPermission()
			) { permissionGranted ->
				if(permissionGranted) {
					contactsPermissionGranted = true
					MainApplication.instance.registerContactsListener(triggerImmediate = true)
				}
			}
			
			AlertCard(
				modifier = Modifier.padding(bottom = cardSpacing),
				show = !contactsPermissionGranted,
				icon = {
					Icon(
						imageVector = Icons.Outlined.Contacts,
						contentDescription = null
					)
				},
				message = {
					Text(stringResource(R.string.message_permissiondetails_contacts_listing))
				},
				button = {
					TextButton(onClick = {
						requestPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
					}) {
						Text(stringResource(R.string.action_enable))
					}
				}
			)
		}
		
		//Server update
		run {
			val serverUpdateData = ReduxEmitterNetwork.remoteUpdateSubject.subscribeAsState(Optional.empty())
				.value.getOrNull()
			
			val connectionManager = LocalConnectionManager.current
			
			AlertCard(
				modifier = Modifier.padding(bottom = cardSpacing),
				show = serverUpdateData != null,
				icon = {
					Icon(
						imageVector = Icons.Outlined.Update,
						contentDescription = null
					)
				},
				message = {
					Text(stringResource(R.string.message_serverupdate))
				},
				button = {
					TextButton(onClick = {
						if(serverUpdateData == null) return@TextButton
						startUpdateActivity(serverUpdateData, context, connectionManager)
					}) {
						Text(stringResource(R.string.action_details))
					}
				}
			)
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

private fun startUpdateActivity(
	updateData: ServerUpdateData,
	context: Context,
	connectionManager: ConnectionManager?
) {
	val intent = Intent(context, ServerUpdate::class.java)
	intent.putExtra(ServerUpdate.PARAM_UPDATE, updateData)
	if(connectionManager != null) {
		intent.putExtra(
			ServerUpdate.PARAM_SERVERVERSION,
			connectionManager.serverSoftwareVersion
		)
		intent.putExtra(
			ServerUpdate.PARAM_SERVERNAME,
			connectionManager.serverDeviceName
		)
	}
	context.startActivity(intent)
}
