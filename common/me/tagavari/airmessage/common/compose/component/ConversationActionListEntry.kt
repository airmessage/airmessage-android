package me.tagavari.airmessage.common.compose.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.common.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.common.helper.LanguageHelper
import me.tagavari.airmessage.common.messaging.ConversationAction

@Composable
fun ConversationActionListEntry(
	conversationAction: ConversationAction
) {
	val context = LocalContext.current
	val message by produceState(conversationAction.getMessageDirect(context), conversationAction) {
		if(conversationAction.supportsBuildMessageAsync) {
			value = conversationAction.buildMessageAsync(context)
		}
	}
	
	ConversationActionListEntryLayout(
		date = conversationAction.date,
		message = AnnotatedString(message)
	)
}

@Composable
private fun ConversationActionListEntryLayout(
	date: Long,
	message: AnnotatedString
) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp, vertical = 16.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		val context = LocalContext.current
		
		//Time
		Text(
			text = LanguageHelper.generateTimeDividerString(context, date),
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			textAlign = TextAlign.Center
		)
		
		//Message
		Text(
			text = message,
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			textAlign = TextAlign.Center
		)
	}
}

@Preview(showBackground = true)
@Composable
private fun ConversationActionListEntryPreview() {
	AirMessageAndroidTheme {
		ConversationActionListEntryLayout(
			date = 1483068660000,
			message = AnnotatedString("Group name changed")
		)
	}
}