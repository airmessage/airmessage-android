package me.tagavari.airmessage.messaging;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.function.Supplier;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.subjects.SingleSubject;
import me.tagavari.airmessage.enums.MessageComponentType;
import me.tagavari.airmessage.util.ProcessProgress;

public class AttachmentInfo extends MessageComponent {
	//Creating the values
	private final String fileName;
	private final String fileType;
	private long fileSize;
	private long sort;
	private File file = null;
	private byte[] fileChecksum = null;
	
	//Creating the state values
	private transient SingleSubject<FileDisplayMetadata> displayMetadataSubject;
	
	public AttachmentInfo(long localID, String guid, String fileName, String fileType, long fileSize, long sort) {
		//Calling the super constructor
		super(localID, guid);
		
		//Setting the values
		this.fileName = fileName;
		this.fileType = fileType;
		this.fileSize = fileSize;
		this.sort = sort;
	}
	
	public AttachmentInfo(long localID, String guid, String fileName, String fileType, long fileSize, long sort, File file) {
		//Calling the main constructor
		this(localID, guid, fileName, fileType, fileSize, sort);
		
		//Setting the file
		this.file = file;
	}
	
	public AttachmentInfo(long localID, String guid, String fileName, String fileType, long fileSize, long sort, byte[] fileChecksum) {
		//Calling the main constructor
		this(localID, guid, fileName, fileType, fileSize, sort);
		
		//Setting the checksum
		this.fileChecksum = fileChecksum;
	}
	
	private AttachmentInfo(long localID, String guid, String fileName, String fileType, long fileSize, long sort, File file, byte[] fileChecksum) {
		//Calling the main constructor
		this(localID, guid, fileName, fileType, fileSize, sort);
		
		//Setting the file and the checksum
		this.file = file;
		this.fileChecksum = fileChecksum;
	}
	
	public byte[] getFileChecksum() {
		return fileChecksum;
	}
	
	public void setFileChecksum(byte[] fileChecksum) {
		this.fileChecksum = fileChecksum;
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public String getContentType() {
		return fileType;
	}
	
	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}
	
	public long getFileSize() {
		return fileSize;
	}
	
	public long getSort() {
		return sort;
	}
	
	public void setFile(File file) {
		this.file = file;
	}
	
	public File getFile() {
		return file;
	}
	
	public <T extends FileDisplayMetadata> Single<T> getDisplayMetadata(Supplier<Single<T>> supplier) {
		if(displayMetadataSubject != null) return (Single<T>) displayMetadataSubject;
		displayMetadataSubject = SingleSubject.create();
		supplier.get().subscribe(displayMetadataSubject);
		return (Single<T>) displayMetadataSubject;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel out, int flags) {
		super.writeToParcel(out, flags);
		
		out.writeString(fileName);
		out.writeString(fileType);
		out.writeLong(fileSize);
		out.writeLong(sort);
		out.writeString(file != null ? file.getPath() : null);
		if(fileChecksum != null) {
			out.writeInt(fileChecksum.length);
			out.writeByteArray(fileChecksum);
		} else {
			out.writeInt(0);
		}
	}
	
	public static final Parcelable.Creator<AttachmentInfo> CREATOR = new Parcelable.Creator<AttachmentInfo>() {
		public AttachmentInfo createFromParcel(Parcel in) {
			return new AttachmentInfo(in);
		}
		
		public AttachmentInfo[] newArray(int size) {
			return new AttachmentInfo[size];
		}
	};
	
	private AttachmentInfo(Parcel in) {
		super(in);
		
		fileName = in.readString();
		fileType = in.readString();
		fileSize = in.readLong();
		sort = in.readLong();
		String filePath = in.readString();
		file = filePath != null ? new File(filePath) : null;
		int fileChecksumLength = in.readInt();
		if(fileChecksumLength > 0) {
			fileChecksum = new byte[fileChecksumLength];
			in.readByteArray(fileChecksum);
		} else {
			fileChecksum = null;
		}
	}
	
	@NonNull
	@Override
	protected AttachmentInfo clone() {
		return new AttachmentInfo(getLocalID(), getGUID(), fileName, fileType, fileSize, sort, file, fileChecksum);
	}
}
