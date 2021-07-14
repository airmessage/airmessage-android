package me.tagavari.airmessage.connection.comm4;

import android.util.Log;
import me.tagavari.airmessage.enums.ConnectionErrorCode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * A thread that handles connecting to and reading from the server
 */
public class ProxyDirectTCPReader extends Thread {
	private static final String TAG = "ProxyDirectTCPReader-4";
	private static final int maxPacketAllocation = 50 * 1024 * 1024; //50 MB
	
	//Creating the constants
	private static final int socketTimeout = 1000 * 10; //10 seconds
	
	//Creating the parameter values
	private final Listener listener;
	
	private final String hostname;
	private final int port;
	private final String hostnameFallback;
	private final int portFallback;
	
	private boolean usingFallback;
	
	ProxyDirectTCPReader(ProxyDirectTCPReader.Listener listener, String hostname, int port, String hostnameFallback, int portFallback) {
		this.listener = listener;
		this.hostname = hostname;
		this.port = port;
		this.hostnameFallback = hostnameFallback;
		this.portFallback = portFallback;
	}
	
	@Override
	public void run() {
		Socket socket;
		DataInputStream inputStream;
		DataOutputStream outputStream;
		try {
			//Returning if the thread is interrupted
			if(isInterrupted()) return;
			
			if(hostnameFallback != null) {
				try {
					//Connecting to the primary server
					socket = new Socket();
					socket.connect(new InetSocketAddress(hostname, port), socketTimeout);
					usingFallback = false;
				} catch(IOException exception) {
					//Printing the stack trace
					exception.printStackTrace();
					
					//Connecting to the fallback server
					socket = new Socket();
					socket.connect(new InetSocketAddress(hostnameFallback, portFallback), socketTimeout);
					usingFallback = true;
				}
			} else {
				//Connecting to the primary server
				socket = new Socket();
				socket.connect(new InetSocketAddress(hostname, port), socketTimeout);
				usingFallback = false;
			}
			
			//Returning if the thread is interrupted
			if(isInterrupted()) {
				try {
					socket.close();
				} catch(IOException exception) {
					exception.printStackTrace();
				}
				
				return;
			}
			
			//Getting the streams
			inputStream = new DataInputStream(socket.getInputStream());
			outputStream = new DataOutputStream(socket.getOutputStream());
		} catch(IOException exception) {
			//Printing the stack trace
			exception.printStackTrace();
			
			//Updating the state
			listener.onClose(ConnectionErrorCode.connection);
			
			//Returning
			return;
		}
		
		//Notifying the listener
		listener.onOpen(outputStream);
		
		//Reading from the input stream
		readLoop:
		while(!isInterrupted()) {
			try {
				//Reading the header data
				int messageType = inputStream.readInt();
				int contentLen = inputStream.readInt();
				
				//Checking if the content length is greater than the maximum packet allocation
				if(contentLen > maxPacketAllocation) {
					//Logging the error
					Log.w(TAG, "Rejecting large packet (size: " + contentLen + ")");
					
					//Closing the connection
					listener.onClose(ConnectionErrorCode.connection);
					break;
				}
				
				//Reading the content
				byte[] content = new byte[contentLen];
				if(contentLen > 0) {
					int bytesRemaining = contentLen;
					int offset = 0;
					int readCount;
					while(bytesRemaining > 0) {
						readCount = inputStream.read(content, offset, bytesRemaining);
						if(readCount == -1) { //No data read, stream is closed
							listener.onClose(ConnectionErrorCode.connection);
							break readLoop;
						}
						
						offset += readCount;
						bytesRemaining -= readCount;
					}
				}
				
				//Processing the data
				listener.onMessage(content, messageType);
			} catch(IOException | RuntimeException exception) {
				//Closing the connection
				exception.printStackTrace();
				listener.onClose(ConnectionErrorCode.connection);
				
				//Breaking
				break;
			}
		}
		
		//Closing the socket
		try {
			socket.close();
		} catch(IOException exception) {
			exception.printStackTrace();
		}
	}
	
	boolean isUsingFallback() {
		return usingFallback;
	}
	
	/**
	 * A listener for updates
	 * Please note that these functions will be called from a worker thread
	 */
	interface Listener {
		void onOpen(DataOutputStream outputStream);
		void onClose(@ConnectionErrorCode int reason);
		void onMessage(byte[] data, int type);
	}
}