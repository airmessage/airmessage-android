package me.tagavari.airmessage;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.view.MenuItem;
import android.view.View;

import com.takisoft.fix.support.v7.preference.RingtonePreference;
import com.takisoft.fix.support.v7.preference.RingtonePreferenceDialogFragmentCompat;

public class Preferences extends AppCompatActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		//Calling the super method
		super.onCreate(savedInstanceState);
		
		//setContentView(R.layout.activity_preferences);
		
		//Adding the fragment
		getSupportFragmentManager().beginTransaction()
				.replace(android.R.id.content, new SettingsFragment())
				.commit();
		
		//Enabling the toolbar and up navigation
		//setActionBar(findViewById(R.id.toolbar));
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
	
	/* @Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	} */
	
	public static class SettingsFragment extends PreferenceFragmentCompat {
		Preference.OnPreferenceClickListener ringtoneClickListener = preference -> {
			//Returning true
			return true;
		};
		Preference.OnPreferenceChangeListener ringtoneChangeListener = (preference, newValue) -> {
			//updateRingtonePreference(preference);
			
			//Returning true
			return true;
		};
		Preference.OnPreferenceChangeListener startOnBootChangeListener = (preference, value) -> {
			//Updating the service state
			getActivity().getPackageManager().setComponentEnabledSetting(new ComponentName(getActivity(), ConnectionService.ServiceStart.class),
					(boolean) value ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
			
			//Returning true (to allow the change)
			return true;
		};
		Preference.OnPreferenceClickListener deleteAttachmentsClickListener = preference -> {
			//Creating a dialog
			AlertDialog dialog = new AlertDialog.Builder(getActivity())
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
								new ConversationsBase.DeleteAttachmentsTask(getActivity().getApplicationContext()).execute();
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
			AlertDialog dialog = new AlertDialog.Builder(getActivity())
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
						new ConversationsBase.DeleteMessagesTask(getActivity().getApplicationContext()).execute();
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
				Snackbar.make(getView(), R.string.message_serverstatus_noconnection, Snackbar.LENGTH_LONG).show();
				
				//Returning
				return true;
			}
			
			//Checking if there is already a mass retrieval in progress
			if(service.isMassRetrievalInProgress()) {
				//Displaying a snackbar
				Snackbar.make(getView(), R.string.message_confirm_resyncmessages_inprogress, Snackbar.LENGTH_LONG).show();
				
				//Returning
				return true;
			}
			
			//Creating a dialog
			AlertDialog dialog = new AlertDialog.Builder(getActivity())
					//Setting the text
					.setTitle(R.string.message_confirm_resyncmessages)
					.setMessage(R.string.message_confirm_resyncmessages_description)
					//Setting the negative button
					.setNegativeButton(android.R.string.cancel, (DialogInterface dialogInterface, int which) -> dialogInterface.dismiss())
					//Setting the positive button
					.setPositiveButton(R.string.action_resync, (DialogInterface dialogInterface, int which) -> new ConversationsBase.SyncMessagesTask(getActivity().getApplicationContext(), getView()).execute())
					//Creating the dialog
					.create();
			
			//Showing the dialog
			dialog.show();
			
			//Returning true
			return true;
		};
		Preference.OnPreferenceChangeListener themeChangeListener = (preference, newValue) -> {
			//Applying the dark mode
			((MainApplication) getActivity().getApplication()).applyDarkMode((String) newValue);
			
			//Recreating the activity
			getActivity().recreate();
			
			//Accepting the change
			return true;
		};
		
		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			//Adding the preferences
			addPreferencesFromResource(R.xml.preferences);
			
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				//Creating the notification channel intent
				Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
				intent.putExtra(Settings.EXTRA_CHANNEL_ID, MainApplication.notificationChannelMessage);
				intent.putExtra(Settings.EXTRA_APP_PACKAGE, getActivity().getPackageName());
				
				//Setting the listener
				findPreference(getResources().getString(R.string.preference_messagenotifications_key)).setIntent(intent);
			} else {
				//Updating the notification information
				//String ringtonePreferenceKey = getResources().getString(R.string.preference_messagenotifications_sound_key);
				//updateRingtonePreference(findPreference(ringtonePreferenceKey));
			}
			
			//Setting the listeners
			//findPreference(getResources().getString(R.string.preference_messagenotifications_sound_key)).setOnPreferenceClickListener(ringtoneClickListener);
			//findPreference(getResources().getString(R.string.preference_messagenotifications_sound_key)).setOnPreferenceChangeListener(ringtoneChangeListener);
			findPreference(getResources().getString(R.string.preference_server_connectionboot_key)).setOnPreferenceChangeListener(startOnBootChangeListener);
			findPreference(getResources().getString(R.string.preference_storage_deleteattachments_key)).setOnPreferenceClickListener(deleteAttachmentsClickListener);
			findPreference(getResources().getString(R.string.preference_storage_deleteall_key)).setOnPreferenceClickListener(deleteMessagesClickListener);
			findPreference(getResources().getString(R.string.preference_storage_deleteattachments_key)).setOnPreferenceClickListener(deleteAttachmentsClickListener);
			findPreference(getResources().getString(R.string.preference_server_resync_key)).setOnPreferenceClickListener(resyncMessagesClickListener);
			findPreference(getResources().getString(R.string.preference_appearance_theme_key)).setOnPreferenceChangeListener(themeChangeListener);
			
			{
				SwitchPreferenceCompat locationSwitch = (SwitchPreferenceCompat) findPreference(getResources().getString(R.string.preference_appearance_location_key));
				locationSwitch.setChecked(ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
				
				ListPreference darkModePreference = (ListPreference) findPreference(getResources().getString(R.string.preference_appearance_theme_key));
				locationSwitch.setEnabled(darkModePreference.getValue().equals(MainApplication.darkModeAutomatic));
				
				locationSwitch.setOnPreferenceChangeListener((preference, value) -> {
					//Opening the application settings if the permission has been granted
					if(ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:" + getActivity().getPackageName())));
						//Otherwise requesting the permission
					else requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, Constants.permissionAccessCoarseLocation);
					//else Constants.requestPermission(getActivity(), new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, Constants.permissionAccessCoarseLocation);
					
					//Returning false (to prevent the system from changing the option)
					return false;
				});
			}
			
			//Setting the intents
			findPreference(getResources().getString(R.string.preference_server_help_key)).setIntent(new Intent(Intent.ACTION_VIEW, Constants.serverSetupAddress));
		}
		
		@Override
		public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
			super.onViewCreated(view, savedInstanceState);
			//Enforcing the maximum content width
			Constants.enforceContentWidth(getResources(), getListView());
		}
		
		/* @Override
		public void onCreate(@Nullable Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			
			//Adding the preferences
			addPreferencesFromResource(R.xml.preferences);
			
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				//Creating the notification channel intent
				Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
				intent.putExtra(Settings.EXTRA_CHANNEL_ID, MainApplication.notificationChannelMessage);
				intent.putExtra(Settings.EXTRA_APP_PACKAGE, getActivity().getPackageName());
				
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
			findPreference(getResources().getString(R.string.preference_appearance_theme_key)).setOnPreferenceChangeListener((preference, newValue) -> {
				//Applying the dark mode
				((MainApplication) getActivity().getApplication()).applyDarkMode((String) newValue);
				
				//Recreating the activity
				getActivity().recreate();
				
				//Accepting the change
				return true;
			});
			
			{
				SwitchPreference locationSwitch = (SwitchPreference) findPreference(getResources().getString(R.string.preference_appearance_location_key));
				locationSwitch.setChecked(ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
				
				ListPreference darkModePreference = (ListPreference) findPreference(getResources().getString(R.string.preference_appearance_theme_key));
				locationSwitch.setEnabled(darkModePreference.getValue().equals(MainApplication.darkModeAutomatic));
				
				locationSwitch.setOnPreferenceChangeListener((preference, value) -> {
					//Opening the application settings if the permission has been granted
					if(ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:" + getActivity().getPackageName())));
					//Otherwise requesting the permission
					else requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, Constants.permissionAccessCoarseLocation);
					//else Constants.requestPermission(getActivity(), new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, Constants.permissionAccessCoarseLocation);
					
					//Returning false (to prevent the system from changing the option)
					return false;
				});
			}
			
			//Setting the intents
			findPreference(getResources().getString(R.string.preference_server_help_key)).setIntent(new Intent(Intent.ACTION_VIEW, Constants.serverSetupAddress));
		} */
		
		@Override
		public void onRequestPermissionsResult(final int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
			//Checking if the request code is contacts access
			if(requestCode == Constants.permissionAccessCoarseLocation) {
				//Checking if the result is a success
				if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					//Enabling the toggle
					((SwitchPreferenceCompat) findPreference(getResources().getString(R.string.preference_appearance_location_key))).setChecked(true);
					
					//Recreating the activity
					getActivity().recreate();
				}
				//Otherwise checking if the result is a denial
				else if(grantResults[0] == PackageManager.PERMISSION_DENIED) {
					//Showing a snackbar
					Snackbar.make(getView(), R.string.message_permissionrejected, Snackbar.LENGTH_LONG)
							.setAction(R.string.screen_settings, view -> startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:" + getActivity().getPackageName()))))
							.show();
				}
			}
		}
		
		@Override
		public void onResume() {
			//Calling the super method
			super.onResume();
			
			//Registering the listener
			//PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(sharedPreferenceListener);
			
			//Updating the server URL
			updateServerURL(findPreference(getResources().getString(R.string.preference_server_serverdetails_key)));
			
			//Updating the notification preference
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
				updateMessageNotificationPreference(findPreference(getResources().getString(R.string.preference_messagenotifications_key)));
		}
		
		@Override
		public void onStop() {
			//Calling the super method
			super.onStop();
			
			//Unregistering the listener
			//PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(sharedPreferenceListener);
		}
		
		@Override
		public void onDisplayPreferenceDialog(Preference preference) {
			// Try if the preference is one of our custom Preferences
			DialogFragment dialogFragment = null;
			if(preference instanceof RingtonePreference) {
				// Create a new instance of TimePreferenceDialogFragment with the key of the related
				// Preference
				dialogFragment = RingtonePreferenceDialogFragmentCompat
						.newInstance(preference.getKey());
			}
			
			// If it was one of our custom Preferences, show its dialog
			if(dialogFragment != null) {
				dialogFragment.setTargetFragment(this, 0);
				dialogFragment.show(this.getFragmentManager(), "android.support.v7.preference" + ".PreferenceFragment.DIALOG");
			}
			// Could not be handled here. Try with the super method.
			else {
				super.onDisplayPreferenceDialog(preference);
			}
		}
		
		@TargetApi(26)
		private void updateMessageNotificationPreference(Preference preference) {
			//Getting the summary
			String summary;
			switch(((NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE)).getNotificationChannel(MainApplication.notificationChannelMessage).getImportance()) {
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
		
		/* private void updateRingtonePreference(Preference preference) {
			//Getting the ringtone name
			String ringtoneUri = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(preference.getKey(), Constants.defaultNotificationSound);
			Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), Uri.parse(ringtoneUri));
			String ringtoneName = ringtone == null ? getResources().getString(R.string.part_unknown) : ringtone.getTitle(getActivity());
			
			//Setting the summary
			preference.setSummary(ringtoneName);
		} */
		
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
			preference.setSummary(((MainApplication) getActivity().getApplication()).getConnectivitySharedPrefs().getString(MainApplication.sharedPreferencesConnectivityKeyHostname, null));
		}
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