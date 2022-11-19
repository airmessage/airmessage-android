package me.tagavari.airmessage.common.connection;

public class CookieBuilder {
	private final StringBuilder stringBuilder = new StringBuilder();
	private boolean cookieAdded = false;
	
	public CookieBuilder with(String key, boolean value) {
		return with(key, Boolean.toString(value));
	}
	
	public CookieBuilder with(String key, short value) {
		return with(key, Short.toString(value));
	}
	
	public CookieBuilder with(String key, float value) {
		return with(key, Float.toString(value));
	}
	
	public CookieBuilder with(String key, int value) {
		return with(key, Integer.toString(value));
	}
	
	public CookieBuilder with(String key, double value) {
		return with(key, Double.toString(value));
	}
	
	public CookieBuilder with(String key, long value) {
		return with(key, Long.toString(value));
	}
	
	public CookieBuilder with(String key, String value) {
		if(cookieAdded) stringBuilder.append("; ");
		else cookieAdded = true;
		
		stringBuilder.append(key).append('=').append(value);
		
		return this;
	}
	
	@Override
	public String toString() {
		return stringBuilder.toString();
	}
}