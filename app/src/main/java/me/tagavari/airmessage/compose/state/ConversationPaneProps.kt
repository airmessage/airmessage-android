package me.tagavari.airmessage.compose.state

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarState
import me.tagavari.airmessage.helper.ProgressState
import me.tagavari.airmessage.messaging.ConversationInfo

@OptIn(ExperimentalMaterial3Api::class)
data class ConversationPaneProps(
	//UI state
	val scrollState: LazyListState,
	val topAppBarState: TopAppBarState,
	
	//Flags
	val isArchived: Boolean = false,
	val isFloatingPane: Boolean = false,
	
	//Data
	val conversations: Result<List<ConversationInfo>>? = null,
	val activeConversationID: Long? = null,
	val syncState: ProgressState? = null,
	
	//Callbacks
	val onNavigateBack: () -> Unit = {},
	val onSelectConversation: (Long) -> Unit = {},
	val onReloadConversations: () -> Unit = {},
	val onNavigateArchived: () -> Unit = {},
	val onNavigateSettings: () -> Unit = {},
	val onNavigateHelp: () -> Unit = {},
	val onNewConversation: () -> Unit = {},
	val onMarkConversationsAsRead: () -> Unit = {},
)

