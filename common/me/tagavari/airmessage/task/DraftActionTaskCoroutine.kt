package me.tagavari.airmessage.task

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.tagavari.airmessage.connection.exception.AMRequestException
import me.tagavari.airmessage.container.ReadableBlob
import me.tagavari.airmessage.data.DatabaseManager
import me.tagavari.airmessage.enums.MessageSendErrorCode
import me.tagavari.airmessage.helper.AttachmentStorageHelper
import me.tagavari.airmessage.helper.DataCompressionHelper
import me.tagavari.airmessage.helper.DataStreamHelper
import me.tagavari.airmessage.messaging.FileDraft
import me.tagavari.airmessage.messaging.QueuedFile
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

object DraftActionTaskCoroutine {
	/**
	 * Prepares a [QueuedFile] from linked to draft
	 * @param context The context to use
	 * @param linkedFile The linked file to prepare
	 * @param conversationID The ID of the conversation to add the draft to
	 * @param compressionTarget The file size limit as required by the conversation (or -1 to disable)
	 * @param isDraftPrepare Whether this file is in the draft preparation directory, and to delete it once we are finished
	 * @param updateTime The time this draft file was updated
	 * @return The completed queued file instance
	 */
	suspend fun prepareLinkedToDraft(
		context: Context,
		linkedFile: QueuedFile,
		conversationID: Long,
		compressionTarget: Int,
		isDraftPrepare: Boolean,
		updateTime: Long
	): QueuedFile {
		assert(linkedFile.file.isA) { "Tried to prepare a non-linked file!" }
		
		val targetFile: File
		val draft: FileDraft
		
		withContext(Dispatchers.IO) {
			//Find a target file
			targetFile = AttachmentStorageHelper.prepareContentFile(context, AttachmentStorageHelper.dirNameDraft, linkedFile.fileName)
			
			//Write the draft file to the database
			draft = DatabaseManager.getInstance().addDraftReference(
				context,
				conversationID,
				targetFile,
				linkedFile.fileName,
				linkedFile.fileSize,
				linkedFile.fileType,
				-1,
				-1,
				updateTime
			) ?: run {
				//Clean up the file
				AttachmentStorageHelper.deleteContentFile(AttachmentStorageHelper.dirNameDraft, targetFile)
				throw AMRequestException(MessageSendErrorCode.localIO)
			}
		}
		
		//Copy and compress the file
		copyCompressAttachment(linkedFile.asReadableBlob(), targetFile, compressionTarget)
		
		//Delete the draft file
		if(isDraftPrepare) {
			linkedFile.file.nullableA?.invalidate()
			
			linkedFile.file.nullableB?.let { file ->
				withContext(Dispatchers.IO) {
					AttachmentStorageHelper.deleteContentFile(AttachmentStorageHelper.dirNameDraftPrepare, file)
				}
			}
		}
		
		return QueuedFile(draft)
	}
	
	/**
	 * Copies a blob (compressing if necessary) to a file
	 * @param file The file to copy from
	 * @param targetFile The file to copy to
	 * @param compressionTarget The upper file size limit (or -1 if not needed)
	 */
	suspend fun copyCompressAttachment(
		file: ReadableBlob,
		targetFile: File,
		compressionTarget: Int
	) {
		val fileData = file.getData()
		val fileSize = fileData.size ?: 0
		
		//Check if the file must be compressed
		if(compressionTarget != -1 && fileSize > compressionTarget) {
			//Check if compression is not applicable
			if(fileData.type == null || !DataCompressionHelper.isCompressable(fileData.type)) {
				throw AMRequestException(MessageSendErrorCode.localFileTooLarge)
			}
			
			//Compress the file to the target file
			@Suppress("BlockingMethodInNonBlockingContext")
			withContext(Dispatchers.IO) {
				DataCompressionHelper.compressFile(file, fileData.type, compressionTarget, targetFile)
			}
		} else {
			//Just copy the file
			@Suppress("BlockingMethodInNonBlockingContext")
			withContext(Dispatchers.IO) {
				BufferedInputStream(file.openInputStream()).use { inputStream ->
					BufferedOutputStream(FileOutputStream(targetFile)).use { outputStream ->
						DataStreamHelper.copyStream(inputStream, outputStream)
					}
				}
			}
		}
	}
}