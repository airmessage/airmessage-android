package me.tagavari.airmessage.fragment;

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
import com.google.android.material.textfield.TextInputLayout;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.activity.Preferences;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.connection.ConnectionOverride;
import me.tagavari.airmessage.constants.ExternalLinkConstants;
import me.tagavari.airmessage.constants.RegexConstants;
import me.tagavari.airmessage.data.SharedPreferencesManager;
import me.tagavari.airmessage.enums.ConnectionErrorCode;
import me.tagavari.airmessage.enums.ConnectionState;
import me.tagavari.airmessage.enums.ProxyType;
import me.tagavari.airmessage.extension.FragmentCommunicationNetworkConfig;
import me.tagavari.airmessage.helper.ErrorDetailsHelper;
import me.tagavari.airmessage.helper.IntentHelper;
import me.tagavari.airmessage.helper.ResourceHelper;
import me.tagavari.airmessage.helper.StringHelper;
import me.tagavari.airmessage.redux.ReduxEmitterNetwork;
import me.tagavari.airmessage.redux.ReduxEventConnection;
import me.tagavari.airmessage.service.ConnectionService;
import me.tagavari.airmessage.util.ConnectionParams;
import me.tagavari.airmessage.util.DirectConnectionDetails;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class FragmentOnboardingManual extends FragmentCommunication<FragmentCommunicationNetworkConfig> {
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
	
	//Creating the disposable values
	private final CompositeDisposable compositeDisposable = new CompositeDisposable();
	
	//Creating the listener values
	private final TextWatcher addressInputWatcher = new TextWatcher() {
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			//Updating the next button
			updateNextButton(s.toString(), inputFallback.getEditText().getText().toString(), inputPassword.getEditText().getText().toString());
			
			//Checking for the error solution as long as there is an error
			if(inputAddress.getError() != null && RegexConstants.internetAddress.matcher(s).find()) inputAddress.setError(null);
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
			if(inputFallback.getError() != null && (s.length() == 0 || RegexConstants.internetAddress.matcher(s).find())) inputFallback.setError(null);
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
			textInputLayout.setError(!input.isEmpty() && !RegexConstants.internetAddress.matcher(input).find() ? getResources().getString(R.string.part_invalidaddress) : null);
		}
	}
	private final View.OnClickListener nextButtonListener = view -> {
		//Ignoring if the connection state is not idle
		if(getCommunicationsCallback().getConnectionManager() == null ||
				getCommunicationsCallback().getConnectionManager().getState() != ConnectionState.disconnected) return;
		
		//Updating the state
		viewModel.currentState = stateConnecting;
		viewModel.errorDetails = null;
		applyState();
		
		//Starting the connection
		startConnection();
	};
	private final View.OnClickListener cancelButtonListener = view -> {
		//Resetting the connection
		ConnectionManager connectionManager = getCommunicationsCallback().getConnectionManager();
		if(connectionManager != null) connectionManager.disconnect(ConnectionErrorCode.user);
		
		//Reverting the state to idle
		viewModel.currentState = stateIdle;
		applyState();
	};
	private final View.OnClickListener doneButtonListener = view -> {
		//Finishing
		complete();
	};
	
	//Creating the state values
	private boolean isComplete = false;
	
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Getting the view model
		viewModel = new ViewModelProvider(this).get(FragmentViewModel.class);
	}
	
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_onboarding_manual, container, false);
	}
	
	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
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
		try {
			DirectConnectionDetails details = SharedPreferencesManager.getDirectConnectionDetails(requireContext());
			inputAddress.getEditText().setText(details.getAddress());
			inputFallback.getEditText().setText(details.getFallbackAddress());
			inputPassword.getEditText().setText(details.getPassword());
		} catch(IOException | GeneralSecurityException exception) {
			exception.printStackTrace();
		}
		
		//Subscribing to connection updates
		compositeDisposable.add(ReduxEmitterNetwork.getConnectionStateSubject().subscribe(this::onConnectionUpdate));
		
		//Updating the state
		applyState();
	}
	
	@Override
	public void onStop() {
		super.onStop();
		
		//Disconnecting
		if(!isComplete) {
			ConnectionManager connectionManager = getCommunicationsCallback().getConnectionManager();
			if(connectionManager != null) connectionManager.disconnect(ConnectionErrorCode.user);
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		//Clearing the composite disposable
		compositeDisposable.clear();
	}
	
	private void configureToolbar(Toolbar toolbar) {
		int colorSecondary = ResourceHelper.resolveColorAttr(getContext(), android.R.attr.textColorSecondary);
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
			IntentHelper.launchUri(getContext(), ExternalLinkConstants.serverSetupAddress);
			return true;
		});
		
		toolbar.setNavigationOnClickListener(navigationView -> getCommunicationsCallback().popStack());
	}
	
	private void onConnectionUpdate(ReduxEventConnection event) {
		//Checking if the activity is waiting for a response
		if(viewModel.currentState == stateConnecting) {
			//Getting the result
			if(event.getState() == ConnectionState.connected) {
				//Setting the state as connected
				viewModel.currentState = stateConnected;
			} else if(event.getState() == ConnectionState.disconnected) {
				//Setting the state as idle
				viewModel.currentState = stateIdle;
				
				//Setting the error
				int errorCode = ((ReduxEventConnection.Disconnected) event).getCode();
				viewModel.errorDetails = ErrorDetailsHelper.getErrorDetails(errorCode, true);
			}
			
			//Updating the state
			applyState();
		}
		//Checking if the activity is connected, and the service is disconnected
		else if(viewModel.currentState == stateConnected && event.getState() == ConnectionState.disconnected) {
			//Cancelling the connection
			viewModel.currentState = stateIdle;
			applyState();
		}
	}
	
	private void applyState() {
		applyState(viewModel.currentState, viewModel.errorDetails);
	}
	
	/**
	 * Applies the current task state to the UI
	 * @param currentState The current state of the activity
	 * @param errorDetails The error details to display, NULL if unavailable
	 */
	private void applyState(int currentState, ErrorDetailsHelper.ErrorDetails errorDetails) {
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
	private void applyErrorDetails(ErrorDetailsHelper.ErrorDetails errorDetails) {
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
			buttonError.setOnClickListener(view -> errorDetails.getButton().getClickListener().invoke(getActivity(), getActivity().getSupportFragmentManager(), getCommunicationsCallback().getConnectionManager()));
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
				RegexConstants.internetAddress.matcher(address).find() &&
				(fallback.isEmpty() || RegexConstants.internetAddress.matcher(fallback).find()) &&
				!password.isEmpty());
	}
	
	private void startConnection() {
		//Collecting the new server connection information
		ConnectionParams.Direct params = new ConnectionParams.Direct(
				inputAddress.getEditText().getText().toString(),
				StringHelper.nullifyEmptyString(inputFallback.getEditText().getText().toString()),
				inputPassword.getEditText().getText().toString()
		);
		
		viewModel.connectionParams = params;
		
		//Getting the connection manager
		ConnectionManager connectionManager = getCommunicationsCallback().getConnectionManager();
		if(connectionManager == null) return;
		
		//Setting the override and connecting
		connectionManager.setConnectionOverride(new ConnectionOverride<>(ProxyType.direct, params));
		connectionManager.connect();
	}
	
	private void complete() {
		//Saving the connection data in the shared preferences
		SharedPreferencesManager.setProxyType(getContext(), ProxyType.direct);
		try {
			SharedPreferencesManager.setDirectConnectionDetails(getContext(), viewModel.connectionParams);
		} catch(IOException | GeneralSecurityException exception) {
			exception.printStackTrace();
			return;
		}
		SharedPreferencesManager.setConnectionConfigured(getContext(), true);
		
		//Enabling connect on boot
		Preferences.updateConnectionServiceBootEnabled(getContext(), Preferences.getPreferenceStartOnBoot(getContext()));
		
		//Finishing the activity
		isComplete = true;
		getCommunicationsCallback().launchConversations();
	}
	
	public static class FragmentViewModel extends ViewModel {
		ConnectionParams.Direct connectionParams;
		
		int currentState = stateIdle;
		ErrorDetailsHelper.ErrorDetails errorDetails;
	}
}