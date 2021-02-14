package me.tagavari.airmessage.connection.comm5;

import androidx.core.util.Consumer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import me.tagavari.airmessage.connection.encryption.EncryptionManager;
import me.tagavari.airmessage.enums.ConnectionErrorCode;

class ProxyDirectTCPWriter extends Thread {
	//Creating the queue
	final BlockingQueue<EncryptedPacket> uploadQueue = new LinkedBlockingQueue<>();
	
	//Creating the parameter values
	private final Consumer<Integer> errorListener;
	private final EncryptionManager encryptionManager;
	private final DataOutputStream outputStream;
	
	public ProxyDirectTCPWriter(Consumer<Integer> errorListener, EncryptionManager encryptionManager, DataOutputStream outputStream) {
		this.errorListener = errorListener;
		this.encryptionManager = encryptionManager;
		this.outputStream = outputStream;
	}
	
	@Override
	public void run() {
		EncryptedPacket packet;
		
		try {
			while(!isInterrupted()) {
				try {
					packet = uploadQueue.take();
					sendDataSync(packet.getData(), packet.getEncrypt());
					
					while((packet = uploadQueue.poll()) != null) {
						sendDataSync(packet.getData(), packet.getEncrypt());
					}
					
					outputStream.flush();
				} catch(IOException | GeneralSecurityException exception) {
					exception.printStackTrace();
					
					errorListener.accept(ConnectionErrorCode.connection);
				}
			}
		} catch(InterruptedException exception) {
			exception.printStackTrace();
		}
	}
	
	void queuePacket(EncryptedPacket packet) {
		uploadQueue.add(packet);
	}
	
	/**
	 * Encrypts data if necessary, and then submits it to the stream
	 * @param data The data to send
	 * @param isEncrypted Whether to encrypt this data
	 */
	private synchronized void sendDataSync(byte[] data, boolean isEncrypted) throws IOException, GeneralSecurityException {
		if(isEncrypted) data = encryptionManager.encrypt(data);
		
		outputStream.writeInt(data.length);
		outputStream.writeBoolean(isEncrypted);
		outputStream.write(data);
	}
}