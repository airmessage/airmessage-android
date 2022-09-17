package me.tagavari.airmessage.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.compose.ConversationsCompose;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.constants.ColorConstants;
import me.tagavari.airmessage.enums.ConnectionErrorCode;
import me.tagavari.airmessage.extension.FragmentBackOverride;
import me.tagavari.airmessage.extension.FragmentCommunicationNetworkConfig;
import me.tagavari.airmessage.fragment.FragmentCommunication;
import me.tagavari.airmessage.fragment.FragmentOnboardingWelcome;
import me.tagavari.airmessage.helper.ResourceHelper;
import me.tagavari.airmessage.helper.ThemeHelper;
import me.tagavari.airmessage.service.ConnectionService;

public class Onboarding extends AppCompatActivity implements FragmentCommunicationNetworkConfig {
	//Constants
	private static final String keyFragment = "fragment";
	
	//Dimensions
	private static final int paneMaxHeight = ResourceHelper.dpToPx(600);
	
	//Fragment state
	private FragmentCommunication<FragmentCommunicationNetworkConfig> currentFragment;
	
	//Listeners
	private final FragmentManager.OnBackStackChangedListener onBackStackChangedListener = () -> {
		currentFragment = (FragmentCommunication<FragmentCommunicationNetworkConfig>) getSupportFragmentManager().findFragmentById(R.id.frame_content);
	};
	
	//Service bindings
	private ConnectionManager connectionManager = null;
	private final ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			ConnectionService.ConnectionBinder binder = (ConnectionService.ConnectionBinder) service;
			
			connectionManager = binder.getConnectionManager();
			
			//Disconnecting and disabling reconnections
			connectionManager.setDisableReconnections(true);
			connectionManager.disconnect(ConnectionErrorCode.user);
		}
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			connectionManager = null;
		}
	};
	
	//Activity state
	boolean isComplete = false;
	
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Setting the content view
		setContentView(R.layout.activity_onboarding);
		
		if(savedInstanceState == null) {
			//Initializing the first fragment
			FragmentCommunication<FragmentCommunicationNetworkConfig> fragment = new FragmentOnboardingWelcome();
			swapFragment(fragment, false);
			currentFragment = fragment;
		} else {
			//Restoring the fragment
			currentFragment = (FragmentCommunication<FragmentCommunicationNetworkConfig>) getSupportFragmentManager().getFragment(savedInstanceState, keyFragment);
		}
		
		//Registering the back stack change listener
		getSupportFragmentManager().addOnBackStackChangedListener(onBackStackChangedListener);
		
		//Configuring the header images
		getWindow().getDecorView().post(() -> {
			//Checking if the view is large
			ViewGroup windowGroup = findViewById(R.id.group_pane);
			if(windowGroup != null) { //group_pane is only available when the large resource is being used
				//Limiting the pane's height
				if(windowGroup.getHeight() > paneMaxHeight) windowGroup.getLayoutParams().height = paneMaxHeight;
			}
		});
		
		//Configuring the AMOLED theme
		if(ThemeHelper.shouldUseAMOLED(this)) setDarkAMOLED();
		
		//Preventing the connection service from launching on boot
		Preferences.updateConnectionServiceBootEnabled(this, false);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		//Binding to the connection service
		bindService(new Intent(this, ConnectionService.class), serviceConnection, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		
		//Unbinding from the connection service
		unbindService(serviceConnection);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		//Unregistering the back stack change listener
		getSupportFragmentManager().removeOnBackStackChangedListener(onBackStackChangedListener);
	}
	
	@Override
	public void onAttachFragment(@NonNull Fragment childFragment) {
		if(childFragment instanceof FragmentCommunication) ((FragmentCommunication<FragmentCommunicationNetworkConfig>) childFragment).setCommunicationsCallback(this);
	}
	
	@Override
	public void onBackPressed() {
		if(!(currentFragment instanceof FragmentBackOverride && ((FragmentBackOverride) currentFragment).onBackPressed())) {
			super.onBackPressed();
		}
	}
	
	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		
		//Saving the fragment
		if(currentFragment != null && currentFragment.isAdded()) getSupportFragmentManager().putFragment(outState, keyFragment, currentFragment);
	}
	
	@Override
	public void swapFragment(FragmentCommunication<FragmentCommunicationNetworkConfig> fragment) {
		swapFragment(fragment, true);
	}
	
	public void swapFragment(FragmentCommunication<FragmentCommunicationNetworkConfig> fragment, boolean addToBackStack) {
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		if(currentFragment != null) transaction.setCustomAnimations(R.anim.windowslide_in, R.anim.windowslide_out, R.anim.windowslideback_in, R.anim.windowslideback_out);
		transaction.replace(R.id.frame_content, fragment);
		if(addToBackStack) transaction.addToBackStack(null);
		transaction.commit();
	}
	
	@Override
	public void popStack() {
		FragmentManager fragmentManager = getSupportFragmentManager();
		if(fragmentManager.getBackStackEntryCount() > 0) fragmentManager.popBackStack();
	}
	
	@Override
	public void launchConversations() {
		isComplete = true;
		
		//Restoring the connection manager state
		connectionManager.setConnectionOverride(null);
		connectionManager.setDisableReconnections(false);
		
		//Starting the new activity
		startActivity(new Intent(this, ConversationsCompose.class));
		
		//Enabling transitions
		//overridePendingTransition(R.anim.fade_in_light, R.anim.activity_slide_up);
		
		//Finishing the activity
		finish();
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