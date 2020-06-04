package me.tagavari.airmessage.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import me.tagavari.airmessage.R;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.extension.FragmentBackOverride;
import me.tagavari.airmessage.extension.FragmentCommunicationSwap;
import me.tagavari.airmessage.fragment.FragmentCommunication;
import me.tagavari.airmessage.fragment.FragmentOnboardingWelcome;
import me.tagavari.airmessage.service.ConnectionService;
import me.tagavari.airmessage.util.Constants;

public class Onboarding extends AppCompatActivity implements FragmentCommunicationSwap {
	private static final String keyFragment = "fragment";
	
	private FragmentCommunication<FragmentCommunicationSwap> currentFragment;
	
	private static final int paneMaxHeight = Constants.dpToPx(600);
	
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Setting the content view
		setContentView(R.layout.activity_onboarding);
		
		if(savedInstanceState == null) {
			//Initializing the first fragment
			swapFragment(new FragmentOnboardingWelcome(), false);
		} else {
			//Restoring the fragment
			currentFragment = (FragmentCommunication<FragmentCommunicationSwap>) getSupportFragmentManager().getFragment(savedInstanceState, keyFragment);
		}
		
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
		if(Constants.shouldUseAMOLED(this)) setDarkAMOLED();
		
		//Preventing the connection service from launching on boot
		Preferences.updateConnectionServiceBootEnabled(this, false);
	}
	
	@Override
	public void onAttachFragment(@NonNull Fragment childFragment) {
		if(childFragment instanceof FragmentCommunication) ((FragmentCommunication<FragmentCommunicationSwap>) childFragment).setCallback(this);
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
	
	public void swapFragment(FragmentCommunication<FragmentCommunicationSwap> fragment) {
		swapFragment(fragment, true);
	}
	
	public void swapFragment(FragmentCommunication<FragmentCommunicationSwap> fragment, boolean addToBackStack) {
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		if(currentFragment != null) transaction.setCustomAnimations(R.anim.windowslide_in, R.anim.windowslide_out, R.anim.windowslideback_in, R.anim.windowslideback_out);
		transaction.replace(R.id.frame_content, fragment);
		if(addToBackStack) transaction.addToBackStack(null);
		transaction.commit();
		
		//Updating the current fragment
		currentFragment = fragment;
	}
	
	public void popStack() {
		FragmentManager fragmentManager = getSupportFragmentManager();
		if(fragmentManager.getBackStackEntryCount() > 0) fragmentManager.popBackStack();
	}
	
	public void launchConversations() {
		//Starting the new activity
		startActivity(new Intent(this, Conversations.class));
		
		//Enabling transitions
		overridePendingTransition(R.anim.fade_in_light, R.anim.activity_slide_up);
		
		//Restoring the connection service's start-on-boot state (if the proxy type is direct)
		//If the proxy type is connect, we'll leave it disabled (as set in this activity's onCreate())
		if(ConnectionManager.proxyType == ConnectionManager.proxyTypeDirect) {
			Preferences.updateConnectionServiceBootEnabled(this, Preferences.getPreferenceStartOnBoot(this));
		}
		
		//Finishing the activity
		finish();
	}
	
	void setDarkAMOLED() {
		findViewById(android.R.id.content).getRootView().setBackgroundColor(Constants.colorAMOLED);
		getWindow().setStatusBarColor(Constants.colorAMOLED);
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) getWindow().setNavigationBarColor(Constants.colorAMOLED); //Leaving the transparent navigation bar on Android 10
	}
}