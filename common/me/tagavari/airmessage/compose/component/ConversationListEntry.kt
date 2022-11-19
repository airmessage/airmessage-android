package me.tagavari.airmessage.compose.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.remember.produceConversationTitle
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.compose.util.wrapImmutableHolder
import me.tagavari.airmessage.enums.ConversationState
import me.tagavari.airmessage.enums.ServiceHandler
import me.tagavari.airmessage.enums.ServiceType
import me.tagavari.airmessage.helper.LanguageHelper
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.messaging.ConversationPreview
import me.tagavari.airmessage.messaging.MemberInfo

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationListEntry(
	conversation: ConversationInfo,
	onClick: () -> Unit,
	onLongClick: (() -> Unit)? = null,
	active: Boolean = false,
	selected: Boolean = false
) {
	Box(modifier = Modifier.height(72.dp)) {
		val backgroundColor by animateColorAsState(
			when {
				selected -> MaterialTheme.colorScheme.surfaceVariant
				active -> MaterialTheme.colorScheme.primaryContainer
				else -> Color.Unspecified
			}
		)
		
		val contentColor by animateColorAsState(
			when {
				selected -> MaterialTheme.colorScheme.onSurfaceVariant
				active -> MaterialTheme.colorScheme.onPrimaryContainer
				else -> MaterialTheme.colorScheme.onBackground
			}
		)

		val haptic = LocalHapticFeedback.current
		Surface(
			modifier = Modifier
				.align(Alignment.Center)
				.padding(horizontal = 4.dp, vertical = 2.dp)
				.clip(MaterialTheme.shapes.large)
				.combinedClickable(
					onClick = onClick,
					onLongClick = onLongClick?.let { callback -> {
						haptic.performHapticFeedback(HapticFeedbackType.LongPress)
						callback()
					} }
				),
			color = backgroundColor,
			contentColor = contentColor
		) {
			Row(
				modifier = Modifier.fillMaxSize(),
				verticalAlignment = Alignment.CenterVertically
			) {
				Spacer(modifier = Modifier.width(6.dp))
				
				//New message indicator
				Box(
					modifier = Modifier
						.alpha(if(conversation.unreadMessageCount > 0) 1F else 0F)
						.size(8.dp)
						.clip(CircleShape)
						.background(MaterialTheme.colorScheme.primary)
				)
				
				Spacer(modifier = Modifier.width(6.dp))
				
				//Group icon
				UserIconGroup(members = conversation.members.wrapImmutableHolder())
				
				Spacer(modifier = Modifier.width(16.dp))
				
				val preview = remember(conversation) { conversation.dynamicPreview }
				
				//Title and preview
				Column(modifier = Modifier.weight(1F)) {
					val context = LocalContext.current
					val title by produceConversationTitle(conversation)
					val previewText = remember(preview) {
						preview?.buildString(context)
					}
					
					Text(
						text = title,
						overflow = TextOverflow.Ellipsis,
						maxLines = 1,
						style = MaterialTheme.typography.bodyLarge
					)
					
					Text(
						text = previewText ?: stringResource(id = R.string.part_unknown),
						style = MaterialTheme.typography.bodyMedium,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
				
				Spacer(modifier = Modifier.width(16.dp))
				
				Column(
					horizontalAlignment = Alignment.End
				) {
					val context = LocalContext.current
					
					val stringNotSent = stringResource(id = R.string.message_senderror)
					
					val isPreviewError = remember(preview) {
						preview is ConversationPreview.Message && preview.isError
					}
					val previewText = remember(preview, isPreviewError) {
						if(isPreviewError) stringNotSent
						else preview?.let { LanguageHelper.getLastUpdateStatusTime(context, it.date) }
							?: ""
					}
					
					Text(
						text = previewText,
						style = MaterialTheme.typography.bodyMedium,
						color = if(isPreviewError) MaterialTheme.colorScheme.error
						else MaterialTheme.colorScheme.onSurfaceVariant
					)

					if(conversation.isMuted) {
						Icon(
							imageVector = Icons.Outlined.NotificationsOff,
							contentDescription = stringResource(id = R.string.action_mute),
							tint = MaterialTheme.colorScheme.onSurfaceVariant,
							modifier = Modifier
								.size(16.dp)
						)
					}
				}
				
				Spacer(modifier = Modifier.width(16.dp))
			}
		}
	}
}

@Preview(
	name = "Message list entry",
	widthDp = 384
)
@Composable
private fun PreviewConversationListEntry() {
	AirMessageAndroidTheme {
		Surface {
			ConversationListEntry(
				conversation = ConversationInfo(
					localID = 0,
					guid = null,
					externalID = -1,
					state = ConversationState.ready,
					serviceHandler = ServiceHandler.appleBridge,
					serviceType = ServiceType.appleMessage,
					conversationColor = 0xFFFF1744.toInt(),
					members = mutableListOf(
						MemberInfo("test", 0xFFFF1744.toInt())
					),
					title = "A cool conversation",
					unreadMessageCount = 1,
					isArchived = false,
					isMuted = true,
					messagePreview = ConversationPreview.Message(
						date = System.currentTimeMillis(),
						isOutgoing = false,
						message = "Test message",
						subject = null,
						attachments = listOf(),
						sendStyle = null,
						isError = false
					)
				),
				onClick = {}
			)
		}
	}
}

@Preview(
	name = "Message error entry",
	widthDp = 384
)
@Composable
private fun PreviewConversationListErrorEntry() {
	AirMessageAndroidTheme {
		Surface {
			ConversationListEntry(
				conversation = ConversationInfo(
					localID = 0,
					guid = null,
					externalID = -1,
					state = ConversationState.ready,
					serviceHandler = ServiceHandler.appleBridge,
					serviceType = ServiceType.appleMessage,
					conversationColor = 0xFFFF1744.toInt(),
					members = mutableListOf(
						MemberInfo("test", 0xFFFF1744.toInt())
					),
					title = "An error conversation",
					unreadMessageCount = 0,
					isArchived = false,
					isMuted = false,
					messagePreview = ConversationPreview.Message(
						date = System.currentTimeMillis(),
						isOutgoing = false,
						message = "Failed message",
						subject = null,
						attachments = listOf(),
						sendStyle = null,
						isError = true
					)
				),
				onClick = {}
			)
		}
	}
}

@Preview(
	name = "Conversation selected entry",
	widthDp = 384
)
@Composable
private fun PreviewConversationListSelectedEntry() {
	AirMessageAndroidTheme {
		Surface {
			ConversationListEntry(
				conversation = ConversationInfo(
					localID = 0,
					guid = null,
					externalID = -1,
					state = ConversationState.ready,
					serviceHandler = ServiceHandler.appleBridge,
					serviceType = ServiceType.appleMessage,
					conversationColor = 0xFFFF1744.toInt(),
					members = mutableListOf(
						MemberInfo("test", 0xFFFF1744.toInt())
					),
					title = "A selected conversation",
					unreadMessageCount = 0,
					isArchived = false,
					isMuted = false,
					messagePreview = ConversationPreview.Message(
						date = System.currentTimeMillis(),
						isOutgoing = false,
						message = "Test message",
						subject = null,
						attachments = listOf(),
						sendStyle = null,
						isError = false
					)
				),
				onClick = {},
				selected = true
			)
		}
	}
}

@Preview(
	name = "Conversation active entry",
	widthDp = 384
)
@Composable
private fun PreviewConversationListActiveEntry() {
	AirMessageAndroidTheme {
		Surface {
			ConversationListEntry(
				conversation = ConversationInfo(
					localID = 0,
					guid = null,
					externalID = -1,
					state = ConversationState.ready,
					serviceHandler = ServiceHandler.appleBridge,
					serviceType = ServiceType.appleMessage,
					conversationColor = 0xFFFF1744.toInt(),
					members = mutableListOf(
						MemberInfo("test", 0xFFFF1744.toInt())
					),
					title = "A selected conversation",
					unreadMessageCount = 0,
					isArchived = false,
					isMuted = false,
					messagePreview = ConversationPreview.Message(
						date = System.currentTimeMillis(),
						isOutgoing = false,
						message = "Test message",
						subject = null,
						attachments = listOf(),
						sendStyle = null,
						isError = false
					)
				),
				onClick = {},
				active = true
			)
		}
	}
}
