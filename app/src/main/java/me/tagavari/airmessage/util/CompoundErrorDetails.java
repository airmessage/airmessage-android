package me.tagavari.airmessage.util;

import androidx.annotation.Nullable;

import me.tagavari.airmessage.connection.exception.AMRequestException;
import me.tagavari.airmessage.enums.ChatCreateErrorCode;
import me.tagavari.airmessage.enums.MessageSendErrorCode;

/**
 * Represents error information that has an error code and an optional error detail string
 */
public abstract class CompoundErrorDetails {
	final int error;
	final String detail;
	
	/**
	 * Represents an error for a message
	 * @param error The error code
	 * @param detail The error detail string, if available
	 */
	public CompoundErrorDetails(int error, @Nullable String detail) {
		this.error = error;
		this.detail = detail;
	}
	
	public int getError() {
		return error;
	}
	
	@Nullable
	public String getDetail() {
		return detail;
	}
	
	public AMRequestException toException() {
		return new AMRequestException(error, detail);
	}
	
	public static class MessageSend extends CompoundErrorDetails {
		public MessageSend(@MessageSendErrorCode int error, @Nullable String detail) {
			super(error, detail);
		}
	}
	
	public static class ChatCreate extends CompoundErrorDetails {
		public ChatCreate(@ChatCreateErrorCode int error, @Nullable String detail) {
			super(error, detail);
		}
	}
}