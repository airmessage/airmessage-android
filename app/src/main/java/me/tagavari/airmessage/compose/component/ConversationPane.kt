package me.tagavari.airmessage.compose.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.await
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.enums.ConversationState
import me.tagavari.airmessage.enums.ServiceHandler
import me.tagavari.airmessage.enums.ServiceType
import me.tagavari.airmessage.helper.ProgressState
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.messaging.ConversationPreview
import me.tagavari.airmessage.messaging.MemberInfo
import me.tagavari.airmessage.task.ConversationActionTask

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ConversationPane(
	modifier: Modifier = Modifier,
	conversations: Result<List<ConversationInfo>>? = null,
	onSelectConversation: (ConversationInfo) -> Unit = {},
	onReloadConversations: () -> Unit = {},
	onNavigateSettings: () -> Unit = {},
	onNewConversation: () -> Unit = {},
	syncState: ProgressState? = null,
	hasUnreadConversations: Boolean = false,
	onMarkConversationsAsRead: () -> Unit = {}
) {
	val snackbarHostState = remember { SnackbarHostState() }
	val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
	val scope = rememberCoroutineScope()
	
	//Action mode
	var selectedConversations by rememberSaveable { mutableStateOf(setOf<Long>()) }
	val isActionMode by remember { derivedStateOf { selectedConversations.isNotEmpty() } }
	fun stopActionMode() {
		selectedConversations = setOf()
	}
	var promptDeleteConversations by remember { mutableStateOf(false) }
	
	//Stop action mode when back is pressed
	BackHandler(isActionMode) {
		stopActionMode()
	}
	
	Scaffold(
		modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
		topBar = {
			Crossfade(targetState = isActionMode) { isActionMode ->
				if(!isActionMode) {
					LargeTopAppBar(
						title = {
							Text(stringResource(id = R.string.app_name), fontWeight = FontWeight.Medium)
						},
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
								if(hasUnreadConversations) {
									DropdownMenuItem(
										text = { Text(stringResource(id = R.string.action_markallread)) },
										onClick = {
											menuOpen = false
											onMarkConversationsAsRead()
										}
									)
								}
								
								DropdownMenuItem(
									text = { Text(stringResource(id = R.string.screen_settings)) },
									onClick = {
										menuOpen = false
										onNavigateSettings()
									}
								)
							}
						},
						colors = TopAppBarDefaults.largeTopAppBarColors(
							titleContentColor = MaterialTheme.colorScheme.primary
						)
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
							
							val context = LocalContext.current
							
							val conversationsSequence = selectedConversations
								.asSequence()
								.mapNotNull { conversationsMap[it] }
							
							val selectionContents = remember(selectedConversations) {
								SelectionContents(
									hasMuted = conversationsSequence.any { it.isMuted },
									hasUnmuted = conversationsSequence.any { !it.isMuted },
									hasArchived = conversationsSequence.any { it.isArchived },
									hasUnarchived = conversationsSequence.any { !it.isArchived }
								)
							}
							
							@OptIn(DelicateCoroutinesApi::class)
							fun setConversationsMuted(muted: Boolean) {
								GlobalScope.launch {
									ConversationActionTask.muteConversations(
										conversationsSequence.toSet(),
										muted
									).await()
								}
							}
							
							@OptIn(DelicateCoroutinesApi::class)
							fun setConversationsArchived(archived: Boolean) {
								val targetConversations = conversationsSequence.toSet()
								
								GlobalScope.launch {
									ConversationActionTask.archiveConversations(
										targetConversations,
										archived
									).await()
								}
								
								scope.launch {
									val result = snackbarHostState.showSnackbar(
										message = context.resources.getQuantityString(
											if(archived) R.plurals.message_conversationarchived
											else R.plurals.message_conversationunarchived,
											targetConversations.size,
											targetConversations.size
										),
										actionLabel = context.resources.getString(R.string.action_undo),
										duration = SnackbarDuration.Short
									)
									
									if(result == SnackbarResult.ActionPerformed) {
										GlobalScope.launch {
											//Reverse the action
											ConversationActionTask.muteConversations(
												targetConversations,
												!archived
											).await()
										}
									}
								}
							}
							
							if(selectionContents.hasMuted) {
								IconButton(onClick = {
									setConversationsMuted(false)
									stopActionMode()
								}) {
									Icon(
										imageVector = Icons.Outlined.NotificationsActive,
										contentDescription = stringResource(id = R.string.action_unmute)
									)
								}
							}
							
							if(selectionContents.hasUnmuted) {
								IconButton(onClick = {
									setConversationsMuted(true)
									stopActionMode()
								}) {
									Icon(
										imageVector = Icons.Outlined.NotificationsOff,
										contentDescription = stringResource(id = R.string.action_mute)
									)
								}
							}
							
							if(selectionContents.hasArchived) {
								IconButton(onClick = {
									setConversationsArchived(false)
									stopActionMode()
								}) {
									Icon(
										imageVector = Icons.Outlined.Unarchive,
										contentDescription = stringResource(id = R.string.action_unarchive)
									)
								}
							}
							
							if(selectionContents.hasUnarchived) {
								IconButton(onClick = {
									setConversationsArchived(true)
									stopActionMode()
								}) {
									Icon(
										imageVector = Icons.Outlined.Archive,
										contentDescription = stringResource(id = R.string.action_archive)
									)
								}
							}
							
							IconButton(onClick = {
								promptDeleteConversations = true
							}) {
								Icon(
									imageVector = Icons.Outlined.Delete,
									contentDescription = stringResource(id = R.string.action_delete)
								)
							}
						},
						colors = TopAppBarDefaults.largeTopAppBarColors(
							scrolledContainerColor = MaterialTheme.colorScheme.primaryContainer
						)
					)
				}
			}
		},
		content = { innerPadding ->
			if(syncState != null) {
				//Syncing conversations
				Column(
					modifier = Modifier
						.fillMaxSize()
						.padding(innerPadding)
				) {
					StatusCardColumn()
					
					Column(
						modifier = Modifier
							.fillMaxWidth()
							.weight(1F),
						horizontalAlignment = Alignment.CenterHorizontally,
						verticalArrangement = Arrangement.Center
					) {
						Text(
							text = stringResource(R.string.progress_sync),
							color = MaterialTheme.colorScheme.onSurfaceVariant
						)
						
						Spacer(modifier = Modifier.height(16.dp))
						
						if(syncState is ProgressState.Determinate) {
							val animatedProgress by animateFloatAsState(
								targetValue = syncState.progress,
								animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
							)
							
							LinearProgressIndicator(
								modifier = Modifier.width(150.dp),
								progress = animatedProgress
							)
						} else {
							LinearProgressIndicator(
								modifier = Modifier.width(150.dp)
							)
						}
					}
				}
			} else if(conversations == null) {
				//Conversations loading
				Box(
					modifier = Modifier
						.fillMaxSize()
						.padding(innerPadding)
				) {
					CircularProgressIndicator(
						modifier = Modifier.align(Alignment.Center)
					)
				}
			} else {
				//Conversations failed to load
				conversations.onFailure {
					Column(
						modifier = modifier
							.fillMaxSize()
							.padding(innerPadding),
						verticalArrangement = Arrangement.Center,
						horizontalAlignment = Alignment.CenterHorizontally
					) {
						Text(text = stringResource(id = R.string.message_loaderror_messages))
						
						TextButton(onClick = onReloadConversations) {
							Text(text = stringResource(id = R.string.action_retry))
						}
					}
				}
				
				//Conversations loaded, show list
				conversations.onSuccess { conversations ->
					ConversationList(
						conversations = conversations,
						contentPadding = innerPadding,
						onClickConversation = onSelectConversation,
						selectedConversations = selectedConversations,
						setSelectedConversations = { selectedConversations = it }
					)
				}
			}
		},
		snackbarHost = { SnackbarHost(snackbarHostState) },
		floatingActionButton = {
			//Don't allow users to create a new conversation while
			//syncing messages
			if(syncState == null) {
				FloatingActionButton(
					modifier = Modifier.navigationBarsPadding(),
					onClick = onNewConversation
				) {
					Icon(Icons.Outlined.Message, stringResource(id = R.string.action_newconversation))
				}
			}
		}
	)
	
	if(promptDeleteConversations) {
		val context = LocalContext.current
		
		AlertDialog(
			onDismissRequest = { promptDeleteConversations = false },
			confirmButton = {
				TextButton(
					onClick = {
						//Get concrete conversation objects
						val conversationsList = selectedConversations
							.mapNotNull { conversationID ->
								conversations?.getOrNull()?.firstOrNull { it.localID == conversationID }
							}
						
						//Delete the conversations
						@OptIn(DelicateCoroutinesApi::class)
						GlobalScope.launch {
							ConversationActionTask.deleteConversations(context, conversationsList).await()
						}
						
						//Dismiss the dialog and stop action mode
						promptDeleteConversations = false
						stopActionMode()
					}
				) {
					Text(stringResource(R.string.action_delete))
				}
			},
			dismissButton = {
				TextButton(
					onClick = {
						promptDeleteConversations = false
					}
				) {
					Text(stringResource(android.R.string.cancel))
				}
			},
			icon = {
				Icon(
					imageVector = Icons.Outlined.Delete,
					contentDescription = null
				)
			},
			title = {
				val selectedCount = selectedConversations.size
				Text(
					text = context.resources.getQuantityString(
						R.plurals.message_confirm_deleteconversation,
						selectedCount
					)
				)
			}
		)
	}
}

@Preview(name = "Conversation list")
@Composable
fun PreviewConversationList() {
	AirMessageAndroidTheme {
		ConversationPane(
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
	}
}

@Preview(name = "Loading state")
@Composable
fun PreviewConversationListLoading() {
	AirMessageAndroidTheme {
		ConversationPane(
			conversations = null
		)
	}
}

@Preview(name = "Error state")
@Composable
fun PreviewConversationListError() {
	AirMessageAndroidTheme {
		ConversationPane(
			conversations = Result.failure(Exception("Failed to load conversations"))
		)
	}
}

@Preview(name = "Syncing state")
@Composable
fun PreviewConversationListSync() {
	AirMessageAndroidTheme {
		ConversationPane(
			syncState = ProgressState.Determinate(progress = 0.5F)
		)
	}
}
