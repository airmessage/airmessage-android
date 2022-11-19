package me.tagavari.airmessage.common.redux

//An event to represent the status of an attachment upload
abstract class ReduxEventAttachmentUpload {
	//While this file is being uploaded
	data class Progress(val bytesProgress: Long, val bytesTotal: Long) : ReduxEventAttachmentUpload()
	
	//When this file has finished being uploaded
	data class Complete(val fileHash: ByteArray) : ReduxEventAttachmentUpload()
}