package me.tagavari.airmessage.connection;

import me.tagavari.airmessage.enums.ConnectionErrorCode;

/**
 * Listener for events of a {@link DataProxy}
 */
public interface DataProxyListener<Packet> {
	void handleOpen();
	void handleClose(@ConnectionErrorCode int reason);
	void handleMessage(Packet packet);
}