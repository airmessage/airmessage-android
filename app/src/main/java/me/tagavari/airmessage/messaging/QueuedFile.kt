package me.tagavari.airmessage.messaging

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.database.getStringOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.tagavari.airmessage.constants.MIMEConstants
import me.tagavari.airmessage.helper.AttachmentStorageHelper
import me.tagavari.airmessage.helper.FileHelper
import me.tagavari.airmessage.util.Union
import java.io.File
import java.util.*


data class QueuedFile constructor(
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
	
	/**
	 * Converts this QueuedFile to a [LocalFile]
	 */
	fun toLocalFile(): LocalFile? {
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
	
	companion object {
		/**
		 * Creates a [QueuedFile] asynchronously from a [Uri]
		 */
		suspend fun fromURI(uri: Uri, context: Context): QueuedFile {
			var fileName = ""
			var fileSize = -1L
			var fileType = MIMEConstants.defaultMIMEType
			
			withContext(Dispatchers.IO) {
				//Query the URI for file size and file name
				context.contentResolver.query(uri, null, null, null, null, null).use { cursor ->
					if(cursor == null || !cursor.moveToFirst()) return@use
					
					cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).let { columnName ->
						if(!cursor.isNull(columnName)) {
							fileName = cursor.getStringOrNull(columnName) ?: ""
						}
					}
					
					cursor.getColumnIndex(OpenableColumns.SIZE).let { columnName ->
						if(!cursor.isNull(columnName)) {
							fileSize = cursor.getLong(columnName)
						}
					}
				}
				
				//Resolve the mime type
				if(uri.scheme == ContentResolver.SCHEME_CONTENT) {
					context.contentResolver.getType(uri)
				} else {
					val fileExtension: String = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
					MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.lowercase(Locale.getDefault()))
				}?.let { fileType = it }
			}
			
			return QueuedFile(
				file = Union.ofA(uri),
				fileName = fileName,
				fileSize = fileSize,
				fileType = fileType
			)
		}
	}
}
