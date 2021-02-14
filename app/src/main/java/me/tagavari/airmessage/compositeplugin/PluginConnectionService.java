package me.tagavari.airmessage.compositeplugin;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.Nullable;

import me.tagavari.airmessage.composite.AppCompatActivityPlugin;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.data.SharedPreferencesManager;
import me.tagavari.airmessage.enums.ProxyType;
import me.tagavari.airmessage.helper.ConnectionServiceLaunchHelper;
import me.tagavari.airmessage.service.ConnectionService;

/**
 * Handles binding and unbinding from the connection service to retrieve a {@link ConnectionManager} instance
 */
public class PluginConnectionService extends AppCompatActivityPlugin {
	private boolean isServiceBound = false;
	private ConnectionManager connectionManager = null;
	
	private final ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			ConnectionService.ConnectionBinder binder = (ConnectionService.ConnectionBinder) service;
			binder.getConnectionManager();
			
			isServiceBound = true;
			connectionManager = binder.getConnectionManager();
			connectionManager.connect();
		}
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			isServiceBound = false;
			connectionManager = null;
		}
	};
	
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Ignoring if the connection isn't configured
		if(!SharedPreferencesManager.isConnectionConfigured(getActivity())) return;
		
		//Starting the persistent connection service if required
		if(SharedPreferencesManager.isConnectionConfigured(getActivity()) && SharedPreferencesManager.getProxyType(getActivity()) == ProxyType.direct) {
			ConnectionServiceLaunchHelper.launchPersistent(getActivity());
		}
		
		//Binding to the connection service
		getActivity().bindService(new Intent(getActivity(), ConnectionService.class), serviceConnection, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		//Unbinding from the connection service
		if(isServiceBound) {
			getActivity().unbindService(serviceConnection);
		}
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
}