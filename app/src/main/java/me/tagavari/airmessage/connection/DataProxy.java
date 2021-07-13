package me.tagavari.airmessage.connection;

import android.content.Context;

import androidx.annotation.Nullable;

import me.tagavari.airmessage.enums.ConnectionErrorCode;
import me.tagavari.airmessage.util.DirectConnectionParams;

/**
 * Represents a method of transmitting data over the internet
 * Users of this class should not care how this data is transmitted
 * All methods should be safe to call from the main thread
 *
 */
public abstract class DataProxy<Packet> {
	/**
	 * Start this proxy's connection to the server
	 */
	public abstract void start(Context context, @Nullable Object override);
	
	/**
	 * Stop this proxy's connection to the server
	 * @param code The error code if this disconnection
	 */
	public abstract void stop(@ConnectionErrorCode int code);
	
	/**
	 * Send a packet to the server
	 * @param packet The packet to send
	 * @return TRUE if the packet was successfully queued
	 */
	public abstract boolean send(Packet packet);
	
	/**
	 * Gets whether this proxy is connected using a fallback method
	 */
	public abstract boolean isUsingFallback();
	
	private DataProxyListener<Packet> listener = null;
	public void setListener(DataProxyListener<Packet> listener) {
		this.listener = listener;
	}
	
	protected void notifyOpen() {
		if(listener != null) listener.handleOpen();
	}
	
	protected void notifyClose(@ConnectionErrorCode int reason) {
		if(listener != null) listener.handleClose(reason);
	}
	
	protected void notifyMessage(Packet packet) {
		if(listener != null) listener.handleMessage(packet);
	}
}