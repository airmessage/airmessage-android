package me.tagavari.airmessage.compose.component

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import me.tagavari.airmessage.compose.provider.LocalAudioPlayback
import me.tagavari.airmessage.compose.remember.MessagingMediaCaptureType
import me.tagavari.airmessage.compose.remember.rememberAudioPlayback
import me.tagavari.airmessage.compose.remember.rememberMediaCapture
import me.tagavari.airmessage.compose.remember.rememberMediaRequest
import me.tagavari.airmessage.compose.state.MessagingViewModel
import me.tagavari.airmessage.compose.state.MessagingViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagingScreen(
	navigationIcon: @Composable () -> Unit = {},
	conversationID: Long
) {
	var inputText by remember { mutableStateOf("") }
	var collapseInputButtons by remember { mutableStateOf(false) }
	
	val application = LocalContext.current.applicationContext as Application
	val viewModel = viewModel<MessagingViewModel>(factory = MessagingViewModelFactory(application, conversationID))
	
	val scrollState = rememberLazyListState()
	val isScrolledToBottom by remember {
		derivedStateOf {
			scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset == 0
		}
	}
	val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
	
	CompositionLocalProvider(
		LocalAudioPlayback provides rememberAudioPlayback()
	) {
		Column(
			modifier = Modifier
				.background(MaterialTheme.colorScheme.background)
				.nestedScroll(scrollBehavior.nestedScrollConnection)
		) {
			Surface(tonalElevation = 2.dp) {
				CenterAlignedTopAppBar(
					modifier = Modifier.height(120.dp),
					//scrollBehavior = scrollBehavior,
					title = {
						Column(
							horizontalAlignment = Alignment.CenterHorizontally,
							verticalArrangement = Arrangement.Center,
							modifier = Modifier.height(120.dp)
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
			
			viewModel.conversation?.let { conversation ->
				MessageList(
					modifier = Modifier.weight(1F),
					conversation = conversation,
					messages = viewModel.messages,
					scrollState = scrollState,
					onLoadPastMessages = { viewModel.loadPastMessages() },
					lazyLoadState = viewModel.lazyLoadState
				)
			} ?: Box(modifier = Modifier.weight(1F))
			
			val scope = rememberCoroutineScope()
			val captureMedia = rememberMediaCapture()
			val requestMedia = rememberMediaRequest()
			
			MessageInputBar(
				modifier = Modifier
					.navigationBarsPadding()
					.imePadding(),
				messageText = inputText,
				onMessageTextChange = { inputText = it },
				attachments = viewModel.queuedFiles,
				onRemoveAttachment = { attachment ->
					viewModel.removeQueuedFile(attachment)
				},
				onSend = {},
				onTakePhoto = {
					if(viewModel.conversation == null) return@MessageInputBar
					
					scope.launch {
						captureMedia.requestCamera(MessagingMediaCaptureType.PHOTO)
							?.let { viewModel.addQueuedFile(it) }
					}
				},
				onOpenContentPicker = {
					scope.launch {
						requestMedia.requestMedia(10 - viewModel.queuedFiles.size)
							.forEach { viewModel.addQueuedFile(it) }
					}
				},
				collapseButtons = collapseInputButtons,
				onChangeCollapseButtons = { collapseInputButtons = it },
				serviceHandler = viewModel.conversation?.serviceHandler,
				serviceType = viewModel.conversation?.serviceType,
				floating = !isScrolledToBottom
			)
		}
	}
}
