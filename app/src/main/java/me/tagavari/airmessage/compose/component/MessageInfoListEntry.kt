package me.tagavari.airmessage.compose.component

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.rx3.await
import me.tagavari.airmessage.MainApplication
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.remember.AudioPlaybackState
import me.tagavari.airmessage.compose.state.LocalConnectionManager
import me.tagavari.airmessage.compose.state.NetworkState
import me.tagavari.airmessage.constants.MIMEConstants
import me.tagavari.airmessage.data.UserCacheHelper
import me.tagavari.airmessage.enums.MessageState
import me.tagavari.airmessage.helper.ConversationColorHelper
import me.tagavari.airmessage.helper.FileHelper.compareMimeTypes
import me.tagavari.airmessage.helper.IntentHelper
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.messaging.MessageInfo
import me.tagavari.airmessage.redux.ReduxEventAttachmentDownload
import me.tagavari.airmessage.util.MessageFlow
import me.tagavari.airmessage.util.MessageFlowSpacing
import me.tagavari.airmessage.util.MessagePartFlow

/**
 * A message list entry that displays a [MessageInfo]
 * @param conversationInfo The conversation of the message
 * @param messageInfo The message info to display
 */
@Composable
fun MessageInfoListEntry(
	conversationInfo: ConversationInfo,
	messageInfo: MessageInfo,
	flow: MessageFlow = MessageFlow(
		anchorTop = false,
		anchorBottom = false
	),
	spacing: MessageFlowSpacing = MessageFlowSpacing.NONE,
	scrollProgress: Float = 0F
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
	val userInfo by produceState<UserCacheHelper.UserInfo?>(initialValue = null, messageInfo) {
		messageInfo.sender?.let { sender ->
			//Get the user
			try {
				value = MainApplication.getInstance().userCacheHelper.getUserInfo(context, sender).await()
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
							userInfo = userInfo
						)
					}
				}
				
				Spacer(modifier = Modifier.width(8.dp))
			}
			
			//Message contents
			Column(
				modifier = Modifier.weight(1F),
				horizontalAlignment = if(isOutgoing) Alignment.End else Alignment.Start
			) {
				messageInfo.messageTextComponent?.let { textComponent ->
					//Maximum 70% width
					Box(
						modifier = Modifier.fillMaxWidth(0.7F),
						contentAlignment = if(isOutgoing) Alignment.TopEnd else Alignment.TopStart
					) {
						MessageBubbleWrapper(
							stickers = textComponent.stickers,
							tapbacks = textComponent.tapbacks
						) {
							MessageBubbleText(
								flow = MessagePartFlow(
									isOutgoing = isOutgoing,
									anchorTop = flow.anchorTop,
									anchorBottom = flow.anchorBottom || messageInfo.attachments.isNotEmpty(),
									tintRatio = scrollProgress
								),
								subject = textComponent.subject,
								text = textComponent.text,
							)
						}
					}
				}
				
				val attachmentsCount = messageInfo.attachments.size
				messageInfo.attachments.forEachIndexed { index, attachment ->
					val attachmentFlow = MessagePartFlow(
						isOutgoing = isOutgoing,
						anchorTop = flow.anchorTop || messageInfo.messageTextComponent != null,
						anchorBottom = flow.anchorBottom || (index + 1) < attachmentsCount,
						tintRatio = scrollProgress
					)
					
					attachment.file?.also { attachmentFile ->
						if(compareMimeTypes(attachment.contentType, MIMEConstants.mimeTypeImage)
									|| compareMimeTypes(attachment.contentType, MIMEConstants.mimeTypeVideo)) {
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
								}
							)
						} else if(compareMimeTypes(attachment.contentType, MIMEConstants.mimeTypeAudio)) {
							MessageBubbleAudio(
								flow = attachmentFlow,
								file = attachmentFile,
								audioPlaybackState = AudioPlaybackState.Stopped
							)
						} else {
							MessageBubbleFile(
								flow = attachmentFlow,
								name = attachment.computedFileName ?: "",
								onClick = {
									IntentHelper.openAttachmentFile(context, attachmentFile, attachment.computedContentType)
								}
							)
						}
					} ?: run {
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
						
						val connectionManager = LocalConnectionManager.current
						MessageBubbleDownload(
							flow = attachmentFlow,
							name = attachment.fileName,
							bytesTotal = bytesTotal,
							bytesDownloaded = bytesDownloaded,
							isDownloading = downloadState != null,
							onClick = {
								//Make sure we have a connection manager
								if(connectionManager == null) {
									Toast.makeText(context, R.string.message_connectionerror, Toast.LENGTH_SHORT).show()
									return@MessageBubbleDownload
								}
								
								//Download the attachment
								NetworkState.downloadAttachment(connectionManager, messageInfo, attachment)
							},
							enabled = downloadState == null
						)
					}
				}
			}
			
			if(messageInfo.hasError) {
			
			}
		}
	}
}