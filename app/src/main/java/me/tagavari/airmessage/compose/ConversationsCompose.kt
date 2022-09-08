package me.tagavari.airmessage.compose

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rxjava3.subscribeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.rx3.asFlow
import me.tagavari.airmessage.activity.Messaging
import me.tagavari.airmessage.activity.NewMessage
import me.tagavari.airmessage.activity.Preferences
import me.tagavari.airmessage.compose.component.ConversationPane
import me.tagavari.airmessage.compose.provider.ConnectionServiceLocalProvider
import me.tagavari.airmessage.compose.state.ConversationsViewModel
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.helper.ProgressState
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxEventMassRetrieval

class ConversationsCompose : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		WindowCompat.setDecorFitsSystemWindows(window, false)
		
		setContent {
			ConnectionServiceLocalProvider(context = this) {
				AirMessageAndroidTheme {
					val context = LocalContext.current
					val viewModel = viewModel<ConversationsViewModel>()
					
					val syncEvent by ReduxEmitterNetwork.massRetrievalUpdateSubject.subscribeAsState(initial = null)
					LaunchedEffect(Unit) {
						ReduxEmitterNetwork.massRetrievalUpdateSubject.asFlow()
							.collect { event ->
								if(event is ReduxEventMassRetrieval.Complete || event is ReduxEventMassRetrieval.Error) {
									//Reload conversations
									viewModel.loadConversations()
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
}
