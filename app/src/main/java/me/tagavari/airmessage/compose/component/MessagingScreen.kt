package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.withContext
import me.tagavari.airmessage.data.DatabaseManager
import me.tagavari.airmessage.helper.ConversationBuildHelper
import me.tagavari.airmessage.messaging.ConversationInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagingScreen(
	navigationIcon: @Composable () -> Unit = {},
	conversationID: Long
) {
	var showContentPicker by remember { mutableStateOf(false) }
	
	val context = LocalContext.current
	
	val conversation by produceState<ConversationInfo?>(initialValue = null, conversationID) {
		value = withContext(Dispatchers.IO) {
			DatabaseManager.getInstance().fetchConversationInfo(context, conversationID)
		}
	}
	
	val conversationTitle by produceState(
		initialValue = conversation?.let { ConversationBuildHelper.buildConversationTitleDirect(context, it) },
		conversation
	) {
		conversation?.let { conversation ->
			value = ConversationBuildHelper.buildMemberTitle(context, conversation.members).await()
		}
	}
	
	Scaffold(
		topBar = {
			Surface(tonalElevation = 2.dp) {
				SmallTopAppBar(
					title = {
						conversationTitle?.let {
							Text(it)
						}
					},
					modifier = Modifier.statusBarsPadding(),
					navigationIcon = navigationIcon
				)
			}
		},
		content = { paddingValues ->
			Column {
				MessageList(modifier = Modifier
					.weight(1F)
					.padding(paddingValues))
				MessageInputBar(
					modifier = Modifier
						.navigationBarsPadding()
						.imePadding(),
					onMessageSent = {},
					showContentPicker = showContentPicker,
					onChangeShowContentPicker = { showContentPicker = it },
					serviceHandler = conversation?.serviceHandler,
					serviceType = conversation?.serviceType
				)
			}
		}
	)
}