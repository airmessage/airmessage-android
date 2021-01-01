package me.tagavari.airmessage.messaging;

import me.tagavari.airmessage.enums.MessagePreviewType;

/**
 * Represents a preview card for a message component
 */
public class MessagePreviewInfo {
	@MessagePreviewType private final int type;
	private final long localID;
	private final byte[] data;
	private final String target;
	private final String title;
	private final String subtitle;
	private final String caption;
	
	public MessagePreviewInfo(@MessagePreviewType int type, long localID, byte[] data, String target, String title, String subtitle, String caption) {
		this.type = type;
		this.localID = localID;
		this.data = data;
		this.target = target;
		this.title = title;
		this.subtitle = subtitle;
		this.caption = caption;
	}
	
	@MessagePreviewType
	public int getType() {
		return type;
	}
	
	public long getLocalID() {
		return localID;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public String getTarget() {
		return target;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getSubtitle() {
		return subtitle;
	}
	
	public String getCaption() {
		return caption;
	}
}
