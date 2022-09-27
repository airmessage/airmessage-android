package me.tagavari.airmessage.compose.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import kotlinx.coroutines.rx3.await
import me.tagavari.airmessage.MainApplication
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.provider.LocalAudioPlayback
import me.tagavari.airmessage.compose.provider.LocalConnectionManager
import me.tagavari.airmessage.compose.remember.AudioPlaybackState
import me.tagavari.airmessage.compose.remember.deriveContactUpdates
import me.tagavari.airmessage.compose.remember.deriveMessagePreview
import me.tagavari.airmessage.compose.state.MessageSelectionState
import me.tagavari.airmessage.compose.state.NetworkState
import me.tagavari.airmessage.constants.MIMEConstants
import me.tagavari.airmessage.data.DatabaseManager
import me.tagavari.airmessage.data.UserCacheHelper
import me.tagavari.airmessage.enums.MessageSendErrorCode
import me.tagavari.airmessage.enums.MessageState
import me.tagavari.airmessage.helper.ConversationColorHelper
import me.tagavari.airmessage.helper.ErrorLanguageHelper.getErrorDisplay
import me.tagavari.airmessage.helper.FileHelper.compareMimeTypes
import me.tagavari.airmessage.helper.IntentHelper
import me.tagavari.airmessage.helper.LanguageHelper
import me.tagavari.airmessage.helper.MessageSendHelperCoroutine
import me.tagavari.airmessage.messaging.AttachmentInfo
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.messaging.MessageInfo
import me.tagavari.airmessage.redux.ReduxEventAttachmentDownload
import me.tagavari.airmessage.task.MessageActionTask
import me.tagavari.airmessage.util.MessageFlow
import me.tagavari.airmessage.util.MessageFlowSpacing
import me.tagavari.airmessage.util.MessagePartFlow
import java.util.*

