package me.tagavari.airmessage;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.regex.Pattern;

public class ServerSetup extends Activity {
	//Creating the reference values
	static final String intentExtraRequired = "isRequired";
	
	//Creating the regular expression string
	private static final Pattern regExValidAddress = Pattern.compile("^(ws(s?)://)?(((www\\.)?+[a-zA-Z0-9\\.\\-\\_]+(\\.[a-zA-Z]{2,3})+)|(\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b))(/[a-zA-Z0-9_\\-\\s./?%#&=]*)?(:([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5]?))?$");
	//private static final Pattern regExValidPort = Pattern.compile("(:([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5]?))$");
	//private static final Pattern regExValidProtocol = Pattern.compile("^ws(s?)://");
	private final InputFilter credentialInputFilter = (CharSequence source, int start, int end, Spanned dest, int dstart, int dend) -> {
		//Denying the input if it contains a space
		if(source.toString().contains(" ")) return "";
		return null;
	};
	//Creating the view values
	private EditText hostnameInputField;
	private EditText passwordInputField;
	private View nextButton;
	//Creating the listener values
	private final TextWatcher addressInputWatcher = new TextWatcher() {
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		
		}
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			//Comparing the string to the regular expression
			if(regExValidAddress.matcher(s).find()) {
				//Enabling the button
				nextButton.setAlpha(1);
				nextButton.setClickable(true);
			} else {
				//Disabling the button
				nextButton.setAlpha(0.38F);
				nextButton.setClickable(false);
			}
		}
		
		@Override
		public void afterTextChanged(Editable s) {
		}
	};
	private View layoutAddress;
	private View layoutSync;
	
	//Creating the other values
	private boolean isRequired;
	private String newHostname;
	private String newPassword;
	/* Stages
	0 - Server verification
	1 - Message copy confirmation
	 */
	private int page = 0;
	private byte connectionLaunchID;
	private BroadcastReceiver serviceBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			//Ignoring the broadcast if the launch ID doesn't match
			if(intent.getByteExtra(Constants.intentParamLaunchID, (byte) 0) != connectionLaunchID) return;
			
			//Unregistering the receiver
			LocalBroadcastManager.getInstance(ServerSetup.this).unregisterReceiver(this);
			
			//Re-enabling the next button
			findViewById(R.id.nextbuttonarrow).setVisibility(View.VISIBLE);
			findViewById(R.id.nextbuttonprogress).setVisibility(View.GONE);
			nextButton.setAlpha(1);
			nextButton.setClickable(true);
			
			//Getting the result
			byte result = intent.getByteExtra(Constants.intentParamResult, ConnectionService.intentResultValueConnection);
			if(result == ConnectionService.intentResultValueSuccess) {
				//Advancing the page
				setPage(1);
				hideSoftKeyboard();
			} else {
				//Showing the error dialog
				showErrorDialog(result);
				
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
		Constants.enforceContentWidth(getResources(), findViewById(R.id.content));
		
		//Setting the address box watcher
		hostnameInputField.addTextChangedListener(addressInputWatcher);
		
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
	}
	
	@Override
	protected void onStop() {
		//Calling the super method
		super.onStop();
		
		//Unregistering the receiver
		LocalBroadcastManager.getInstance(ServerSetup.this).unregisterReceiver(serviceBroadcastReceiver);
	}
	
	public void onNextButtonClick(View view) {
		//Checking if the page is 0
		if(page == 0) {
			//Checking if the server address is valid
			if(regExValidAddress.matcher(hostnameInputField.getText()).find()) {
				//Starting the connection
				startConnection();
				
				//Setting the connection data
				//Starting the service
				///startService(new Intent(this, ConnectionService.class));
				
				//Switching the next button for a loading button
				findViewById(R.id.nextbuttonarrow).setVisibility(View.GONE);
				findViewById(R.id.nextbuttonprogress).setVisibility(View.VISIBLE);
				nextButton.setAlpha(0.38F);
				nextButton.setClickable(false);
				
				//Disabling the input fields
				hostnameInputField.setEnabled(false);
				passwordInputField.setEnabled(false);
			}
		}
		//Otherwise checking if the page is 1
		else if(page == 1) {
			//Checking if there are messages
			if(ConversationManager.getConversations() != null) {
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
		editor.putString(MainApplication.sharedPreferencesConnectivityKeyHostname, newHostname); //The raw, unprocessed hostname (No protocol or port)
		editor.putString(MainApplication.sharedPreferencesConnectivityKeyPassword, newPassword);
		editor.apply();
		
		//Starting the new activity
		startActivity(new Intent(this, Conversations.class));
		
		//Enabling transitions
		overridePendingTransition(R.anim.fade_in_light, R.anim.activity_slide_up);
		
		//Finishing the activity
		finish();
	}
	
	private void setPage(int newPage) {
		//Returning if the page matches
		if(page == newPage) return;
		
		//Switching the page
		switch(newPage) {
			case 0:
				//Sliding the layout left
				layoutAddress.setX(-layoutAddress.getWidth());
				layoutAddress.animate().translationX(0);
				
				layoutSync.setX(0);
				layoutSync.animate().translationX(layoutSync.getWidth());
				
				//Enabling the input fields
				hostnameInputField.setEnabled(true);
				passwordInputField.setEnabled(true);
				
				//Setting the "NEXT" button
				((TextView) findViewById(R.id.nextbuttonlabel)).setText(R.string.action_next);
				if(isRequired) findViewById(R.id.backbutton).setVisibility(View.GONE);
				
				break;
			case 1:
				//Sliding the layout right
				layoutAddress.setX(0);
				layoutAddress.animate().translationX(-layoutAddress.getWidth());
				
				layoutSync.setX(layoutSync.getWidth());
				layoutSync.animate().translationX(0);
				layoutSync.setVisibility(View.VISIBLE);
				
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
		page = newPage;
	}
	
	private void startConnection() {
		//Saving the new server information
		newHostname = hostnameInputField.getText().toString();
		newPassword = passwordInputField.getText().toString();
		
		//Setting the values in the service class
		ConnectionService.hostname = newHostname;
		ConnectionService.password = newPassword;
		
		//Telling the service to connect
		startService(new Intent(this, ConnectionService.class).setAction(ConnectionService.selfIntentActionConnect).putExtra(Constants.intentParamLaunchID, connectionLaunchID = ConnectionService.getNextLaunchID()));
		
		//Adding the broadcast listener
		LocalBroadcastManager.getInstance(this).registerReceiver(serviceBroadcastReceiver, new IntentFilter(ConnectionService.localBCResult));
	}
	
	public void onBackButtonClick(View view) {
		switch(page) {
			case 0:
				//Finishing the activity (if the server change isn't required)
				if(!isRequired) finish();
				
				break;
			case 1:
				//Retracting the page
				setPage(0);
				
				//Disconnecting from the server
				/* ConnectionService connectionService = ConnectionService.getInstance();
				if(connectionService != null) connectionService.disconnect(); */
				
				break;
		}
	}
	
	@Override
	public void onBackPressed() {
		//Retracting the page
		if(page > 0) setPage(page - 1);
		else super.onBackPressed();
	}
	
	private void hideSoftKeyboard() {
		View view = this.getCurrentFocus();
		if(view != null) {
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
	}
	
	private void showErrorDialog(byte reason) {
		//Creating the alert dialog variable
		AlertDialog alertDialog = null;
		
		switch(reason) {
			case ConnectionService.intentResultValueInternalException: //Internal exception
				alertDialog = new AlertDialog.Builder(this)
						.setTitle(R.string.message_setup_connect_connectionerror)
						.setMessage(R.string.message_serverstatus_internalexception)
						.setPositiveButton(R.string.action_dismiss, (dialog, which) -> dialog.dismiss())
						.create();
				break;
			case ConnectionService.intentResultValueBadRequest: //Bad request
				alertDialog = new AlertDialog.Builder(this)
						.setTitle(R.string.message_setup_connect_connectionerror)
						.setMessage(R.string.message_serverstatus_badrequest)
						.setPositiveButton(R.string.action_dismiss, (dialog, which) -> dialog.dismiss())
						.create();
				break;
			case ConnectionService.intentResultValueConnection: //Connection failed
				alertDialog = new AlertDialog.Builder(this)
						.setTitle(R.string.message_setup_connect_connectionerror)
						.setMessage(R.string.message_connectionerrror)
						.setPositiveButton(R.string.action_dismiss, (dialog, which) -> dialog.dismiss())
						.create();
				break;
			case ConnectionService.intentResultValueUnauthorized: //Authentication failed
				alertDialog = new AlertDialog.Builder(this)
						.setTitle(R.string.message_setup_connect_connectionerror)
						.setMessage(R.string.message_serverstatus_authfail)
						.setPositiveButton(R.string.action_dismiss, (dialog, which) -> dialog.dismiss())
						.create();
				break;
			case ConnectionService.intentResultValueClientOutdated: //Client outdated
				alertDialog = new AlertDialog.Builder(this)
						.setTitle(R.string.message_setup_connect_connectionerror)
						.setMessage(R.string.message_serverstatus_clientoutdated)
						.setPositiveButton(R.string.action_update, (dialog, which) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName()))))
						.setNegativeButton(R.string.action_dismiss, (dialog, which) -> dialog.dismiss())
						.create();
				break;
			case ConnectionService.intentResultValueServerOutdated: //Server outdated
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
		if(requestAfter) new ConversationsBase.SyncMessagesTask(getApplicationContext(), null).execute();
		else new ConversationsBase.DeleteMessagesTask(getApplicationContext()).execute();
		
		//Finishing the activity
		finishSetup();
	}
	
	public void onClickLaunchServerGuide(View view) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Constants.serverSetupAddress);
		
		if(intent.resolveActivity(getPackageManager()) != null) startActivity(intent);
		else Toast.makeText(this, R.string.message_intenterror_browser, Toast.LENGTH_SHORT).show();
		startActivity(intent);
	}
}