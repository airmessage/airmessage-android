package me.tagavari.airmessage.connection.caladium;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.crashlytics.android.Crashlytics;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLHandshakeException;

import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.connection.CommunicationsManager;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.connection.MassRetrievalParams;
import me.tagavari.airmessage.connection.request.ConversationInfoRequest;
import me.tagavari.airmessage.service.ConnectionService;
import me.tagavari.airmessage.util.Constants;

public class ClientCommCaladium extends CommunicationsManager {
	//Creating the connection values
	private ProtocolManager protocolManager = null;
	ConnectionThread connectionThread = null;
	private int currentState = ConnectionManager.stateDisconnected;
	
	private static final int socketTimeout = 1000 * 10; //10 seconds
	private static final long handshakeExpiryTime = 1000 * 10; //10 seconds
	final Handler handler = new Handler();
	
	//Creating the transmission values
	static final int nhtClose = -1;
	static final int nhtPing = -2;
	static final int nhtPong = -3;
	static final int nhtInformation = 0;

	//Creating the other values
	private boolean connectionEstablished = false;

	final Runnable handshakeExpiryRunnable = () -> {
		if(connectionThread != null) connectionThread.closeConnection(ConnectionManager.intentResultCodeConnection, true);
	};
	
	public ClientCommCaladium(ConnectionManager connectionManager, Context context) {
		super(connectionManager, context);
	}
	
	@Override
	public boolean connect(byte launchID) {
		//Passing the request to the overload method
		return connect(launchID, false);
	}
	
	private boolean connect(byte launchID, boolean reconnectionRequest) {
		//Calling the super method
		super.connect(launchID);
		
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
		
		//Setting the state as connecting
		currentState = ConnectionManager.stateConnecting;
		
		//Starting the connection
		connectionThread = new ConnectionThread(cleanHostname, port, cleanHostnameFallback, portFallback, reconnectionRequest);
		connectionThread.start();
		
		//Returning true
		return true;
	}
	
	@Override
	public void disconnectReconnect() {
		//Keeps the connection drop reconnect flag enabled
		connectionThread.initiateClose(ConnectionManager.intentResultCodeConnection, false);
	}
	
	@Override
	public void disconnect() {
		super.disconnect();
		connectionThread.initiateClose(ConnectionManager.intentResultCodeConnection, false);
	}
	
	@Override
	public int getState() {
		return currentState;
	}
	
	@Override
	public boolean isConnectedFallback() {
		if(connectionThread == null) return false;
		return connectionThread.isUsingFallback();
	}
	
	public boolean queuePacket(ConnectionManager.PacketStruct packet) {
		return connectionThread != null && connectionThread.queuePacket(packet);
	}
	
