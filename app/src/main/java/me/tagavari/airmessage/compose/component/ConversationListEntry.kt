package me.tagavari.airmessage.compose.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.material.transition.MaterialElevationScale
import kotlinx.coroutines.rx3.await
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.enums.ConversationState
import me.tagavari.airmessage.enums.ServiceHandler
import me.tagavari.airmessage.enums.ServiceType
import me.tagavari.airmessage.helper.ConversationBuildHelper
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
	selected: Boolean = false
) {
	val context = LocalContext.current
	val title by produceState(
		initialValue = ConversationBuildHelper.buildConversationTitleDirect(context, conversation),
		conversation
	) {
		value = ConversationBuildHelper.buildConversationTitle(context, conversation).await()
	}
	
	val preview = conversation.dynamicPreview
	
	Box(modifier = Modifier.height(72.dp)) {
		val backgroundColor by animateColorAsState(
			if(selected) MaterialTheme.colorScheme.primaryContainer
			else MaterialTheme.colorScheme.surface
		)
		
		Surface(
			modifier = Modifier
				.align(Alignment.Center)
				.padding(horizontal = 4.dp, vertical = 2.dp)
				.clip(MaterialTheme.shapes.large)
				.combinedClickable(
					onClick = onClick,
					onLongClick = onLongClick
				),
			color = backgroundColor
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
				UserIconGroup(members = conversation.members)
				
				Spacer(modifier = Modifier.width(16.dp))
				
				//Title and preview
				Column(modifier = Modifier.weight(1F)) {
					Text(
						text = title,
						overflow = TextOverflow.Ellipsis,
						maxLines = 1,
						style = MaterialTheme.typography.bodyLarge
					)
					
					Text(
						text = preview?.buildString(LocalContext.current) ?: stringResource(id = R.string.part_unknown),
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
					
					Icon(
						painter = painterResource(id = R.drawable.notifications_off_outlined),
						contentDescription = stringResource(id = R.string.action_mute),
						tint = MaterialTheme.colorScheme.onSurfaceVariant,
						modifier = Modifier
							.size(16.dp)
							.alpha(if(conversation.isMuted) 1F else 0F)
					)
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