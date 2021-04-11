package me.tagavari.airmessage.connection.comm4;

import androidx.core.util.Consumer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import me.tagavari.airmessage.enums.ConnectionErrorCode;

public class ProxyDirectTCPWriter extends Thread {
	//Creating the queue
	final BlockingQueue<HeaderPacket> uploadQueue = new LinkedBlockingQueue<>();
	
	//Creating the parameter values
	private final Consumer<Integer> errorListener;
	private final DataOutputStream outputStream;
	
	public ProxyDirectTCPWriter(Consumer<Integer> errorListener, DataOutputStream outputStream) {
		this.errorListener = errorListener;
		this.outputStream = outputStream;
	}
	
	@Override
	public void run() {
		HeaderPacket packet;
		
		try {
			while(!isInterrupted()) {
				try {
					packet = uploadQueue.take();
					
					sendDataSync(packet.getData(), packet.getType());
					
					while((packet = uploadQueue.poll()) != null) {
						sendDataSync(packet.getData(), packet.getType());
					}
					
					outputStream.flush();
				} catch(IOException exception) {
					exception.printStackTrace();
					
					errorListener.accept(ConnectionErrorCode.connection);
				}
			}
		} catch(InterruptedException exception) {
			exception.printStackTrace();
		}
	}
	
	void queuePacket(HeaderPacket packet) {
		uploadQueue.add(packet);
	}
	
	/**
	 * Sends data to the stream
	 * @param data The data to send
	 * @param type THe header type
	 */
	private synchronized void sendDataSync(byte[] data, int type) throws IOException {
		outputStream.writeInt(type);
		outputStream.writeInt(data.length);
		outputStream.write(data);
	}
}