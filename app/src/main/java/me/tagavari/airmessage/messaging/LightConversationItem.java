package me.tagavari.airmessage.messaging;

public class LightConversationItem {
	//Creating the message values
	private String message;
	private final long date;
	private final long localID;
	private final long serverID;
	private final boolean isPinned;
	private final boolean isError;
	
	public LightConversationItem(String message, long date, long localID, long serverID, boolean isError) {
		//Setting the values
		this.message = message;
		this.date = date;
		this.localID = localID;
		this.serverID = serverID;
		this.isPinned = false;
		this.isError = isError;
	}
	
	public LightConversationItem(String message, long date, boolean isPinned) {
		this.message = message;
		this.date = date;
		localID = serverID = -1;
		this.isPinned = isPinned;
		this.isError = false;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public long getDate() {
		return date;
	}
	
	public long getLocalID() {
		return localID;
	}
	
	public long getServerID() {
		return serverID;
	}
	
	public boolean isPinned() {
		return isPinned;
	}
	
	public boolean isError() {
		return isError;
	}
}