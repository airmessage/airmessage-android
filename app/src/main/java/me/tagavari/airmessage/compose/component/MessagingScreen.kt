package me.tagavari.airmessage.compose.component

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import me.tagavari.airmessage.compose.provider.LocalAudioPlayback
import me.tagavari.airmessage.compose.provider.LocalConnectionManager
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
	val application = LocalContext.current.applicationContext as Application
	val viewModel = viewModel<MessagingViewModel>(factory = MessagingViewModelFactory(application, conversationID))
	
	val scrollState = rememberLazyListState()
	val isScrolledToBottom by remember {
		derivedStateOf {
			scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset == 0
		}
	}
	val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
	
	LaunchedEffect(Unit) {
		//Hook up scroll to bottom listener
		viewModel.scrollToBottomFlow.collect {
			scrollState.animateScrollToItem(0)
		}
	}
	
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
					//scrollBehavior = scrollBehavior,
					title = {
						Column(
							horizontalAlignment = Alignment.CenterHorizontally,
							verticalArrangement = Arrangement.Top
						) {
							viewModel.conversation?.let { conversation ->
								UserIconGroup(members = conversation.members)
							}
							
							Spacer(modifier = Modifier.height(1.dp))
							
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
			
			val connectionManager = LocalConnectionManager.current
			val scope = rememberCoroutineScope()
			val captureMedia = rememberMediaCapture()
			val requestMedia = rememberMediaRequest()
			val attachmentsScrollState = rememberScrollState()
			
			MessageInputBar(
				modifier = Modifier
					.navigationBarsPadding()
					.imePadding(),
				messageText = viewModel.inputText,
				onMessageTextChange = { viewModel.inputText = it },
				attachments = viewModel.queuedFiles,
				onRemoveAttachment = { attachment ->
					viewModel.removeQueuedFile(attachment)
				},
				attachmentsScrollState = attachmentsScrollState,
				onSend = { viewModel.submitInput(connectionManager) },
				onTakePhoto = {
					if(viewModel.conversation == null) return@MessageInputBar
					
					scope.launch {
						captureMedia.requestCamera(MessagingMediaCaptureType.PHOTO)
							?.let { file ->
								viewModel.addQueuedFile(file)
								attachmentsScrollState.animateScrollTo(Int.MAX_VALUE)
							}
					}
				},
				onOpenContentPicker = {
					scope.launch {
						val uriList = requestMedia.requestMedia(10 - viewModel.queuedFiles.size)
						if(uriList.isNotEmpty()) {
							for(uri in uriList) {
								viewModel.addQueuedFile(uri)
							}
							
							attachmentsScrollState.animateScrollTo(Int.MAX_VALUE)
						}
					}
				},
				collapseButtons = viewModel.collapseInputButtons,
				onChangeCollapseButtons = { viewModel.collapseInputButtons = it },
				serviceHandler = viewModel.conversation?.serviceHandler,
				serviceType = viewModel.conversation?.serviceType,
				floating = !isScrolledToBottom
			)
		}
	}
}
