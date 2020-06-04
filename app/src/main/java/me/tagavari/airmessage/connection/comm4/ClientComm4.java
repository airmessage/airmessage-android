package me.tagavari.airmessage.connection.comm4;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.nio.ByteBuffer;
import java.util.List;

import me.tagavari.airmessage.connection.CommunicationsManager;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.connection.MassRetrievalParams;
import me.tagavari.airmessage.connection.ProxyNoOp;
import me.tagavari.airmessage.connection.request.ConversationInfoRequest;

public class ClientComm4 extends CommunicationsManager<PacketStructIn, PacketStructOut> {
	//Creating the connection values
	private ProtocolManager protocolManager = null;
	
	private static final long handshakeExpiryTime = 1000 * 10; //10 seconds
	final Handler handler = new Handler();
	
	//Creating the transmission values
	static final int nhtClose = -1;
	static final int nhtPing = -2;
	static final int nhtPong = -3;
	static final int nhtInformation = 0;

	final Runnable handshakeExpiryRunnable = () -> dataProxy.stop(ConnectionManager.connResultConnection);
	
	//Creating the state values
	private boolean connectionOpened = false;
	
	public ClientComm4(ConnectionManager connectionManager, int proxyType, Context context) {
		super(connectionManager, context);
		
		//Assigning the proxy
		if(proxyType == ConnectionManager.proxyTypeDirect) setProxy(new ProxyDirectTCP());
		else setProxy(new ProxyNoOp<>());
		
		//Hooking into the data proxy
		dataProxy.addDataListener(this);
	}
	
	@Override
	public void initiateClose() {
		if(connectionOpened) {
			//Notifying the server before disconnecting
			queuePacket(new PacketStructOut(nhtClose, new byte[0], () -> dataProxy.stop(ConnectionManager.connResultConnection)));
		} else {
			//The server isn't connected, disconnect right away
			dataProxy.stop(ConnectionManager.connResultConnection);
		}
	}
	
	@Override
	public void onOpen() {
		//Calling the super method
		super.onOpen();
		
		//Starting the handshake expiry timer
		handler.postDelayed(handshakeExpiryRunnable, handshakeExpiryTime);
		
		//Setting the state
		connectionOpened = true;
	}
	
	@Override
	public void onClose(int reason) {
		//Calling the super method
		super.onClose(reason);
		
		//Cancelling the handshake expiry timer
		handler.removeCallbacks(handshakeExpiryRunnable);
	}
	
	@Override
	public void onMessage(PacketStructIn message) {
		//Calling the super method
		super.onMessage(message);
		
		//Processing the data
		if(protocolManager == null) processFloatingData(message.getHeader(), message.getData());
		else protocolManager.processData(message.getHeader(), message.getData());
	}
	
	@Override
	public boolean isConnectedFallback() {
		if(dataProxy instanceof ProxyDirectTCP) return ((ProxyDirectTCP) dataProxy).isUsingFallback();
		else return false;
	}
	
	@Override
	public boolean sendPing() {
		if(protocolManager == null) return false;
		else return protocolManager.sendPing();
	}
	
	@Override
	public ConnectionManager.Packager getPackager() {
		if(protocolManager == null) return null;
		return protocolManager.getPackager();
	}
	
	@Override
	public String getHashAlgorithm() {
		if(protocolManager == null) return null;
		return protocolManager.getHashAlgorithm();
	}
	
	@Override
	public boolean sendMessage(short requestID, String chatGUID, String message) {
		if(protocolManager == null) return false;
		return protocolManager.sendMessage(requestID, chatGUID, message);
	}
	
	@Override
	public boolean sendMessage(short requestID, String[] chatMembers, String message, String service) {
		if(protocolManager == null) return false;
		return protocolManager.sendMessage(requestID, chatMembers, message, service);
	}
	
	@Override
	public boolean addDownloadRequest(short requestID, String attachmentGUID, Runnable sentRunnable) {
		if(protocolManager == null) return false;
		return protocolManager.addDownloadRequest(requestID, attachmentGUID, sentRunnable);
	}
	
	@Override
	public boolean sendConversationInfoRequest(List<ConversationInfoRequest> list) {
		if(protocolManager == null) return false;
		return protocolManager.sendConversationInfoRequest(list);
	}
	
	@Override
	public boolean uploadFilePacket(short requestID, int requestIndex, String conversationGUID, byte[] data, String fileName, boolean isLast) {
		if(protocolManager == null) return false;
		return protocolManager.uploadFilePacket(requestID, requestIndex, conversationGUID, data, fileName, isLast);
	}
	
