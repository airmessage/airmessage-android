package me.tagavari.airmessage.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

import com.google.android.material.textfield.TextInputLayout;

import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.connection.CommunicationsManager;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.extension.FragmentCommunicationSwap;
import me.tagavari.airmessage.service.ConnectionService;
import me.tagavari.airmessage.util.Constants;
import me.tagavari.airmessage.util.StateUtils;

public class FragmentOnboardingManual extends FragmentCommunication<FragmentCommunicationSwap> {
	//Creating the constants
	private static final int stateIdle = 0;
	private static final int stateConnecting = 1;
	private static final int stateConnected = 2;
	
	//Creating the view values
	private ViewGroup groupContent;
	private TextInputLayout inputAddress, inputFallback, inputPassword;
	private Button buttonNext, buttonDone, buttonCancel;
	private ViewGroup groupProgress, groupError, groupConnected;
	private TextView labelError;
	private TextView labelConnected;
	private Button buttonError;
	
	//Creating the fragment values
	private FragmentViewModel viewModel;
	private boolean isCompleting = false; //If this activity is finishing with a successful state
	
	//Creating the listener values
	private final TextWatcher addressInputWatcher = new TextWatcher() {
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			//Updating the next button
			updateNextButton(s.toString(), inputFallback.getEditText().getText().toString(), inputPassword.getEditText().getText().toString());
			
			//Checking for the error solution as long as there is an error
			if(inputAddress.getError() != null && Constants.regExValidAddress.matcher(s).find()) inputAddress.setError(null);
		}
		