	@Override
	public boolean sendPing() {
		super.sendPing();
		
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
	public int checkCommVerApplicability(int version) {
		return Integer.compare(version, 4);
	}
	
	private void updateStateDisconnected(int reason, boolean forwardRequest) {
		//Returning if the state is already disconnected
		if(currentState == ConnectionManager.stateDisconnected) return;
		
		//Cleaning up from the base connection manager
		super.handleDisconnection();
		
		//Setting the state
		currentState = ConnectionManager.stateDisconnected;
		
		//Setting the connection established flag
		connectionEstablished = false;
		
		//Stopping the timers
		handler.removeCallbacks(handshakeExpiryRunnable);
		
		//Invalidating the protocol manager
		protocolManager = null;
		
		//Cancelling an active mass retrieval
		new Handler(Looper.getMainLooper()).post(() -> connectionManager.cancelMassRetrieval(context));
		
		//Attempting to connect via a legacy method, and checking for a non-pass
		if(!forwardRequest || !forwardRequest(launchID, true)) {
			new Handler(Looper.getMainLooper()).post(() -> {
				//Checking if this is the most recent launch
				if(ConnectionManager.getCurrentLaunchID() == launchID) {
					//Setting the last connection result
					connectionManager.setLastConnectionResult(reason);
					
					//Checking if the service is expected to shut down
					if(connectionManager.getFlagShutdownRequested()) {
						//Notifying the connection listeners
						connectionManager.broadcastState(context, ConnectionManager.stateDisconnected, reason, launchID);
					} else {
						//Checking if a connection existed for reconnection and the preference is enabled
						if(connectionManager.getFlagDropReconnect()/* && PreferenceManager.getDefaultSharedPreferences(MainApplication.getInstance()).getBoolean(MainApplication.getInstance().getResources().getString(R.string.preference_server_dropreconnect_key), false)*/) {
							//Updating the notification
							//connectionService.postConnectedNotification(false, false);
							
							//Notifying the connection listeners
							connectionManager.broadcastState(context, ConnectionManager.stateConnecting, 0, launchID);
							
							//Reconnecting
							connect(connectionManager.getNextLaunchID(), true);
						} else {
							//Updating the notification
							//connectionService.postDisconnectedNotification(false);
							
							//Notifying the connection listeners
							connectionManager.broadcastState(context, ConnectionManager.stateDisconnected, reason, launchID);
						}
					}
					
					//Clearing the flags
					connectionManager.setFlagMarkEndTime(false);
					connectionManager.setFlagDropReconnect(false);
				} else {
					//Notifying the connection listeners
					connectionManager.broadcastState(context, ConnectionManager.stateDisconnected, reason, launchID);
				}
			});
		}
	}
	
	void updateStateConnected() {
		//Setting the connection established flag
		connectionEstablished = true;
		
		//Reading the shared preferences connectivity information
		SharedPreferences sharedPrefs = MainApplication.getInstance().getConnectivitySharedPrefs();
		String lastConnectionHostname = sharedPrefs.getString(MainApplication.sharedPreferencesConnectivityKeyLastConnectionHostname, null);
		long lastConnectionTime = sharedPrefs.getLong(MainApplication.sharedPreferencesConnectivityKeyLastConnectionTime, System.currentTimeMillis());
		
		//Updating the shared preferences connectivity information
		SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();
		if(!ConnectionManager.hostname.equals(lastConnectionHostname)) sharedPrefsEditor.putString(MainApplication.sharedPreferencesConnectivityKeyLastConnectionHostname, ConnectionManager.hostname);
		sharedPrefsEditor.putLong(MainApplication.sharedPreferencesConnectivityKeyLastConnectionTime, System.currentTimeMillis());
		sharedPrefsEditor.apply();
		
		//Running on the main thread
		new Handler(Looper.getMainLooper()).post(() -> {
			//Checking if this is the most recent launch
			if(ConnectionManager.getCurrentLaunchID() == launchID) {
				//Setting the last connection result
				connectionManager.setLastConnectionResult(ConnectionManager.intentResultCodeSuccess);
				
				//Setting the state
				currentState = ConnectionManager.stateConnected;
				
				//Retrieving the pending conversation info
				sendConversationInfoRequest(connectionManager.getPendingConversations());
				
				//Setting the flags
				connectionManager.setFlagMarkEndTime(true);
				connectionManager.setFlagDropReconnect(true);
				
				//Checking if the last connection is the same as the current one
				if(ConnectionManager.hostname.equals(lastConnectionHostname)) {
					//Fetching the messages since the last connection time
					connectionManager.retrieveMessagesSince(lastConnectionTime, System.currentTimeMillis());
				}
			}
		});
		
		//Notifying the connection listeners
		connectionManager.broadcastState(context, ConnectionManager.stateConnected, -1, launchID);
		
		//Updating the notification
		//if(connectionService.foregroundServiceRequested()) connectionService.postConnectedNotification(true, connectionThread.isUsingFallback());
		//else connectionService.clearNotification();
		
		//Scheduling the ping
		connectionManager.getServiceCallbacks().schedulePing();
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
			
			//Checking if the client can't handle the communications version
			int verApplicability = checkCommVerApplicability(communicationsVersion);
			if(verApplicability != 0) {
				//Terminating the connection
				connectionThread.closeConnection(verApplicability < 0 ? ConnectionManager.intentResultCodeServerOutdated : ConnectionManager.intentResultCodeClientOutdated, verApplicability < 0);
				return;
			}
			
			//Communications subversions 1-5 are no longer supported
			if(communicationsSubVersion <= 5) {
				connectionThread.closeConnection(ConnectionManager.intentResultCodeServerOutdated, false);
				return;
			}
			
			protocolManager = findProtocolManager(communicationsSubVersion);
			if(protocolManager == null) {
				connectionThread.closeConnection(ConnectionManager.intentResultCodeClientOutdated, false);
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
		private final boolean reconnectionRequest;
		
		private Socket socket;
		private DataInputStream inputStream;
		private DataOutputStream outputStream;
		private ConnectionThread.WriterThread writerThread = null;
		
		private boolean usingFallback;
		
		private ConnectionThread(String hostname, int port, String hostnameFallback, int portFallback, boolean reconnectionRequest) {
			this.hostname = hostname;
			this.port = port;
			this.hostnameFallback = hostnameFallback;
			this.portFallback = portFallback;
			this.reconnectionRequest = reconnectionRequest;
		}
		
		@Override
		public void run() {
			try {
				//Returning if the thread is interrupted
				if(isInterrupted()) return;
				
				//Checking if the request is a reconnection request
				if(reconnectionRequest) {
					//Looping while there are available reconnection attempts
					int reconnectionCount = 0;
					boolean success = false;
					while(!isInterrupted() && !success && reconnectionCount < ConnectionService.dropReconnectDelayMillis.length) {
						//Waiting for the delay to pass
						try {
							sleep(ConnectionService.dropReconnectDelayMillis[reconnectionCount]);
						} catch(InterruptedException interruptedException) {
							interruptedException.printStackTrace();
							return;
						}
						
						//Attempting another connection
						success = true;
						try {
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
						} catch(SocketException | SocketTimeoutException exception) {
							//Printing the stack trace
							exception.printStackTrace();
							
							//Increasing the connection count
							reconnectionCount++;
							
							success = false;
						}
					}
				} else {
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
			if(writerThread == null) {
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
}