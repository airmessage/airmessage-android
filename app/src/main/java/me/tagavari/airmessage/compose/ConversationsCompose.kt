package me.tagavari.airmessage.compose

import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.rx3.asFlow
import me.tagavari.airmessage.activity.Messaging
import me.tagavari.airmessage.activity.NewMessage
import me.tagavari.airmessage.activity.Preferences
import me.tagavari.airmessage.compose.component.ConversationPane
import me.tagavari.airmessage.compose.provider.ConnectionServiceLocalProvider
import me.tagavari.airmessage.compose.provider.LocalConnectionManager
import me.tagavari.airmessage.compose.state.ConversationsViewModel
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.connection.ConnectionManager
import me.tagavari.airmessage.data.SharedPreferencesManager.isConnectionConfigured
import me.tagavari.airmessage.enums.ServiceHandler
import me.tagavari.airmessage.fragment.FragmentSync
import me.tagavari.airmessage.helper.NotificationHelper
import me.tagavari.airmessage.helper.ProgressState
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxEventConnection
import me.tagavari.airmessage.redux.ReduxEventMassRetrieval
import me.tagavari.airmessage.redux.ReduxEventMessaging

class ConversationsCompose : FragmentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		//Check if the user needs to configure their server connection
		if(!isConnectionConfigured(this)) {
			//Redirect to onboarding
			startActivity(Intent(this, OnboardingCompose::class.java))
			finish()
			return
		}
		
		WindowCompat.setDecorFitsSystemWindows(window, false)
		
		setContent {
			ConnectionServiceLocalProvider(context = this) {
				AirMessageAndroidTheme {
					val context = LocalContext.current
					val viewModel = viewModel<ConversationsViewModel>()
					
					val syncEvent by remember {
						ReduxEmitterNetwork.massRetrievalUpdateSubject.asFlow()
							.filter { it is ReduxEventMassRetrieval.Start
									|| it is ReduxEventMassRetrieval.Progress
									|| it is ReduxEventMassRetrieval.Complete
									|| it is ReduxEventMassRetrieval.Error }
					}.collectAsState(initial = null)
					
					LaunchedEffect(Unit) {
						ReduxEmitterNetwork.massRetrievalUpdateSubject.asFlow()
							.collect { event ->
								if(event is ReduxEventMassRetrieval.Complete || event is ReduxEventMassRetrieval.Error) {
									//Reload conversations
									viewModel.loadConversations()
								}
							}
					}
					
					fun showSyncFragment(connectionManager: ConnectionManager) {
						//Ignore if we're already showing the fragment
						if(supportFragmentManager.findFragmentByTag(keyFragmentSync) != null) return
						
						//Create and show the sync fragment
						val fragmentSync = FragmentSync(
							connectionManager.serverDeviceName,
							connectionManager.serverInstallationID,
							viewModel.conversations?.getOrNull()?.any { it.serviceHandler == ServiceHandler.appleBridge } ?: true
						)
						fragmentSync.isCancelable = false
						fragmentSync.show(supportFragmentManager, keyFragmentSync)
					}
					
					val connectionManager = LocalConnectionManager.current
					
					//Listen for sync events
					LaunchedEffect(connectionManager) {
						ReduxEmitterNetwork.messageUpdateSubject.asFlow()
							.filterIsInstance<ReduxEventMessaging.Sync>()
							.collect {
								if(connectionManager == null) return@collect
								showSyncFragment(connectionManager)
							}
					}
					
					//Check sync status when connected
					LaunchedEffect(connectionManager) {
						ReduxEmitterNetwork.connectionStateSubject.asFlow()
							.filterIsInstance<ReduxEventConnection.Connected>()
							.collect {
								if(connectionManager == null) return@collect
								
								println("Is connected, is pending sync: ${connectionManager.isPendingSync}")
								if(connectionManager.isPendingSync) {
									showSyncFragment(connectionManager)
								}
							}
					}
					
					ConversationPane(
						conversations = viewModel.conversations,
						onSelectConversation = { conversation ->
							//Launch the conversation activity
							Intent(context, MessagingCompose::class.java).apply {
								putExtra(Messaging.intentParamTargetID, conversation.localID)
							}.let { context.startActivity(it) }
						},
						onReloadConversations = {
							viewModel.loadConversations()
						},
						onNavigateSettings = {
							context.startActivity(Intent(context, Preferences::class.java))
						},
						onNewConversation = {
							context.startActivity(Intent(context, NewMessage::class.java))
						},
						syncState = syncEvent.let { event ->
							when(event) {
								is ReduxEventMassRetrieval.Start -> ProgressState.Indeterminate
								is ReduxEventMassRetrieval.Progress -> ProgressState.Determinate(
									event.receivedItems.toFloat() / event.totalItems.toFloat()
								)
								else -> null
							}
						},
						hasUnreadConversations = viewModel.hasUnreadConversations,
						onMarkConversationsAsRead = { viewModel.markConversationsAsRead() }
					)
				}
			}
		}
	}
	
	override fun onResume() {
		super.onResume()
		
		//Clear all message notifications
		ContextCompat.getSystemService(this, NotificationManager::class.java)?.let { notificationManager ->
			notificationManager.activeNotifications.asSequence()
				.filter { it.tag == NotificationHelper.notificationTagMessage
						|| it.id == NotificationHelper.notificationIDMessageSummary }
				.forEach {
					notificationManager.cancel(it.tag, it.id)
				}
		}
	}
	
	companion object {
		private const val keyFragmentSync = "fragment_sync"
	}
}
