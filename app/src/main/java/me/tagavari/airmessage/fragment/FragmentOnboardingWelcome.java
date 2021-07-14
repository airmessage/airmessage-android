package me.tagavari.airmessage.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.data.SharedPreferencesManager;
import me.tagavari.airmessage.enums.ProxyType;
import me.tagavari.airmessage.extension.FragmentCommunicationNetworkConfig;
import me.tagavari.airmessage.util.ConnectionParams;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class FragmentOnboardingWelcome extends FragmentCommunication<FragmentCommunicationNetworkConfig> {
	//Creating the constants
	private static final String TAG = FragmentOnboardingWelcome.class.getName();
	
	private static final int requestCodeSignInGoogle = 0;
	private static final int requestCodeSignInEmail = 1;
	private static final int requestCodeFixGoogleAPI = 10;
	
	//Creating the sign-in values
	private FirebaseAuth mAuth;
	private GoogleSignInClient googleSignInClient;
	
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
		
		//Setting up Firebase sign-in
		mAuth = FirebaseAuth.getInstance();
		
		//Setting up Google sign-in
		{
			GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
					.requestIdToken(getString(R.string.default_web_client_id))
					.requestEmail()
					.build();
			googleSignInClient = GoogleSignIn.getClient(requireContext(), gso);
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		// Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
		if(requestCode == requestCodeSignInGoogle) {
			Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
			try {
				// Google Sign In was successful, authenticate with Firebase
				GoogleSignInAccount account = task.getResult(ApiException.class);
				firebaseAuthWithGoogle(account);
			} catch(ApiException exception) {
				exception.printStackTrace();
				
				if(exception.getStatusCode() != GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
					//Displaying an error snackbar
					Snackbar.make(requireView(), R.string.message_signinerror, Snackbar.LENGTH_LONG).show();
				}
			}
		}
	}
	
	private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
		Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());
		
		AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
		mAuth.signInWithCredential(credential).addOnCompleteListener(requireActivity(), task -> {
			if(task.isSuccessful()) {
				// Sign in success, update UI with the signed-in user's information
				Log.d(TAG, "signInWithCredential:success");
				
				//Completing authentication
				completeAuth();
			} else {
				// If sign in fails, display a message to the user.
				Log.w(TAG, "signInWithCredential:failure", task.getException());
				
				//Displaying an error snackbar
				Snackbar.make(requireView(), R.string.message_signinerror, Snackbar.LENGTH_LONG).show();
			}
		});
	}
	
	private void completeAuth() {
		//Advancing to the connection fragment
		getCommunicationsCallback().swapFragment(new FragmentOnboardingConnect());
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
		//Checking if Google Play Services are available
		int googleAPIAvailability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(requireContext());
		if(googleAPIAvailability == ConnectionResult.SUCCESS) {
			//Starting Google sign-in
			Intent signInIntent = googleSignInClient.getSignInIntent();
			startActivityForResult(signInIntent, requestCodeSignInGoogle);
		} else {
			//Prompting the user
			GoogleApiAvailability.getInstance().getErrorDialog(requireActivity(), googleAPIAvailability, requestCodeFixGoogleAPI).show();
		}
	}
	
	private void launchAuthEmail(View view) {
	
	}
}