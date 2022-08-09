package me.tagavari.airmessage.compose.component

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
				CenterAlignedTopAppBar(
					modifier = Modifier.height(120.dp).statusBarsPadding(),
					title = {
						Column(
							horizontalAlignment = Alignment.CenterHorizontally,
							verticalArrangement = Arrangement.Center,
						) {
							viewModel.conversation?.let { conversation ->
								UserIconGroup(members = conversation.members)
							}
							
							Spacer(modifier = Modifier.height(2.dp))
							
							viewModel.conversationTitle?.let { title ->
								Text(
									text = title,
									style = MaterialTheme.typography.bodySmall
								)
							}
						}
					},
					navigationIcon = navigationIcon
				)
			}
		},
		content = { paddingValues ->
			Column {
				viewModel.conversation?.let { conversation ->
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
					serviceHandler = viewModel.conversation?.serviceHandler,
					serviceType = viewModel.conversation?.serviceType,
					floating = !isScrolledToBottom
				)
			}
		}
	)
}