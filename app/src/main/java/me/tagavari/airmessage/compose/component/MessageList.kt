package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.messaging.ConversationItem
import me.tagavari.airmessage.util.MessageFlow

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
		items(
			items = messages.asReversed(),
			key = { it.localID }
		) { conversationItem ->
			ConversationItemListEntry(
				conversationInfo = conversation,
				conversationItem = conversationItem,
				flow = MessageFlow(
					anchorTop = false,
					anchorBottom = false
				)
			)
		}
	}
}