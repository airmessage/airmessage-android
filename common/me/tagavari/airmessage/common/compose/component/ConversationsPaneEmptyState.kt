package me.tagavari.airmessage.common.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.R
import me.tagavari.airmessage.common.compose.ui.theme.AirMessageAndroidTheme

@Composable
fun ConversationsPaneEmptyState(
	modifier: Modifier = Modifier,
	type: ConversationsPaneEmptyStateType = ConversationsPaneEmptyStateType.CONVERSATIONS
) {
	Column(
		modifier = modifier.padding(16.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		Icon(
			modifier = Modifier.size(48.dp),
			imageVector = Icons.Default.ChatBubble,
			contentDescription = null,
			tint = MaterialTheme.colorScheme.onSurfaceVariant
		)
		
		Spacer(modifier = Modifier.height(16.dp))
		
		Text(
			text = stringResource(
				id = when(type) {
					ConversationsPaneEmptyStateType.CONVERSATIONS -> R.string.message_blankstate_conversations
					ConversationsPaneEmptyStateType.ARCHIVED -> R.string.message_blankstate_conversations_archived
				}
			),
			style = MaterialTheme.typography.titleLarge,
			textAlign = TextAlign.Center
		)
		
		Spacer(modifier = Modifier.height(8.dp))
		
		Text(
			text = stringResource(
				id = when(type) {
					ConversationsPaneEmptyStateType.CONVERSATIONS -> R.string.message_blankstate_conversations_description
					ConversationsPaneEmptyStateType.ARCHIVED -> R.string.message_blankstate_conversations_archived_description
				}
			),
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			textAlign = TextAlign.Center
		)
	}
}

enum class ConversationsPaneEmptyStateType {
	CONVERSATIONS,
	ARCHIVED
}

@Preview(name = "Empty conversations", showBackground = true, widthDp = 400, heightDp = 600)
@Composable
private fun ConversationsPaneEmptyStatePreview() {
	AirMessageAndroidTheme {
		ConversationsPaneEmptyState(
			modifier = Modifier.fillMaxSize()
		)
	}
}

@Preview(name = "Empty archived conversations", showBackground = true, widthDp = 400, heightDp = 600)
@Composable
private fun ConversationsPaneEmptyStateArchivedPreview() {
	AirMessageAndroidTheme {
		ConversationsPaneEmptyState(
			modifier = Modifier.fillMaxSize(),
			type = ConversationsPaneEmptyStateType.ARCHIVED
		)
	}
}
