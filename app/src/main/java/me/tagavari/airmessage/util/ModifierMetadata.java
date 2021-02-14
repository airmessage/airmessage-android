package me.tagavari.airmessage.util;

/**
 * A helper class to hold data about the positioning of a modifier
 */
public class ModifierMetadata {
	private final long messageID;
	private final int componentIndex;
	
	/**
	 * Creates a new modifier metadata
	 * @param messageID The ID of the message the modifier is attached to
	 * @param componentIndex The index of the component the modifier is attached to
	 */
	public ModifierMetadata(long messageID, int componentIndex) {
		this.messageID = messageID;
		this.componentIndex = componentIndex;
	}
	
	public long getMessageID() {
		return messageID;
	}
	
	public int getComponentIndex() {
		return componentIndex;
	}
}