package me.tagavari.airmessage.messaging;

import android.content.Context;
import android.net.Uri;

import org.lukhnos.nnio.file.Paths;

import java.io.File;

import me.tagavari.airmessage.MainApplication;

public class DraftFile {
	private final long localID;
	private final File file;
	private final String fileName;
	private final long fileSize;
	private final String fileType;
	private final long modificationDate;
	
	private final File originalFile;
	private final Uri originalUri;
	
	public DraftFile(long localID, File file, String fileName, long fileSize, String fileType, long modificationDate) {
		this.localID = localID;
		this.file = file;
		this.fileName = fileName;
		this.fileSize = fileSize;
		this.fileType = fileType;
		this.modificationDate = modificationDate;
		
		this.originalFile = null;
		this.originalUri = null;
	}
	
	public DraftFile(long localID, File file, String fileName, long fileSize, String fileType, long modificationDate, File originalFile, Uri originalUri) {
		this.localID = localID;
		this.file = file;
		this.fileName = fileName;
		this.fileSize = fileSize;
		this.fileType = fileType;
		this.modificationDate = modificationDate;
		
		this.originalFile = originalFile;
		this.originalUri = originalUri;
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
	
	public long getModificationDate() {
		return modificationDate;
	}
	
	public File getOriginalFile() {
		return originalFile;
	}
	
	public Uri getOriginalUri() {
		return originalUri;
	}
	
	public static String getRelativePath(Context context, File file) {
		return MainApplication.getDraftDirectory(context).toURI().relativize(file.toURI()).getPath();
	}
	
	public static File getAbsolutePath(Context context, String path) {
		return Paths.get(MainApplication.getDraftDirectory(context).getPath()).resolve(path).toFile();
	}
}
