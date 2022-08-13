package me.tagavari.airmessage.messaging

import android.content.Context
import android.net.Uri
import me.tagavari.airmessage.helper.FileHelper
import me.tagavari.airmessage.util.Union
import java.io.File

data class QueuedFile(
	val localID: Long? = null,
	val file: Union<Uri, File>,
	val fileName: String,
	val fileSize: Long,
	val fileType: String,
	val mediaStoreID: Long? = null,
	val modificationDate: Long? = null
) {
	constructor(file: File) : this(
		file = Union.ofB(file),
		fileName = file.name,
		fileSize = file.length(),
		fileType = FileHelper.getMimeType(file)
	)
	
	constructor(fileDraft: FileDraft) : this(
		localID = fileDraft.localID,
		file = Union.ofB(fileDraft.file),
		fileName = fileDraft.fileName,
		fileSize = fileDraft.fileSize,
		fileType = fileDraft.fileType,
		mediaStoreID = fileDraft.mediaStoreID,
		modificationDate = fileDraft.modificationDate
	)
	
	fun toFileLinked() = FileLinked(
		file.map(mapA = { Union.ofB(it) }, mapB = { Union.ofA(it) }),
		fileName,
		fileSize,
		fileType,
		null,
		if(mediaStoreID != null && modificationDate != null) FileLinked.MediaStore(mediaStoreID, modificationDate) else null
	)
	
	fun toFileDraft(): FileDraft {
		if(localID == null) {
			throw IllegalStateException("Cannot convert a non-prepared QueuedFile to FileDraft!")
		}
		
		return FileDraft(
			localID,
			file.b,
			fileName,
			fileSize,
			fileType,
			mediaStoreID,
			modificationDate
		)
	}
}
