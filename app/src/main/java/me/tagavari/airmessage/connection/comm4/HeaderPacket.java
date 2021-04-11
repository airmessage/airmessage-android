package me.tagavari.airmessage.connection.comm4;

/**
 * A packet sent or received from a {@link me.tagavari.airmessage.connection.DataProxy}
 * Has an extra 'type' value that determines the type of this message
 */
class HeaderPacket {
	private byte[] data;
	private int type;
	
	public HeaderPacket(byte[] data, int type) {
		this.data = data;
		this.type = type;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public int getType() {
		return type;
	}
}