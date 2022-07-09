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
import me.tagavari.airmessage.data.DatabaseManager.ConversationLazyLoader
import me.tagavari.airmessage.helper.ConversationBuildHelper
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.messaging.ConversationItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagingScreen(
	navigationIcon: @Composable () -> Unit = {},
	conversationID: Long
) {
	var showContentPicker by remember { mutableStateOf(false) }
	
	val context = LocalContext.current
	
	//Load the conversation from its ID
	val conversation by produceState<ConversationInfo?>(initialValue = null, conversationID) {
		value = withContext(Dispatchers.IO) {
			DatabaseManager.getInstance().fetchConversationInfo(context, conversationID)
		}
	}
	
	//Load the conversation title
	val conversationTitle by produceState(
		initialValue = conversation?.let { ConversationBuildHelper.buildConversationTitleDirect(context, it) },
		conversation
	) {
		conversation?.let { conversation ->
			value = ConversationBuildHelper.buildMemberTitle(context, conversation.members).await()
		}
	}
	
	//Create the lazy loader
	val lazyLoader by remember {
		derivedStateOf {
			conversation?.let { conversation ->
				ConversationLazyLoader(DatabaseManager.getInstance(), conversation)
			}
		}
	}
	
	var messages by remember {
		mutableStateOf(mutableListOf<ConversationItem>())
	}
	
	LaunchedEffect(lazyLoader) {
		val lazyLoader = lazyLoader ?: return@LaunchedEffect
		
		//Load the initial messages
		messages = withContext(Dispatchers.IO) {
			lazyLoader.loadNextChunk(context)
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
				conversation?.let { conversation ->
					MessageList(
						modifier = Modifier
							.weight(1F)
							.padding(paddingValues),
						conversation = conversation,
						messages = messages
					)
				} ?: Box(modifier = Modifier.weight(1F))
				
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