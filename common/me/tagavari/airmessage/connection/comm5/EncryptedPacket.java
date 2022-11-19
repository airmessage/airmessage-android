package me.tagavari.airmessage.connection.comm5;

/**
 * A packet sent or received from a {@link me.tagavari.airmessage.connection.DataProxy}
 * Has an extra 'encrypt' flag that determines if this packet is encrypted during transit
 */
public class EncryptedPacket {
	private byte[] data;
	private boolean encrypt;
	
	public EncryptedPacket(byte[] data, boolean encrypt) {
		this.data = data;
		this.encrypt = encrypt;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public boolean getEncrypt() {
		return encrypt;
	}
}