package me.tagavari.airmessage.compose.component

import androidx.compose.runtime.Composable
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.messaging.ConversationItem
import me.tagavari.airmessage.messaging.MessageInfo
import me.tagavari.airmessage.util.MessageFlow

@Composable
fun ConversationItemListEntry(
	conversationInfo: ConversationInfo,
	conversationItem: ConversationItem,
	flow: MessageFlow
) {
	if(conversationItem is MessageInfo) {
		MessageInfoListEntry(
			conversationInfo = conversationInfo,
			messageInfo = conversationItem,
			flow = flow
		)
	}
}