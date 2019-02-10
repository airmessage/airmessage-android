package me.tagavari.airmessage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.regex.Pattern;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class ServerSetup extends AppCompatActivity {
	//Creating the reference values
	static final String intentExtraRequired = "isRequired";
	
	//Creating the regular expression string
	private static final Pattern regExValidAddress = Pattern.compile("^(((www\\.)?+[a-zA-Z0-9.\\-_]+(\\.[a-zA-Z]{2,3})+)|(\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b))(/[a-zA-Z0-9_\\-\\s./?%#&=]*)?(:([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5]?))?$");
	//private static final Pattern regExValidPort = Pattern.compile("(:([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5]?))$");
	//private static final Pattern regExValidProtocol = Pattern.compile("^ws(s?)://");
	private final InputFilter credentialInputFilter = (CharSequence source, int start, int end, Spanned dest, int dstart, int dend) -> {
		//Denying the input if it contains a space
		if(source.toString().contains(" ")) return "";
		return null;
	};
	
	//Creating the activity values
	private ActivityViewModel viewModel;
	private boolean isComplete = false;
	
	//Creating the view values
	private EditText hostnameInputField;
	private EditText passwordInputField;
	private View nextButton;
	
	//Creating the listener values
	private final TextWatcher hostnameInputWatcher = new TextWatcher() {
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			//Updating the next button
			updateNextButtonState(s.toString(), passwordInputField.getText().toString());
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
			updateNextButtonState(hostnameInputField.getText().toString(), s.toString());
		}
		
		@Override
		public void afterTextChanged(Editable s) {}
	};
	private View layoutAddress;
	private View layoutSync;
	
	//Creating the other values
	private boolean isRequired;
	
	private BroadcastReceiver serviceBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			//Ignoring the broadcast if the launch ID doesn't match or the result is connecting
			int state;
			if(intent.getByteExtra(Constants.intentParamLaunchID, (byte) 0) != viewModel.connectionLaunchID ||
					(state = intent.getIntExtra(Constants.intentParamState, -1)) == ConnectionService.stateConnecting) return;
			
			//Unregistering the receiver
			LocalBroadcastManager.getInstance(ServerSetup.this).unregisterReceiver(this);
			
			//Re-enabling the next button
			setLoading(false);
			
			//Getting the result
			if(state == ConnectionService.stateConnected) {
				//Advancing the page
				setPage(ActivityViewModel.pageSync, false);
				hideSoftKeyboard();
			} else {
				//Showing the error dialog
				showErrorDialog(intent.getIntExtra(Constants.intentParamCode, -1));
				
				//Enabling the input fields
				hostnameInputField.setEnabled(true);
				passwordInputField.setEnabled(true);
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Calling the super method
		super.onCreate(savedInstanceState);
		
		//Setting the content view
		setContentView(R.layout.activity_serversetup);
		
		//Getting the views
		hostnameInputField = findViewById(R.id.input_address);
		passwordInputField = findViewById(R.id.input_password);
		nextButton = findViewById(R.id.nextbutton);
		layoutAddress = findViewById(R.id.layout_serververification);
		layoutSync = findViewById(R.id.layout_serversync);
		
		passwordInputField.setFilters(new InputFilter[]{credentialInputFilter});
		passwordInputField.setOnEditorActionListener((view, actionID, event) -> {
			if(actionID == EditorInfo.IME_ACTION_GO) {
				onNextButtonClick(null);
				return true;
			}
			return false;
		});
		
		//Enforcing the maximum content width
		{
			int maxContentWidth = Constants.dpToPx(500);
			ViewGroup content = findViewById(R.id.content);
			for(int i = 0; i < content.getChildCount(); i++) Constants.enforceContentWidth(maxContentWidth, content.getChildAt(i));
		}
		
		//Getting the view models
		viewModel = ViewModelProviders.of(this).get(ActivityViewModel.class);
		
		//Setting the address box watcher
		hostnameInputField.addTextChangedListener(hostnameInputWatcher);
		passwordInputField.addTextChangedListener(passwordInputWatcher);
		
		//Getting if the change is required
		isRequired = getIntent().getBooleanExtra(intentExtraRequired, false);
		
		//Enabling the back button if the change is not required
		if(!isRequired) findViewById(R.id.backbutton).setVisibility(View.VISIBLE);
		
		//Disabling the next button (because for some reason the XML isn't enough)
		nextButton.setClickable(false);
		
		//Filling in the input fields with previous information
		SharedPreferences sharedPreferences = ((MainApplication) getApplication()).getConnectivitySharedPrefs();
		hostnameInputField.append(sharedPreferences.getString(MainApplication.sharedPreferencesConnectivityKeyHostname, ""));
		passwordInputField.append(sharedPreferences.getString(MainApplication.sharedPreferencesConnectivityKeyPassword, ""));
		
		//Restoring the current page
		getWindow().getDecorView().post(() -> {
			setPage(viewModel.page, true);
			setLoading(viewModel.isPageLoading);
		});
		
		//Re-registering the receiver
		if(viewModel.page == ActivityViewModel.pageVerification && viewModel.isPageLoading) LocalBroadcastManager.getInstance(this).registerReceiver(serviceBroadcastReceiver, new IntentFilter(ConnectionService.localBCStateUpdate));
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		//Setting the connection values
		if(viewModel.newHostname != null && viewModel.newPassword != null) {
			ConnectionService.hostname = viewModel.newHostname;
			ConnectionService.password = viewModel.newPassword;
		}
		
		//Configuring the header images
		getWindow().getDecorView().post(() -> {
			float windowHeight = Constants.pxToDp(Constants.getWindowHeight(this));
			ImageView imageHeader = findViewById(R.id.image_header);
			if(windowHeight < 577) {
				//Short banner
				imageHeader.setImageResource(R.drawable.onboarding_download_short);
			} else if(windowHeight < 772) {
				//Medium banner
				imageHeader.setImageResource(R.drawable.onboarding_download_medium);
			} else {
				//Tall banner
				imageHeader.setImageResource(R.drawable.onboarding_download_tall);
			}
		});
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		
		if(!isComplete && viewModel.originalHostname != null && viewModel.originalPassword != null) {
			//Restoring the connection values
			ConnectionService.hostname = viewModel.originalHostname;
			ConnectionService.password = viewModel.originalPassword;
			
			//Starting the connection
			ConnectionService service = ConnectionService.getInstance();
			if(service != null) service.reconnect();
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		//Unregistering the receiver
		LocalBroadcastManager.getInstance(ServerSetup.this).unregisterReceiver(serviceBroadcastReceiver);
	}
	
	public void onNextButtonClick(View view) {
		//Checking if the page is the verification page
		if(viewModel.page == ActivityViewModel.pageVerification) {
			//Checking if the server parameters are valid
			if(regExValidAddress.matcher(hostnameInputField.getText()).find() && passwordInputField.getText().length() > 0) {
				//Starting the connection
				startConnection();
				
				//Setting the connection data
				//Starting the service
				///startService(new Intent(this, ConnectionService.class));
				
				//Setting the page as loading
				setLoading(true);
			}
		}
		//Otherwise checking if the page is the synchronization page
		else if(viewModel.page == ActivityViewModel.pageSync) {
			//Checking if there are any messages
			List<ConversationManager.ConversationInfo> conversations = ConversationManager.getConversations();
			if(conversations != null && !conversations.isEmpty()) {
				//Showing a warning
				new AlertDialog.Builder(this)
						.setTitle(R.string.message_setup_sync_warning_title)
						.setMessage(R.string.message_setup_sync_description)
						.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
						.setPositiveButton(R.string.action_skip, (dialog, which) -> finishSetup())
						.create().show();
			} else {
				//Finishing the setup
				finishSetup();
			}
		}
	}
	
	private void finishSetup() {
		//Saving the connection data in the shared preferences
		SharedPreferences.Editor editor = ((MainApplication) getApplication()).getConnectivitySharedPrefs().edit();
		editor.putString(MainApplication.sharedPreferencesConnectivityKeyHostname, viewModel.newHostname); //The raw, unprocessed hostname (No protocol or port)
		editor.putString(MainApplication.sharedPreferencesConnectivityKeyPassword, viewModel.newPassword);
		editor.apply();
		
		//Starting the new activity
		startActivity(new Intent(this, Conversations.class));
		
		//Enabling transitions
		overridePendingTransition(R.anim.fade_in_light, R.anim.activity_slide_up);
		
		//Finishing the activity
		isComplete = true;
		finish();
	}
	
	private void setPage(int newPage, boolean restore) {
		//Returning if the page matches
		if(!restore && viewModel.page == newPage) return;
		
		//Switching the page
		switch(newPage) {
			case 0:
				layoutAddress.setVisibility(View.VISIBLE);
				
				if(restore) {
					layoutSync.setVisibility(View.GONE);
				} else {
					//Sliding the layout left
					layoutAddress.setX(-layoutAddress.getWidth());
					layoutAddress.animate().translationX(0);
					
					layoutSync.setX(0);
					layoutSync.animate().translationX(layoutSync.getWidth());
				}
				
				//Enabling the input fields
				hostnameInputField.setEnabled(true);
				passwordInputField.setEnabled(true);
				
				//Setting the "NEXT" button
				((TextView) findViewById(R.id.nextbuttonlabel)).setText(R.string.action_next);
				if(isRequired) findViewById(R.id.backbutton).setVisibility(View.GONE);
				
				break;
			case 1:
				layoutSync.setVisibility(View.VISIBLE);
				
				if(restore) {
					layoutAddress.setVisibility(View.GONE);
				} else {
					//Sliding the layout right
					layoutAddress.setX(0);
					layoutAddress.animate().translationX(-layoutAddress.getWidth());
					
					layoutSync.setX(layoutSync.getWidth());
					layoutSync.animate().translationX(0);
				}
				
				//Updating the "delete messages" section
				View deleteMessagesLabel = layoutSync.findViewById(R.id.deletemessages_label);
				View deleteMessagesButton = layoutSync.findViewById(R.id.deletemessages_button);
				
				List<ConversationManager.ConversationInfo> conversations = ConversationManager.getConversations();
				if(conversations == null || conversations.isEmpty()) {
					deleteMessagesLabel.setVisibility(View.GONE);
					deleteMessagesButton.setVisibility(View.GONE);
				} else {
					deleteMessagesLabel.setVisibility(View.VISIBLE);
					deleteMessagesButton.setVisibility(View.VISIBLE);
				}
				
				//Setting the "SKIP" button
				((TextView) findViewById(R.id.nextbuttonlabel)).setText(R.string.action_skip);
				findViewById(R.id.backbutton).setVisibility(View.VISIBLE);
				
				break;
		}
		
		//Updating the page
		viewModel.page = newPage;
	}
	
	private void setLoading(boolean loading) {
		//Switching the next button for a loading button
		findViewById(R.id.nextbuttonarrow).setVisibility(loading ? View.GONE : View.VISIBLE);
		findViewById(R.id.nextbuttonprogress).setVisibility(loading ? View.VISIBLE : View.GONE);
		nextButton.setAlpha(loading ? 0.38F : 1);
		nextButton.setClickable(!loading);
		
		//Setting the input fields' state
		hostnameInputField.setEnabled(!loading);
		passwordInputField.setEnabled(!loading);
		
		//Setting the loading state for restore
		viewModel.isPageLoading = loading;
	}
	
	private void startConnection() {
		//Saving the new server information
		viewModel.newHostname = hostnameInputField.getText().toString();
		viewModel.newPassword = passwordInputField.getText().toString();
		
		//Setting the values in the service class
		ConnectionService.hostname = viewModel.newHostname;
		ConnectionService.password = viewModel.newPassword;
		
		//Telling the service to connect
		startService(new Intent(this, ConnectionService.class).setAction(ConnectionService.selfIntentActionConnect).putExtra(Constants.intentParamLaunchID, viewModel.connectionLaunchID = ConnectionService.getNextLaunchID()));
		
		//Adding the broadcast listener
		LocalBroadcastManager.getInstance(this).registerReceiver(serviceBroadcastReceiver, new IntentFilter(ConnectionService.localBCStateUpdate));
	}
	
	public void onBackButtonClick(View view) {
		//Retracting the page
		if(viewModel.page > 0) setPage(viewModel.page - 1, false);
		//Otherwise finishing the activity (if the server change isn't required)
		else if(!isRequired) finish();
	}
	
	@Override
	public void onBackPressed() {
		//Retracting the page
		if(viewModel.page > 0) setPage(viewModel.page - 1, false);
		else super.onBackPressed();
	}
	
	private void hideSoftKeyboard() {
		View view = this.getCurrentFocus();
		if(view != null) {
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
	}
	
	private void showErrorDialog(int reason) {
		//Creating the alert dialog variable
		AlertDialog alertDialog = null;
		
		switch(reason) {
			case ConnectionService.intentResultCodeInternalException: //Internal exception
				alertDialog = new AlertDialog.Builder(this)
						.setTitle(R.string.message_setup_connect_connectionerror)
						.setMessage(R.string.message_serverstatus_internalexception)
						.setPositiveButton(R.string.action_dismiss, (dialog, which) -> dialog.dismiss())
						.create();
				break;
			case ConnectionService.intentResultCodeBadRequest: //Bad request
				alertDialog = new AlertDialog.Builder(this)
						.setTitle(R.string.message_setup_connect_connectionerror)
						.setMessage(R.string.message_serverstatus_badrequest)
						.setPositiveButton(R.string.action_dismiss, (dialog, which) -> dialog.dismiss())
						.create();
				break;
			case ConnectionService.intentResultCodeConnection: //Connection failed
				alertDialog = new AlertDialog.Builder(this)
						.setTitle(R.string.message_setup_connect_connectionerror)
						.setMessage(R.string.message_connectionerrror)
						.setPositiveButton(R.string.action_dismiss, (dialog, which) -> dialog.dismiss())
						.create();
				break;
			case ConnectionService.intentResultCodeUnauthorized: //Authentication failed
				alertDialog = new AlertDialog.Builder(this)
						.setTitle(R.string.message_setup_connect_connectionerror)
						.setMessage(R.string.message_serverstatus_authfail)
						.setPositiveButton(R.string.action_dismiss, (dialog, which) -> dialog.dismiss())
						.create();
				break;
			case ConnectionService.intentResultCodeClientOutdated: //Client outdated
				alertDialog = new AlertDialog.Builder(this)
						.setTitle(R.string.message_setup_connect_connectionerror)
						.setMessage(R.string.message_serverstatus_clientoutdated)
						.setPositiveButton(R.string.action_update, (dialog, which) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName()))))
						.setNegativeButton(R.string.action_dismiss, (dialog, which) -> dialog.dismiss())
						.create();
				break;
			case ConnectionService.intentResultCodeServerOutdated: //Server outdated
				alertDialog = new AlertDialog.Builder(this)
						.setTitle(R.string.message_setup_connect_connectionerror)
						.setMessage(R.string.message_serverstatus_serveroutdated)
						.setPositiveButton(R.string.screen_help, (dialog, which) -> startActivity(new Intent(Intent.ACTION_VIEW, Constants.serverUpdateAddress)))
						.setNegativeButton(R.string.action_dismiss, (dialog, which) -> dialog.dismiss())
						.create();
				break;
		}
		
		//Displaying the dialog
		if(alertDialog != null) alertDialog.show();
	}
	
	public void buttonSyncMessages(View view) {
		//Deleting the messages
		deleteMessages(true);
	}
	
	public void buttonDeleteMessages(View view) {
		//Deleting the messages
		deleteMessages(false);
	}
	
	private void deleteMessages(final boolean requestAfter) {
		//Deleting / syncing the messages
		if(requestAfter) new ConversationsBase.SyncMessagesTask(getApplicationContext(), null, new ConnectionService.MassRetrievalParams()).execute();
		else new ConversationsBase.DeleteMessagesTask(getApplicationContext()).execute();
		
		//Finishing the activity
		finishSetup();
	}
	
	private void updateNextButtonState(String hostnameInput, String passwordInput) {
		//Comparing the string to the regular expression
		if(regExValidAddress.matcher(hostnameInput).find() && !passwordInput.isEmpty()) {
			//Enabling the button
			nextButton.setAlpha(1);
			nextButton.setClickable(true);
		} else {
			//Disabling the button
			nextButton.setAlpha(0.38F);
			nextButton.setClickable(false);
		}
	}
	
	public void onClickLaunchServerGuide(View view) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Constants.serverSetupAddress);
		
		if(intent.resolveActivity(getPackageManager()) != null) startActivity(intent);
		else Toast.makeText(this, R.string.message_intenterror_browser, Toast.LENGTH_SHORT).show();
		startActivity(intent);
	}
	
	public static class ActivityViewModel extends ViewModel {
		private final String originalHostname;
		private final String originalPassword;
		
		public ActivityViewModel() {
			//Recording the original connection information
			originalHostname = ConnectionService.hostname;
			originalPassword = ConnectionService.password;
		}
		
		private String newHostname = null;
		private String newPassword = null;
		
		private static final int pageVerification = 0;
		private static final int pageSync = 1;
		private int page = pageVerification;
		private boolean isPageLoading = false;
		
		private byte connectionLaunchID;
	}
}