package me.tagavari.airmessage.compose.component

import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.await
import me.tagavari.airmessage.compose.R
import me.tagavari.airmessage.compose.ConversationDetailsCompose
import me.tagavari.airmessage.compose.provider.LocalAudioPlayback
import me.tagavari.airmessage.compose.provider.LocalConnectionManager
import me.tagavari.airmessage.compose.remember.MessagingMediaCaptureType
import me.tagavari.airmessage.compose.remember.rememberAudioPlayback
import me.tagavari.airmessage.compose.remember.rememberMediaCapture
import me.tagavari.airmessage.compose.remember.rememberMediaRequest
import me.tagavari.airmessage.compose.state.MessagingViewModelData
import me.tagavari.airmessage.compose.util.wrapImmutableHolder
import me.tagavari.airmessage.container.ConversationReceivedContent
import me.tagavari.airmessage.container.LocalFile
import me.tagavari.airmessage.container.ReadableBlobUri
import me.tagavari.airmessage.contract.ContractCreateDynamicDocument
import me.tagavari.airmessage.data.ForegroundState
import me.tagavari.airmessage.helper.*
import me.tagavari.airmessage.messaging.AttachmentInfo
import me.tagavari.airmessage.messaging.MessageComponentText
import me.tagavari.airmessage.messaging.MessageInfo
import me.tagavari.airmessage.task.ConversationActionTask

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MessagingScreen(
	modifier: Modifier = Modifier,
	viewModel: MessagingViewModelData,
	floatingPane: Boolean = false,
	conversationID: Long,
	navigationIcon: @Composable () -> Unit = {},
	receivedContentFlow: Flow<ConversationReceivedContent>,
	scrollState: LazyListState = rememberLazyListState(),
	onProcessedReceivedContent: () -> Unit
) {
	val connectionManager = LocalConnectionManager.current
	
	val isScrolledToBottom by remember {
		derivedStateOf {
			scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset == 0
		}
	}
	
	val haptic = LocalHapticFeedback.current
	
	LaunchedEffect(Unit) {
		viewModel.messageAdditionFlow.collect { event ->
			//Scroll to the bottom when a new message is received
			scrollState.animateScrollToItem(0)
			
			//Create feedback when a new incoming message is received
			if(event == MessagingViewModelData.MessageAdditionEvent.INCOMING_MESSAGE) {
				SoundHelper.playSound(viewModel.soundPool, viewModel.soundIDMessageIncoming)
				haptic.performHapticFeedback(HapticFeedbackType.LongPress)
			}
		}
	}
	
	LaunchedEffect(viewModel.conversationSuggestions) {
		//Scroll to the bottom when reply suggestions are generated
		viewModel.conversationSuggestions
			.filter { it.isNotEmpty() }
			.collect {
				scrollState.animateScrollToItem(0)
			}
	}
	
	LaunchedEffect(receivedContentFlow) {
		//Wait until we have a valid conversation
		snapshotFlow { viewModel.conversation != null }
			.filter { it }
			//If the conversation changes, (for example the user changes
			//conversation name) don't reapply content
			.distinctUntilChanged()
			//Switch to the received content flow, such that we only
			//pull from it once we have a valid conversation
			.combine(receivedContentFlow) { _, content -> content }
			.filterNotNull()
			.collect { content ->
				content.text?.let {
					viewModel.inputText = it
				}
				viewModel.addQueuedFileBlobs(content.attachments.map { ReadableBlobUri(it) })
				
				onProcessedReceivedContent()
			}
	}
	
	val lifecycleOwner = LocalLifecycleOwner.current
	DisposableEffect(lifecycleOwner) {
		val observer = object : DefaultLifecycleObserver {
			override fun onPause(owner: LifecycleOwner) {
				//Save the draft input when the activity goes to the background
				viewModel.saveInputDraft()
			}
			
			override fun onResume(owner: LifecycleOwner) {
				//Clear unread messages when we return to the foreground
				@OptIn(DelicateCoroutinesApi::class)
				GlobalScope.launch {
					ConversationActionTask.unreadConversations(listOf(viewModel.conversationID), 0).await()
				}
			}
		}
		
		lifecycleOwner.lifecycle.addObserver(observer)
		
		onDispose {
			lifecycleOwner.lifecycle.removeObserver(observer)
		}
	}
	
	//Save the draft when this view goes out of scope
	DisposableEffect(Unit) {
		onDispose {
			viewModel.saveInputDraft()
		}
	}
	
	//Register this as a foreground conversation
	DisposableEffect(conversationID) {
		ForegroundState.loadedConversationIDs.add(conversationID)
		
		onDispose {
			ForegroundState.loadedConversationIDs.remove(conversationID)
		}
	}
	
	val isActionMode = !viewModel.messageSelectionState.isEmpty()
	fun stopActionMode() {
		viewModel.messageSelectionState.clear()
	}
	
	//Stop action mode when back is pressed
	BackHandler(isActionMode) {
		stopActionMode()
	}
	
	CompositionLocalProvider(
		LocalAudioPlayback provides rememberAudioPlayback()
	) {
		Column(
			modifier = modifier.then(
				if(floatingPane) Modifier.background(MaterialTheme.colorScheme.surface)
				else Modifier
			)
		) {
			Crossfade(targetState = isActionMode) { isActionMode ->
				if(!isActionMode) {
					CenterAlignedTopAppBar(
						title = {
							val conversationDetailsLauncher = rememberLauncherForActivityResult(contract = ConversationDetailsCompose.ResultContract) { location ->
								if(location == null) return@rememberLauncherForActivityResult
								
								viewModel.sendLocation(connectionManager, location)
							}
							
							if(floatingPane) {
								viewModel.conversationTitle?.let { title ->
									Text(
										modifier = Modifier
											.clip(RoundedCornerShape(12.dp))
											.clickable(onClick = {
												conversationDetailsLauncher.launch(
													conversationID
												)
											})
											.padding(horizontal = 8.dp, vertical = 4.dp),
										text = title,
										overflow = TextOverflow.Ellipsis,
										maxLines = 1,
									)
								}
							} else {
								Column(
									modifier = Modifier
										.padding(horizontal = 24.dp)
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
										UserIconGroup(members = conversation.members.wrapImmutableHolder())
									}
									
									Spacer(modifier = Modifier.height(1.dp))
									
									viewModel.conversationTitle?.let { title ->
										Text(
											text = title,
											style = MaterialTheme.typography.bodySmall,
											overflow = TextOverflow.Ellipsis,
											maxLines = 1
										)
									}
								}
							}
						},
						navigationIcon = navigationIcon,
						colors = TopAppBarDefaults.smallTopAppBarColors(
							containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
						)
					)
				} else {
					val selectionCount = viewModel.messageSelectionState.size
					
					TopAppBar(
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
								
								fun getSelectedMessageText(messageID: Long): MessageComponentText? {
									return (viewModel.messages.firstOrNull { it.localID == messageID } as? MessageInfo)
										?.messageTextComponent
								}
								
								fun getSelectedMessageAttachment(attachmentID: Long): AttachmentInfo? {
									return viewModel.messages.asSequence()
										.mapNotNull { it as? MessageInfo }
										.flatMap { it.attachments }
										.firstOrNull { it.localID == attachmentID }
								}
								
								//Get selected information
								val messageData = viewModel.messageSelectionState.selectedMessageIDs.firstOrNull()
									?.let { getSelectedMessageText(it) }
								val attachmentData = viewModel.messageSelectionState.selectedAttachmentIDs.firstOrNull()
									?.let { getSelectedMessageAttachment(it) }
								
								//Save to file (show if we have an attachment with a downloaded file selected)
								if(attachmentData?.file != null) {
									val saveFileLauncher = rememberLauncherForActivityResult(contract = ContractCreateDynamicDocument()) { result ->
										//Check the result of the intent
										val exportURI = result ?: return@rememberLauncherForActivityResult
										
										//Get the attachment file
										val attachmentFile = attachmentData.file
										
										//Export the file
										ExternalStorageHelper.exportFile(context, attachmentFile, exportURI)
										
										stopActionMode()
									}
									
									IconButton(onClick = {
										viewModel.messageSelectionState.selectedAttachmentIDs.firstOrNull()
											?.let { getSelectedMessageAttachment(it) }
											?.let { attachment ->
												saveFileLauncher.launch(
													ContractCreateDynamicDocument.Params(
														name = attachment.computedFileName,
														type = attachment.computedContentType
													)
												)
											}
									}) {
										Icon(
											imageVector = Icons.Outlined.SaveAlt,
											contentDescription = stringResource(id = R.string.action_save)
										)
									}
								}
								
								//Copy to clipboard
								if(messageData != null || attachmentData?.file != null) {
									IconButton(onClick = {
										//Get the clipboard manager
										val clipboardManager = context.getSystemService(ClipboardManager::class.java) ?: return@IconButton
										
										viewModel.messageSelectionState.selectedMessageIDs.firstOrNull()
											?.let { getSelectedMessageText(it) }
											?.let { message ->
												val text = LanguageHelper.textComponentToString(context.resources, message) ?: return@IconButton
												
												//Copy the text to the clipboard
												clipboardManager.setPrimaryClip(ClipData.newPlainText(null, text))
												
												//Show a confirmation toast
												if(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
													Toast.makeText(context, R.string.message_textcopied, Toast.LENGTH_SHORT).show()
												}
											}
										
										viewModel.messageSelectionState.selectedAttachmentIDs.firstOrNull()
											?.let { getSelectedMessageAttachment(it) }
											?.let { attachment ->
												val file = attachment.file ?: return@IconButton
												val fileUri = FileProvider.getUriForFile(context, AttachmentStorageHelper.getFileAuthority(context), file)
												
												//Copy the file to the clipboard
												clipboardManager.setPrimaryClip(ClipData.newUri(
													context.contentResolver,
													attachment.computedFileName,
													fileUri
												))
												
												//Show a confirmation toast
												if(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
													Toast.makeText(context, R.string.message_attachmentcopied, Toast.LENGTH_SHORT).show()
												}
											}
										
										stopActionMode()
									}) {
										Icon(
											imageVector = Icons.Outlined.ContentCopy,
											contentDescription = stringResource(id = R.string.action_copy)
										)
									}
								}
								
								//Share
								if(messageData != null || attachmentData?.file != null) {
									IconButton(onClick = {
										viewModel.messageSelectionState.selectedMessageIDs.firstOrNull()
											?.let { getSelectedMessageText(it) }
											?.let { message ->
												val text = LanguageHelper.textComponentToString(context.resources, message) ?: return@IconButton
												
												Intent().apply {
													action = Intent.ACTION_SEND
													putExtra(Intent.EXTRA_TEXT, text)
													type = "text/plain"
													clipData = ClipData.newPlainText(null, text)
												}
													.let { Intent.createChooser(it, null) }
													.let { context.startActivity(it) }
											}
										
										viewModel.messageSelectionState.selectedAttachmentIDs.firstOrNull()
											?.let { getSelectedMessageAttachment(it) }
											?.let { attachment ->
												val file = attachment.file ?: return@IconButton
												val fileUri = FileProvider.getUriForFile(context, AttachmentStorageHelper.getFileAuthority(context), file)
												
												Intent().apply {
													action = Intent.ACTION_SEND
													putExtra(Intent.EXTRA_STREAM, fileUri)
													type = attachment.contentType
													clipData = ClipData.newUri(
														context.contentResolver,
														attachment.computedFileName,
														fileUri
													)
													flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
												}
													.let { Intent.createChooser(it, null) }
													.let { context.startActivity(it) }
											}
										
										stopActionMode()
									}) {
										Icon(
											imageVector = Icons.Outlined.Share,
											contentDescription = stringResource(id = R.string.action_sharemessage)
										)
									}
								}
							}
						},
						colors = TopAppBarDefaults.smallTopAppBarColors(
							containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
						)
					)
				}
			}
			
			Box(modifier = Modifier.weight(1F)) {
				val sendEffectState = rememberSendEffectPaneState()
				
				viewModel.conversation?.let { conversation ->
					MessageList(
						modifier = Modifier.fillMaxSize(),
						conversation = conversation,
						messages = viewModel.messages,
						messageStateIndices = viewModel.messageStateIndices,
						scrollState = scrollState,
						onDownloadAttachment = { messageInfo, attachmentInfo ->
							viewModel.downloadAttachment(connectionManager, messageInfo, attachmentInfo)
						},
						messageSelectionState = viewModel.messageSelectionState,
						onLoadPastMessages = { viewModel.loadPastMessages() },
						lazyLoadState = viewModel.lazyLoadState,
						actionSuggestions = viewModel.conversationSuggestions.collectAsState(initial = listOf()).value,
						onSelectActionSuggestion = { action ->
							action.replyString?.let { message ->
								viewModel.sendTextMessage(connectionManager, message)
							}
							
							action.remoteAction?.let { remoteAction ->
								try {
									remoteAction.actionIntent.send()
								} catch(exception: PendingIntent.CanceledException) {
									exception.printStackTrace()
								}
							}
						},
						isPlayingEffect = sendEffectState.activeEffect != null,
						onPlayEffect = sendEffectState.playEffect
					)
				}
				
				SendEffectPane(
					modifier = Modifier.fillMaxSize(),
					activeEffect = sendEffectState.activeEffect,
					onFinishEffect = sendEffectState.clearEffect
				)
			}
			
			val scope = rememberCoroutineScope()
			val attachmentsScrollState = rememberScrollState()
			val captureMedia = rememberMediaCapture { file ->
				viewModel.addQueuedFile(
					LocalFile(
						file = file,
						fileName = file.name,
						fileType = FileHelper.getMimeType(file),
						fileSize = file.length(),
						directoryID = AttachmentStorageHelper.dirNameDraftPrepare
					)
				)
				
				scope.launch {
					attachmentsScrollState.animateScrollTo(Int.MAX_VALUE)
				}
			}
			val requestMedia = rememberMediaRequest { uriList ->
				if(uriList.isNotEmpty()) {
					viewModel.addQueuedFileBlobs(uriList.map { ReadableBlobUri(it) })
					
					scope.launch {
						attachmentsScrollState.animateScrollTo(Int.MAX_VALUE)
					}
				}
			}
			
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
					
					captureMedia.requestCamera(MessagingMediaCaptureType.PHOTO)
				},
				onOpenContentPicker = {
					scope.launch {
						requestMedia.requestMedia(10 - viewModel.queuedFiles.size)
					}
				},
				collapseButtons = viewModel.collapseInputButtons,
				onChangeCollapseButtons = { viewModel.collapseInputButtons = it },
				serviceHandler = viewModel.conversation?.serviceHandler,
				serviceType = viewModel.conversation?.serviceType,
				floating = !isScrolledToBottom,
				rounded = floatingPane
			)
		}
	}
}