package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.tagavari.airmessage.constants.TimingConstants
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.messaging.ConversationItem
import me.tagavari.airmessage.messaging.MessageInfo
import me.tagavari.airmessage.util.MessageFlow
import me.tagavari.airmessage.util.MessageFlowSpacing

@Composable
fun MessageList(
	modifier: Modifier = Modifier,
	conversation: ConversationInfo,
	messages: List<ConversationItem>
) {
	LazyColumn(
		modifier = modifier,
		reverseLayout = true
	) {
		val reversedMessages = messages.asReversed()
		
		items(
			key = { reversedMessages[it].localID },
			count = reversedMessages.size
		) { index ->
			val conversationItem = reversedMessages[index]
			
			if(conversationItem is MessageInfo) {
				val messageAbove = reversedMessages.getOrNull(index + 1)
				val messageBelow = reversedMessages.getOrNull(index - 1)
				
				val flow = MessageFlow(
					anchorTop = messageAbove is MessageInfo
							&& conversationItem.sender == messageAbove.sender
							&& (conversationItem.date - messageAbove.date) < TimingConstants.conversationBurstTimeMillis,
					anchorBottom = messageBelow is MessageInfo
							&& conversationItem.sender == messageBelow.sender
							&& (messageBelow.date - conversationItem.date) < TimingConstants.conversationBurstTimeMillis
				)
				val spacing = when {
					messageAbove == null -> MessageFlowSpacing.NONE
					flow.anchorTop -> MessageFlowSpacing.RELATED
					else -> MessageFlowSpacing.GAP
				}
				
				MessageInfoListEntry(
					conversationInfo = conversation,
					messageInfo = conversationItem,
					flow = flow,
					spacing = spacing
				)
			}
		}
	}
}