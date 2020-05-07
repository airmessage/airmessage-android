package me.tagavari.airmessage.connection;

public final class ProxyNoOp<D, P> extends DataProxy<D, P> {
	@Override
	public void start() {
		onClose(ConnectionManager.connResultBadRequest);
	}
	
	@Override
	public void stop(int code) {
	
	}
	
	@Override
	public boolean send(P packet) {
		return false;
	}
}