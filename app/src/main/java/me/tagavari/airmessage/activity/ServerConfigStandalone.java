package me.tagavari.airmessage.activity;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import me.tagavari.airmessage.extension.FragmentCommunicationSwap;
import me.tagavari.airmessage.fragment.FragmentCommunication;
import me.tagavari.airmessage.fragment.FragmentOnboardingManual;

public class ServerConfigStandalone extends AppCompatActivity implements FragmentCommunicationSwap {
	private static final String keyFragment = "fragment";
	
	private FragmentOnboardingManual currentFragment;
	
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
		((FragmentOnboardingManual) fragment).setCallback(this);
	}
	
	@Override
	public void swapFragment(FragmentCommunication<FragmentCommunicationSwap> fragment) {
		//No fragments to swap
	}
	
	@Override
	public void popStack() {
		finish();
	}
	
	@Override
	public void launchConversations() {
		finish();
	}
}