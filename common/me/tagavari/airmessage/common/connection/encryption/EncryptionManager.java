package me.tagavari.airmessage.common.connection.encryption;

import java.security.GeneralSecurityException;

public interface EncryptionManager {
	/**
	 * Encrypts the provided data to be sent over the network
	 * @param inData The data to encrypt
	 * @return The encrypted data
	 * @throws GeneralSecurityException If there was an error during the encryption process
	 */
	byte[] encrypt(byte[] inData) throws GeneralSecurityException;
	
	/**
	 * Decrypts the provided data received from the network
	 * @param inData The data to decrypt
	 * @return The decrypted data
	 * @throws GeneralSecurityException If there was an error during the encryption process
	 */
	byte[] decrypt(byte[] inData) throws GeneralSecurityException;
}