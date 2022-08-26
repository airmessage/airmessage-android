package me.tagavari.airmessage.compose.component

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import me.tagavari.airmessage.compose.remember.MessagingMediaCaptureType
import me.tagavari.airmessage.compose.remember.rememberMediaCapture
import me.tagavari.airmessage.compose.remember.rememberMediaRequest
import me.tagavari.airmessage.compose.state.MessagingViewModel
import me.tagavari.airmessage.compose.state.MessagingViewModelFactory
import me.tagavari.airmessage.enums.ServiceHandler
import me.tagavari.airmessage.enums.ServiceType

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
	
	Scaffold(
		topBar = {
			Surface(tonalElevation = 2.dp) {
				CenterAlignedTopAppBar(
					modifier = Modifier
						.height(120.dp)
						.statusBarsPadding(),
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
					onMessageSent = {},
					onTakePhoto = {
						if(viewModel.conversation == null) return@MessageInputBar
						
						scope.launch {
							captureMedia.requestCamera(MessagingMediaCaptureType.PHOTO)
								?.let { viewModel.addQueuedFile(it) }
						}
					},
					onTakeVideo = {
						//Use low-res video recordings if we're sending over SMS / MMS
						val conversation = viewModel.conversation ?: return@MessageInputBar
						val useLowResMedia = (conversation.serviceHandler == ServiceHandler.appleBridge && conversation.serviceType == ServiceType.appleSMS)
								|| (conversation.serviceHandler == ServiceHandler.systemMessaging && conversation.serviceType == ServiceType.systemSMS)
						
						scope.launch {
							captureMedia.requestCamera(if(useLowResMedia) MessagingMediaCaptureType.LOW_RES_VIDEO else MessagingMediaCaptureType.VIDEO)
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
	)
}