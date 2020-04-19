package me.tagavari.airmessage.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.extension.FragmentBackOverride;
import me.tagavari.airmessage.extension.FragmentCommunicationSwap;
import me.tagavari.airmessage.util.Constants;

public class FragmentOnboardingWelcome extends FragmentCommunication<FragmentCommunicationSwap> {
	//Creating the constants
	private static final String TAG = FragmentOnboardingWelcome.class.getName();
	
	private static final int requestCodeSignInGoogle = 0;
	private static final int requestCodeSignInEmail = 1;
	
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
			} catch(ApiException e) {
				// Google Sign In failed, update UI appropriately
				Log.w(TAG, "Google sign in failed", e);
				
				//Displaying an error snackbar
				Snackbar.make(requireView(), R.string.message_signinerror, Snackbar.LENGTH_LONG).show();
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
		//Updating the account type
		MainApplication.getInstance().getConnectivitySharedPrefs().edit()
				.putInt(MainApplication.sharedPreferencesConnectivityKeyAccountType, Constants.connectivityAccountTypeConnect)
				.apply();
		
		//Advancing to the connection fragment
		callback.swapFragment(new FragmentOnboardingConnect());
	}
	
	private void launchManualConnect(View view) {
		callback.swapFragment(new FragmentOnboardingManual());
	}
	
	private void launchAuthGoogle(View view) {
		Intent signInIntent = googleSignInClient.getSignInIntent();
		startActivityForResult(signInIntent, requestCodeSignInGoogle);
	}
	
	private void launchAuthEmail(View view) {
	
	}
}