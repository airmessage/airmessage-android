package me.tagavari.airmessage.messaging;

import android.net.Uri;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.Objects;

import me.tagavari.airmessage.util.Union;

/**
 * Represents a reference to a file that can be loaded as a draft
 */
public class FileLinked {
	private final Union<File, Uri> file;
	
	private final String fileName;
	private final long fileSize;
	private final String fileType;
	@Nullable private final FileDisplayMetadata metadata;
	@Nullable private final MediaStore mediaStoreData;
	
	public FileLinked(Union<File, Uri> file, String fileName, long fileSize, String fileType) {
		this(file, fileName, fileSize, fileType, null, null);
	}
	
	public FileLinked(Union<File, Uri> file, String fileName, long fileSize, String fileType, @Nullable FileDisplayMetadata metadata) {
		this(file, fileName, fileSize, fileType, metadata, null);
	}
	
	public FileLinked(Union<File, Uri> file, String fileName, long fileSize, String fileType, @Nullable MediaStore mediaStoreData) {
		this(file, fileName, fileSize, fileType, null, mediaStoreData);
	}
	
	public FileLinked(Union<File, Uri> file, String fileName, long fileSize, String fileType, @Nullable FileDisplayMetadata metadata, @Nullable MediaStore mediaStoreData) {
		this.file = file;
		this.fileName = fileName;
		this.fileSize = fileSize;
		this.fileType = fileType;
		this.metadata = metadata;
		this.mediaStoreData = mediaStoreData;
	}
	
	public Union<File, Uri> getFile() {
		return file;
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public long getFileSize() {
		return fileSize;
	}
	
	public String getFileType() {
		return fileType;
	}
	
	@Nullable
	public FileDisplayMetadata getMetadata() {
		return metadata;
	}
	
	@Nullable
	public MediaStore getMediaStoreData() {
		return mediaStoreData;
	}
	
	@Override
	public boolean equals(@Nullable Object obj) {
		if(obj == null || obj.getClass() != this.getClass()) return false;
		
		FileLinked other = (FileLinked) obj;
		
		//If we don't have any mediastore data, we can't compare
		if(mediaStoreData == null || other.getMediaStoreData() == null) return false;
	
		//Compare mediastore data
		return (mediaStoreData.getMediaStoreID() == other.getMediaStoreData().getMediaStoreID()) ||
				(Objects.equals(fileType, other.getFileType()) && mediaStoreData.getModificationDate() == other.getMediaStoreData().getModificationDate());
	}
	
	public static class MediaStore {
		private final long mediaStoreID;
		private final long modificationDate;
		
		public MediaStore(long mediaStoreID, long modificationDate) {
			this.mediaStoreID = mediaStoreID;
			this.modificationDate = modificationDate;
		}
		
		public long getMediaStoreID() {
			return mediaStoreID;
		}
		
		public long getModificationDate() {
			return modificationDate;
		}
	}
}