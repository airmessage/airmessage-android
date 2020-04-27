package me.tagavari.airmessage.connection.comm4;

final class PacketStructOut {
	public final int type;
	public final byte[] content;
	public Runnable sentRunnable;
	
	PacketStructOut(int type, byte[] content) {
		this.type = type;
		this.content = content;
	}
	
	PacketStructOut(int type, byte[] content, Runnable sentRunnable) {
		this(type, content);
		this.sentRunnable = sentRunnable;
	}
}