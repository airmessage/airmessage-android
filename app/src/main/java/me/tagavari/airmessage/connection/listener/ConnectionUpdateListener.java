package me.tagavari.airmessage.connection.listener;

import me.tagavari.airmessage.enums.ConnectionErrorCode;

public interface ConnectionUpdateListener {
	void onConnecting();
	void onOpen();
	void onClose(@ConnectionErrorCode int code);
}