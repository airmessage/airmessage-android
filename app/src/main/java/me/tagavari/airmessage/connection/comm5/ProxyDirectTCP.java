package me.tagavari.airmessage.connection.comm5;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import me.tagavari.airmessage.connection.DataProxy;
import me.tagavari.airmessage.connection.encryption.EncryptionAES;
import me.tagavari.airmessage.connection.encryption.EncryptionManager;
import me.tagavari.airmessage.constants.NetworkConstants;
import me.tagavari.airmessage.constants.RegexConstants;
import me.tagavari.airmessage.data.SharedPreferencesManager;
import me.tagavari.airmessage.enums.ConnectionErrorCode;
import me.tagavari.airmessage.flavor.CrashlyticsBridge;
import me.tagavari.airmessage.util.ConnectionParams;
import me.tagavari.airmessage.util.DirectConnectionDetails;

import java.io.DataOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Establishes a direct connection with the server
 */
class ProxyDirectTCP extends DataProxy<EncryptedPacket> {
	//Creating the handler
	private final Handler handler = new Handler(Looper.getMainLooper());
	
	//Creating the state values
	private boolean isRunning = false;
	private ProxyDirectTCPReader readerThread;
	private ProxyDirectTCPWriter writerThread;
	
	//Creating the listeners
	private final ProxyDirectTCPReader.Listener readerThreadListener = new ProxyDirectTCPReader.Listener() {
		@Override
		public void onOpen(EncryptionManager encryptionManager, DataOutputStream outputStream) {
			//Starting the writer thread
			writerThread = new ProxyDirectTCPWriter(ProxyDirectTCP.this::stopAsync, encryptionManager, outputStream);
			writerThread.start();
			
			notifyOpen();
		}
		
		@Override
		public void onClose(int reason) {
			stopAsync(reason);
		}
		
		@Override
		public void onMessage(byte[] data, boolean wasEncrypted) {
			notifyMessage(new EncryptedPacket(data, wasEncrypted));
		}
	};
	
	@Override
	public void start(Context context, @Nullable ConnectionParams override) {
		//Returning if this proxy is already running
		if(isRunning) {
			CrashlyticsBridge.recordException(new IllegalStateException("Tried to start proxy, but it is already running!"));
			return;
		}
		
		ConnectionParams.Direct connectionParams;
		
		if(override instanceof ConnectionParams.Direct) {
			connectionParams = (ConnectionParams.Direct) override;
		} else {
			try {
				DirectConnectionDetails details = SharedPreferencesManager.getDirectConnectionDetails(context);
				String address = details.getAddress();
				String fallbackAddress = details.getFallbackAddress();
				String password;
				
				if(override instanceof ConnectionParams.Security) {
					password = ((ConnectionParams.Security) override).getPassword();
				} else {
					password = details.getPassword();
				}
				
				if(address == null || password == null) {
					notifyClose(ConnectionErrorCode.internalError);
					return;
				}
				
				connectionParams = new ConnectionParams.Direct(
						address,
						fallbackAddress,
						password
				);
			} catch(IOException | GeneralSecurityException exception) {
				exception.printStackTrace();
				notifyClose(ConnectionErrorCode.internalError);
				return;
			}
		}
		
		String hostname, hostnameFallback;
		int port, portFallback;
		EncryptionManager encryptionManager;
		
		//Parsing the address
		if(RegexConstants.port.matcher(connectionParams.getAddress()).find()) {
			String[] targetDetails = connectionParams.getAddress().split(":");
			hostname = targetDetails[0];
			port = Integer.parseInt(targetDetails[1]);
		} else {
			hostname = connectionParams.getAddress();
			port = NetworkConstants.defaultPort;
		}
		
		//Parsing the fallback address
		if(connectionParams.getFallbackAddress() != null) {
			if(RegexConstants.port.matcher(connectionParams.getFallbackAddress()).find()) {
				String[] targetDetails = connectionParams.getFallbackAddress().split(":");
				hostnameFallback = targetDetails[0];
				portFallback = Integer.parseInt(targetDetails[1]);
			} else {
				hostnameFallback = connectionParams.getFallbackAddress();
				portFallback = NetworkConstants.defaultPort;
			}
		} else {
			hostnameFallback = null;
			portFallback = -1;
		}
		
		//Handling the password
		encryptionManager = new EncryptionAES(connectionParams.getPassword());
		
		//Starting the connection thread
		readerThread = new ProxyDirectTCPReader(readerThreadListener, hostname, port, hostnameFallback, portFallback, encryptionManager);
		readerThread.start();
		
		//Updating the running state
		isRunning = true;
	}
	
	private void stopAsync(@ConnectionErrorCode int code) {
		handler.post(() -> stop(code));
	}
	
	@Override
	public void stop(@ConnectionErrorCode int code) {
		//Returning if this proxy is not running
		if(!isRunning) return;
		
		//Stopping the threads
		if(readerThread != null) readerThread.interrupt();
		if(writerThread != null) writerThread.interrupt();
		
		//Calling the listener
		notifyClose(code);
		
		//Updating the running state
		isRunning = false;
	}
	
	@Override
	public boolean isUsingFallback() {
		return readerThread != null && readerThread.isUsingFallback();
	}
	
	@Override
	public boolean send(EncryptedPacket packet) {
		//Queuing the packet
		if(writerThread == null) return false;
		writerThread.queuePacket(packet);
		return true;
	}
}