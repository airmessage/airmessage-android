package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rxjava3.subscribeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.compose.provider.LocalConnectionManager
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxEventConnection

/**
 * Displays a vertical list of conversations, as well as notice cards
 * at the top of the list
 */
@Composable
fun ConversationList(
	modifier: Modifier = Modifier,
	conversations: List<ConversationInfo>,
	contentPadding: PaddingValues = PaddingValues(0.dp),
	onClickConversation: (ConversationInfo) -> Unit,
	selectedConversations: Set<Long>,
	setSelectedConversations: (Set<Long>) -> Unit
) {
	//Network state
	val connectionState by ReduxEmitterNetwork.connectionStateSubject.subscribeAsState(initial = null)
	
	LazyColumn(
		modifier = modifier,
		contentPadding = contentPadding
	) {
		//Connection state
		val localConnectionState: ReduxEventConnection? = connectionState
		if(localConnectionState is ReduxEventConnection.Disconnected) {
			item {
				ConnectionErrorCard(
					connectionManager = LocalConnectionManager.current,
					code = localConnectionState.code
				)
			}
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
						onClickConversation(conversationInfo)
					} else {
						toggleSelection()
					}
				},
				onLongClick = { toggleSelection() },
				selected = selectedConversations.contains(conversationInfo.localID)
			)
		}
	}
}