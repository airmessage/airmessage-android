package me.tagavari.airmessage.messaging;

public class StickerInfo {
	//Creating the sticker values
	private long localID;
	private String guid;
	private long messageID;
	private int messageIndex;
	private String sender;
	private long date;
	
	public StickerInfo(long localID, String guid, long messageID, int messageIndex, String sender, long date) {
		this.localID = localID;
		this.guid = guid;
		this.messageID = messageID;
		this.messageIndex = messageIndex;
		this.sender = sender;
		this.date = date;
	}
	
	public long getLocalID() {
		return localID;
	}
	
	public String getGuid() {
		return guid;
	}
	
	public long getMessageID() {
		return messageID;
	}
	
	public int getMessageIndex() {
		return messageIndex;
	}
	
	public String getSender() {
		return sender;
	}
	
	public long getDate() {
		return date;
	}
}
