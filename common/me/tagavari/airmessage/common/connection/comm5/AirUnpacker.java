package me.tagavari.airmessage.common.connection.comm5;

import androidx.annotation.NonNull;
import me.tagavari.airmessage.common.connection.exception.LargeAllocationException;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class AirUnpacker {
	private static final long maxPacketAllocation = 50 * 1024 * 1024; //50 MB
	
	@NonNull
	private final ByteBuffer byteBuffer;
	
	public AirUnpacker(@NonNull ByteBuffer byteBuffer) {
		this.byteBuffer = byteBuffer;
	}
	
	public AirUnpacker(@NonNull byte[] byteArray) {
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
	
	@NonNull
	public String unpackString() throws BufferUnderflowException, LargeAllocationException {
		return new String(unpackPayload(), StandardCharsets.UTF_8);
	}
	
	public String unpackNullableString() throws BufferUnderflowException, LargeAllocationException {
		if(unpackBoolean()) {
			return unpackString();
		} else {
			return null;
		}
	}
	
	@NonNull
	public byte[] unpackPayload() throws BufferUnderflowException, LargeAllocationException {
		int length = unpackInt();
		if(length >= maxPacketAllocation) {
			throw new LargeAllocationException(length, maxPacketAllocation);
		}
		byte[] data = new byte[length];
		byteBuffer.get(data);
		return data;
	}
	
	public byte[] unpackNullablePayload() throws BufferUnderflowException, LargeAllocationException {
		if(unpackBoolean()) {
			return unpackPayload();
		} else {
			return null;
		}
	}
}