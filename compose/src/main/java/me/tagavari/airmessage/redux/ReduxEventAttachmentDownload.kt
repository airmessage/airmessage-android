package me.tagavari.airmessage.redux

import androidx.compose.runtime.Immutable
import java.io.File

//An event to represent the status of an attachment download
@Immutable
sealed class ReduxEventAttachmentDownload {
	data class Start(val fileLength: Long) : ReduxEventAttachmentDownload()
	data class Progress(val bytesProgress: Long, val bytesTotal: Long) : ReduxEventAttachmentDownload()
	@Immutable data class Complete(val file: File) : ReduxEventAttachmentDownload()
}
