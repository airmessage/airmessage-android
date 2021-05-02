package me.tagavari.airmessage.fragment;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.GeneralSecurityException;
import java.util.Locale;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.activity.Preferences;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.connection.ConnectionOverride;
import me.tagavari.airmessage.data.SharedPreferencesManager;
import me.tagavari.airmessage.enums.ConnectionErrorCode;
import me.tagavari.airmessage.enums.ConnectionState;
import me.tagavari.airmessage.enums.ProxyType;
import me.tagavari.airmessage.extension.FragmentBackOverride;
import me.tagavari.airmessage.extension.FragmentCommunicationNetworkConfig;
import me.tagavari.airmessage.helper.ErrorDetailsHelper;
import me.tagavari.airmessage.helper.ResourceHelper;
import me.tagavari.airmessage.helper.StringHelper;
import me.tagavari.airmessage.redux.ReduxEmitterNetwork;
import me.tagavari.airmessage.redux.ReduxEventConnection;
import me.tagavari.airmessage.util.ConnectionParams;

public class FragmentOnboardingConnect extends FragmentCommunication<FragmentCommunicationNetworkConfig> implements FragmentBackOverride {
	//Creating the view values
	private ViewGroup groupContent;
	
	private ViewGroup groupProgress;
	
	private ViewGroup groupError;
	private TextView labelError;
	private Button buttonError;
	
	private ViewGroup groupAuth;
	private TextInputLayout inputAuth;
	private Button buttonAuth;
	
	//Creating the fragment values
	private FragmentViewModel viewModel;
	
	//Creating the disposable values
	private final CompositeDisposable compositeDisposable = new CompositeDisposable();
	
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
		
		groupAuth = groupContent.findViewById(R.id.layout_auth);
		inputAuth = groupContent.findViewById(R.id.input_auth);
		buttonAuth = groupContent.findViewById(R.id.button_auth);
		
