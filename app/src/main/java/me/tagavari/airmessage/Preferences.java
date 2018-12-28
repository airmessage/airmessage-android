package me.tagavari.airmessage;

import android.annotation.SuppressLint;
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
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.takisoft.preferencex.RingtonePreference;
import com.takisoft.preferencex.RingtonePreferenceDialogFragmentCompat;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;
import androidx.fragment.app.DialogFragment;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

public class Preferences extends AppCompatActivity {
	//Creating the reference values
	private static final int permissionRequestLocation = 0;
	private static final AdvancedSyncTime[] advancedSyncTimes = {
			new AdvancedSyncTime(1 * 60 * 60 * 1000L, R.plurals.message_advancedsync_time_hour, 1), //1 hour
			new AdvancedSyncTime(2 * 60 * 60 * 1000L, R.plurals.message_advancedsync_time_hour, 2), //2 hour
			new AdvancedSyncTime(3 * 60 * 60 * 1000L, R.plurals.message_advancedsync_time_hour, 3), //3 hour
			new AdvancedSyncTime(4 * 60 * 60 * 1000L, R.plurals.message_advancedsync_time_hour, 4), //4 hour
			new AdvancedSyncTime(8 * 60 * 60 * 1000L, R.plurals.message_advancedsync_time_hour, 8), //8 hour
			new AdvancedSyncTime(12 * 60 * 60 * 1000L, R.plurals.message_advancedsync_time_hour, 12), //12 hour
			new AdvancedSyncTime(1 * 24 * 60 * 60 * 1000L, R.plurals.message_advancedsync_time_day, 1), //1 day
			new AdvancedSyncTime(2 * 24 * 60 * 60 * 1000L, R.plurals.message_advancedsync_time_day, 2), //2 day
			new AdvancedSyncTime(3 * 24 * 60 * 60 * 1000L, R.plurals.message_advancedsync_time_day, 3), //3 day
			new AdvancedSyncTime(4 * 24 * 60 * 60 * 1000L, R.plurals.message_advancedsync_time_day, 4), //4 day
			new AdvancedSyncTime(5 * 24 * 60 * 60 * 1000L, R.plurals.message_advancedsync_time_day, 5), //5 day
			new AdvancedSyncTime(6 * 24 * 60 * 60 * 1000L, R.plurals.message_advancedsync_time_day, 6), //6 day
			new AdvancedSyncTime(1 * 7 * 24 * 60 * 60 * 1000L, R.plurals.message_advancedsync_time_week, 1), //1 week
			new AdvancedSyncTime(2 * 7 * 24 * 60 * 60 * 1000L, R.plurals.message_advancedsync_time_week, 2), //2 week
			new AdvancedSyncTime(3 * 7 * 24 * 60 * 60 * 1000L, R.plurals.message_advancedsync_time_week, 3), //3 week
			new AdvancedSyncTime(1 * 4 * 7 * 24 * 60 * 60 * 1000L, R.plurals.message_advancedsync_time_month, 1), //1 month
			new AdvancedSyncTime(2 * 4 * 7 * 24 * 60 * 60 * 1000L, R.plurals.message_advancedsync_time_month, 2), //2 month
			new AdvancedSyncTime(4 * 4 * 7 * 24 * 60 * 60 * 1000L, R.plurals.message_advancedsync_time_month, 4), //4 month
			new AdvancedSyncTime(8 * 4 * 7 * 24 * 60 * 60 * 1000L, R.plurals.message_advancedsync_time_month, 8), //8 month
			new AdvancedSyncTime(1 * 12 * 4 * 7 * 24 * 60 * 60 * 1000L, R.plurals.message_advancedsync_time_year, 1), //1 year
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		//Calling the super method
		super.onCreate(savedInstanceState);

		//Setting the content view
		setContentView(R.layout.activity_preferences);
		
		//Adding the fragment
		getSupportFragmentManager().beginTransaction()
				.replace(R.id.container, new SettingsFragment())
				.commit();
		
		//Enabling the toolbar and up navigation
		setSupportActionBar(findViewById(R.id.toolbar));
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
		/* Preference.OnPreferenceClickListener ringtoneClickListener = preference -> {
			//Returning true
			return true;
		};
		Preference.OnPreferenceChangeListener ringtoneChangeListener = (preference, newValue) -> {
			//updateRingtonePreference(preference);
			
			//Returning true
			return true;
		}; */
		Preference.OnPreferenceChangeListener startOnBootChangeListener = (preference, value) -> {
			//Updating the service state
			getActivity().getPackageManager().setComponentEnabledSetting(new ComponentName(getActivity(), ConnectionService.ServiceStartBoot.class),
					(boolean) value ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
					PackageManager.DONT_KILL_APP);
			
			//Returning true (to allow the change)
			return true;
		};
		/* Preference.OnPreferenceChangeListener useForegroundServiceChangeListener = (preference, value) -> {
			//Casting the value
			boolean boolValue = (boolean) value;
			//Updating the service
			ConnectionService service = ConnectionService.getInstance();
			if(service != null) service.setForegroundState(boolValue);
			
			//Disabling the "start on boot" switch if the service is now a background service
			if(boolValue) ((SwitchPreferenceCompat) findPreference(getResources().getString(R.string.preference_server_disconnectionnotification_key))).setChecked(true);
			else ((SwitchPreferenceCompat) findPreference(getResources().getString(R.string.preference_server_connectionboot_key))).setChecked(false);
			
			//Updating the state of the dependant items
			findPreference(getResources().getString(R.string.preference_server_connectionboot_key)).setEnabled(boolValue);
			findPreference(getResources().getString(R.string.preference_server_disconnectionnotification_key)).setEnabled(!boolValue);
			
			//Returning true (to allow the change)
			return true;
		}; */
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
						//Clearing the attachments in memory
						List<ConversationManager.ConversationInfo> conversations = ConversationManager.getConversations();
						if(conversations != null) {
							for(ConversationManager.ConversationInfo conversationInfo : conversations) {
								List<ConversationManager.ConversationItem> conversationItems = conversationInfo.getConversationItems();
								if(conversationItems == null) continue;
								for(ConversationManager.ConversationItem item : conversationItems) {
									if(!(item instanceof ConversationManager.MessageInfo)) continue;
									for(ConversationManager.AttachmentInfo attachmentInfo : ((ConversationManager.MessageInfo) item).getAttachments()) {
										attachmentInfo.discardFile(getActivity());
									}
								}
							}
						}
						
						//Displaying a snackbar
						Snackbar.make(getView(), R.string.message_confirm_deleteattachments_started, Snackbar.LENGTH_SHORT).show();
						
						//Deleting the attachment files on disk and in the database
						new ConversationsBase.DeleteAttachmentsTask(getActivity().getApplicationContext()).execute();
					})
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
						
						//Displaying a snackbar
						Snackbar.make(getView(), R.string.message_confirm_deletemessages_started, Snackbar.LENGTH_SHORT).show();
					})
					//Creating the dialog
					.create();
			
			//Showing the dialog
			dialog.show();
			
			//Returning true
			return true;
		};
		Preference.OnPreferenceClickListener syncMessagesClickListener = preference -> {
			//Checking if the service is ready
			ConnectionService service = ConnectionService.getInstance();
			if(service == null || service.getCurrentState() != ConnectionService.stateConnected) {
				//Displaying a snackbar
				Snackbar.make(getView(), R.string.message_serverstatus_noconnection, Snackbar.LENGTH_LONG).show();
				
				//Returning
				return true;
			}
			
			//Checking if there is already a mass retrieval in progress
			if(service.isMassRetrievalInProgress()) {
				//Displaying a snackbar
				Snackbar.make(getView(), R.string.message_confirm_resyncmessages_inprogress, Snackbar.LENGTH_SHORT).show();
				
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
					.setPositiveButton(R.string.action_sync, (DialogInterface dialogInterface, int which) -> new ConversationsBase.SyncMessagesTask(getActivity().getApplicationContext(), getView()).execute())
					/*.setNeutralButton(R.string.action_advanced, (DialogInterface dialogInterface, int which) -> {
						dialogInterface.dismiss();
						
						//Creating the dialog manager
						AdvancedSyncDialogManager _dialogManager = new AdvancedSyncDialogManager(getLayoutInflater());
						
						//Creating the dialog
						AlertDialog _dialog = new AlertDialog.Builder(getActivity())
								.setTitle(R.string.message_confirm_resyncmessages_advanced)
								.setView(_dialogManager.getView())
								.setNegativeButton(android.R.string.cancel, (DialogInterface _dialogInterface, int _which) -> _dialogInterface.dismiss())
								.setPositiveButton(R.string.action_sync, (DialogInterface _dialogInterface, int _which) -> new ConversationsBase.SyncMessagesTask(getActivity().getApplicationContext(), getView()).execute())
								.create();
						
						//Setting the dialog state updater
						_dialogManager.setStateListener(state -> _dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(state));
						
						//Showing the dialog
						_dialog.show();
					})*/
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
			}/* else {
				//Updating the notification information
				String ringtonePreferenceKey = getResources().getString(R.string.preference_messagenotifications_sound_key);
				updateRingtonePreference(findPreference(ringtonePreferenceKey));
			} */
			
			//Setting the dependant states
			/* {
				SwitchPreferenceCompat foregroundServiceSwitch = (SwitchPreferenceCompat) findPreference(getResources().getString(R.string.preference_server_foregroundservice_key));
				findPreference(getResources().getString(R.string.preference_server_connectionboot_key)).setEnabled(foregroundServiceSwitch.isChecked());
				findPreference(getResources().getString(R.string.preference_server_disconnectionnotification_key)).setEnabled(!foregroundServiceSwitch.isChecked());
			} */
			
			{
				SwitchPreferenceCompat locationSwitch = (SwitchPreferenceCompat) findPreference(getResources().getString(R.string.preference_appearance_location_key));
				locationSwitch.setChecked(ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
				
				ListPreference darkModePreference = (ListPreference) findPreference(getResources().getString(R.string.preference_appearance_theme_key));
				locationSwitch.setEnabled(darkModePreference.getValue().equals(MainApplication.darkModeAutomatic));
				
				locationSwitch.setOnPreferenceChangeListener((preference, value) -> {
					//Opening the application settings if the permission has been granted
					if(ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:" + getActivity().getPackageName())));
						//Otherwise requesting the permission
					else requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, permissionRequestLocation);
					//else Constants.requestPermission(getActivity(), new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, Constants.permissionAccessCoarseLocation);
					
					//Returning false (to prevent the system from changing the option)
					return false;
				});
			}
			
			//Setting the intents
			findPreference(getResources().getString(R.string.preference_server_help_key)).setIntent(new Intent(Intent.ACTION_VIEW, Constants.serverSetupAddress));
			
			//Setting the listeners
			//findPreference(getResources().getString(R.string.preference_messagenotifications_sound_key)).setOnPreferenceClickListener(ringtoneClickListener);
			//findPreference(getResources().getString(R.string.preference_messagenotifications_sound_key)).setOnPreferenceChangeListener(ringtoneChangeListener);
			//findPreference(getResources().getString(R.string.preference_server_foregroundservice_key)).setOnPreferenceChangeListener(useForegroundServiceChangeListener);
			findPreference(getResources().getString(R.string.preference_server_connectionboot_key)).setOnPreferenceChangeListener(startOnBootChangeListener);
			findPreference(getResources().getString(R.string.preference_storage_deleteattachments_key)).setOnPreferenceClickListener(deleteAttachmentsClickListener);
			//findPreference(getResources().getString(R.string.preference_storage_deleteall_key)).setOnPreferenceClickListener(deleteMessagesClickListener);
			findPreference(getResources().getString(R.string.preference_storage_deleteattachments_key)).setOnPreferenceClickListener(deleteAttachmentsClickListener);
			findPreference(getResources().getString(R.string.preference_server_downloadmessages_key)).setOnPreferenceClickListener(syncMessagesClickListener);
			findPreference(getResources().getString(R.string.preference_appearance_theme_key)).setOnPreferenceChangeListener(themeChangeListener);
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
			findPreference(getResources().getString(R.string.preference_server_resync_key)).setOnPreferenceClickListener(syncMessagesClickListener);
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
			if(requestCode == permissionRequestLocation) {
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
				dialogFragment = RingtonePreferenceDialogFragmentCompat.newInstance(preference.getKey());
			}
			
			// If it was one of our custom Preferences, show its dialog
			if(dialogFragment != null) {
				dialogFragment.setTargetFragment(this, 0);
				dialogFragment.show(this.getFragmentManager(), "androidx.preference" + ".PreferenceFragment.DIALOG");
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
		
		private class AdvancedSyncDialogManager {
			private final View view;
			private Consumer<Boolean> stateListener = null;
			
			private int currentSliderID;
			
			AdvancedSyncDialogManager(LayoutInflater inflater) {
				view = inflater.inflate(R.layout.dialog_advancedsync, null);
				
				SeekBar sliderMessages = view.findViewById(R.id.slider_messages);
				TextView labelMessages = view.findViewById(R.id.label_messages);
				SeekBar sliderAttachments = view.findViewById(R.id.slider_attachments);
				TextView labelAttachments = view.findViewById(R.id.label_attachments);
				sliderMessages.setOnSeekBarChangeListener(new SeekBarListener(sliderMessages, sliderAttachments, true, labelMessages));
				sliderAttachments.setOnSeekBarChangeListener(new SeekBarListener(sliderAttachments, sliderMessages, false, labelAttachments));
			}
			
			void setStateListener(Consumer<Boolean> listener) {
				stateListener = listener;
			}
			
			View getView() {
				return view;
			}
			
			private class SeekBarListener implements SeekBar.OnSeekBarChangeListener {
				private final SeekBar otherBar;
				private final boolean isMessagesSlider;
				private final TextView descriptiveLabel;
				
				private int otherBarStartProgress;
				private boolean isActive;
				
				@SuppressLint("ClickableViewAccessibility")
				SeekBarListener(SeekBar thisBar, SeekBar otherBar, boolean isMessagesSlider, TextView descriptiveLabel) {
					//Setting the parameters
					this.otherBar = otherBar;
					this.isMessagesSlider = isMessagesSlider;
					this.descriptiveLabel = descriptiveLabel;
					
					//Setting the touch prevention listener on the other seekbar
					otherBar.setOnTouchListener((view, event) -> isActive);
					
					//Updating the label
					updateLabel(thisBar.getProgress());
				}
				
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					//Updating the label
					updateLabel(progress);
					
					//Updating the button state
					if(isMessagesSlider) stateListener.accept(progress > 0);
					
					//Returning if this is not the active seekbar (to prevent both seekbars from being activated at once)
					if(currentSliderID != seekBar.getId()) return;
					
					//Enforcing the position onto the other seekbar
					if(isMessagesSlider) {
						if(otherBar.getProgress() > seekBar.getProgress()) otherBar.setProgress(seekBar.getProgress());
						else if(otherBar.getProgress() != otherBarStartProgress) otherBar.setProgress(Math.min(otherBarStartProgress, seekBar.getProgress()));
					} else {
						if(otherBar.getProgress() < seekBar.getProgress()) otherBar.setProgress(seekBar.getProgress());
						else if(otherBar.getProgress() != otherBarStartProgress) otherBar.setProgress(Math.max(otherBarStartProgress, seekBar.getProgress()));
					}
				}
				
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
					currentSliderID = seekBar.getId();
					otherBarStartProgress = otherBar.getProgress();
					isActive = true;
				}
				
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
					isActive = false;
				}
				
				private void updateLabel(int progress) {
					//Updating the label
					String text;
					if(progress == 0) text = getResources().getString(isMessagesSlider ? R.string.message_advancedsync_downloadmessages_none : R.string.message_advancedsync_downloadattachments_none);
					else if(progress - 1 == advancedSyncTimes.length) text = getResources().getString(isMessagesSlider ? R.string.message_advancedsync_downloadmessages_all : R.string.message_advancedsync_downloadattachments_all);
					else {
						AdvancedSyncTime data = advancedSyncTimes[progress - 1];
						text = getResources().getString(isMessagesSlider ? R.string.message_advancedsync_downloadmessages : R.string.message_advancedsync_downloadattachments, getResources().getQuantityString(data.pluralRes, data.quantity, data.quantity));
					}
					descriptiveLabel.setText(text);
				}
			}
		}
	}
	
	private static class AdvancedSyncTime {
		private final long duration;
		@PluralsRes
		private final int pluralRes;
		private final int quantity;
		
		AdvancedSyncTime(long duration, int pluralRes, int quantity) {
			this.duration = duration;
			this.pluralRes = pluralRes;
			this.quantity = quantity;
		}
	}
}