package me.tagavari.airmessage.compose.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import me.tagavari.airmessage.R
import me.tagavari.airmessage.enums.ConversationState
import me.tagavari.airmessage.enums.ServiceHandler
import me.tagavari.airmessage.enums.ServiceType
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.messaging.ConversationPreview
import me.tagavari.airmessage.messaging.MemberInfo

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ConversationList(
	modifier: Modifier = Modifier,
	conversations: Result<List<ConversationInfo>>? = null,
	onSelectConversation: (ConversationInfo) -> Unit = {},
	onReloadConversations: () -> Unit = {},
	onNavigateSettings: () -> Unit = {}
) {
	val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
	
	//Action mode
	var selectedConversations by rememberSaveable { mutableStateOf(setOf<Long>()) }
	val isActionMode by remember { derivedStateOf { selectedConversations.isNotEmpty() } }
	fun stopActionMode() {
		selectedConversations = setOf()
	}
	
	//Stop action mode when back is pressed
	BackHandler(isActionMode) {
		stopActionMode()
	}
	
	Scaffold(
		modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
		topBar = {
			if(!isActionMode) {
				LargeTopAppBar(
					title = { Text(stringResource(id = R.string.app_name)) },
					scrollBehavior = scrollBehavior,
					actions = {
						var menuOpen by remember { mutableStateOf(false) }
						
						IconButton(onClick = { menuOpen = !menuOpen }) {
							Icon(Icons.Default.MoreVert, contentDescription = "")
						}
						
						DropdownMenu(
							expanded = menuOpen,
							onDismissRequest = { menuOpen = false }
						) {
							DropdownMenuItem(
								text = { Text(stringResource(id = R.string.screen_settings)) },
								onClick = {
									menuOpen = false
									onNavigateSettings()
								}
							)
						}
					}
				)
			} else {
				//Associate a conversation's ID to itself
				val conversationsMap = remember(conversations) {
					conversations?.getOrNull()?.associateBy { it.localID } ?: mapOf()
				}
				
				LargeTopAppBar(
					title = {
						Text(selectedConversations.size.let { size ->
							pluralStringResource(id = R.plurals.message_selectioncount, size, size)
						})
					},
					scrollBehavior = scrollBehavior,
					navigationIcon = {
						IconButton(onClick = { stopActionMode() }) {
							Icon(
								imageVector = Icons.Filled.Close,
								contentDescription = stringResource(id = android.R.string.cancel)
							)
						}
					},
					actions = {
						data class SelectionContents(
							val hasMuted: Boolean,
							val hasUnmuted: Boolean,
							val hasArchived: Boolean,
							val hasUnarchived: Boolean
						)
						
						val selectionContents = remember(selectedConversations) {
							val conversationsSequence = selectedConversations
								.asSequence()
								.mapNotNull { conversationsMap[it] }
							
							SelectionContents(
								hasMuted = conversationsSequence.any { it.isMuted },
								hasUnmuted = conversationsSequence.any { !it.isMuted },
								hasArchived = conversationsSequence.any { it.isArchived },
								hasUnarchived = conversationsSequence.any { !it.isArchived }
							)
						}
						
						if(selectionContents.hasMuted) {
							IconButton(onClick = {}) {
								Icon(
									imageVector = Icons.Outlined.NotificationsActive,
									contentDescription = stringResource(id = R.string.action_unmute)
								)
							}
						}
						
						if(selectionContents.hasUnmuted) {
							IconButton(onClick = {}) {
								Icon(
									imageVector = Icons.Outlined.NotificationsOff,
									contentDescription = stringResource(id = R.string.action_mute)
								)
							}
						}
						
						if(selectionContents.hasArchived) {
							IconButton(onClick = {}) {
								Icon(
									imageVector = Icons.Outlined.Unarchive,
									contentDescription = stringResource(id = R.string.action_unarchive)
								)
							}
						}
						
						if(selectionContents.hasUnarchived) {
							IconButton(onClick = {}) {
								Icon(
									imageVector = Icons.Outlined.Archive,
									contentDescription = stringResource(id = R.string.action_archive)
								)
							}
						}
						
						IconButton(onClick = {}) {
							Icon(
								imageVector = Icons.Outlined.Delete,
								contentDescription = stringResource(id = R.string.action_delete)
							)
						}
					}
				)
			}
		},
		content = { innerPadding ->
			//Combine inner and system insets padding
			val systemInsets = WindowInsets.navigationBars.asPaddingValues()
			val layoutDirection = LocalLayoutDirection.current
			
			val contentPadding = remember(innerPadding, systemInsets, layoutDirection) {
				PaddingValues(
					start = innerPadding.calculateStartPadding(layoutDirection) + systemInsets.calculateStartPadding(layoutDirection),
					top = innerPadding.calculateTopPadding() + systemInsets.calculateTopPadding(),
					end = innerPadding.calculateEndPadding(layoutDirection) + systemInsets.calculateEndPadding(layoutDirection),
					bottom = innerPadding.calculateBottomPadding() + systemInsets.calculateBottomPadding()
				)
			}
			
			if(conversations == null) {
				Box(
					modifier = modifier
						.fillMaxSize()
						.padding(contentPadding)
				) {
					CircularProgressIndicator(
						modifier = Modifier.align(Alignment.Center)
					)
				}
			} else {
				conversations.onFailure {
					Column(
						modifier = modifier
							.fillMaxSize()
							.padding(contentPadding),
						verticalArrangement = Arrangement.Center,
						horizontalAlignment = Alignment.CenterHorizontally
					) {
						Text(text = stringResource(id = R.string.message_loaderror_messages))
						
						TextButton(onClick = onReloadConversations) {
							Text(text = stringResource(id = R.string.action_retry))
						}
					}
				}
				
				conversations.onSuccess { conversations ->
					LazyColumn(
						contentPadding = contentPadding
					) {
						items(
							items = conversations,
							key = { it.localID }
						) { conversationInfo ->
							fun toggleSelection() {
								conversationInfo.localID.let { localID ->
									selectedConversations = selectedConversations
										.toMutableSet().apply {
											if(contains(localID)) {
												remove(localID)
											} else {
												add(localID)
											}
										}
								}
							}
							
							ConversationListEntry(
								conversation = conversationInfo,
								onClick = {
									if(isActionMode) {
										toggleSelection()
									} else {
										onSelectConversation(conversationInfo)
									}
								},
								onLongClick = { toggleSelection() },
								selected = selectedConversations.contains(conversationInfo.localID)
							)
						}
					}
				}
			}
		}
	)
}

@Preview(name = "Conversation list")
@Composable
fun PreviewConversationList() {
	ConversationList(
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
		onReloadConversations = {}
	)
}

@Preview(name = "Loading state")
@Composable
fun PreviewConversationListLoading() {
	ConversationList(
		conversations = null,
		onReloadConversations = {}
	)
}

@Preview(name = "Error state")
@Composable
fun PreviewConversationListError() {
	ConversationList(
		conversations = Result.failure(Exception("Failed to load conversations")),
		onReloadConversations = {}
	)
}