		inputAuth.getEditText().addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				buttonAuth.setEnabled(s.length() > 0);
				inputAuth.setErrorEnabled(false);
			}
			
			@Override
			public void afterTextChanged(Editable s) {}
		});
		buttonAuth.setOnClickListener(buttonView -> {
			startConnection();
			viewModel.passwordEntered = true;
		});
		
		//Setting up the toolbar
		configureToolbar(view.findViewById(R.id.toolbar));
		
		//Starting the connection if this is the first launch
		if(viewModel.isFirstLaunch) {
			startConnection();
			viewModel.isFirstLaunch = false;
		}
		
		//Updating the state
		applyState();
		
		//Subscribing to connection updates
		compositeDisposable.add(ReduxEmitterNetwork.getConnectionStateSubject().subscribe(this::onConnectionUpdate));
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		//Clearing the composite disposable
		compositeDisposable.clear();
	}
	
	@Override
	public boolean onBackPressed() {
		promptExit();
		
		return true;
	}
	
	private void onConnectionUpdate(ReduxEventConnection event) {
		//Checking if we haven't connected yet, and the connection service is disconnected
		if(viewModel.state == State.waiting && viewModel.isFirstLaunch && event.getState() == ConnectionState.disconnected) {
			//Connecting
			startConnection();
			viewModel.isFirstLaunch = false;
		}
		//Checking if we're waiting for a response
		else if(viewModel.state == State.connecting) {
			//Getting the result
			if(event.getState() == ConnectionState.connected) {
				//Finishing
				complete();
			} else if(event.getState() == ConnectionState.disconnected) {
				//Getting the error code
				int errorCode = ((ReduxEventConnection.Disconnected) event).getCode();
				
				if(errorCode == ConnectionErrorCode.unauthorized) {
					//Setting the state as unauthorized
					viewModel.state = State.auth;
				} else {
					//Setting the state as errored
					viewModel.state = State.error;
					
					//Setting the error
					viewModel.errorDetails = ErrorDetailsHelper.getErrorDetails(errorCode, true);
				}
				
				//Updating the state
				applyState();
			}
		}
	}
	
	/**
	 * Applies the current task state to the UI
	 */
	private void applyState() {
		TransitionManager.beginDelayedTransition(groupContent);
		
		if(viewModel.state == State.waiting || viewModel.state == State.connecting) {
			//Showing the progress group
			groupProgress.setVisibility(View.VISIBLE);
			groupError.setVisibility(View.GONE);
			groupAuth.setVisibility(View.GONE);
		} else if(viewModel.state == State.error) {
			//Showing the error group
			groupProgress.setVisibility(View.GONE);
			groupError.setVisibility(View.VISIBLE);
			groupAuth.setVisibility(View.GONE);
			
			//Updating the error
			applyErrorDetails(viewModel.errorDetails);
		} else if(viewModel.state == State.auth) {
			//Showing the auth group
			groupProgress.setVisibility(View.GONE);
			groupError.setVisibility(View.GONE);
			groupAuth.setVisibility(View.VISIBLE);
			
			inputAuth.setError(getResources().getString(R.string.message_serverstatus_authfail));
			inputAuth.setErrorEnabled(viewModel.passwordEntered);
		}
	}
	
	/**
	 * Applies the current task's error details to the UI
	 * @param errorDetails The error details to display, NULL if unavailable
	 */
	private void applyErrorDetails(ErrorDetailsHelper.ErrorDetails errorDetails) {
		//Setting the text
		labelError.setText(errorDetails.getLabel());
		
		//Setting the button
		if(errorDetails.getButton() == null) {
			//Defaulting to "retry" if no action was provided
			buttonError.setText(R.string.action_retry);
			buttonError.setOnClickListener(view -> startConnection());
		} else {
			buttonError.setText(errorDetails.getButton().getLabel());
			buttonError.setOnClickListener(view -> errorDetails.getButton().getClickListener().accept(requireActivity(), getCommunicationsCallback().getConnectionManager()));
		}
	}
	
	private void startConnection() {
		//Getting the connection manager
		ConnectionManager connectionManager = getCommunicationsCallback().getConnectionManager();
		if(connectionManager == null) return;
		
		//Setting the override and connecting
		String inputPassword = inputAuth.getEditText().getText().toString();
		ConnectionParams params;
		if(!TextUtils.isEmpty(inputPassword)) {
			params = new ConnectionParams.Security(inputPassword.trim());
		} else {
			params = null;
		}
		connectionManager.setConnectionOverride(new ConnectionOverride<>(ProxyType.connect, params));
		connectionManager.connect();
		
		//Updating the state
		viewModel.state = State.connecting;
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
		int colorSecondary = ResourceHelper.resolveColorAttr(getContext(), android.R.attr.textColorSecondary);
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
		getCommunicationsCallback().popStack();
	}
	
	private void complete() {
		//Saving the connection data in the shared preferences
		SharedPreferencesManager.setProxyType(getContext(), ProxyType.connect);
		SharedPreferencesManager.setConnectionConfigured(getContext(), true);
		try {
			SharedPreferencesManager.setDirectConnectionPassword(getContext(), StringHelper.nullifyEmptyString(inputAuth.getEditText().getText().toString().trim()));
		} catch(GeneralSecurityException | IOException exception) {
			exception.printStackTrace();
			FirebaseCrashlytics.getInstance().recordException(exception);
		}
		
		//Disabling the connection on boot
		Preferences.updateConnectionServiceBootEnabled(getContext(), false);
		
		//Finishing the activity
		getCommunicationsCallback().launchConversations();
	}
	
	public static class FragmentViewModel extends ViewModel {
		boolean isFirstLaunch = true;
		@State int state = State.waiting;
		ErrorDetailsHelper.ErrorDetails errorDetails = null;
		boolean passwordEntered = false;
		
		public FragmentViewModel() {
		}
	}
	
	@Retention(RetentionPolicy.SOURCE)
	@IntDef({State.waiting, State.connecting, State.error})
	private @interface State {
		int waiting = 0;
		int connecting = 1;
		int error = 2;
		int auth = 3;
	}
}