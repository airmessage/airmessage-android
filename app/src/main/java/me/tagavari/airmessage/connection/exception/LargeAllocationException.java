package me.tagavari.airmessage.connection.exception;

//When the system tries to allocate more memory than a hardcoded limit
public class LargeAllocationException extends RuntimeException {
	public LargeAllocationException(long size, long limit) {
		super("Tried to allocate " + size + ", but limit is " + limit);
	}
	
	public LargeAllocationException(long size, long limit, Throwable cause) {
		super("Tried to allocate " + size + ", but limit is " + limit, cause);
	}
}