	@Override
	public boolean uploadFilePacket(short requestID, int requestIndex, String[] conversationMembers, byte[] data, String fileName, String service, boolean isLast) {
		if(protocolManager == null) return false;
		return protocolManager.uploadFilePacket(requestID, requestIndex, conversationMembers, data, fileName, service, isLast);
	}
	
	@Override
	public boolean requestRetrievalTime(long timeLower, long timeUpper) {
		if(protocolManager == null) return false;
		return protocolManager.requestRetrievalTime(timeLower, timeUpper);
	}
	
	@Override
	public boolean requestRetrievalAll(short requestID, MassRetrievalParams params) {
		if(protocolManager == null) return false;
		return protocolManager.requestRetrievalAll(requestID, params);
	}
	
	@Override
	public boolean requestChatCreation(short requestID, String[] members, String service) {
		if(protocolManager == null) return false;
		return protocolManager.requestChatCreation(requestID, members, service);
	}
	
	@Override
	public boolean sendPushToken(String token) {
		//No support
		return false;
	}
	
	@Override
	public int checkCommVerApplicability(int version) {
		return Integer.compare(version, 4);
	}
	
	public boolean isConnectionOpened() {
		return connectionOpened;
	}
	
	boolean queuePacket(PacketStructOut packet) {
		return dataProxy.send(packet);
	}
	
	/**
	 * Disconnects the connection manager from the server with a custom error code
	 *
	 * @param code The error code to disconnect with
	 */
	void disconnect(int code) {
		//Disconnecting the proxy
		dataProxy.stop(code);
	}
	
	/**
	 * Processes any data before a protocol manager is selected, usually to handle version processing
	 */
	private void processFloatingData(int messageType, byte[] data) {
		if(messageType == nhtInformation) {
			//Restarting the authentication timer
			handler.removeCallbacks(handshakeExpiryRunnable);
			handler.postDelayed(handshakeExpiryRunnable, handshakeExpiryTime);
			
			//Reading the communications version information
			ByteBuffer dataBuffer = ByteBuffer.wrap(data);
			int communicationsVersion = dataBuffer.getInt();
			int communicationsSubVersion = dataBuffer.getInt();
			
			//Checking if the client can't handle this communications version
			int verApplicability = checkCommVerApplicability(communicationsVersion);
			if(verApplicability != 0) {
				//Terminating the connection
				dataProxy.stop(verApplicability < 0 ? ConnectionManager.connResultServerOutdated : ConnectionManager.connResultClientOutdated);
				return;
			}
			
			//Communications subversions 1-5 are no longer supported
			if(communicationsSubVersion <= 5) {
				dataProxy.stop(ConnectionManager.connResultServerOutdated);
				return;
			}
			
			protocolManager = findProtocolManager(communicationsSubVersion);
			if(protocolManager == null) {
				dataProxy.stop(ConnectionManager.connResultClientOutdated);
				return;
			}
			
			//Updating the protocol version
			new Handler(Looper.getMainLooper()).post(() -> connectionManager.setActiveCommunicationsInfo(communicationsVersion, communicationsSubVersion));
			
			//Sending the handshake data
			protocolManager.sendAuthenticationRequest();
		}
	}
	
	private ProtocolManager findProtocolManager(int subVersion) {
		switch(subVersion) {
			default:
				return null;
			case 6:
				return new ClientProtocol6(context, connectionManager, this);
		}
	}
	
	protected class ConnectionThread extends Thread {
		//Creating the reference connection values
		private final String hostname;
		private final int port;
		private final String hostnameFallback;
		private final int portFallback;
		
		private Socket socket;
		private DataInputStream inputStream;
		private DataOutputStream outputStream;
		private ConnectionThread.WriterThread writerThread = null;
		
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
				writerThread = new ConnectionThread.WriterThread();
				writerThread.start();
			} catch(IOException exception) {
				//Printing the stack trace
				exception.printStackTrace();
				
				//Updating the state
				closeConnection(ConnectionManager.intentResultCodeConnection, !(exception instanceof ConnectException) && !(exception instanceof SocketTimeoutException));
				
				//Returning
				return;
			}
			
			//Starting the handshake timer
			handler.postDelayed(handshakeExpiryRunnable, handshakeExpiryTime);
			
			//Reading from the input stream
			readLoop:
			while(!isInterrupted()) {
				try {
					//Reading the header data
					int messageType = inputStream.readInt();
					int contentLen = inputStream.readInt();
					
					//Checking if the content length is greater than the maximum packet allocation
					if(contentLen > ConnectionManager.maxPacketAllocation) {
						//Logging the error
						Logger.getGlobal().log(Level.WARNING, "Rejecting large packet (type: " + messageType + " - size: " + contentLen + ")");
						
						//Closing the connection
						closeConnection(ConnectionManager.intentResultCodeConnection, !connectionEstablished);
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
								closeConnection(ConnectionManager.intentResultCodeConnection, !connectionEstablished);
								break readLoop;
							}
							
							offset += readCount;
							bytesRemaining -= readCount;
						}
					}
					
