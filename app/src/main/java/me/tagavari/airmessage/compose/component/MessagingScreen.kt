package me.tagavari.airmessage.compose.component

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.withContext
import me.tagavari.airmessage.compose.state.MessagingViewModel
import me.tagavari.airmessage.compose.state.MessagingViewModelFactory
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
	var collapseInputButtons by remember { mutableStateOf(false) }
	
	val context = LocalContext.current
	
	//Load the conversation from its ID
	val conversation by produceState<ConversationInfo?>(initialValue = null, conversationID) {
		value = withContext(Dispatchers.IO) {
			DatabaseManager.getInstance().fetchConversationInfo(context, conversationID)
		}
	}
	
	val application = LocalContext.current.applicationContext as Application
	val viewModel = viewModel<MessagingViewModel>(factory = MessagingViewModelFactory(application, conversationID))
	
	val scrollState = rememberLazyListState()
	val isScrolledToBottom by remember {
		derivedStateOf {
			scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset == 0
		}
	}
	
	Scaffold(
		topBar = {
			Surface(tonalElevation = 2.dp) {
				SmallTopAppBar(
					title = {
						viewModel.conversationTitle?.let {
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
						messages = viewModel.messages,
						scrollState = scrollState
					)
				} ?: Box(modifier = Modifier.weight(1F))
				
				MessageInputBar(
					modifier = Modifier
						.navigationBarsPadding()
						.imePadding(),
					onMessageSent = {},
					showContentPicker = showContentPicker,
					onChangeShowContentPicker = { showContentPicker = it },
					collapseButtons = collapseInputButtons,
					onChangeCollapseButtons = { collapseInputButtons = it },
					serviceHandler = conversation?.serviceHandler,
					serviceType = conversation?.serviceType,
					floating = !isScrolledToBottom
				)
			}
		}
	)
}