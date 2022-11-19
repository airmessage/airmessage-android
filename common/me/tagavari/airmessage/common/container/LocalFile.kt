package me.tagavari.airmessage.common.container

import me.tagavari.airmessage.common.helper.AttachmentStorageHelper
import java.io.File

/**
 * A file with storage information
 */
class LocalFile(
	val file: File,
	val fileName: String,
	val fileType: String,
	val fileSize: Long,
	private val directoryID: String
) {
	/**
	 * Moves this file and cleans up its directory structure
	 */
	fun moveFile(targetFile: File): Boolean {
		if(!file.exists()) return true
		return file.renameTo(targetFile) && deleteFile()
	}
	
	/**
	 * Deletes this file and its directory structure
	 */
	fun deleteFile(): Boolean {
		return AttachmentStorageHelper.deleteContentFile(directoryID, file)
	}
}
