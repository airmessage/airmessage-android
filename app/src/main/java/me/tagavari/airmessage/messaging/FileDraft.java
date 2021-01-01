package me.tagavari.airmessage.messaging;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.Serializable;

/**
 * Represents a file queued to be sent
 */
public class FileDraft implements Serializable, Parcelable {
	private final long localID;
	private final File file;
	private final String fileName;
	private final long fileSize;
	private final String fileType;
	private final long mediaStoreID;
	private final long modificationDate;
	
	public FileDraft(long localID, File file, String fileName, long fileSize, String fileType) {
		this.localID = localID;
		this.file = file;
		this.fileName = fileName;
		this.fileSize = fileSize;
		this.fileType = fileType;
		this.mediaStoreID = -1;
		this.modificationDate = -1;
	}
	
	public FileDraft(long localID, File file, String fileName, long fileSize, String fileType, long mediaStoreID, long modificationDate) {
		this.localID = localID;
		this.file = file;
		this.fileName = fileName;
		this.fileSize = fileSize;
		this.fileType = fileType;
		this.mediaStoreID = mediaStoreID;
		this.modificationDate = modificationDate;
	}
	
	private FileDraft(Parcel in) {
		this.localID = in.readLong();
		this.file = new File(in.readString());
		this.fileName = in.readString();
		this.fileSize = in.readLong();
		this.fileType = in.readString();
		this.mediaStoreID = in.readLong();
		this.modificationDate = in.readLong();
	}
	
	public long getLocalID() {
		return localID;
	}
	
	public File getFile() {
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
	
	public long getMediaStoreID() {
		return mediaStoreID;
	}
	
	public long getModificationDate() {
		return modificationDate;
	}
	
	@Override
	public boolean equals(@Nullable Object obj) {
		if(obj == null || obj.getClass() != this.getClass()) return false;
		
		//Require other draft files to have a defined MediaStore ID, and have the MediaStore ID and modification date match
		FileDraft other = (FileDraft) obj;
		return other.getMediaStoreID() != -1 && getMediaStoreID() == other.getMediaStoreID() && getModificationDate() == other.getModificationDate();
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	public void writeToParcel(Parcel out, int flags) {
		out.writeLong(localID);
		out.writeString(file.getPath());
		out.writeString(fileName);
		out.writeLong(fileSize);
		out.writeString(fileType);
		out.writeLong(mediaStoreID);
		out.writeLong(modificationDate);
	}
	
	public static final Parcelable.Creator<FileDraft> CREATOR = new Parcelable.Creator<FileDraft>() {
		public FileDraft createFromParcel(Parcel in) {
			return new FileDraft(in);
		}
		
		public FileDraft[] newArray(int size) {
			return new FileDraft[size];
		}
	};
}
