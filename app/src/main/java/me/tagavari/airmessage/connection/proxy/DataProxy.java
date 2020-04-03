package me.tagavari.airmessage.connection.proxy;

import java.util.HashSet;
import java.util.Set;

import me.tagavari.airmessage.connection.ConnectionManager;

/**
 * Represents a method of transmitting data over the internet
 * Users of this class should not care how this data is transmitted
 * All methods should be safe to call from the main thread
 */
public abstract class DataProxy {
	/**
	 * Start this proxy's connection to the server
	 */
	public abstract void start();
	
	/**
	 * Cancel this proxy's connection to the server
	 * @param code The error code if this cancellation
	 */
	public abstract void stop(int code);
	
	/**
	 * Send a packet to the server
	 * @param packet The packet to send
	 * @return TRUE if the packet was successfully queued
	 */
	public abstract boolean send(ConnectionManager.PacketStruct packet);
	
	private final Set<DataProxyListener> listeners = new HashSet<>();
	public void addDataListener(DataProxyListener listener) {
		listeners.add(listener);
	}
	
	void onOpen() {
		for(DataProxyListener listener : listeners) listener.onOpen();
	}
	
	void onClose(int reason) {
		for(DataProxyListener listener : listeners) listener.onClose(reason);
	}
	
	void onMessage(int type, byte[] content) {
		for(DataProxyListener listener : listeners) listener.onMessage(type, content);
	}
}