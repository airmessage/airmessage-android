package me.tagavari.airmessage.common.connection.request;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import androidx.annotation.NonNull;
import me.tagavari.airmessage.common.connection.exception.AMRequestException;
import me.tagavari.airmessage.common.enums.MessageSendErrorCode;
import me.tagavari.airmessage.common.util.ConversationTarget;

import java.io.*;

/**
 * Represents a request to upload and send a message attachment
 */
public class FilePushRequest extends FileProcessingRequest {
	//Creating the reference values
	public static final int stageLinked = 0; //Has a link to the file, but no copy of it
	public static final int stageQueued = 1; //A copy of the file is stored and referenced in the chat's drafts
	public static final int stageAttached = 2; //The file is linked to an attachment
	public static final int stageFinished = 3; //The file processing request is completed, the file has been uploaded to the server
	
	//Creating the conversation values
	private final long conversationID;
	private final ConversationTarget conversationTarget;
	private final StageDataSource currentStage;
	private final StageDataTarget targetStage;
	private final int compressionTarget;
	
	public FilePushRequest(long conversationID, ConversationTarget conversationTarget, StageDataSource currentStage, StageDataTarget targetStage) {
		this(conversationID, conversationTarget, currentStage, targetStage, -1);
	}
	
	public FilePushRequest(long conversationID, ConversationTarget conversationTarget, StageDataSource currentStage, StageDataTarget targetStage, int compressionTarget) {
		if(currentStage.getIndex() >= targetStage.getIndex() || targetStage.getIndex() == stageLinked || currentStage.getIndex() == stageFinished) {
			throw new IllegalArgumentException("Stage data must be in order; received stage " + currentStage.getIndex() + " to stage " + targetStage.getIndex());
		}
		
		this.conversationID = conversationID;
		this.conversationTarget = conversationTarget;
		this.currentStage = currentStage;
		this.targetStage = targetStage;
		this.compressionTarget = compressionTarget;
	}
	
	public long getConversationID() {
		return conversationID;
	}
	
	public ConversationTarget getConversationTarget() {
		return conversationTarget;
	}
	
	public StageDataSource getCurrentStage() {
		return currentStage;
	}
	
	public StageDataTarget getTargetStage() {
		return targetStage;
	}
	
	public int getCompressionTarget() {
		return compressionTarget;
	}
	
	public static class FileStreamData implements Closeable {
		final Closeable closeable;
		final FileDescriptor fileDescriptor;
		final long length;
		
		public FileStreamData(Closeable closeable, FileDescriptor fileDescriptor, long length) {
			this.closeable = closeable;
			this.fileDescriptor = fileDescriptor;
			this.length = length;
		}
		
		public FileDescriptor getFileDescriptor() {
			return fileDescriptor;
		}
		
		public long getLength() {
			return length;
		}
		
		@Override
		public void close() throws IOException {
			closeable.close();
		}
		
		public static FileStreamData fromURI(Context context, Uri uri) throws FileNotFoundException {
			AssetFileDescriptor assetFileDescriptor = context.getContentResolver().openAssetFileDescriptor(uri, "r");
			return new FileStreamData(assetFileDescriptor, assetFileDescriptor.getFileDescriptor(), assetFileDescriptor.getLength());
		}
		
		public static FileStreamData fromFile(File file) throws IOException {
			FileInputStream fileInputStream = new FileInputStream(file);
			return new FileStreamData(fileInputStream, fileInputStream.getFD(), file.length());
		}
	}
	
	public static abstract class StageData {
		public abstract int getIndex();
	}
	
	public static abstract class StageDataSource extends StageData {
		private final String fileName, fileType;
		
		public StageDataSource(String fileName, String fileType) {
			this.fileName = fileName;
			this.fileType = fileType;
		}
		
		public String getFileName() {
			return fileName;
		}
		
		public String getFileType() {
			return fileType;
		}
		
		public abstract FileStreamData getStreamData(Context context) throws AMRequestException;
	}
	public static abstract class StageDataTarget extends StageData {}
	
	public static abstract class StageAttachmentTarget extends StageDataTarget {
		private final long attachmentID;
		
