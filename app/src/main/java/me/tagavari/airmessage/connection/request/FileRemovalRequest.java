package me.tagavari.airmessage.connection.request;

import me.tagavari.airmessage.messaging.DraftFile;

public class FileRemovalRequest extends FileProcessingRequest {
	private final DraftFile draftFile;
	private final long updateTime;
	
	public FileRemovalRequest(DraftFile draftFile, long updateTime) {
		this.draftFile = draftFile;
		this.updateTime = updateTime;
	}
	
	public DraftFile getDraftFile() {
		return draftFile;
	}
	
	public long getUpdateTime() {
		return updateTime;
	}
}
