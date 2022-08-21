package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.helper.FileHelper
import me.tagavari.airmessage.messaging.QueuedFile

@Composable
fun AttachmentQueueEntry(
	queuedFile: QueuedFile
) {
	if(FileHelper.compareMimeTypes(queuedFile.fileType, "image/*")
		|| FileHelper.compareMimeTypes(queuedFile.fileType, "video/*")) {
		Box(
			modifier = Modifier.size(100.dp)
		) {
			AttachmentQueueEntryMedia(queuedFile = queuedFile)
		}
	}
}