package me.tagavari.airmessage.connection.comm5;

import me.tagavari.airmessage.connection.DataProxy;

public abstract class DataProxy5 extends DataProxy<PacketStructIn, PacketStructOut> {
	/**
	 * Called to check whether this data proxy requires authenticating connecting clients
	 * @return TRUE if this proxy requires authentication
	 */
	public abstract boolean requiresAuthentication();
}