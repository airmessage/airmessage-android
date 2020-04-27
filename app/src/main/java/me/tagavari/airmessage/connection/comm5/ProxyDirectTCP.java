package me.tagavari.airmessage.connection.comm5;

import android.os.Handler;
import android.os.Looper;

import com.crashlytics.android.Crashlytics;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLHandshakeException;

import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.connection.DataProxy;
import me.tagavari.airmessage.util.Constants;

class ProxyDirectTCP extends DataProxy<PacketStructIn, PacketStructOut> {
	//Creating the state values
	private boolean isRunning = false;
	private ConnectionThread connectionThread;
	
	@Override
	public void start() {
		//Returning if this proxy is already running
		if(isRunning) return;
		
		//Parsing the hostname
		String cleanHostname = ConnectionManager.hostname;
		int port = Constants.defaultPort;
		if(ConnectionManager.regExValidPort.matcher(cleanHostname).find()) {
			String[] targetDetails = ConnectionManager.hostname.split(":");
			cleanHostname = targetDetails[0];
			port = Integer.parseInt(targetDetails[1]);
		}
		
		String cleanHostnameFallback = null;
		int portFallback = -1;
		
		if(ConnectionManager.hostnameFallback != null) {
			cleanHostnameFallback = ConnectionManager.hostnameFallback;
			portFallback = Constants.defaultPort;
			
			if(ConnectionManager.regExValidPort.matcher(cleanHostnameFallback).find()) {
				String[] targetDetails = ConnectionManager.hostnameFallback.split(":");
				cleanHostnameFallback = targetDetails[0];
				portFallback = Integer.parseInt(targetDetails[1]);
			}
		}
		
		//Starting the connection thread
		connectionThread = new ConnectionThread(cleanHostname, port, cleanHostnameFallback, portFallback);
		connectionThread.start();
		
		//Updating the running state
		isRunning = true;
	}
	
	@Override
	public void stop(int code) {
		//Running on the main thread
		new Handler(Looper.getMainLooper()).post(() -> {
			//Returning if this proxy is not running
			if(!isRunning) return;
			
			//Closing the connection thread
			connectionThread.interrupt();
			connectionThread = null;
			
			//Calling the listener
			onClose(code);
			
			//Updating the running state
			isRunning = false;
		});
	}
	
	@Override
	public boolean send(PacketStructOut packet) {
		//Queuing the packet
		if(connectionThread == null) return false;
		return connectionThread.queuePacket(packet);
	}
	
	public boolean isUsingFallback() {
		return connectionThread != null && connectionThread.isUsingFallback();
	}
	
	protected class ConnectionThread extends Thread {
		//Creating the constants
		private static final int socketTimeout = 1000 * 10; //10 seconds
		
		//Creating the reference connection values
		private final String hostname;
		private final int port;
		private final String hostnameFallback;
		private final int portFallback;
		
		private Socket socket;
		private DataInputStream inputStream;
		private DataOutputStream outputStream;
		private WriterThread writerThread = null;
		
		private boolean usingFallback;
		
		private ConnectionThread(String hostname, int port, String hostnameFallback, int portFallback) {
			this.hostname = hostname;
			this.port = port;
			this.hostnameFallback = hostnameFallback;
			this.portFallback = portFallback;
		}
		
		@Override
		public void run() {
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
				
				//Starting the writer thread
				writerThread = new WriterThread();
				writerThread.start();
			} catch(IOException exception) {
				//Printing the stack trace
				exception.printStackTrace();
				
				//Updating the state
				closeConnection(ConnectionManager.intentResultCodeConnection);
				
				//Returning
				return;
			}
			
			//Calling the connection listener
			onOpen();
			
			//Reading from the input stream
			readLoop:
			while(!isInterrupted()) {
				try {
					//Reading the header data
					int contentLen = inputStream.readInt();
					boolean isEncrypted = inputStream.readBoolean();
					
					//Checking if the content length is greater than the maximum packet allocation
					if(contentLen > ConnectionManager.maxPacketAllocation) {
						//Logging the error
						Logger.getGlobal().log(Level.WARNING, "Rejecting large packet (size: " + contentLen + ")");
						
						//Closing the connection
						closeConnection(ConnectionManager.intentResultCodeConnection);
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
								closeConnection(ConnectionManager.intentResultCodeConnection);
								break readLoop;
							}
							
							offset += readCount;
							bytesRemaining -= readCount;
						}
					}
					
					//Processing the data
					onMessage(new PacketStructIn(content, isEncrypted));
				} catch(IOException | RuntimeException exception) {
					//Closing the connection
					exception.printStackTrace();
					closeConnection(ConnectionManager.intentResultCodeConnection);
					
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
		
		boolean queuePacket(PacketStructOut packet) {
			if(writerThread == null) return false;
			writerThread.uploadQueue.add(packet);
			return true;
		}
		
		@Override
		public void interrupt() {
			//Interrupting the writer thread before interrupting this thread
			if(writerThread != null) writerThread.interrupt();
			super.interrupt();
		}
		
		void closeConnection(int reason) {
			//Interrupting this thread
			interrupt();
			
			//Updating the state
			new Handler(Looper.getMainLooper()).post(() -> {
				if(isRunning) {
					onClose(reason);
					isRunning = false;
				}
			});
		}
		
		synchronized boolean sendDataSync(byte[] data, boolean isEncrypted, boolean flush) {
			try {
				//Writing the message
				outputStream.writeInt(data.length);
				outputStream.writeBoolean(isEncrypted);
				outputStream.write(data);
				if(flush) outputStream.flush();
				
				//Returning true
				return true;
			} catch(IOException exception) {
				//Logging the exception
				exception.printStackTrace();
				
				//Closing the connection
				if(socket.isConnected()) {
					closeConnection(ConnectionManager.intentResultCodeConnection);
				} else {
					Crashlytics.logException(exception);
				}
				
				//Returning false
				return false;
			}
		}
		
		boolean isUsingFallback() {
			return usingFallback;
		}
		
		class WriterThread extends Thread {
			//Creating the queue
			final BlockingQueue<PacketStructOut> uploadQueue = new LinkedBlockingQueue<>();
			
			@Override
			public void run() {
				PacketStructOut packet;
				
				try {
					while(!isInterrupted()) {
						try {
							packet = uploadQueue.take();
							
							try {
								sendDataSync(packet.getData(), packet.getEncrypt(), false);
							} finally {
								if(packet.getSentRunnable() != null) packet.getSentRunnable().run();
							}
							
							while((packet = uploadQueue.poll()) != null) {
								try {
									sendDataSync(packet.getData(), packet.getEncrypt(), false);
								} finally {
									if(packet.getSentRunnable() != null) packet.getSentRunnable().run();
								}
							}
							
							outputStream.flush();
						} catch(IOException exception) {
							exception.printStackTrace();
							
							if(socket.isConnected()) {
								closeConnection(ConnectionManager.intentResultCodeConnection);
							} else {
								Crashlytics.logException(exception);
							}
						}
					}
				} catch(InterruptedException exception) {
					//exception.printStackTrace();
					//closeConnection(intentResultCodeConnection, false); //Can only be interrupted from closeConnection, so this is pointless
					
					return;
				}
			}
		}
	}
}