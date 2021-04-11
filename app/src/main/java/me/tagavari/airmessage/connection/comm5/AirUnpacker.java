package me.tagavari.airmessage.connection.comm5;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class AirUnpacker {
	private final ByteBuffer byteBuffer;
	
	public AirUnpacker(ByteBuffer byteBuffer) {
		this.byteBuffer = byteBuffer;
	}
	
	public AirUnpacker(byte[] byteArray) {
		this(ByteBuffer.wrap(byteArray));
	}
	
	public boolean unpackBoolean() throws BufferUnderflowException {
		return byteBuffer.get() == 1;
	}
	
	public short unpackShort() throws BufferUnderflowException {
		return byteBuffer.getShort();
	}
	
	public int unpackInt() throws BufferUnderflowException {
		return byteBuffer.getInt();
	}
	
	public int unpackArrayHeader() throws BufferUnderflowException {
		return unpackInt();
	}
	
	public long unpackLong() throws BufferUnderflowException {
		return byteBuffer.getLong();
	}
	
	public double unpackDouble() throws BufferUnderflowException {
		return byteBuffer.getDouble();
	}
	
	public String unpackString() throws BufferUnderflowException {
		return new String(unpackPayload(), StandardCharsets.UTF_8);
	}
	
	public String unpackNullableString() throws BufferUnderflowException {
		if(unpackBoolean()) {
			return unpackString();
		} else {
			return null;
		}
	}
	
	public byte[] unpackPayload() throws BufferUnderflowException {
		int length = unpackInt();
		byte[] data = new byte[length];
		byteBuffer.get(data);
		return data;
	}
	
	public byte[] unpackNullablePayload() throws BufferUnderflowException {
		if(unpackBoolean()) {
			return unpackPayload();
		} else {
			return null;
		}
	}
}