package me.tagavari.airmessage.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.extension.FragmentBackOverride;
import me.tagavari.airmessage.extension.FragmentCommunicationSwap;
import me.tagavari.airmessage.service.ConnectionService;
import me.tagavari.airmessage.util.Constants;
import me.tagavari.airmessage.util.StateUtils;

public class FragmentOnboardingConnect extends FragmentCommunication<FragmentCommunicationSwap> implements FragmentBackOverride {
	//Creating the view values
	private ViewGroup groupContent;
	private ViewGroup groupProgress, groupError;
	private TextView labelError;
	private Button buttonError;
	
	//Creating the fragment values
	private FragmentViewModel viewModel;
	
	private BroadcastReceiver serviceBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			//Checking if the activity is waiting for a response
			if(viewModel.isConnecting) {
				//Ignoring the broadcast if the launch ID doesn't match or the result is connecting
				int state;
				if(intent.getByteExtra(Constants.intentParamLaunchID, (byte) 0) != viewModel.connectionLaunchID || (state = intent.getIntExtra(Constants.intentParamState, -1)) == ConnectionManager.stateConnecting) return;
				
				//Getting the result
				if(state == ConnectionManager.stateConnected) {
					//Finishing
					complete();
				} else {
					//Setting the state as idle
					viewModel.isConnecting = false;
					
					//Setting the error
					int errorCode = intent.getIntExtra(Constants.intentParamCode, -1);
					viewModel.errorDetails = StateUtils.getErrorDetails(errorCode, true);
				}
				
				//Updating the state
				applyState();
			}
		}
	};
	
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_onboarding_connect, container, false);
	}
	
	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		//Getting the view model
		viewModel = new ViewModelProvider(this).get(FragmentViewModel.class);
		
		//Getting the views
		groupContent = view.findViewById(R.id.content);
		groupProgress = groupContent.findViewById(R.id.layout_progress);
		groupError = groupContent.findViewById(R.id.layout_error);
		labelError = groupContent.findViewById(R.id.label_error);
		buttonError = groupContent.findViewById(R.id.button_error);
		
		//Setting up the toolbar
		configureToolbar(view.findViewById(R.id.toolbar));
		
		//Starting the connection if this is the first launch
		if(viewModel.isFirstLaunch) {
			startConnection();
			viewModel.isFirstLaunch = false;
		}
		
		//Updating the state
		applyState();
	}
	
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Registering the receiver
		LocalBroadcastManager.getInstance(requireContext()).registerReceiver(serviceBroadcastReceiver, new IntentFilter(ConnectionManager.localBCStateUpdate));
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		//Enabling configuration mode
		ConnectionService connectionService = ConnectionService.getInstance();
		if(connectionService != null) {
			connectionService.setConfigurationMode(true);
		}
		
		//Terminating existing connections
		ConnectionManager connectionManager = ConnectionService.getConnectionManager();
		if(connectionManager != null) {
			if(connectionManager.isConnected()) connectionManager.disconnect();
		}
	}
	
	@Override
	public void onStop() {
		super.onStop();
		
		//Disabling configuration mode
		ConnectionService connectionService = ConnectionService.getInstance();
		if(connectionService != null && !requireActivity().isChangingConfigurations()) {
			connectionService.setConfigurationMode(false);
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		//Unregistering the receiver
		LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(serviceBroadcastReceiver);
	}
	
	@Override
	public boolean onBackPressed() {
		promptExit();
		
		return true;
	}
	
	private void applyState() {
		applyState(viewModel.isConnecting, viewModel.errorDetails);
	}
	
	/**
	 * Applies the current task state to the UI
	 * @param isConnecting TRUE if the state is currently connecting
	 * @param errorDetails The error details to display, NULL if unavailable
	 */
	private void applyState(boolean isConnecting, StateUtils.ErrorDetails errorDetails) {
		TransitionManager.beginDelayedTransition(groupContent);
		
		if(isConnecting) {
			//Showing the progress group
			groupProgress.setVisibility(View.VISIBLE);
			groupError.setVisibility(View.GONE);
		} else {
			//Showing the error group
			groupProgress.setVisibility(View.GONE);
			groupError.setVisibility(View.VISIBLE);
			
			//Updating the error
			applyErrorDetails(errorDetails);
		}
	}
	
	/**
	 * Applies the current task's error details to the UI
	 * @param errorDetails The error details to display, NULL if unavailable
	 */
	private void applyErrorDetails(StateUtils.ErrorDetails errorDetails) {
		//Setting the text
		labelError.setText(errorDetails.getLabel());
		
		//Setting the button
		if(errorDetails.getButton() == null) {
			//Defaulting to "retry" if no action was provided
			buttonError.setText(R.string.action_retry);
			buttonError.setOnClickListener(view -> startConnection());
		} else {
			buttonError.setText(errorDetails.getButton().getLabel());
			buttonError.setOnClickListener(view -> errorDetails.getButton().getClickListener().accept(requireActivity()));
		}
	}
	
	private void startConnection() {
		//Setting the proxy type
		ConnectionManager.proxyType = ConnectionManager.proxyTypeConnect;
		
		//Telling the service to connect
		requireActivity().startService(new Intent(requireContext(), ConnectionService.class)
				.setAction(ConnectionService.selfIntentActionConnect)
				.putExtra(Constants.intentParamLaunchID, viewModel.connectionLaunchID = ConnectionManager.getNextLaunchID())
				.putExtra(ConnectionService.selfIntentExtraConfig, true));
		
		//Updating the state
		viewModel.isConnecting = true;
		applyState();
	}
	
	private void promptExit() {
		new MaterialAlertDialogBuilder(requireContext())
				.setMessage(R.string.dialog_signout_message)
				.setNegativeButton(android.R.string.cancel, null)
				.setPositiveButton(R.string.action_signout, (dialog, which) -> signOut())
				.create().show();
	}
	
	private void configureToolbar(Toolbar toolbar) {
		int colorSecondary = Constants.resolveColorAttr(getContext(), android.R.attr.textColorSecondary);
		{
			Drawable drawable = getResources().getDrawable(R.drawable.close_circle, null);
			drawable.setColorFilter(colorSecondary, PorterDuff.Mode.SRC_IN);
			toolbar.setNavigationIcon(drawable);
		}
		
		toolbar.setNavigationOnClickListener(navigationView -> promptExit());
	}
	
	private void signOut() {
		//Signing out
		FirebaseAuth.getInstance().signOut();
		
		//Returning to the previous screen
		callback.popStack();
	}
	
	private void complete() {
		//Saving the connection data in the shared preferences
		SharedPreferences.Editor editor = MainApplication.getInstance().getConnectivitySharedPrefs().edit();
		editor.putInt(MainApplication.sharedPreferencesConnectivityKeyAccountType, Constants.connectivityAccountTypeConnect);
		editor.putBoolean(MainApplication.sharedPreferencesConnectivityKeyConnectServerConfirmed, true);
		
		editor.apply();
		
		//Finishing the activity
		callback.launchConversations();
	}
	
	public static class FragmentViewModel extends ViewModel {
		boolean isFirstLaunch = true;
		boolean isConnecting = false;
		StateUtils.ErrorDetails errorDetails = null;
		byte connectionLaunchID;
		
		public FragmentViewModel() {
		}
	}
}