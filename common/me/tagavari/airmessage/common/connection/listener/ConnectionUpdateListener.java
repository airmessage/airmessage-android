package me.tagavari.airmessage.common.connection.listener;

import me.tagavari.airmessage.common.enums.ConnectionErrorCode;

public interface ConnectionUpdateListener {
	void onConnecting();
	void onOpen();
	void onClose(@ConnectionErrorCode int code);
}