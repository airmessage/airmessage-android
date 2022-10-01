package me.tagavari.airmessage.compose.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CoroutineScope
import me.tagavari.airmessage.R
import me.tagavari.airmessage.messaging.ConversationInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsPaneActionModeAppBar(
	props: ConversationPaneProps,
	scope: CoroutineScope,
	selectedConversations: Collection<ConversationInfo>,
	scrollBehavior: TopAppBarScrollBehavior,
	snackbarHostState: SnackbarHostState,
	onStopActionMode: () -> Unit
) {
	//Use a small app bar in floating pane or archived mode
	if(props.isFloatingPane || props.isArchived) {
		TopAppBar(
			title = { Title(count = selectedConversations.size) },
			scrollBehavior = scrollBehavior,
			navigationIcon = { NavigationIcon(onStopActionMode = onStopActionMode) },
			actions = {
				ConversationsPaneActionModeAppBarActions(
					selectedConversations = selectedConversations,
					scope = scope,
					snackbarHostState = snackbarHostState,
					onStopActionMode = onStopActionMode
				)
			},
			colors = TopAppBarDefaults.smallTopAppBarColors(
				scrolledContainerColor = MaterialTheme.colorScheme.primaryContainer
			)
		)
	} else {
		LargeTopAppBar(
			title = { Title(count = selectedConversations.size) },
			scrollBehavior = scrollBehavior,
			navigationIcon = { NavigationIcon(onStopActionMode = onStopActionMode) },
			actions = {
				ConversationsPaneActionModeAppBarActions(
					selectedConversations = selectedConversations,
					scope = scope,
					snackbarHostState = snackbarHostState,
					onStopActionMode = onStopActionMode
				)
			},
			colors = TopAppBarDefaults.largeTopAppBarColors(
				scrolledContainerColor = MaterialTheme.colorScheme.primaryContainer
			)
		)
	}
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Title(count: Int) {
	Text(
		text = pluralStringResource(id = R.plurals.message_selectioncount, count, count)
	)
}

@Composable
private fun NavigationIcon(
	onStopActionMode: () -> Unit
) {
	IconButton(onClick = onStopActionMode) {
		Icon(
			imageVector = Icons.Filled.Close,
			contentDescription = stringResource(id = android.R.string.cancel)
		)
	}
}
