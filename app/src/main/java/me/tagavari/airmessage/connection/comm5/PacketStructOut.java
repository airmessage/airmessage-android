package me.tagavari.airmessage.connection.comm5;

public class PacketStructOut {
	private final byte[] data;
	private final boolean encrypt;
	private Runnable sentRunnable;
	
	PacketStructOut(byte[] data, boolean encrypt) {
		this.data = data;
		this.encrypt = encrypt;
	}
	
	PacketStructOut(byte[] data, boolean encrypt, Runnable sentRunnable) {
		this(data, encrypt);
		this.sentRunnable = sentRunnable;
	}
	
	byte[] getData() {
		return data;
	}
	
	boolean getEncrypt() {
		return encrypt;
	}
	
	Runnable getSentRunnable() {
		return sentRunnable;
	}
}