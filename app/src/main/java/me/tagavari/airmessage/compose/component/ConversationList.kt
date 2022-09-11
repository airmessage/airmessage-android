package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.messaging.ConversationInfo

/**
 * Displays a vertical list of conversations, as well as notice cards
 * at the top of the list
 */
@Composable
fun ConversationList(
	modifier: Modifier = Modifier,
	conversations: List<ConversationInfo>,
	activeConversationID: Long? = null,
	contentPadding: PaddingValues = PaddingValues(0.dp),
	onClickConversation: (Long) -> Unit,
	selectedConversations: Set<Long>,
	setSelectedConversations: (Set<Long>) -> Unit
) {
	LazyColumn(
		modifier = modifier,
		contentPadding = contentPadding
	) {
		item {
			StatusCardColumn()
		}
		
		items(
			items = conversations,
			key = { it.localID }
		) { conversationInfo ->
			fun toggleSelection() {
				conversationInfo.localID.let { localID ->
					setSelectedConversations(
						selectedConversations.toMutableSet().apply {
							if(contains(localID)) {
								remove(localID)
							} else {
								add(localID)
							}
						}
					)
				}
			}
			
			ConversationListEntry(
				conversation = conversationInfo,
				onClick = {
					if(selectedConversations.isEmpty()) {
						onClickConversation(conversationInfo.localID)
					} else {
						toggleSelection()
					}
				},
				onLongClick = { toggleSelection() },
				selected = selectedConversations.contains(conversationInfo.localID),
				active = activeConversationID == conversationInfo.localID
			)
		}
	}
}