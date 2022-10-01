package me.tagavari.airmessage.compose.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.rx3.asFlow
import me.tagavari.airmessage.compose.provider.LocalConnectionManager
import me.tagavari.airmessage.connection.ConnectionManager
import me.tagavari.airmessage.enums.ServiceHandler
import me.tagavari.airmessage.fragment.FragmentSync
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxEventConnection
import me.tagavari.airmessage.redux.ReduxEventMessaging

/**
 * A composable that shows the sync dialog when
 * conversations must be re-synced
 */
@Composable
fun SyncDialog(
	activity: FragmentActivity,
	conversations: List<ConversationInfo>
) {
	val connectionManager = LocalConnectionManager.current
	
	val deleteMessages by lazy(LazyThreadSafetyMode.NONE) {
		conversations.any { it.serviceHandler == ServiceHandler.appleBridge }
	}
	
	//Listen for sync events
	LaunchedEffect(connectionManager) {
		ReduxEmitterNetwork.messageUpdateSubject.asFlow()
			.filterIsInstance<ReduxEventMessaging.Sync>()
			.collect {
				if(connectionManager == null) return@collect
				
				showSyncDialog(activity, connectionManager, deleteMessages)
			}
	}
	
	//Check sync status when connected
	LaunchedEffect(connectionManager) {
		ReduxEmitterNetwork.connectionStateSubject.asFlow()
			.filterIsInstance<ReduxEventConnection.Connected>()
			.collect {
				if(connectionManager == null) return@collect
				
				if(connectionManager.isPendingSync) {
					showSyncDialog(activity, connectionManager, deleteMessages)
				}
			}
	}
}

/**
 * Opens the sync dialog for the specified activity
 * @param activity The activity for which to show the dialog
 * @param connectionManager The current connection manager
 * @param deleteMessages True to prompt the user to delete messages,
 * false to allow the user to skip
 */
private fun showSyncDialog(activity: FragmentActivity, connectionManager: ConnectionManager, deleteMessages: Boolean) {
	//Ignore if we're already showing the fragment
	if(activity.supportFragmentManager.findFragmentByTag(keyFragmentSync) != null) return
	
	//Create and show the sync fragment
	val fragmentSync = FragmentSync(
		connectionManager.serverDeviceName,
		connectionManager.serverInstallationID,
		deleteMessages
	)
	fragmentSync.isCancelable = false
	fragmentSync.show(activity.supportFragmentManager, keyFragmentSync)
}

private const val keyFragmentSync = "fragment_sync"
