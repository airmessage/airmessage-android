package me.tagavari.airmessage.connection;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a method of transmitting data over the internet
 * Users of this class should not care how this data is transmitted
 * All methods should be safe to call from the main thread
 *
 * Generics represent:
 * D - incoming data packet structure
 * P - outgoing data packet structure
 */
public abstract class DataProxy<D, P> {
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
	public abstract boolean send(P packet);
	
	private final Set<DataProxyListener<D>> listeners = new HashSet<>();
	public void addDataListener(DataProxyListener<D> listener) {
		listeners.add(listener);
	}
	
	protected void onOpen() {
		for(DataProxyListener<D> listener : listeners) listener.onOpen();
	}
	
	protected void onClose(int reason) {
		for(DataProxyListener<D> listener : listeners) listener.onClose(reason);
	}
	
	protected void onMessage(D data) {
		for(DataProxyListener<D> listener : listeners) listener.onMessage(data);
	}
}