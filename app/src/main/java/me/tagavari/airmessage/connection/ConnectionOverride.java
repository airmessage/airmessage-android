package me.tagavari.airmessage.connection;

import androidx.annotation.Nullable;

import me.tagavari.airmessage.enums.ProxyType;

public class ConnectionOverride<T> {
	@ProxyType private final int proxyType;
	@Nullable private final T value;
	
	public ConnectionOverride(int proxyType, @Nullable T value) {
		this.proxyType = proxyType;
		this.value = value;
	}
	
	public int getProxyType() {
		return proxyType;
	}
	
	@Nullable
	public T getValue() {
		return value;
	}
}