package me.tagavari.airmessage.connection.comm5;

import java.security.GeneralSecurityException;

final class PacketStructIn {
	private byte[] data;
	private final boolean isEncrypted;
	
	PacketStructIn(byte[] data, boolean isEncrypted) {
		this.data = data;
		this.isEncrypted = isEncrypted;
	}
	
	byte[] getData() {
		return data;
	}
	
	boolean isEncrypted() {
		return isEncrypted;
	}
	
	void decrypt(EncryptionManager encryptionManager) throws GeneralSecurityException {
		data = encryptionManager.decrypt(data);
	}
}