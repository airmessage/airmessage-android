package me.tagavari.airmessage.connection.comm5;

import me.tagavari.airmessage.connection.DataProxy;

class ProxyConnect extends DataProxy<PacketStructIn, PacketStructOut> {
	@Override
	public void start() {
	
	}
	
	@Override
	public void stop(int code) {
	
	}
	
	@Override
	public boolean send(PacketStructOut packet) {
		return false;
	}
}