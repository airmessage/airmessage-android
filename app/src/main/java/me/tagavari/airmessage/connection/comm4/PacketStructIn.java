package me.tagavari.airmessage.connection.comm4;

final class PacketStructIn {
	private final int header;
	private final byte[] data;
	
	PacketStructIn(int header, byte[] data) {
		this.header = header;
		this.data = data;
	}
	
	int getHeader() {
		return header;
	}
	
	byte[] getData() {
		return data;
	}
}