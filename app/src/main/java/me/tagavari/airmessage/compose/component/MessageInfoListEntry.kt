package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.rx3.await
import me.tagavari.airmessage.MainApplication
import me.tagavari.airmessage.data.UserCacheHelper
import me.tagavari.airmessage.enums.MessageState
import me.tagavari.airmessage.helper.ConversationColorHelper
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.messaging.MessageInfo
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
	spacing: MessageFlowSpacing = MessageFlowSpacing.NONE
) {
	val context = LocalContext.current
	
	//Compute the message information
	val senderMember = messageInfo.sender?.let { sender -> conversationInfo.members.find { it.address == sender } }
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
		modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = spacing.padding)
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
		Row {
			//User indicator
			if(!isOutgoing) {
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
			Column {
				messageInfo.messageTextComponent?.let { textComponent ->
					MessageBubbleWrapper(
						stickers = textComponent.stickers,
						tapbacks = textComponent.tapbacks
					) {
						MessageBubbleText(
							flow = MessagePartFlow(
								isOutgoing = isOutgoing,
								anchorTop = flow.anchorTop,
								anchorBottom = flow.anchorBottom || messageInfo.attachments.isNotEmpty()
							),
							subject = textComponent.subject,
							text = textComponent.text
						)
					}
				}
			}
			
			if(messageInfo.hasError) {
			
			}
		}
	}
}