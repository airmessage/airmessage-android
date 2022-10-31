package me.tagavari.airmessage.fragment;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.security.GeneralSecurityException;

import me.tagavari.airmessage.R;
import me.tagavari.airmessage.data.SharedPreferencesManager;
import me.tagavari.airmessage.enums.ProxyType;
import me.tagavari.airmessage.extension.FragmentCommunicationNetworkConfig;
import me.tagavari.airmessage.flavor.WelcomeGoogleSignIn;
import me.tagavari.airmessage.util.ConnectionParams;

public class FragmentOnboardingWelcome extends FragmentCommunication<FragmentCommunicationNetworkConfig> {
	//Creating the constants
	public static final String TAG = FragmentOnboardingWelcome.class.getName();

	//Creating the sign-in values
	private WelcomeGoogleSignIn welcomeGoogleSignIn;
	
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_onboarding_welcome, container, false);
	}
	
	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		//Hooking up the views
		view.findViewById(R.id.button_connect_google).setOnClickListener(this::launchAuthGoogle);
		view.findViewById(R.id.button_connect_email).setOnClickListener(this::launchAuthEmail);
		view.findViewById(R.id.button_manual).setOnClickListener(this::launchManualConnect);
		view.findViewById(R.id.button_manual).setOnLongClickListener(this::launchSkipConfig);
		((TextView) view.findViewById(R.id.label_privacypolicy)).setMovementMethod(LinkMovementMethod.getInstance());
		
		//Setting up Firebase sign-in
		welcomeGoogleSignIn = new WelcomeGoogleSignIn(this);
		if(!welcomeGoogleSignIn.isSupported()) {
			view.findViewById(R.id.button_connect_google).setVisibility(View.GONE);
		}
	}
	
	private void launchManualConnect(View view) {
		getCommunicationsCallback().swapFragment(new FragmentOnboardingManual());
	}
	
	private boolean launchSkipConfig(View view) {
		//Showing a confirmation dialog
		new MaterialAlertDialogBuilder(requireActivity())
				.setMessage(R.string.dialog_skipsetup_message)
				.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
				.setPositiveButton(R.string.action_skip, (dialog, which) -> {
					//Saving blank connection values
					try {
						SharedPreferencesManager.setDirectConnectionDetails(getContext(), new ConnectionParams.Direct("127.0.0.1", null, "password"));
					} catch(IOException | GeneralSecurityException exception) {
						exception.printStackTrace();
						return;
					}
					SharedPreferencesManager.setProxyType(getContext(), ProxyType.direct);
					SharedPreferencesManager.setConnectionConfigured(getContext(), true);
					
					//Finishing the activity
					getCommunicationsCallback().launchConversations();
				}).create().show();
		return true;
	}
	
	private void launchAuthGoogle(View view) {
		welcomeGoogleSignIn.launch();
	}
	
	private void launchAuthEmail(View view) {
	
	}
}