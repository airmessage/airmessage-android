package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagingScreen(
	navigationIcon: @Composable () -> Unit = {},
) {
	var showContentPicker by remember { mutableStateOf(false) }
	
	Scaffold(
		topBar = {
			Surface(tonalElevation = 2.dp) {
				SmallTopAppBar(
					title = { Text("Conversation") },
					modifier = Modifier.statusBarsPadding(),
					navigationIcon = navigationIcon
				)
			}
		},
		content = { paddingValues ->
			Column {
				MessageList(modifier = Modifier
					.weight(1F)
					.padding(paddingValues))
				MessageInputBar(
					modifier = Modifier
						.navigationBarsPadding()
						.imePadding(),
					onMessageSent = {},
					showContentPicker = showContentPicker,
					onChangeShowContentPicker = { showContentPicker = it }
				)
			}
		}
	)
}