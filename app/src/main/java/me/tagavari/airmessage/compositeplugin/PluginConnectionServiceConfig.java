package me.tagavari.airmessage.compositeplugin;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import me.tagavari.airmessage.composite.AppCompatActivityPlugin;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.data.SharedPreferencesManager;
import me.tagavari.airmessage.enums.ConnectionErrorCode;
import me.tagavari.airmessage.enums.ProxyType;
import me.tagavari.airmessage.helper.ConnectionServiceLaunchHelper;
import me.tagavari.airmessage.service.ConnectionService;

/**
 * Handles binding and unbinding from the connection service to retrieve a {@link ConnectionManager} instance
 */
public class PluginConnectionServiceConfig extends AppCompatActivityPlugin {
	private boolean isServiceBound = false;
	private ConnectionManager connectionManager = null;
	
	private final ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			ConnectionService.ConnectionBinder binder = (ConnectionService.ConnectionBinder) service;
			
			//Setting the values
			isServiceBound = true;
			connectionManager = binder.getConnectionManager();
			
			//Disconnecting and disabling reconnections
			prepareConnectionManager();
		}
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			//Restoring the previous connection state
			restoreConnectionManager();
			
			//Resetting the values
			isServiceBound = false;
			connectionManager = null;
		}
	};
	
	private boolean isComplete;
	
	@Override
	protected void onStart() {
		super.onStart();
		
		//Starting the persistent connection service if required
		if(SharedPreferencesManager.isConnectionConfigured(getActivity()) && SharedPreferencesManager.getProxyType(getActivity()) == ProxyType.direct) {
			ConnectionServiceLaunchHelper.launchPersistent(getActivity());
		}
		
		//Binding to the connection service
		getActivity().bindService(new Intent(getActivity(), ConnectionService.class), serviceConnection, Context.BIND_AUTO_CREATE);
		
		//Updating the connection manager
		if(isServiceBound) prepareConnectionManager();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		
		//Unbinding from the connection service
		getActivity().unbindService(serviceConnection);
		
		//Resetting the connection manager
		if(isServiceBound) restoreConnectionManager();
	}
	
	/**
	 * Prepares the connection manager for configuration under this activity
	 */
	private void prepareConnectionManager() {
		//Disconnecting and disabling reconnections
		connectionManager.setDisableReconnections(true);
		connectionManager.disconnect(ConnectionErrorCode.user);
	}
	
	/**
	 * Restores the connection manager to its previous state
	 */
	private void restoreConnectionManager() {
		if(!isComplete()) {
			//Restoring the service state
			connectionManager.disconnect(ConnectionErrorCode.user);
			connectionManager.connect();
		}
		
		//Disabling overrides
		connectionManager.setConnectionOverride(null);
		
		//Re-enabling reconnections
		connectionManager.setDisableReconnections(false);
	}
	
	/**
	 * Gets whether we are currently bound to the connection service
	 */
	public boolean isServiceBound() {
		return isServiceBound;
	}
	
	/**
	 * Gets the active connection manager, or NULL if we are not currently bound
	 */
	public ConnectionManager getConnectionManager() {
		return connectionManager;
	}
	
	/**
	 * Gets if this activity has finished its job and is completing
	 */
	public boolean isComplete() {
		return isComplete;
	}
	
	/**
	 * Sets if this activity has finished its job and is completing
	 */
	public void setComplete(boolean complete) {
		isComplete = complete;
	}
}