package me.tagavari.airmessage;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.view.MenuItem;

import static me.tagavari.airmessage.R.xml.preferences;

public class Preferences extends PreferenceActivity {
	//Creating the listeners
	SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
		@Override
		public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
			//Finding the preference
			Preference preference = findPreference(key);
			
			//Getting the notification string
			String notificationSound = getResources().getString(R.string.preference_messagenotifications_sound_key);
			
			//Checking if the preference is the notification sound
			if(key.equals(notificationSound)) {
				//Updating the ringtone preference
				updateRingtonePreference(notificationSound, (RingtonePreference) preference);
			}
		}
	};
	Preference.OnPreferenceChangeListener startOnBootChangeListener = (preference, value) -> {
		//Updating the service state
		getPackageManager().setComponentEnabledSetting(new ComponentName(Preferences.this, ConnectionService.ServiceStart.class),
				(boolean) value ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
				PackageManager.DONT_KILL_APP);
		
		//Returning true (to allow the change)
		return true;
	};
	Preference.OnPreferenceClickListener deleteAttachmentsClickListener = preference -> {
		//Creating a dialog
		AlertDialog dialog = new AlertDialog.Builder(Preferences.this)
				//Setting the name
				.setMessage(R.string.message_confirm_deleteattachments)
				//Setting the negative button
				.setNegativeButton(android.R.string.cancel, (DialogInterface dialogInterface, int which) -> {
					//Dismissing the dialog
					dialogInterface.dismiss();
				})
				//Setting the positive button
				.setPositiveButton(R.string.action_delete, (DialogInterface dialogInterface, int which) -> {
							//Deleting the attachments
							new Conversations.DeleteAttachmentsTask(getApplicationContext()).execute();
						}
				)
				//Creating the dialog
				.create();
		
		//Showing the dialog
		dialog.show();
		
		//Returning true
		return true;
	};
	Preference.OnPreferenceClickListener deleteMessagesClickListener = preference -> {
		//Creating a dialog
		AlertDialog dialog = new AlertDialog.Builder(Preferences.this)
				//Setting the name
				.setMessage(R.string.message_confirm_deletemessages)
				//Setting the negative button
				.setNegativeButton(android.R.string.cancel, (DialogInterface dialogInterface, int which) -> {
					//Dismissing the dialog
					dialogInterface.dismiss();
				})
				//Setting the positive button
				.setPositiveButton(R.string.action_delete, (DialogInterface dialogInterface, int which) -> {
					//Deleting the messages
					new Conversations.DeleteMessagesTask(getApplicationContext()).execute();
				})
				//Creating the dialog
				.create();
		
		//Showing the dialog
		dialog.show();
		
		//Returning true
		return true;
	};
	Preference.OnPreferenceClickListener resyncMessagesClickListener = preference -> {
		//Checking if the service is ready
		ConnectionService service = ConnectionService.getInstance();
		if(service == null || !service.isConnected()) {
			//Displaying a snackbar
			Snackbar.make(getListView(), R.string.message_serverstatus_noconnection, Snackbar.LENGTH_LONG).show();
			
			//Returning
			return true;
		}
		
		//Checking if there is already a mass retrieval in progress
		if(service.isMassRetrievalInProgress()) {
			//Displaying a snackbar
			Snackbar.make(getListView(), R.string.message_confirm_resyncmessages_inprogress, Snackbar.LENGTH_LONG).show();
			
			//Returning
			return true;
		}
		
		//Creating a dialog
		AlertDialog dialog = new AlertDialog.Builder(Preferences.this)
				//Setting the text
				.setTitle(R.string.message_confirm_resyncmessages)
				.setMessage(R.string.message_confirm_resyncmessages_description)
				//Setting the negative button
				.setNegativeButton(android.R.string.cancel, (DialogInterface dialogInterface, int which) -> dialogInterface.dismiss())
				//Setting the positive button
				.setPositiveButton(R.string.action_resync, (DialogInterface dialogInterface, int which) -> new Conversations.SyncMessagesTask(getApplicationContext(), getListView()).execute())
				//Creating the dialog
				.create();
		
		//Showing the dialog
		dialog.show();
		
		//Returning true
		return true;
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		//Calling the super method
		super.onCreate(savedInstanceState);
		
		//Setting the content view
		setContentView(R.layout.activity_preferences);
		
		//Enabling the toolbar and up navigation
		setActionBar(findViewById(R.id.toolbar));
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		//Enforcing the maximum content width
		Constants.enforceContentWidth(getResources(), findViewById(android.R.id.list));
		
		//Adding the preferences
		addPreferencesFromResource(preferences);
		
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			//Creating the notification channel intent
			Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
			intent.putExtra(Settings.EXTRA_CHANNEL_ID, MainApplication.notificationChannelMessage);
			intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
			
			//Setting the listener
			findPreference(getResources().getString(R.string.preference_messagenotifications_key)).setIntent(intent);
		} else {
			//Updating the notification information
			String ringtonePreferenceKey = getResources().getString(R.string.preference_messagenotifications_sound_key);
			updateRingtonePreference(ringtonePreferenceKey, (RingtonePreference) findPreference(ringtonePreferenceKey));
		}
		
		//Setting the listeners
		findPreference(getResources().getString(R.string.preference_server_connectionboot_key)).setOnPreferenceChangeListener(startOnBootChangeListener);
		findPreference(getResources().getString(R.string.preference_storage_deleteattachments_key)).setOnPreferenceClickListener(deleteAttachmentsClickListener);
		findPreference(getResources().getString(R.string.preference_storage_deleteall_key)).setOnPreferenceClickListener(deleteMessagesClickListener);
		findPreference(getResources().getString(R.string.preference_storage_deleteattachments_key)).setOnPreferenceClickListener(deleteAttachmentsClickListener);
		findPreference(getResources().getString(R.string.preference_server_resync_key)).setOnPreferenceClickListener(resyncMessagesClickListener);
		
		//Setting the intents
		findPreference(getResources().getString(R.string.preference_server_help_key)).setIntent(new Intent(Intent.ACTION_VIEW, Constants.serverSetupAddress));
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		//Checking if the up button has been pressed
		if(item.getItemId() == android.R.id.home) {
			//Finishing the activity
			finish();
			
			//Returning true
			return true;
		}
		
		//Returning false
		return false;
	}
	
	@Override
	protected void onResume() {
		//Calling the super method
		super.onResume();
		
		//Registering the listener
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(sharedPreferenceListener);
		
		//Updating the server URL
		updateServerURL(findPreference(getResources().getString(R.string.preference_server_serverdetails_key)));
		
		//Updating the notification preference
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			updateMessageNotificationPreference(findPreference(getResources().getString(R.string.preference_messagenotifications_key)));
	}
	
	@Override
	protected void onStop() {
		//Calling the super method
		super.onStop();
		
		//Unregistering the listener
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(sharedPreferenceListener);
	}
	
	@TargetApi(26)
	private void updateMessageNotificationPreference(Preference preference) {
		//Getting the summary
		String summary;
		switch(((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).getNotificationChannel(MainApplication.notificationChannelMessage).getImportance()) {
			case 0:
				summary = getResources().getString(R.string.notificationchannelimportance_0);
				break;
			case 1:
				summary = getResources().getString(R.string.notificationchannelimportance_1);
				break;
			case 2:
				summary = getResources().getString(R.string.notificationchannelimportance_2);
				break;
			case 3:
				summary = getResources().getString(R.string.notificationchannelimportance_3);
				break;
			case 4:
				summary = getResources().getString(R.string.notificationchannelimportance_4);
				break;
			/* case 5:
				summary = getResources().getString(R.string.notificationchannelimportance_5);
				break; */
			default:
				summary = getResources().getString(R.string.part_unknown);
				break;
		}
		
		//Setting the summary
		preference.setSummary(summary);
	}
	
	private void updateRingtonePreference(String key, RingtonePreference preference) {
		//Getting the ringtone name
		String ringtone = PreferenceManager.getDefaultSharedPreferences(Preferences.this).getString(key, "default ringtone");
		String ringtoneName = RingtoneManager.getRingtone(Preferences.this, Uri.parse(ringtone)).getTitle(Preferences.this);
		
		//Setting the summary
		preference.setSummary(ringtoneName);
	}
	
	private void updateTextNumberPreference(EditTextPreference editTextPreference, int summaryString) {
		//Setting the summary
		editTextPreference.setSummary(getResources().getString(summaryString, Integer.parseInt(editTextPreference.getText())));
	}
	
	private void updateListPreference(ListPreference listPreference) {
		//Setting the summary
		listPreference.setSummary(listPreference.getEntry());
	}
	
	private void updateServerURL(Preference preference) {
		//Setting the summary
		preference.setSummary(getSharedPreferences(MainApplication.sharedPreferencesFile, Context.MODE_PRIVATE).getString(MainApplication.sharedPreferencesKeyHostname, null));
	}
}
	
	/* @Override
	public void onRequestPermissionsResult(final int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
		//Checking if the request code is contacts access
		if(requestCode == Constants.permissionReadContacts) {
			//Checking if the result is a success
			if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				//Getting the switch preference
				SwitchPreference switchPreference = (SwitchPreference) findPreference(getResources().getString(R.string.preference_none_contacts_key));
				
				//Enabling the switch
				switchPreference.setChecked(true);
				
				//Refreshing the users
				UserCacheHelper.refreshUsers(this, ConversationManager.getConversations().toArray(new ConversationManager.ConversationInfo[0]));
			}
			//Otherwise checking if the result is a denial
			else if(grantResults[0] == PackageManager.PERMISSION_DENIED) {
				//Creating a snackbar
				Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), R.string.permission_rejected, Snackbar.LENGTH_LONG).setAction(R.string.button_settings, new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						//Opening the application settings
						Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
						intent.setData(Uri.parse("package:" + getPackageName()));
						startActivity(intent);
					}
				});
				
				//Coloring the snackbar
				//((TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text)).setTextColor(Color.WHITE);
				((TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_action)).setTextColor(getResources().getColor(R.color.colorAccent, null));
				
				//Showing the snackbar
				snackbar.show();
			}
		}
	} */