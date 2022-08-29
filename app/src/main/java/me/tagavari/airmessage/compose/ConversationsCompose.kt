package me.tagavari.airmessage.compose

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import me.tagavari.airmessage.activity.Messaging
import me.tagavari.airmessage.activity.NewMessage
import me.tagavari.airmessage.activity.Preferences
import me.tagavari.airmessage.compose.component.ConversationList
import me.tagavari.airmessage.compose.state.ConversationsViewModel
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme

class ConversationsCompose : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		WindowCompat.setDecorFitsSystemWindows(window, false)
		
		setContent {
			val context = LocalContext.current
			val viewModel = viewModel<ConversationsViewModel>()
			
			AirMessageAndroidTheme {
				ConversationList(
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
					}
				)
			}
		}
	}
}