		@Override
		public void afterTextChanged(Editable s) {}
	};
	private final TextWatcher fallbackInputWatcher = new TextWatcher() {
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			//Updating the next button
			updateNextButton(inputAddress.getEditText().getText().toString(), s.toString(), inputPassword.getEditText().getText().toString());
			
			//Checking for the error solution as long as there is an error
			if(inputFallback.getError() != null && (s.length() == 0 || Constants.regExValidAddress.matcher(s).find())) inputFallback.setError(null);
		}
		
		@Override
		public void afterTextChanged(Editable s) {}
	};
	private final TextWatcher passwordInputWatcher = new TextWatcher() {
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			//Updating the next button
			updateNextButton(inputAddress.getEditText().getText().toString(), inputFallback.getEditText().getText().toString(), s.toString());
		}
		
		@Override
		public void afterTextChanged(Editable s) {}
	};
	private class HostnameFocusListener implements View.OnFocusChangeListener {
		private final TextInputLayout textInputLayout;
		
		 HostnameFocusListener(TextInputLayout textInputLayout) {
			this.textInputLayout = textInputLayout;
		}
		
		@Override
		public void onFocusChange(View view, boolean hasFocus) {
			//Updating the error state when the input is deselected
			if(hasFocus) return;
			
			String input = textInputLayout.getEditText().getText().toString();
			textInputLayout.setError(!input.isEmpty() && !Constants.regExValidAddress.matcher(input).find() ?
									 getResources().getString(R.string.part_invalidaddress) : null);
		}
	}
	private final View.OnClickListener nextButtonListener = view -> {
		//Updating the state
		viewModel.currentState = stateConnecting;
		viewModel.errorDetails = null;
		applyState();
		
		//Starting the connection
		startConnection();
	};
	private final View.OnClickListener cancelButtonListener = view -> {
		//Resetting the connection
		ConnectionManager connectionManager = ConnectionService.getConnectionManager();
		ConnectionManager.hostname = null;
		ConnectionManager.hostnameFallback = null;
		ConnectionManager.password = null;
		if(connectionManager != null) connectionManager.disconnect();
		
		//Reverting the state to idle
		viewModel.currentState = stateIdle;
		applyState();
	};
	private final View.OnClickListener doneButtonListener = view -> {
		//Finishing
		complete();
	};
	
	private BroadcastReceiver serviceBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			//Checking if the activity is waiting for a response
			if(viewModel.currentState == stateConnecting) {
				//Ignoring the broadcast if the launch ID doesn't match or the result is connecting
				int state;
				if(intent.getByteExtra(Constants.intentParamLaunchID, (byte) 0) != viewModel.connectionLaunchID || (state = intent.getIntExtra(Constants.intentParamState, -1)) == ConnectionManager.stateConnecting) return;
				
				//Getting the result
				if(state == ConnectionManager.stateConnected) {
					//Setting the state as connected
					viewModel.currentState = stateConnected;
				} else {
					//Setting the state as idle
					viewModel.currentState = stateIdle;
					
					//Setting the error
					int errorCode = intent.getIntExtra(Constants.intentParamCode, -1);
					viewModel.errorDetails = StateUtils.getErrorDetails(errorCode, true);
				}
				
				//Updating the state
				applyState();
			}
			//Checking if the activity is connected, and the service is disconnected
			else if(viewModel.currentState == stateConnected && intent.getIntExtra(Constants.intentParamState, -1) == ConnectionManager.stateDisconnected) {
				//Cancelling the connection
				viewModel.currentState = stateIdle;
				applyState();
			}
		}
	};
	
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_onboarding_manual, container, false);
	}
	
	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		//Getting the view model
		viewModel = new ViewModelProvider(this).get(FragmentViewModel.class);
		
		//Setting up the toolbar
		configureToolbar(view.findViewById(R.id.toolbar));
		
		//Getting the views
		groupContent = view.findViewById(R.id.content);
		
		inputAddress = groupContent.findViewById(R.id.input_address);
		inputFallback = groupContent.findViewById(R.id.input_fallbackaddress);
		inputPassword = groupContent.findViewById(R.id.input_password);
		
		buttonNext = groupContent.findViewById(R.id.button_next);
		buttonDone = groupContent.findViewById(R.id.button_done);
		buttonCancel = groupContent.findViewById(R.id.button_cancel);
		
		groupProgress = groupContent.findViewById(R.id.group_progress);
		groupError = groupContent.findViewById(R.id.group_error);
		groupConnected = groupContent.findViewById(R.id.group_connected);
		
		labelError = groupError.findViewById(R.id.label_error);
		labelConnected = groupConnected.findViewById(R.id.label_connected);
		
		buttonError = groupContent.findViewById(R.id.button_error);
		
		//Setting the listeners
		inputAddress.getEditText().addTextChangedListener(addressInputWatcher);
		inputAddress.getEditText().setOnFocusChangeListener(new HostnameFocusListener(inputAddress));
		inputFallback.getEditText().addTextChangedListener(fallbackInputWatcher);
		inputFallback.getEditText().setOnFocusChangeListener(new HostnameFocusListener(inputFallback));
		inputPassword.getEditText().addTextChangedListener(passwordInputWatcher);
		
		buttonNext.setOnClickListener(nextButtonListener);
		buttonDone.setOnClickListener(doneButtonListener);
		buttonCancel.setOnClickListener(cancelButtonListener);
		
		//Filling in the input fields with previous information
		SharedPreferences sharedPreferences = MainApplication.getInstance().getConnectivitySharedPrefs();
		inputAddress.getEditText().setText(sharedPreferences.getString(MainApplication.sharedPreferencesConnectivityKeyHostname, null));
		inputFallback.getEditText().setText(sharedPreferences.getString(MainApplication.sharedPreferencesConnectivityKeyHostnameFallback, null));
		inputPassword.getEditText().setText(sharedPreferences.getString(MainApplication.sharedPreferencesConnectivityKeyPassword, null));
		
		//Updating the state
		applyState();
		
		//Registering the receiver
		LocalBroadcastManager.getInstance(requireContext()).registerReceiver(serviceBroadcastReceiver, new IntentFilter(ConnectionManager.localBCStateUpdate));
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		//Disabling existing connections
		ConnectionService connectionService = ConnectionService.getInstance();
		if(connectionService != null) {
			connectionService.setConfigurationMode(true);
		}
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
		if(connectionService != null) {
			connectionService.setConfigurationMode(false);
		}
		
		if(!isCompleting && viewModel.originalHostname != null && viewModel.originalPassword != null && !requireActivity().isChangingConfigurations()) {
			//Restoring the connection values
			ConnectionManager.hostname = viewModel.originalHostname;
			ConnectionManager.hostnameFallback = viewModel.originalHostnameFallback;
			ConnectionManager.password = viewModel.originalPassword;
			
			//Resetting the connection
			ConnectionManager connectionManager = ConnectionService.getConnectionManager();
			if(connectionManager != null) {
				if(connectionManager.isConnected()) connectionManager.disconnect();
				else connectionManager.connect(requireContext());
			}
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		//Unregistering the receiver
		LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(serviceBroadcastReceiver);
	}
	
	private void configureToolbar(Toolbar toolbar) {
		int colorSecondary = Constants.resolveColorAttr(getContext(), android.R.attr.textColorSecondary);
		{
			Drawable drawable = getResources().getDrawable(R.drawable.close, null);
			drawable.setColorFilter(colorSecondary, PorterDuff.Mode.SRC_IN);
			toolbar.setNavigationIcon(drawable);
		}
		toolbar.setTitle(R.string.screen_manualconfiguration);
		toolbar.setTitleTextColor(colorSecondary);
		
		toolbar.inflateMenu(R.menu.menu_help);
		for(int i = 0; i < toolbar.getMenu().size(); i++) {
			toolbar.getMenu().getItem(i).getIcon().setColorFilter(colorSecondary, PorterDuff.Mode.SRC_IN);
		}
		toolbar.setOnMenuItemClickListener(item -> {
			Constants.launchUri(getContext(), Constants.serverSetupAddress);
			return true;
		});
		
		toolbar.setNavigationOnClickListener(navigationView -> callback.popStack());
	}
	
	private void applyState() {
		applyState(viewModel.currentState, viewModel.errorDetails);
	}
	
	/**
	 * Applies the current task state to the UI
	 * @param currentState The current state of the activity
	 * @param errorDetails The error details to display, NULL if unavailable
	 */
	private void applyState(int currentState, StateUtils.ErrorDetails errorDetails) {
		TransitionManager.beginDelayedTransition(groupContent);
		
		//Setting the input field states
		inputAddress.getEditText().setEnabled(currentState == stateIdle);
		inputFallback.getEditText().setEnabled(currentState == stateIdle);
		inputPassword.getEditText().setEnabled(currentState == stateIdle);
		
		//Progress bar
		groupProgress.setVisibility(currentState == stateConnecting ? View.VISIBLE : View.GONE);
		
		//Error details
		applyErrorDetails(currentState == stateIdle ? errorDetails : null);
		
		//Connected info
		if(currentState == stateConnected) {
			groupConnected.setVisibility(View.VISIBLE);
			
			String serverDeviceName = ConnectionService.getConnectionManager().getServerDeviceName();
			if(serverDeviceName != null) labelConnected.setText(getResources().getString(R.string.message_connection_connectedcomputer, serverDeviceName));
			else labelConnected.setText(getResources().getString(R.string.message_connection_connected));
		} else {
			groupConnected.setVisibility(View.GONE);
		}
		
		//Next button
		buttonNext.setVisibility(currentState == stateIdle ? View.VISIBLE : View.GONE);
		
		//Cancel and done buttons
		buttonCancel.setVisibility(currentState == stateConnected ? View.VISIBLE : View.GONE);
		buttonDone.setVisibility(currentState == stateConnected ? View.VISIBLE : View.GONE);
	}
	
	/**
	 * Applies the current task's error details to the UI
	 * @param errorDetails The error details to display, NULL if unavailable
	 */
	private void applyErrorDetails(StateUtils.ErrorDetails errorDetails) {
		//Hiding the view if there is no error
		if(errorDetails == null) {
			groupError.setVisibility(View.GONE);
			return;
		}
		
		//Showing the view
		groupError.setVisibility(View.VISIBLE);
		
		//Setting the text
		labelError.setText(errorDetails.getLabel());
		
		//Setting the button
		if(errorDetails.getButton() == null) {
			buttonError.setVisibility(View.GONE);
		} else {
			buttonError.setVisibility(View.GONE);
			buttonError.setText(errorDetails.getButton().getLabel());
			buttonError.setOnClickListener(view -> errorDetails.getButton().getClickListener().accept(requireActivity()));
		}
	}
	
	/**
	 * Evaluates text input and enables or disables the NEXT button accordingly
	 */
	private void updateNextButton() {
		updateNextButton(inputAddress.getEditText().getText().toString(), inputFallback.getEditText().getText().toString(), inputPassword.getEditText().getText().toString());
	}
	
	private void updateNextButton(String address, String fallback, String password) {
		buttonNext.setEnabled(
				Constants.regExValidAddress.matcher(address).find() &&
				(fallback.isEmpty() || Constants.regExValidAddress.matcher(fallback).find()) &&
				!password.isEmpty());
	}
	
	private void startConnection() {
		//Saving the new server information
		viewModel.currentHostname = inputAddress.getEditText().getText().toString();
		viewModel.currentHostnameFallback = inputFallback.getEditText().getText().toString();
		if(viewModel.currentHostnameFallback.isEmpty()) viewModel.currentHostnameFallback = null;
		viewModel.currentPassword = inputPassword.getEditText().getText().toString();
		
		//Setting the values in the service class
		ConnectionManager.hostname = viewModel.currentHostname;
		ConnectionManager.hostnameFallback = viewModel.currentHostnameFallback;
		ConnectionManager.password = viewModel.currentPassword;
		
		//Telling the service to connect
		requireActivity().startService(new Intent(requireContext(), ConnectionService.class)
				.setAction(ConnectionService.selfIntentActionConnect)
				.putExtra(Constants.intentParamLaunchID, viewModel.connectionLaunchID = ConnectionManager.getNextLaunchID())
				.putExtra(ConnectionService.selfIntentExtraConfig, true));
	}
	
	private void complete() {
		//Saving the connection data in the shared preferences
		SharedPreferences.Editor editor = MainApplication.getInstance().getConnectivitySharedPrefs().edit();
		editor.putInt(MainApplication.sharedPreferencesConnectivityKeyAccountType, Constants.connectivityAccountTypeDirect);
		editor.putBoolean(MainApplication.sharedPreferencesConnectivityKeyConnectServerConfirmed, true);
		
		editor.putString(MainApplication.sharedPreferencesConnectivityKeyHostname, viewModel.currentHostname);
		editor.putString(MainApplication.sharedPreferencesConnectivityKeyHostnameFallback, viewModel.currentHostnameFallback);
		editor.putString(MainApplication.sharedPreferencesConnectivityKeyPassword, viewModel.currentPassword);
		editor.apply();
		
		//Finishing the activity
		isCompleting = true;
		callback.launchConversations();
	}
	
	public static class FragmentViewModel extends ViewModel {
		private final String originalHostname;
		private final String originalHostnameFallback;
		private final String originalPassword;
		
		private String currentHostname;
		private String currentHostnameFallback;
		private String currentPassword;
		
		private int currentState = stateIdle;
		private StateUtils.ErrorDetails errorDetails;
		
		private byte connectionLaunchID;
		
		public FragmentViewModel() {
			//Recording the original connection information
			originalHostname = ConnectionManager.hostname;
			originalHostnameFallback = ConnectionManager.hostnameFallback;
			originalPassword = ConnectionManager.password;
		}
	}
}