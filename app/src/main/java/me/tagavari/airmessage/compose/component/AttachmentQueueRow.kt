package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import me.tagavari.airmessage.messaging.QueuedFile

@Composable
fun AttachmentQueueRow(
	attachments: List<QueuedFile>
) {
	Row {
		for(attachment in attachments) {
			AttachmentQueueEntry(queuedFile = attachment)
		}
	}
}