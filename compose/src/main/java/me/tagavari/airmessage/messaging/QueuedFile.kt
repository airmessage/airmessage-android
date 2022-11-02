package me.tagavari.airmessage.messaging

import androidx.compose.runtime.Immutable
import me.tagavari.airmessage.constants.FileNameConstants
import me.tagavari.airmessage.constants.MIMEConstants
import me.tagavari.airmessage.container.LocalFile
import me.tagavari.airmessage.container.ReadableBlob
import me.tagavari.airmessage.container.ReadableBlobFile
import me.tagavari.airmessage.helper.AttachmentStorageHelper
import me.tagavari.airmessage.helper.FileHelper
import me.tagavari.airmessage.util.Union
import java.io.File

@Immutable
data class QueuedFile constructor(
	val localID: Long? = null,
	val file: Union<ReadableBlob, File>,
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
	
	/**
	 * Converts this QueuedFile to a [LocalFile]
	 */
	fun asLocalFile(): LocalFile? {
		val localFile = file.nullableB
			?: return null
		
		return LocalFile(
			file = localFile,
			fileName = fileName,
			fileType = fileType,
			fileSize = fileSize,
			directoryID = AttachmentStorageHelper.dirNameDraft
		)
	}
	
	/**
	 * Converts this QueuedFile to a [ReadableBlob]
	 */
	fun asReadableBlob(): ReadableBlob {
		return file.map(
			mapA = { it },
			mapB = { file -> ReadableBlobFile(file) }
		)
	}
	
	companion object {
		/**
		 * Creates a [QueuedFile] asynchronously from a [ReadableBlob]
		 */
		suspend fun fromReadableBlob(readableBlob: ReadableBlob): QueuedFile {
			val data = readableBlob.getData()
			
			return QueuedFile(
				file = Union.ofA(readableBlob),
				fileName = data.name ?: FileNameConstants.defaultFileName,
				fileSize = data.size ?: -1,
				fileType = data.type ?: MIMEConstants.defaultMIMEType
			)
		}
	}
}
