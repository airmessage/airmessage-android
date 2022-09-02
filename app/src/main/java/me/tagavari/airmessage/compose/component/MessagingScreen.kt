package me.tagavari.airmessage.compose.component

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import me.tagavari.airmessage.compose.provider.LocalAudioPlayback
import me.tagavari.airmessage.compose.provider.LocalConnectionManager
import me.tagavari.airmessage.compose.remember.MessagingMediaCaptureType
import me.tagavari.airmessage.compose.remember.rememberAudioPlayback
import me.tagavari.airmessage.compose.remember.rememberMediaCapture
import me.tagavari.airmessage.compose.remember.rememberMediaRequest
import me.tagavari.airmessage.compose.state.MessagingViewModel
import me.tagavari.airmessage.compose.state.MessagingViewModelFactory
import me.tagavari.airmessage.helper.SoundHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagingScreen(
	conversationID: Long,
	navigationIcon: @Composable () -> Unit = {},
	onClickDetails: () -> Unit
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
	
	val haptic = LocalHapticFeedback.current
	
	LaunchedEffect(Unit) {
		viewModel.messageAdditionFlow.collect { event ->
			//Scroll to the bottom when a new message is received
			scrollState.animateScrollToItem(0)
			
			//Create feedback when a new incoming message is received
			if(event == MessagingViewModel.MessageAdditionEvent.INCOMING_MESSAGE) {
				SoundHelper.playSound(viewModel.soundPool, viewModel.soundIDMessageIncoming)
				haptic.performHapticFeedback(HapticFeedbackType.LongPress)
			}
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
							modifier = Modifier
								.clip(RoundedCornerShape(12.dp))
								.clickable(onClick = onClickDetails)
								.padding(horizontal = 8.dp, vertical = 4.dp),
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
				onSend = {
					val messagePrepared = viewModel.submitInput(connectionManager)
					if(messagePrepared) {
						SoundHelper.playSound(viewModel.soundPool, viewModel.soundIDMessageOutgoing)
					}
				},
				onSendFile = { file ->
					viewModel.submitFileDirect(connectionManager, file)
				},
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
