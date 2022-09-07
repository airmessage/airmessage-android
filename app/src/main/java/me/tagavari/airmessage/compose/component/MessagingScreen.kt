package me.tagavari.airmessage.compose.component

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CopyAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import io.reactivex.rxjava3.kotlin.toFlowable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.ConversationDetailsCompose
import me.tagavari.airmessage.compose.provider.LocalAudioPlayback
import me.tagavari.airmessage.compose.provider.LocalConnectionManager
import me.tagavari.airmessage.compose.remember.MessagingMediaCaptureType
import me.tagavari.airmessage.compose.remember.rememberAudioPlayback
import me.tagavari.airmessage.compose.remember.rememberMediaCapture
import me.tagavari.airmessage.compose.remember.rememberMediaRequest
import me.tagavari.airmessage.compose.state.MessagingViewModel
import me.tagavari.airmessage.compose.state.MessagingViewModelFactory
import me.tagavari.airmessage.container.LocalFile
import me.tagavari.airmessage.container.ReadableBlobUri
import me.tagavari.airmessage.helper.AttachmentStorageHelper
import me.tagavari.airmessage.helper.FileHelper
import me.tagavari.airmessage.helper.LanguageHelper
import me.tagavari.airmessage.helper.SoundHelper
import me.tagavari.airmessage.messaging.MessageInfo

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MessagingScreen(
	conversationID: Long,
	navigationIcon: @Composable () -> Unit = {}
) {
	val application = LocalContext.current.applicationContext as Application
	val connectionManager = LocalConnectionManager.current
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
				Crossfade(targetState = !viewModel.messageSelectionState.isEmpty()) { isActionMode ->
					if(!isActionMode) {
						CenterAlignedTopAppBar(
							//scrollBehavior = scrollBehavior,
							title = {
								val conversationDetailsLauncher = rememberLauncherForActivityResult(contract = ConversationDetailsCompose.ResultContract) { location ->
									if(location == null) return@rememberLauncherForActivityResult
									
									viewModel.sendLocation(connectionManager, location)
								}
								
								Column(
									modifier = Modifier
										.clip(RoundedCornerShape(12.dp))
										.clickable(onClick = {
											conversationDetailsLauncher.launch(
												conversationID
											)
										})
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
					} else {
						val selectionCount = viewModel.messageSelectionState.size
						fun stopActionMode() {
							viewModel.messageSelectionState.clear()
						}
						
						SmallTopAppBar(
							title = {
								Text(pluralStringResource(id = R.plurals.message_selectioncount, selectionCount, selectionCount))
							},
							navigationIcon = {
								IconButton(onClick = { stopActionMode() }) {
									Icon(
										imageVector = Icons.Filled.Close,
										contentDescription = stringResource(id = android.R.string.cancel)
									)
								}
							},
							actions = {
								if(selectionCount == 1) {
									val context = LocalContext.current
									
									IconButton(onClick = {
										//Get the clipboard manager
										val clipboardManager = context.getSystemService(ClipboardManager::class.java) ?: return@IconButton
										
										viewModel.messageSelectionState.selectedMessageIDs.firstOrNull()?.let { messageID ->
											//Find the selected message
											val message = (viewModel.messages.firstOrNull { it.localID == messageID } as? MessageInfo)
												?.messageTextComponent ?: return@IconButton
											
											//Copy the text to the clipboard
											clipboardManager.setPrimaryClip(ClipData.newPlainText(
												null,
												LanguageHelper.textComponentToString(context.resources, message)
											))
										}
										
										viewModel.messageSelectionState.selectedAttachmentIDs.firstOrNull()?.let { attachmentID ->
											//Find the selected attachment
											val attachment = viewModel.messages.asSequence()
												.mapNotNull { it as? MessageInfo }
												.flatMap { it.attachments }
												.firstOrNull { it.localID == attachmentID }
												?: return@IconButton
											
											val file = attachment.file ?: return@IconButton
											val fileUri = FileProvider.getUriForFile(context, AttachmentStorageHelper.getFileAuthority(context), file)
											
											//Copy the file to the clipboard
											clipboardManager.setPrimaryClip(ClipData.newUri(
												context.contentResolver,
												null,
												fileUri
											))
										}
										
										//Show a confirmation toast
										if(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
											Toast.makeText(context, R.string.message_textcopied, Toast.LENGTH_SHORT).show()
										}
										
										stopActionMode()
									}) {
										Icon(
											imageVector = Icons.Outlined.ContentCopy,
											contentDescription = stringResource(id = R.string.action_copy)
										)
									}
								}
							}
						)
					}
				}
			}
			
			viewModel.conversation?.let { conversation ->
				MessageList(
					modifier = Modifier.weight(1F),
					conversation = conversation,
					messages = viewModel.messages,
					scrollState = scrollState,
					messageSelectionState = viewModel.messageSelectionState,
					onLoadPastMessages = { viewModel.loadPastMessages() },
					lazyLoadState = viewModel.lazyLoadState
				)
			} ?: Box(modifier = Modifier.weight(1F))
			
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
				onAddAttachments = { attachments ->
					viewModel.addQueuedFileBlobs(attachments)
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
								viewModel.addQueuedFile(
									LocalFile(
										file = file,
										fileName = file.name,
										fileType = FileHelper.getMimeType(file),
										fileSize = file.length(),
										directoryID = AttachmentStorageHelper.dirNameDraftPrepare
									)
								)
								
								attachmentsScrollState.animateScrollTo(Int.MAX_VALUE)
							}
					}
				},
				onOpenContentPicker = {
					scope.launch {
						val uriList = requestMedia.requestMedia(10 - viewModel.queuedFiles.size)
						if(uriList.isNotEmpty()) {
							viewModel.addQueuedFileBlobs(uriList.map { ReadableBlobUri(it) })
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
