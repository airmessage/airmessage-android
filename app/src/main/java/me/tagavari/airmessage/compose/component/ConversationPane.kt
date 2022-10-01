@file:OptIn(ExperimentalMaterial3Api::class)

package me.tagavari.airmessage.compose.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.enums.ConversationState
import me.tagavari.airmessage.enums.ServiceHandler
import me.tagavari.airmessage.enums.ServiceType
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.messaging.ConversationPreview
import me.tagavari.airmessage.messaging.MemberInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationPane(
	modifier: Modifier = Modifier,
	props: ConversationPaneProps
) {
	val snackbarHostState = remember { SnackbarHostState() }
	val scope = rememberCoroutineScope()
	
	//Use a large top app bar if not floating
	val scrollBehavior = if(props.isFloatingPane) {
		TopAppBarDefaults.pinnedScrollBehavior(state = props.topAppBarState)
	} else {
		TopAppBarDefaults.exitUntilCollapsedScrollBehavior(state = props.topAppBarState)
	}
	
	//Action mode
	var selectedConversationIDs by rememberSaveable { mutableStateOf(setOf<Long>()) }
	val isActionMode by remember { derivedStateOf { selectedConversationIDs.isNotEmpty() } }
	fun stopActionMode() {
		selectedConversationIDs = setOf()
	}
	
	//Stop action mode when back is pressed
	BackHandler(isActionMode) {
		stopActionMode()
	}
	
	//Use a tinted background color in floating mode
	val backgroundColor = if(props.isFloatingPane) {
		MaterialTheme.colorScheme.inverseOnSurface
	} else {
		MaterialTheme.colorScheme.background
	}
	
	//Don't allow the user to create a new conversation while syncing
	val allowNewConversation = props.syncState == null
	
	Scaffold(
		modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
		topBar = {
			Crossfade(targetState = isActionMode) { isActionMode ->
				if(!isActionMode) {
					ConversationPaneAppBar(
						props = props,
						backgroundColor = backgroundColor,
						scrollBehavior = scrollBehavior,
						allowNewConversation = allowNewConversation
					)
				} else {
					val selectedConversations = remember(selectedConversationIDs) {
						selectedConversationIDs.mapNotNull { conversationID ->
							props.conversations?.getOrNull()?.firstOrNull { it.localID == conversationID }
						}
					}
					
					ConversationsPaneActionModeAppBar(
						props = props,
						scope = scope,
						selectedConversations = selectedConversations,
						scrollBehavior = scrollBehavior,
						snackbarHostState = snackbarHostState,
						onStopActionMode = ::stopActionMode
					)
				}
			}
		},
		content = { innerPadding ->
			val syncState = props.syncState
			if(syncState != null) {
				//Syncing conversations
				ConversationsPaneSyncState(
					modifier = Modifier
						.fillMaxSize()
						.padding(innerPadding),
					syncState = syncState
				)
			} else if(props.conversations == null) {
				//Conversations loading
				ConversationsPaneLoadingState(
					modifier = Modifier
						.fillMaxSize()
						.padding(innerPadding)
				)
			} else {
				//Conversations failed to load
				props.conversations.onFailure {
					ConversationsPaneErrorState(
						modifier = modifier
							.fillMaxSize()
							.padding(innerPadding),
						onRetry = props.onReloadConversations
					)
				}
				
				//Conversations loaded, show list
				props.conversations.onSuccess { conversations ->
					ConversationList(
						conversations = conversations,
						scrollState = props.scrollState,
						contentPadding = innerPadding,
						activeConversationID = props.activeConversationID,
						onClickConversation = props.onSelectConversation,
						selectedConversations = selectedConversationIDs,
						setSelectedConversations = { selectedConversationIDs = it }
					)
				}
			}
		},
		snackbarHost = { SnackbarHost(snackbarHostState) },
		floatingActionButton = {
			if(!props.isFloatingPane && allowNewConversation) {
				FloatingActionButton(
					modifier = Modifier.navigationBarsPadding(),
					onClick = props.onNewConversation
				) {
					Icon(Icons.Outlined.Message, stringResource(id = R.string.action_newconversation))
				}
			}
		},
		containerColor = backgroundColor
	)
}

@Preview(name = "Conversation pane")
@Composable
fun PreviewConversationPane() {
	AirMessageAndroidTheme {
		ConversationPane(
			props = ConversationPaneProps(
				scrollState = rememberLazyListState(),
				topAppBarState = rememberTopAppBarState(),
				conversations = Result.success(listOf(
					ConversationInfo(
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
					)
				))
			)
		)
	}
}

@Preview(name = "Conversation pane archived")
@Composable
fun PreviewConversationPaneArchived() {
	AirMessageAndroidTheme {
		ConversationPane(
			props = ConversationPaneProps(
				scrollState = rememberLazyListState(),
				topAppBarState = rememberTopAppBarState(),
				conversations = Result.success(listOf(
					ConversationInfo(
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
					)
				)),
				isArchived = true
			)
		)
	}
}
