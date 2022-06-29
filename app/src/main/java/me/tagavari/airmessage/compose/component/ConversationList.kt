package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import me.tagavari.airmessage.R
import me.tagavari.airmessage.enums.ConversationState
import me.tagavari.airmessage.enums.ServiceHandler
import me.tagavari.airmessage.enums.ServiceType
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.messaging.ConversationPreview
import me.tagavari.airmessage.messaging.MemberInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationList(
	modifier: Modifier = Modifier,
	conversations: Result<List<ConversationInfo>>?,
	onReloadConversations: () -> Unit
) {
	Scaffold(
		topBar = {
			LargeTopAppBar(
				title = { Text(stringResource(id = R.string.app_name)) },
			)
		},
		content = { innerPadding ->
			if(conversations == null) {
				Box(
					modifier = modifier
						.fillMaxSize()
						.padding(innerPadding)
				) {
					CircularProgressIndicator(
						modifier = Modifier.align(Alignment.Center)
					)
				}
			} else {
				conversations.onFailure {
					Column(
						modifier = modifier
							.fillMaxSize()
							.padding(innerPadding),
						verticalArrangement = Arrangement.Center,
						horizontalAlignment = Alignment.CenterHorizontally
					) {
						Text(text = stringResource(id = R.string.message_loaderror_messages))
						
						TextButton(onClick = onReloadConversations) {
							Text(text = stringResource(id = R.string.action_retry))
						}
					}
				}
				
				conversations.onSuccess { conversations ->
					LazyColumn(
						contentPadding = innerPadding
					) {
						items(conversations) { conversationInfo ->
							ConversationListEntry(conversation = conversationInfo)
						}
					}
				}
			}
		}
	)
}

@Preview(name = "Conversation list")
@Composable
fun PreviewConversationList() {
	ConversationList(
		conversations = Result.success(listOf(
			ConversationInfo(
				localID = 0,
				guid = null,
				externalID = -1,
				state = ConversationState.ready,
				serviceHandler = ServiceHandler.appleBridge,
				serviceType = ServiceType.appleMessage,
				conversationColor = 0xFFFF1744.toInt(),
				members = mutableListOf(
					MemberInfo("test", 0xFFFF1744.toInt())
				),
				title = "A cool conversation",
				unreadMessageCount = 1,
				isArchived = false,
				isMuted = true,
				messagePreview = ConversationPreview.Message(
					date = System.currentTimeMillis(),
					isOutgoing = false,
					message = "Test message",
					subject = null,
					attachments = listOf(),
					sendStyle = null,
					isError = false
				)
			)
		)),
		onReloadConversations = {}
	)
}

@Preview(name = "Loading state")
@Composable
fun PreviewConversationListLoading() {
	ConversationList(
		conversations = null,
		onReloadConversations = {}
	)
}

@Preview(name = "Error state")
@Composable
fun PreviewConversationListError() {
	ConversationList(
		conversations = Result.failure(Exception("Failed to load conversations")),
		onReloadConversations = {}
	)
}