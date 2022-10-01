package me.tagavari.airmessage.compose.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import me.tagavari.airmessage.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationPaneAppBar(
	props: ConversationPaneProps,
	backgroundColor: Color,
	scrollBehavior: TopAppBarScrollBehavior,
	allowNewConversation: Boolean
) {
	if(props.isArchived) {
		//Use small app bar titled "Archived"
		TopAppBar(
			title = {
				Text(stringResource(R.string.screen_archived))
			},
			navigationIcon = {
				IconButton(onClick = props.onNavigateBack) {
					Icon(
						imageVector = Icons.Filled.ArrowBack,
						contentDescription = stringResource(id = R.string.action_back)
					)
				}
			},
			scrollBehavior = scrollBehavior
		)
	} else if(props.isFloatingPane) {
		//Use small app bar in floating mode
		TopAppBar(
			title = {
				Text(
					text = stringResource(R.string.app_name),
					fontWeight = FontWeight.Medium
				)
			},
			scrollBehavior = scrollBehavior,
			actions = {
				ConversationPaneAppBarActions(
					props = props,
					allowNewConversation = allowNewConversation
				)
			},
			colors = TopAppBarDefaults.smallTopAppBarColors(
				containerColor = backgroundColor,
				scrolledContainerColor = backgroundColor,
				titleContentColor = MaterialTheme.colorScheme.primary
			)
		)
	} else {
		//Use large app bar
		LargeTopAppBar(
			title = {
				Text(
					text = stringResource(id = R.string.app_name),
					fontWeight = FontWeight.Medium
				)
			},
			scrollBehavior = scrollBehavior,
			actions = {
				ConversationPaneAppBarActions(
					props = props,
					allowNewConversation = allowNewConversation
				)
			},
			colors = TopAppBarDefaults.largeTopAppBarColors(
				titleContentColor = MaterialTheme.colorScheme.primary
			)
		)
	}
}