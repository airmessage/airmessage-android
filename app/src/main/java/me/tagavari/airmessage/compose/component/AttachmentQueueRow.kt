package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.messaging.QueuedFile

@Composable
fun AttachmentQueueRow(
	attachments: List<QueuedFile>
) {
	Row(
		modifier = Modifier
			.height(192.dp)
			.horizontalScroll(rememberScrollState())
			.padding(8.dp),
		horizontalArrangement = Arrangement.spacedBy(8.dp)
	) {
		for(attachment in attachments) {
			AttachmentQueueEntry(queuedFile = attachment)
		}
	}
}