package me.tagavari.airmessage.common.task;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.reactivex.rxjava3.schedulers.Schedulers;
import me.tagavari.airmessage.common.connection.exception.AMRequestException;
import me.tagavari.airmessage.common.container.ReadableBlob;
import me.tagavari.airmessage.common.container.ReadableBlobFile;
import me.tagavari.airmessage.common.container.ReadableBlobUri;
import me.tagavari.airmessage.common.data.DatabaseManager;
import me.tagavari.airmessage.common.enums.MessageSendErrorCode;
import me.tagavari.airmessage.common.helper.AttachmentStorageHelper;
import me.tagavari.airmessage.common.helper.DataCompressionHelper;
import me.tagavari.airmessage.common.helper.DataStreamHelper;
import me.tagavari.airmessage.common.messaging.FileDraft;
import me.tagavari.airmessage.common.messaging.FileLinked;

@Deprecated
public class DraftActionTask {
	/**
	 * Prepares a {@link FileLinked} to a {@link FileDraft}
	 * @param context The context to use
	 * @param linkedFile The linked file to prepare
	 * @param conversationID The ID of the conversation to add the draft to
	 * @param compressionTarget The file size limit as required by the conversation (or -1 to disable)
	 * @param isDraftPrepare Whether this file is in the draft preparation directory, and to delete it once we are finished
	 * @param updateTime The time this draft file was updated
	 * @return A single for the completed draft
	 */
	public static Single<FileDraft> prepareLinkedToDraft(Context context, FileLinked linkedFile, long conversationID, int compressionTarget, boolean isDraftPrepare, long updateTime) {
		return Single.create((SingleEmitter<FileDraft> emitter) -> {
			//Finding a target file
			File targetFile = AttachmentStorageHelper.prepareContentFile(context, AttachmentStorageHelper.dirNameDraft, linkedFile.getFileName());
			
			//Writing the draft file to the database
			FileDraft draft = DatabaseManager.getInstance().addDraftReference(context, conversationID, targetFile, linkedFile.getFileName(), linkedFile.getFileSize(), linkedFile.getFileType(), linkedFile.getMediaStoreData() == null ? -1 : linkedFile.getMediaStoreData().getMediaStoreID(), linkedFile.getMediaStoreData() == null ? -1 : linkedFile.getMediaStoreData().getModificationDate(), updateTime);
			if(draft == null) {
				//Cleaning up the file
				AttachmentStorageHelper.deleteContentFile(AttachmentStorageHelper.dirNameDraft, targetFile);
				
				emitter.onError(new AMRequestException(MessageSendErrorCode.localIO));
				return;
			}
			
			emitter.onSuccess(draft);
		}).subscribeOn(Schedulers.single()).observeOn(Schedulers.io()).doOnSuccess(draft -> {
			//Copying and compressing the file
			copyCompressStreamToFile(linkedFile.getFile().map(ReadableBlobFile::new, ReadableBlobUri::new), linkedFile.getFileSize(), linkedFile.getFileType(), draft.getFile(), compressionTarget);
		}).doFinally(() -> {
			//Deleting the source file
			if(isDraftPrepare && linkedFile.getFile().isA()) {
				AttachmentStorageHelper.deleteContentFile(AttachmentStorageHelper.dirNameDraftPrepare, linkedFile.getFile().getA());
			}
		}).observeOn(AndroidSchedulers.mainThread());
	}
	
	/**
	 * Copies a FileDescriptor (compressing if necessary) to a file
	 * @param blob The blob to copy from
	 * @param fileSize The size of the file
	 * @param fileType The type of the file
	 * @param targetFile The file to copy to
	 * @param compressionTarget The upper file size limit (or -1 if not needed)
	 */
	private static void copyCompressStreamToFile(ReadableBlob blob, long fileSize, String fileType, File targetFile, int compressionTarget) throws AMRequestException {
		//Checking if the file must be compressed
		if(compressionTarget != -1 && fileSize > compressionTarget) {
			//Checking if compression is not applicable
			if(!DataCompressionHelper.isCompressable(fileType)) {
				throw new AMRequestException(MessageSendErrorCode.localFileTooLarge);
			}
			
			//Compressing the file to the target file
			try {
				DataCompressionHelper.compressFile(blob, fileType, compressionTarget, targetFile);
			} catch(IOException exception) {
				throw new AMRequestException(MessageSendErrorCode.localIO, exception);
			}
		} else {
			//Just copy the file
			try(OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(targetFile))) {
				DataStreamHelper.copyStream(new BufferedInputStream(blob.openInputStream()), outputStream);
			} catch(IOException exception) {
				throw new AMRequestException(MessageSendErrorCode.localIO, exception);
			}
		}
	}
}