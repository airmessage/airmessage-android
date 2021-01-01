package me.tagavari.airmessage.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.StringJoiner;

public class DirectConnectionParams {
	private final String address;
	@Nullable private final String fallbackAddress;
	private final String password;
	
	public DirectConnectionParams(String address, @Nullable String fallbackAddress, String password) {
		this.address = address;
		this.fallbackAddress = fallbackAddress;
		this.password = password;
	}
	
	public String getAddress() {
		return address;
	}
	
	@Nullable
	public String getFallbackAddress() {
		return fallbackAddress;
	}
	
	public String getPassword() {
		return password;
	}
	
	@NonNull
	@Override
	public String toString() {
		return new StringJoiner(" | ", DirectConnectionParams.class.getSimpleName() + " [", "]")
				.add("address=" + address)
				.add("fallbackAddress=" + fallbackAddress)
				.add("password=" + password)
				.toString();
	}
}