package me.tagavari.airmessage.messaging;

public class LightConversationItem {
	//Creating the message values
	private String message;
	private final long date;
	private final long localID;
	private final long serverID;
	private final boolean isPinned;
	
	public LightConversationItem(String message, long date, long localID, long serverID) {
		//Setting the values
		this.message = message;
		this.date = date;
		this.localID = localID;
		this.serverID = serverID;
		this.isPinned = false;
	}
	
	public LightConversationItem(String message, long date, boolean isPinned) {
		this.message = message;
		this.date = date;
		localID = serverID = -1;
		this.isPinned = isPinned;
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
}