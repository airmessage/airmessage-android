package me.tagavari.airmessage.connection;

public interface DataProxyListener<D> {
	void onOpen();
	void onClose(int reason);
	void onMessage(D data);
}