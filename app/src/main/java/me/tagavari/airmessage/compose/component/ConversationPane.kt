package me.tagavari.airmessage.compose.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow
import kotlinx.coroutines.rx3.await
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.provider.LocalConnectionManager
import me.tagavari.airmessage.compose.state.ConversationsViewModel
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.connection.ConnectionManager
import me.tagavari.airmessage.enums.ConversationState
import me.tagavari.airmessage.enums.ServiceHandler
import me.tagavari.airmessage.enums.ServiceType
import me.tagavari.airmessage.helper.ProgressState
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.messaging.ConversationPreview
import me.tagavari.airmessage.messaging.MemberInfo
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxEventConnection
import me.tagavari.airmessage.redux.ReduxEventMassRetrieval
import me.tagavari.airmessage.redux.ReduxEventMessaging
import me.tagavari.airmessage.task.ConversationActionTask

@Composable
fun ConversationPane(
	modifier: Modifier = Modifier,
	floatingPane: Boolean = false,
	activeConversationID: Long?,
	onShowSyncDialog: (connectionManager: ConnectionManager, deleteMessages: Boolean) -> Unit,
	onSelectConversation: (Long) -> Unit,
	onNavigateSettings: () -> Unit,
	onNewConversation: () -> Unit
) {
	val viewModel = viewModel<ConversationsViewModel>()
	
	val syncEvent by remember {
		ReduxEmitterNetwork.massRetrievalUpdateSubject.asFlow()
			.filter { it is ReduxEventMassRetrieval.Start
					|| it is ReduxEventMassRetrieval.Progress
					|| it is ReduxEventMassRetrieval.Complete
					|| it is ReduxEventMassRetrieval.Error }
	}.collectAsState(initial = null)
	
	LaunchedEffect(Unit) {
		ReduxEmitterNetwork.massRetrievalUpdateSubject.asFlow()
			.collect { event ->
				if(event is ReduxEventMassRetrieval.Complete || event is ReduxEventMassRetrieval.Error) {
					//Reload conversations
					viewModel.loadConversations()
				}
			}
	}
	
	val connectionManager = LocalConnectionManager.current
	val currentOnShowSyncDialog by rememberUpdatedState(onShowSyncDialog)
	
	//Listen for sync events
	LaunchedEffect(connectionManager) {
		ReduxEmitterNetwork.messageUpdateSubject.asFlow()
			.filterIsInstance<ReduxEventMessaging.Sync>()
			.collect {
				if(connectionManager == null) return@collect
				
				val deleteMessages = viewModel.conversations?.getOrNull()?.any { it.serviceHandler == ServiceHandler.appleBridge } ?: true
				currentOnShowSyncDialog(connectionManager, deleteMessages)
			}
	}
	
	//Check sync status when connected
	LaunchedEffect(connectionManager) {
		ReduxEmitterNetwork.connectionStateSubject.asFlow()
			.filterIsInstance<ReduxEventConnection.Connected>()
			.collect {
				if(connectionManager == null) return@collect
				
				println("Is connected, is pending sync: ${connectionManager.isPendingSync}")
				if(connectionManager.isPendingSync) {
					val deleteMessages = viewModel.conversations?.getOrNull()?.any { it.serviceHandler == ServiceHandler.appleBridge } ?: true
					currentOnShowSyncDialog(connectionManager, deleteMessages)
				}
			}
	}
	
	ConversationPaneLayout(
		modifier = modifier,
		floatingPane = floatingPane,
		conversations = viewModel.conversations,
		activeConversationID = activeConversationID,
		onSelectConversation = onSelectConversation,
		onReloadConversations = {
			viewModel.loadConversations()
		},
		onNavigateSettings = onNavigateSettings,
		onNewConversation = onNewConversation,
		syncState = syncEvent.let { event ->
			when(event) {
				is ReduxEventMassRetrieval.Start -> ProgressState.Indeterminate
				is ReduxEventMassRetrieval.Progress -> ProgressState.Determinate(
					event.receivedItems.toFloat() / event.totalItems.toFloat()
				)
				else -> null
			}
		},
		hasUnreadConversations = viewModel.hasUnreadConversations,
		onMarkConversationsAsRead = { viewModel.markConversationsAsRead() }
	)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun ConversationPaneLayout(
	modifier: Modifier = Modifier,
	floatingPane: Boolean = false,
	conversations: Result<List<ConversationInfo>>? = null,
	activeConversationID: Long? = null,
	onSelectConversation: (Long) -> Unit = {},
	onReloadConversations: () -> Unit = {},
	onNavigateSettings: () -> Unit = {},
	onNewConversation: () -> Unit = {},
	syncState: ProgressState? = null,
	hasUnreadConversations: Boolean = false,
	onMarkConversationsAsRead: () -> Unit = {},
) {
	val snackbarHostState = remember { SnackbarHostState() }
	val scrollBehavior = if(floatingPane) {
		TopAppBarDefaults.pinnedScrollBehavior()
	} else {
		TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
	}
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
	
	//Use a tinted background color in floating mode
	val backgroundColor = if(floatingPane) {
		MaterialTheme.colorScheme.inverseOnSurface
	} else {
		MaterialTheme.colorScheme.background
	}
	
	//Don't allow the user to create a new conversation while syncing
	val allowNewConversation = syncState == null
	
	Scaffold(
		modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
		topBar = {
			Crossfade(targetState = isActionMode) { isActionMode ->
				if(!isActionMode) {
					@Composable
					fun actions() {
						//New conversation button
						if(floatingPane && allowNewConversation) {
							IconButton(onClick = onNewConversation) {
								Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.action_newconversation))
							}
						}
						
						//Overflow menu
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
					}
					
					if(floatingPane) {
						TopAppBar(
							title = {
								Text(stringResource(id = R.string.app_name), fontWeight = FontWeight.Medium)
							},
							scrollBehavior = scrollBehavior,
							actions = { actions() },
							colors = TopAppBarDefaults.smallTopAppBarColors(
								containerColor = backgroundColor,
								scrolledContainerColor = backgroundColor,
								titleContentColor = MaterialTheme.colorScheme.primary
							)
						)
					} else {
						LargeTopAppBar(
							title = {
								Text(stringResource(id = R.string.app_name), fontWeight = FontWeight.Medium)
							},
							scrollBehavior = scrollBehavior,
							actions = { actions() },
							colors = TopAppBarDefaults.largeTopAppBarColors(
								titleContentColor = MaterialTheme.colorScheme.primary
							)
						)
					}
				} else {
					//Associate a conversation's ID to itself
					val conversationsMap = remember(conversations) {
						conversations?.getOrNull()?.associateBy { it.localID } ?: mapOf()
					}
					
					@Composable
					fun title() {
						Text(selectedConversations.size.let { size ->
							pluralStringResource(id = R.plurals.message_selectioncount, size, size)
						})
					}
					
					@Composable
					fun navigationIcon() {
						IconButton(onClick = { stopActionMode() }) {
							Icon(
								imageVector = Icons.Filled.Close,
								contentDescription = stringResource(id = android.R.string.cancel)
							)
						}
					}
					
					@Composable
					fun actions() {
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
					}
					
					if(floatingPane) {
						TopAppBar(
							title = { title() },
							scrollBehavior = scrollBehavior,
							navigationIcon = { navigationIcon() },
							actions = { actions() },
							colors = TopAppBarDefaults.smallTopAppBarColors(
								scrolledContainerColor = MaterialTheme.colorScheme.primaryContainer
							)
						)
					} else {
						LargeTopAppBar(
							title = { title() },
							scrollBehavior = scrollBehavior,
							navigationIcon = { navigationIcon() },
							actions = { actions() },
							colors = TopAppBarDefaults.largeTopAppBarColors(
								scrolledContainerColor = MaterialTheme.colorScheme.primaryContainer
							)
						)
					}
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
						activeConversationID = activeConversationID,
						onClickConversation = onSelectConversation,
						selectedConversations = selectedConversations,
						setSelectedConversations = { selectedConversations = it }
					)
				}
			}
		},
		snackbarHost = { SnackbarHost(snackbarHostState) },
		floatingActionButton = {
			if(!floatingPane && allowNewConversation) {
				FloatingActionButton(
					modifier = Modifier.navigationBarsPadding(),
					onClick = onNewConversation
				) {
					Icon(Icons.Outlined.Message, stringResource(id = R.string.action_newconversation))
				}
			}
		},
		containerColor = backgroundColor
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
		ConversationPaneLayout(
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
		ConversationPaneLayout(
			conversations = null
		)
	}
}

@Preview(name = "Error state")
@Composable
fun PreviewConversationListError() {
	AirMessageAndroidTheme {
		ConversationPaneLayout(
			conversations = Result.failure(Exception("Failed to load conversations"))
		)
	}
}

@Preview(name = "Syncing state")
@Composable
fun PreviewConversationListSync() {
	AirMessageAndroidTheme {
		ConversationPaneLayout(
			syncState = ProgressState.Determinate(progress = 0.5F)
		)
	}
}
