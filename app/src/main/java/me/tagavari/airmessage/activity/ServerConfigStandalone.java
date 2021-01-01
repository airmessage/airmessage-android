package me.tagavari.airmessage.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.constants.ColorConstants;
import me.tagavari.airmessage.enums.ConnectionErrorCode;
import me.tagavari.airmessage.extension.FragmentCommunicationNetworkConfig;
import me.tagavari.airmessage.fragment.FragmentCommunication;
import me.tagavari.airmessage.fragment.FragmentOnboardingManual;
import me.tagavari.airmessage.helper.ThemeHelper;
import me.tagavari.airmessage.service.ConnectionService;

public class ServerConfigStandalone extends AppCompatActivity implements FragmentCommunicationNetworkConfig {
	//Constants
	private static final String keyFragment = "fragment";
	
	//Fragment values
	private FragmentOnboardingManual currentFragment;
	
	//Service bindings
	private ConnectionManager connectionManager = null;
	private final ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			ConnectionService.ConnectionBinder binder = (ConnectionService.ConnectionBinder) service;
			
			connectionManager = binder.getConnectionManager();
			
			//Disconnecting and disabling reconnections
			prepareConnectionManager();
		}
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			//Restoring the previous connection state
			restoreConnectionManager();
			
			connectionManager = null;
		}
	};
	
	//Activity state
	boolean isComplete = false;
	
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if(savedInstanceState == null) {
			//Initializing the fragment
			currentFragment = new FragmentOnboardingManual();
			getSupportFragmentManager().beginTransaction().add(android.R.id.content, new FragmentOnboardingManual()).commit();
		} else {
			//Restoring the fragment
			currentFragment = (FragmentOnboardingManual) getSupportFragmentManager().getFragment(savedInstanceState, keyFragment);
		}
		
		//Configuring the AMOLED theme
		if(ThemeHelper.shouldUseAMOLED(this)) setDarkAMOLED();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		//Binding to the connection service
		bindService(new Intent(this, ConnectionService.class), serviceConnection, Context.BIND_AUTO_CREATE);
		
		//Updating the connection manager
		if(connectionManager != null) prepareConnectionManager();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		
		//Unbinding from the connection service
		unbindService(serviceConnection);
		
		//Resetting the connection manager
		if(connectionManager != null) restoreConnectionManager();
	}
	
	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		
		//Saving the fragment
		if(currentFragment != null && currentFragment.isAdded()) getSupportFragmentManager().putFragment(outState, keyFragment, currentFragment);
	}
	
	@Override
	public void onAttachFragment(@NonNull Fragment fragment) {
		super.onAttachFragment(fragment);
		
		//Setting the activity callbacks
		((FragmentOnboardingManual) fragment).setCommunicationsCallback(this);
	}
	
	@Override
	public void swapFragment(FragmentCommunication<FragmentCommunicationNetworkConfig> fragment) {
		//No fragments to swap
	}
	
	@Override
	public void popStack() {
		finish();
	}
	
	@Override
	public void launchConversations() {
		isComplete = true;
		finish();
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
		if(!isComplete) {
			//Restoring the service state
			connectionManager.disconnect(ConnectionErrorCode.user);
			connectionManager.connect();
		}
		
		//Disabling overrides
		connectionManager.setConnectionOverride(null);
		
		//Re-enabling reconnections
		connectionManager.setDisableReconnections(false);
	}
	
	@Nullable
	@Override
	public ConnectionManager getConnectionManager() {
		return connectionManager;
	}
	
	void setDarkAMOLED() {
		findViewById(android.R.id.content).getRootView().setBackgroundColor(ColorConstants.colorAMOLED);
		getWindow().setStatusBarColor(ColorConstants.colorAMOLED);
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) getWindow().setNavigationBarColor(ColorConstants.colorAMOLED); //Leaving the transparent navigation bar on Android 10
	}
}