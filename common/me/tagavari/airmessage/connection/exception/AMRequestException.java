package me.tagavari.airmessage.connection.exception;

/**
 * An exception thrown when a network-related AirMessage error occurs
 */
public class AMRequestException extends Exception {
	private final int errorCode;
	private final String errorDetails;
	
	public AMRequestException(int errorCode) {
		super(buildMessage(errorCode, null));
		this.errorCode = errorCode;
		this.errorDetails = null;
	}
	
	public AMRequestException(int errorCode, String errorDetails) {
		super(buildMessage(errorCode, errorDetails));
		this.errorCode = errorCode;
		this.errorDetails = errorDetails;
	}
	
	public AMRequestException(int errorCode, Throwable cause) {
		super(buildMessage(errorCode, null), cause);
		this.errorCode = errorCode;
		this.errorDetails = null;
	}
	
	public int getErrorCode() {
		return errorCode;
	}
	
	public String getErrorDetails() {
		return errorDetails;
	}
	
	private static String buildMessage(int errorCode, String errorDetails) {
		if(errorDetails != null) {
			return "AirMessage request error (error code " + errorCode + "): " + errorDetails;
		} else {
			return "AirMessage request error (error code " + errorCode + ")";
		}
	}
}