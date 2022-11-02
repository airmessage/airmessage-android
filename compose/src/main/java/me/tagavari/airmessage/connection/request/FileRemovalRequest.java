package me.tagavari.airmessage.connection.request;

import me.tagavari.airmessage.messaging.FileDraft;

/**
 * Represents a request to delete a draft file
 */
public class FileRemovalRequest extends FileProcessingRequest {
	private final FileDraft draftFile;
	private final long updateTime;
	
	public FileRemovalRequest(FileDraft draftFile, long updateTime) {
		this.draftFile = draftFile;
		this.updateTime = updateTime;
	}
	
	public FileDraft getDraftFile() {
		return draftFile;
	}
	
	public long getUpdateTime() {
		return updateTime;
	}
}
