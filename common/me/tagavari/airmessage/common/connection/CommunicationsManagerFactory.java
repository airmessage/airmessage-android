package me.tagavari.airmessage.common.connection;

import me.tagavari.airmessage.common.connection.listener.CommunicationsManagerListener;
import me.tagavari.airmessage.common.enums.ProxyType;

public interface CommunicationsManagerFactory {
	CommunicationsManager<?> create(CommunicationsManagerListener listener, @ProxyType int proxyType);
}