		public StageAttachmentTarget(long attachmentID) {
			this.attachmentID = attachmentID;
		}
		
		public long getAttachmentID() {
			return attachmentID;
		}
	}
	
	public static class StageLinkedSource extends StageDataSource {
		private final Uri uri;
		private final File file;
		
		public StageLinkedSource(String fileName, String fileType, @NonNull Uri uri) {
			super(fileName, fileType);
			
			this.uri = uri;
			this.file = null;
		}
		
		public StageLinkedSource(String fileName, String fileType, @NonNull File file) {
			super(fileName, fileType);
			
			this.uri = null;
			this.file = file;
		}
		
		@Override
		public int getIndex() {
			return stageLinked;
		}
		
		@Override
		public FileStreamData getStreamData(Context context) throws AMRequestException {
			if(uri != null) {
				try {
					return FileStreamData.fromURI(context, uri);
				} catch(IllegalArgumentException | SecurityException exception) {
					throw new AMRequestException(MessageSendErrorCode.localInternal, exception);
				} catch(FileNotFoundException exception) {
					throw new AMRequestException(MessageSendErrorCode.localInvalidContent, exception);
				}
			} else {
				try {
					return FileStreamData.fromFile(file);
				} catch(IOException exception) {
					throw new AMRequestException(MessageSendErrorCode.localIO, exception);
				}
			}
		}
	}
	
	public static class StageQueuedSource extends StageDataSource {
		private final File file;
		
		public StageQueuedSource(String fileName, String fileType, File file) {
			super(fileName, fileType);
			
			this.file = file;
		}
		
		public File getFile() {
			return file;
		}
		
		@Override
		public int getIndex() {
			return stageQueued;
		}
		
		@Override
		public FileStreamData getStreamData(Context context) throws AMRequestException {
			try {
				return FileStreamData.fromFile(file);
			} catch(IOException exception) {
				throw new AMRequestException(MessageSendErrorCode.localIO, exception);
			}
		}
	}
	
	public static class StageAttachedSource extends StageDataSource {
		private final File file;
		
		public StageAttachedSource(String fileName, String fileType, File file) {
			super(fileName, fileType);
			
			this.file = file;
		}
		
		public File getFile() {
			return file;
		}
		
		@Override
		public int getIndex() {
			return stageAttached;
		}
		
		@Override
		public FileStreamData getStreamData(Context context) throws AMRequestException {
			try {
				return FileStreamData.fromFile(file);
			} catch(IOException exception) {
				throw new AMRequestException(MessageSendErrorCode.localIO, exception);
			}
		}
	}
	
	public static class StageQueuedTarget extends StageDataTarget {
		private final String fileType;
		private final String fileName;
		private final long mediaStoreID;
		private final long modificationDate;
		private final long updateDate;
		
		public StageQueuedTarget(String fileType, String fileName, long mediaStoreID) {
			this(fileType, fileName, mediaStoreID, -1, -1);
		}
		
		public StageQueuedTarget(String fileType, String fileName, long mediaStoreID, long modificationDate, long updateDate) {
			this.fileType = fileType;
			this.fileName = fileName;
			this.mediaStoreID = mediaStoreID;
			this.modificationDate = modificationDate;
			this.updateDate = updateDate;
		}
		
		public String getFileType() {
			return fileType;
		}
		
		public String getFileName() {
			return fileName;
		}
		
		public long getMediaStoreID() {
			return mediaStoreID;
		}
		
		public long getModificationDate() {
			return modificationDate;
		}
		
		public long getUpdateDate() {
			return updateDate;
		}
		
		@Override
		public int getIndex() {
			return stageQueued;
		}
	}
	
	public static class StageAttachedTarget extends StageAttachmentTarget {
		public StageAttachedTarget(long attachmentID) {
			super(attachmentID);
		}
		
		@Override
		public int getIndex() {
			return stageAttached;
		}
	}
	
	public static class StageFinishedTarget extends StageAttachmentTarget {
		public StageFinishedTarget(long attachmentID) {
			super(attachmentID);
		}
		
		@Override
		public int getIndex() {
			return stageFinished;
		}
	}
}
