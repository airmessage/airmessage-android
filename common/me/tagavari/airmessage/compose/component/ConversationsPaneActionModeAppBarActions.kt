package me.tagavari.airmessage.compose.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.await
import me.tagavari.airmessage.R
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.task.ConversationActionTask

@Composable
fun ConversationsPaneActionModeAppBarActions(
	selectedConversations: Collection<ConversationInfo>,
	scope: CoroutineScope,
	snackbarHostState: SnackbarHostState,
	onStopActionMode: () -> Unit
) {
	val context = LocalContext.current
	
	//Index the selected conversation contents
	val selectionContents = remember(selectedConversations) {
		SelectionContents(
			hasMuted = selectedConversations.any { it.isMuted },
			hasUnmuted = selectedConversations.any { !it.isMuted },
			hasArchived = selectedConversations.any { it.isArchived },
			hasUnarchived = selectedConversations.any { !it.isArchived }
		)
	}
	
	//Map the conversations to their IDs
	val conversationIDs: Set<Long> = remember(selectedConversations) {
		selectedConversations.mapTo(mutableSetOf()) { it.localID }
	}
	
	var promptDeleteConversations by rememberSaveable { mutableStateOf(false) }
	
	@OptIn(DelicateCoroutinesApi::class)
	fun setConversationsMuted(muted: Boolean) {
		GlobalScope.launch {
			ConversationActionTask.muteConversations(
				conversationIDs,
				muted
			).await()
		}
	}
	
	@OptIn(DelicateCoroutinesApi::class)
	fun setConversationsArchived(archived: Boolean) {
		GlobalScope.launch {
			ConversationActionTask.archiveConversations(
				conversationIDs,
				archived
			).await()
		}
		
		scope.launch {
			val result = snackbarHostState.showSnackbar(
				message = context.resources.getQuantityString(
					if(archived) R.plurals.message_conversationarchived
					else R.plurals.message_conversationunarchived,
					conversationIDs.size,
					conversationIDs.size
				),
				actionLabel = context.resources.getString(R.string.action_undo),
				duration = SnackbarDuration.Short
			)
			
			if(result == SnackbarResult.ActionPerformed) {
				GlobalScope.launch {
					//Reverse the action
					ConversationActionTask.muteConversations(
						conversationIDs,
						!archived
					).await()
				}
			}
		}
	}
	
	if(selectionContents.hasMuted) {
		IconButton(onClick = {
			setConversationsMuted(false)
			onStopActionMode()
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
			onStopActionMode()
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
			onStopActionMode()
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
			onStopActionMode()
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
	
	if(promptDeleteConversations) {
		AlertDialog(
			onDismissRequest = { promptDeleteConversations = false },
			confirmButton = {
				TextButton(
					onClick = {
						//Delete the conversations
						@OptIn(DelicateCoroutinesApi::class)
						GlobalScope.launch {
							ConversationActionTask.deleteConversations(context, selectedConversations).await()
						}
						
						//Dismiss the dialog and stop action mode
						promptDeleteConversations = false
						onStopActionMode()
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

private data class SelectionContents(
	val hasMuted: Boolean,
	val hasUnmuted: Boolean,
	val hasArchived: Boolean,
	val hasUnarchived: Boolean
)
