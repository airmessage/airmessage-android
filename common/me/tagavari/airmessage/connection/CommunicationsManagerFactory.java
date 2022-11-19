package me.tagavari.airmessage.connection;

import me.tagavari.airmessage.connection.listener.CommunicationsManagerListener;
import me.tagavari.airmessage.enums.ProxyType;

public interface CommunicationsManagerFactory {
	CommunicationsManager<?> create(CommunicationsManagerListener listener, @ProxyType int proxyType);
}