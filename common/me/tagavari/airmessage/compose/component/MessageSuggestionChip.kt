package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.messaging.AMConversationAction
import me.tagavari.airmessage.util.MessageFlowRadius

@Composable
fun MessageSuggestionChip(
	action: AMConversationAction,
	onClick: () -> Unit
) {
	val shape = if(action.isReplyAction) {
		RoundedCornerShape(
			topStart = MessageFlowRadius.large,
			topEnd = MessageFlowRadius.large,
			bottomEnd = MessageFlowRadius.small,
			bottomStart = MessageFlowRadius.large
		)
	} else {
		RoundedCornerShape(MessageFlowRadius.large)
	}
	
	Row(
		modifier = Modifier
			.clip(shape)
			.clickable(onClick = onClick)
			.border(
				width = 2.dp,
				color = MaterialTheme.colorScheme.outline,
				shape = shape
			)
			.heightIn(min = 40.dp)
			.padding(horizontal = 12.dp, vertical = 8.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		action.replyString?.let { text ->
			Text(
				text = text,
				color = MaterialTheme.colorScheme.tertiary
			)
		}
		
		action.remoteAction?.let { action ->
			action.icon?.let { icon ->
				AsyncImage(
					modifier = Modifier.size(24.dp),
					model = icon,
					contentDescription = null
				)
				
				Spacer(modifier = Modifier.width(8.dp))
			}
			Text(
				text = action.title,
				color = MaterialTheme.colorScheme.tertiary
			)
		}
	}
}

@Preview
@Composable
private fun PreviewMessageSuggestionChipText() {
	AirMessageAndroidTheme {
		Surface {
			MessageSuggestionChip(
				action = AMConversationAction.createReplyAction("Let's do it"),
				onClick = {}
			)
		}
	}
}