					//Processing the data
					if(protocolManager == null) processFloatingData(messageType, content);
					else protocolManager.processData(messageType, content);
				} catch(SSLHandshakeException exception) {
					//Closing the connection
					exception.printStackTrace();
					closeConnection(ConnectionManager.intentResultCodeConnection, true);
					
					//Breaking
					break;
				} catch(IOException | RuntimeException exception) {
					//Closing the connection
					exception.printStackTrace();
					closeConnection(ConnectionManager.intentResultCodeConnection, !connectionEstablished);
					
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
		
		boolean queuePacket(ConnectionManager.PacketStruct packet) {
			if(writerThread == null) return false;
			writerThread.uploadQueue.add(packet);
			return true;
		}
		
		void initiateClose(int resultCode, boolean forwardRequest) {
			//Updating the state
			updateStateDisconnected(resultCode, forwardRequest);
			
			//Ending the writer thread after notifying the connected server
			if(writerThread != null) {
				queuePacket(new ConnectionManager.PacketStruct(nhtClose, new byte[0], () -> {
					writerThread.interrupt();
				}));
			}
			
			//Interrupting this thread immediately
			interrupt();
		}
		
		void closeConnection(int reason, boolean forwardRequest) {
			//Finishing the threads
			if(writerThread != null) writerThread.interrupt();
			interrupt();
			
			//Updating the state
			updateStateDisconnected(reason, forwardRequest && !isInterrupted());
		}
		
		synchronized boolean sendDataSync(int messageType, byte[] data, boolean flush) {
			if(outputStream == null) return false;
			
			try {
				//Writing the message
				outputStream.writeInt(messageType);
				outputStream.writeInt(data.length);
				outputStream.write(data);
				if(flush) outputStream.flush();
				
				//Returning true
				return true;
			} catch(IOException exception) {
				//Logging the exception
				exception.printStackTrace();
				
				//Closing the connection
				if(socket.isConnected()) {
					closeConnection(ConnectionManager.intentResultCodeConnection, false);
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
			final BlockingQueue<ConnectionManager.PacketStruct> uploadQueue = new LinkedBlockingQueue<>();
			
			@Override
			public void run() {
				ConnectionManager.PacketStruct packet;
				
				try {
					while(!isInterrupted()) {
						try {
							packet = uploadQueue.take();
							
							try {
								//outputStream.write(ByteBuffer.allocate(Integer.SIZE / 8 * 2).putInt(packet.type).putInt(packet.content.length).array());
								//outputStream.write(packet.content);
								sendDataSync(packet.type, packet.content, false);
							} finally {
								if(packet.sentRunnable != null) packet.sentRunnable.run();
							}
							
							while((packet = uploadQueue.poll()) != null) {
								try {
									//outputStream.write(ByteBuffer.allocate(Integer.SIZE / 8 * 2).putInt(packet.type).putInt(packet.content.length).array());
									//outputStream.write(packet.content);
									sendDataSync(packet.type, packet.content, false);
								} finally {
									if(packet.sentRunnable != null) packet.sentRunnable.run();
								}
							}
							
							outputStream.flush();
						} catch(IOException exception) {
							exception.printStackTrace();
							
							if(socket.isConnected()) {
								closeConnection(ConnectionManager.intentResultCodeConnection, false);
							} else {
								Crashlytics.logException(exception);
							}
						}
					}
					//closeConnection(intentResultCodeConnection, false);
				} catch(InterruptedException exception) {
					//exception.printStackTrace();
					//closeConnection(intentResultCodeConnection, false); //Can only be interrupted from closeConnection, so this is pointless
					
					return;
				}
			}
			
			/* private void sendPacket(PacketStruct packet) throws IOException {
				outputStream.write(ByteBuffer.allocate(Integer.SIZE / 8 * 2).putInt(packet.type).putInt(packet.content.length).array());
				outputStream.write(packet.content);
				outputStream.flush();
			} */
		}
	}
	
	@Override
	public boolean checkSupportsFeature(String feature) {
		//Forwarding the request to the protocol manager
		if(protocolManager == null) return false;
		return protocolManager.checkSupportsFeature(feature);
	}
	
	@Override
	public boolean requiresPersistence() {
		return true;
	}
}