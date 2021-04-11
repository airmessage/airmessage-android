package me.tagavari.airmessage.util;

import me.tagavari.airmessage.enums.MessageState;

/**
 * A class for passing around data related to an activity status update
 */
public class ActivityStatusUpdate {
	private final long messageID;
	@MessageState private final int messageState;
	private final long dateRead;
	
	/**
	 * Constructs a new activity status update
	 * @param messageID The ID of the message that is being updated
	 * @param messageState The message's new state
	 * @param dateRead The date the message was read
	 */
	public ActivityStatusUpdate(long messageID, @MessageState int messageState, long dateRead) {
		this.messageID = messageID;
		this.messageState = messageState;
		this.dateRead = dateRead;
	}
	
	/**
	 * Constructs a new activity status update
	 * @param messageID The ID of the message that is being updated
	 * @param messageState The message's new state
	 */
	public ActivityStatusUpdate(long messageID, @MessageState int messageState) {
		this(messageID, messageState, -1);
	}
	
	public long getMessageID() {
		return messageID;
	}
	
	@MessageState
	public int getMessageState() {
		return messageState;
	}
	
	public long getDateRead() {
		return dateRead;
	}
}