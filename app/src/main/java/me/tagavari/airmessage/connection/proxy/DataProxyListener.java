package me.tagavari.airmessage.connection.proxy;

public interface DataProxyListener {
	void onOpen();
	void onClose(int reason);
	void onMessage(int type, byte[] content);
}