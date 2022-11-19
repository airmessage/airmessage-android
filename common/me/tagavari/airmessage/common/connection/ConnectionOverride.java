package me.tagavari.airmessage.common.connection;

import androidx.annotation.Nullable;
import me.tagavari.airmessage.common.enums.ProxyType;
import me.tagavari.airmessage.common.util.ConnectionParams;

public class ConnectionOverride<T extends ConnectionParams> {
	@ProxyType private final int proxyType;
	@Nullable private final T value;
	
	public ConnectionOverride(@ProxyType int proxyType, @Nullable T value) {
		this.proxyType = proxyType;
		this.value = value;
	}
	
	@ProxyType
	public int getProxyType() {
		return proxyType;
	}
	
	@Nullable
	public T getValue() {
		return value;
	}
}