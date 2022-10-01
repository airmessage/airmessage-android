package me.tagavari.airmessage.compose.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import me.tagavari.airmessage.R

@Composable
fun ConversationPaneAppBarActions(
	props: ConversationPaneProps,
	allowNewConversation: Boolean
) {
	if(!props.isArchived) {
		//New conversation button
		if(props.isFloatingPane && allowNewConversation) {
			IconButton(onClick = props.onNewConversation) {
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
			val hasUnreadConversations = remember(props.conversations) {
				props.conversations?.getOrNull()?.any { it.unreadMessageCount > 0 }
					?: false
			}
			
			if(hasUnreadConversations) {
				DropdownMenuItem(
					text = { Text(stringResource(id = R.string.action_markallread)) },
					onClick = {
						menuOpen = false
						props.onMarkConversationsAsRead()
					}
				)
			}
			
			DropdownMenuItem(
				text = { Text(stringResource(id = R.string.screen_settings)) },
				onClick = {
					menuOpen = false
					props.onNavigateSettings()
				}
			)
			
			DropdownMenuItem(
				text = { Text(stringResource(id = R.string.screen_helpandfeedback)) },
				onClick = {
					menuOpen = false
					props.onNavigateHelp()
				}
			)
		}
	}
}
