package me.tagavari.airmessage.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import me.tagavari.airmessage.R;
import me.tagavari.airmessage.extension.FragmentBackOverride;
import me.tagavari.airmessage.extension.FragmentCommunicationSwap;
import me.tagavari.airmessage.fragment.FragmentCommunication;
import me.tagavari.airmessage.fragment.FragmentOnboardingWelcome;
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
			swapFragment(new FragmentOnboardingWelcome());
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
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		if(currentFragment != null) transaction.setCustomAnimations(R.anim.windowslide_in, R.anim.windowslide_out, R.anim.windowslideback_in, R.anim.windowslideback_out);
		transaction.replace(R.id.frame_content, fragment);
		transaction.addToBackStack(null);
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
		
		//Finishing the activity
		finish();
	}
}