/**
 * A message list entry that displays a [MessageInfo]
 * @param conversationInfo The conversation of the message
 * @param messageInfo The message info to display
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MessageInfoListEntry(
	conversationInfo: ConversationInfo,
	messageInfo: MessageInfo,
	flow: MessageFlow = MessageFlow(
		anchorTop = false,
		anchorBottom = false
	),
	selectionState: MessageSelectionState = MessageSelectionState(),
	showTimeDivider: Boolean = false,
	showStatus: Boolean = false,
	spacing: MessageFlowSpacing = MessageFlowSpacing.NONE,
	scrollProgress: Float = 0F,
	onDownloadAttachment: (MessageInfo, AttachmentInfo) -> Unit
) {
	val context = LocalContext.current
	
	//Compute the message information
	val senderMember = remember(messageInfo.sender, conversationInfo.members) {
		messageInfo.sender?.let { sender -> conversationInfo.members.find { it.address == sender } }
	}
	val isOutgoing = messageInfo.isOutgoing
	val displayAvatar = !isOutgoing && !flow.anchorTop
	val displaySender = conversationInfo.isGroupChat && displayAvatar
	val isUnconfirmed = messageInfo.messageState == MessageState.ghost
	
	//Load the message contact
	val userInfo by produceState<UserCacheHelper.UserInfo?>(initialValue = null, messageInfo.sender, deriveContactUpdates()) {
		messageInfo.sender?.let { sender ->
			//Get the user
			try {
				value = MainApplication.instance.userCacheHelper.getUserInfo(context, sender).await()
			} catch(exception: Throwable) {
				exception.printStackTrace()
			}
		}
	}
	
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(top = spacing.padding)
	) {
		//Time divider
		if(showTimeDivider) {
			Text(
				modifier = Modifier
					.align(Alignment.CenterHorizontally)
					.padding(horizontal = 16.dp, vertical = 8.dp),
				text = LanguageHelper.generateTimeDividerString(context, messageInfo.date),
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				textAlign = TextAlign.Center
			)
		}
		
		//Sender name
		if(displaySender) {
			(userInfo?.contactName ?: messageInfo.sender)?.let { sender ->
				Text(
					text = sender,
					modifier = Modifier.padding(start = 60.dp, bottom = 2.5.dp),
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
		}
		
		//Horizontal message split
		Row(modifier = Modifier.fillMaxWidth()) {
			//User indicator
			if(!isOutgoing && conversationInfo.isGroupChat) {
				Box(modifier = Modifier.size(40.dp, 40.dp)) {
					if(!flow.anchorTop) {
						MemberImage(
							modifier = Modifier.fillMaxSize(),
							color = Color(senderMember?.color ?: ConversationColorHelper.backupUserColor),
							thumbnailURI = userInfo?.thumbnailURI
						)
					}
				}
				
				Spacer(modifier = Modifier.width(8.dp))
			}
			
			//Message contents
			val messageContentsAlpha by animateFloatAsState(if(isUnconfirmed) 0.5F else 1F)
			Column(
				modifier = Modifier
					.weight(1F)
					.alpha(messageContentsAlpha),
				horizontalAlignment = if(isOutgoing) Alignment.End else Alignment.Start,
				verticalArrangement = Arrangement.spacedBy(MessageFlowSpacing.RELATED.padding)
			) {
				messageInfo.messageTextComponent?.let { textComponent ->
					//Maximum 70% width
					Box(
						modifier = Modifier.fillMaxWidth(0.7F),
						contentAlignment = if(isOutgoing) Alignment.TopEnd else Alignment.TopStart
					) {
						val isSelected = selectionState.selectedMessageIDs.contains(textComponent.localID)
						val messagePreview by deriveMessagePreview(messageInfo)
						
						MessageBubbleWrapper(
							isOutgoing = isOutgoing,
							stickers = textComponent.stickers,
							tapbacks = textComponent.tapbacks,
							hideStickers = isSelected
						) {
							val textFlow = MessagePartFlow(
								isOutgoing = isOutgoing,
								isSelected = isSelected,
								anchorTop = flow.anchorTop,
								anchorBottom = flow.anchorBottom || messageInfo.attachments.isNotEmpty(),
								tintRatio = scrollProgress
							)
							val onSetSelected = { selected: Boolean ->
								selectionState.setSelectionMessageID(textComponent.localID, selected)
							}
							
							messagePreview?.also { messagePreview ->
								MessageBubbleLinkPreview(
									flow = textFlow,
									preview = messagePreview,
									onClick = {
										//Open the URL
										IntentHelper.launchUri(context, Uri.parse(messagePreview.target))
									},
									onSetSelected = onSetSelected
								)
							} ?: run {
								MessageBubbleText(
									flow = textFlow,
									subject = textComponent.subject,
									text = textComponent.text,
									sendStyle = messageInfo.sendStyle,
									onSetSelected = onSetSelected
								)
							}
						}
					}
				}
				
				val attachmentsCount = messageInfo.attachments.size
				messageInfo.attachments.forEachIndexed { index, attachment ->
					val isSelected = selectionState.selectedAttachmentIDs.contains(attachment.localID)
					
					MessageBubbleWrapper(
						isOutgoing = isOutgoing,
						stickers = attachment.stickers,
						tapbacks = attachment.tapbacks,
						hideStickers = isSelected
					) {
						val attachmentFlow = MessagePartFlow(
							isOutgoing = isOutgoing,
							isSelected = isSelected,
							anchorTop = flow.anchorTop || messageInfo.messageTextComponent != null,
							anchorBottom = flow.anchorBottom || (index + 1) < attachmentsCount,
							tintRatio = scrollProgress
						)
						
						val attachmentFile = attachment.file
						if(attachmentFile == null) {
							//Get the current download state
							val downloadState = NetworkState.attachmentRequests[attachment.localID]?.collectAsState()
							
							val (bytesTotal, bytesDownloaded) = downloadState?.value?.getOrNull().let<ReduxEventAttachmentDownload?, Pair<Long, Long?>> { event ->
								when(event) {
									null -> Pair(attachment.fileSize, null)
									is ReduxEventAttachmentDownload.Start -> Pair(event.fileLength, 0)
									is ReduxEventAttachmentDownload.Progress -> Pair(event.bytesTotal, event.bytesProgress)
									is ReduxEventAttachmentDownload.Complete -> Pair(attachment.fileSize, attachment.fileSize)
								}
							}
							
							val isDownloading = downloadState?.value?.isSuccess == true
							
							MessageBubbleDownload(
								flow = attachmentFlow,
								name = attachment.fileName,
								bytesTotal = bytesTotal,
								bytesDownloaded = bytesDownloaded,
								isDownloading = isDownloading,
								onClick = { onDownloadAttachment(messageInfo, attachment) },
								enabled = !isDownloading,
								onSetSelected = { selected ->
									selectionState.setSelectionAttachmentID(attachment.localID, selected)
								}
							)
						} else {
							when {
								compareMimeTypes(attachment.contentType, MIMEConstants.mimeTypeImage)
										|| compareMimeTypes(attachment.contentType, MIMEConstants.mimeTypeVideo) -> {
									MessageBubbleVisual(
										flow = attachmentFlow,
										file = attachmentFile,
										type = attachment.contentType,
										onClick = {
											IntentHelper.openAttachmentFile(
												context,
												attachmentFile,
												attachment.computedContentType
											)
										},
										onSetSelected = { selected ->
											selectionState.setSelectionAttachmentID(attachment.localID, selected)
										},
										sendStyle = messageInfo.sendStyle
									)
								}
								compareMimeTypes(attachment.contentType, MIMEConstants.mimeTypeAudio) -> {
									val playbackManager = LocalAudioPlayback.current
									val playbackState by playbackManager.stateForKey(key = attachmentFile)
									val scope = rememberCoroutineScope()
									
									MessageBubbleAudio(
										flow = attachmentFlow,
										file = attachmentFile,
										audioPlaybackState = playbackState,
										onTogglePlayback = {
											scope.launch {
												val state: AudioPlaybackState = playbackState
												
												if(state is AudioPlaybackState.Playing) {
													if(state.playing) {
														playbackManager.pause()
													} else {
														playbackManager.resume()
													}
												} else {
													playbackManager.play(key = attachmentFile, Uri.fromFile(attachmentFile))
												}
											}
										},
										onSetSelected = { selected ->
											selectionState.setSelectionAttachmentID(attachment.localID, selected)
										}
									)
								}
								compareMimeTypes(attachment.contentType, MIMEConstants.mimeTypeVCard) -> {
									MessageBubbleContact(
										flow = attachmentFlow,
										file = attachmentFile,
										onClick = {
											IntentHelper.openAttachmentFile(
												context,
												attachmentFile,
												attachment.computedContentType
											)
										},
										onSetSelected = { selected ->
											selectionState.setSelectionAttachmentID(attachment.localID, selected)
										}
									)
								}
								compareMimeTypes(attachment.contentType, MIMEConstants.mimeTypeVLocation) -> {
									MessageBubbleLocation(
										flow = attachmentFlow,
										file = attachmentFile,
										date = Date(messageInfo.date),
										onClick = { locationData ->
											//Use Google Maps to open the Maps app on Android
											Uri.parse(
												"https://www.google.com/maps/search/?api=1&query=${locationData.coords.latitude},${locationData.coords.longitude}"
											).let { IntentHelper.launchUri(context, it) }
										},
										onSetSelected = { selected ->
											selectionState.setSelectionAttachmentID(attachment.localID, selected)
										}
									)
								}
								else -> {
									MessageBubbleFile(
										flow = attachmentFlow,
										name = attachment.computedFileName ?: "",
										onClick = {
											IntentHelper.openAttachmentFile(context, attachmentFile, attachment.computedContentType)
										},
										onSetSelected = { selected ->
											selectionState.setSelectionAttachmentID(attachment.localID, selected)
										}
									)
								}
							}
						}
					}
				}
				
				AnimatedVisibility(visible = showStatus) {
					Text(
						modifier = Modifier.padding(horizontal = 2.dp),
						text = when(messageInfo.messageState) {
							MessageState.delivered ->
								AnnotatedString(stringResource(R.string.state_delivered))
							MessageState.read -> buildAnnotatedString {
								withStyle(SpanStyle(fontWeight = FontWeight.Medium)) {
									append(stringResource(R.string.state_read))
								}
								
								append(' ')
								
								append(LanguageHelper.getDeliveryStatusTime(LocalContext.current, messageInfo.dateRead))
							}
							else ->
								AnnotatedString(stringResource(R.string.part_unknown))
						},
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			}
			
			if(messageInfo.hasError) {
				val haptic = LocalHapticFeedback.current
				val scope = rememberCoroutineScope()
				
				var showErrorDialog by remember { mutableStateOf(false) }
				var showErrorDetailDialog by remember { mutableStateOf(false) }
				var errorDetailDialogText by remember { mutableStateOf<String?>(null) }
				fun openDetailDialog() {
					scope.launch {
						if(errorDetailDialogText == null) {
							val errorDetails = withContext(Dispatchers.IO) {
								DatabaseManager.getInstance().getMessageErrorDetails(messageInfo.localID)
							}
							
							errorDetailDialogText = errorDetails ?: ""
						}
						
						if(errorDetailDialogText!!.isEmpty()) {
							Toast.makeText(context, R.string.message_messageerror_details_unavailable, Toast.LENGTH_SHORT).show()
						} else {
							showErrorDetailDialog = true
						}
					}
				}
				
				CompositionLocalProvider(
					LocalMinimumTouchTargetEnforcement provides false
				) {
					Icon(
						modifier = Modifier
							.clip(CircleShape)
							.combinedClickable(
								onClick = {
									showErrorDialog = true
								},
								onLongClick = {
									haptic.performHapticFeedback(HapticFeedbackType.LongPress)
									openDetailDialog()
								},
							)
							.padding(8.dp),
						imageVector = Icons.Outlined.ErrorOutline,
						contentDescription = null,
						tint = MaterialTheme.colorScheme.error
					)
				}
				
				if(showErrorDialog) {
					val errorDisplay = remember(conversationInfo, messageInfo.errorCode) {
						getErrorDisplay(context, conversationInfo, messageInfo.errorCode)
					}
					val connectionManager = LocalConnectionManager.current
					
					AlertDialog(
						onDismissRequest = { showErrorDialog = false },
						icon = { Icon(Icons.Outlined.Feedback, contentDescription = null) },
						title = {
							Text(stringResource(id = R.string.message_messageerror_title))
						},
						text = {
							Text(errorDisplay.message)
						},
						dismissButton = {
							TextButton(onClick = { showErrorDialog = false }) {
								Text(stringResource(id = R.string.action_dismiss))
							}
						},
						confirmButton = {
							if(errorDisplay.isRecoverable) {
								TextButton(onClick = {
									//Dismiss the dialog
									showErrorDialog = false
									
									//Re-send the message
									@OptIn(DelicateCoroutinesApi::class)
									GlobalScope.launch {
										//Clear the message's error code
										MessageActionTask.updateMessageErrorCode(conversationInfo, messageInfo, MessageSendErrorCode.none, null).await()
										
										//Send the message again
										MessageSendHelperCoroutine.sendMessage(context, connectionManager, conversationInfo, messageInfo)
									}
								}) {
									Text(stringResource(id = R.string.action_retry))
								}
							} else {
								TextButton(onClick = {
									//Dismiss the dialog
									showErrorDialog = false
									
									//Remove the message
									@OptIn(DelicateCoroutinesApi::class)
									GlobalScope.launch {
										MessageActionTask.deleteMessages(context, conversationInfo, listOf(messageInfo)).await()
									}
								}) {
									Text(stringResource(id = R.string.action_deletemessage))
								}
							}
						}
					)
				}
				
				if(showErrorDetailDialog && errorDetailDialogText != null) {
					AlertDialog(
						onDismissRequest = { showErrorDetailDialog = false },
						title = {
							Text(stringResource(id = R.string.message_messageerror_details_title))
						},
						text = {
							Text(
								text = errorDetailDialogText!!,
								fontFamily = FontFamily.Monospace
							)
						},
						dismissButton = {
							TextButton(onClick = { showErrorDialog = false }) {
								Text(stringResource(id = R.string.action_dismiss))
							}
						},
						confirmButton = {
							TextButton(onClick = {
								showErrorDialog = false
								
								val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
								clipboard.setPrimaryClip(ClipData.newPlainText("Error details", errorDetailDialogText!!))
							}) {
								Text(stringResource(id = R.string.action_copy))
							}
						}
					)
				}
			}
		}
	}
}