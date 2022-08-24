package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.tagavari.airmessage.helper.FileHelper
import me.tagavari.airmessage.messaging.QueuedFile

@Composable
fun AttachmentQueueEntry(
	queuedFile: QueuedFile
) {
	if(FileHelper.compareMimeTypes(queuedFile.fileType, "image/*")
		|| FileHelper.compareMimeTypes(queuedFile.fileType, "video/*")) {
		Box(
			modifier = Modifier.fillMaxHeight()
		) {
			AttachmentQueueEntryMedia(queuedFile = queuedFile)
		}
	}
}