package me.tagavari.airmessage.activity;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Telephony;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.PluralsRes;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Consumer;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.composite.AppCompatCompositeActivity;
import me.tagavari.airmessage.compositeplugin.PluginQNavigation;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.connection.MassRetrievalParams;
import me.tagavari.airmessage.messaging.AttachmentInfo;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.ConversationItem;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.receiver.StartBootReceiver;
import me.tagavari.airmessage.service.ConnectionService;
import me.tagavari.airmessage.service.SystemMessageImportService;
import me.tagavari.airmessage.util.Constants;
import me.tagavari.airmessage.util.ConversationUtils;
import me.tagavari.airmessage.view.HostnameEditTextPreference;

public class Preferences extends AppCompatCompositeActivity implements PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
	//Creating the reference values
	private static final int permissionRequestLocation = 0;
	private static final int permissionRequestSMS = 1;
	private static final int activityRequestRingtone = 0;
	private static final int activityRequestDefaultMessagingApp = 1;
	
	//Creating the plugin values
	private PluginQNavigation pluginQNavigation;
	
	public Preferences() {
		addPlugin(pluginQNavigation = new PluginQNavigation());
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		//Calling the super method
		super.onCreate(savedInstanceState);
		
		//Setting the content view
		setContentView(R.layout.activity_preferences);
		
		if(savedInstanceState == null) {
			// Create the fragment only when the activity is created for the first time.
			// ie. not after orientation changes
			Fragment fragment = getSupportFragmentManager().findFragmentByTag(SettingsFragment.FRAGMENT_TAG);
			if(fragment == null) fragment = new SettingsFragment();
			
			FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
			fragmentTransaction.replace(R.id.container, fragment, SettingsFragment.FRAGMENT_TAG);
			fragmentTransaction.commit();
		}
		
		//Enabling the toolbar and up navigation
		setSupportActionBar(findViewById(R.id.toolbar));
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		//Configuring the AMOLED theme
		if(Constants.shouldUseAMOLED(this)) setDarkAMOLED();
		
		//Setting the status bar color
		Constants.updateChromeOSStatusBar(this);
	}
	
	@Override
	public boolean onPreferenceStartScreen(PreferenceFragmentCompat preferenceFragmentCompat, PreferenceScreen preferenceScreen) {
		FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
		SettingsFragment fragment = new SettingsFragment();
		Bundle bundle = new Bundle();
		bundle.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, preferenceScreen.getKey());
		fragment.setArguments(bundle);
		fragmentTransaction.replace(R.id.container, fragment, preferenceScreen.getKey());
		fragmentTransaction.addToBackStack(preferenceScreen.getKey());
		fragmentTransaction.commit();
		
		return true;
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
	
	void setDarkAMOLED() {
		Constants.setActivityAMOLEDBase(this);
		findViewById(R.id.appbar).setBackgroundColor(Constants.colorAMOLED);
	}
	
	/* @Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	} */
	
	public static class SettingsFragment extends PreferenceFragmentCompat {
		static final String FRAGMENT_TAG = "preferencefragment";
		
		Preference.OnPreferenceClickListener notificationSoundClickListener = preference -> {
			Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
			intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
			intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
			intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
			intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI);
			
			Uri existingValue = getNotificationSound(getContext());
			if(existingValue == null) {
				//Silent
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
			} else {
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingValue);
			}
			
			startActivityForResult(intent, activityRequestRingtone);
			
			return true;
		};
		Preference.OnPreferenceChangeListener startOnBootChangeListener = (preference, value) -> {
			//Updating the service state
			getActivity().getPackageManager().setComponentEnabledSetting(new ComponentName(getActivity(), StartBootReceiver.class),
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
			AlertDialog dialog = new MaterialAlertDialogBuilder(getActivity())
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
						List<ConversationInfo> conversations = ConversationUtils.getConversations();
						if(conversations != null) {
							for(ConversationInfo conversationInfo : conversations) {
								List<ConversationItem> conversationItems = conversationInfo.getConversationItems();
								if(conversationItems == null) continue;
								for(ConversationItem item : conversationItems) {
									if(!(item instanceof MessageInfo)) continue;
									for(AttachmentInfo attachmentInfo : ((MessageInfo) item).getAttachments()) {
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
			AlertDialog dialog = new MaterialAlertDialogBuilder(getActivity())
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
			//Checking if the connection manager
			ConnectionManager connectionManager = ConnectionService.getConnectionManager();
			if(connectionManager == null || connectionManager.getCurrentState() != ConnectionManager.stateConnected) {
				//Displaying a snackbar
				Snackbar.make(getView(), R.string.message_serverstatus_noconnection, Snackbar.LENGTH_LONG).show();
				
				//Returning
				return true;
			}
			
			//Checking if there is already a mass retrieval in progress
			if(connectionManager.isMassRetrievalInProgress()) {
				//Displaying a snackbar
				Snackbar.make(getView(), R.string.message_confirm_resyncmessages_inprogress, Snackbar.LENGTH_SHORT).show();
				
				//Returning
				return true;
			}
			
			//Creating a dialog
			AlertDialog dialog = new MaterialAlertDialogBuilder(getActivity())
					//Setting the text
					.setTitle(R.string.message_confirm_resyncmessages)
					.setMessage(R.string.message_confirm_resyncmessages_description)
					//Setting the negative button
					.setNegativeButton(android.R.string.cancel, (DialogInterface dialogInterface, int which) -> dialogInterface.dismiss())
					//Setting the positive button
					.setPositiveButton(R.string.action_sync, (DialogInterface dialogInterface, int which) -> new ConversationsBase.SyncMessagesTask(getActivity().getApplicationContext(), getView(), new MassRetrievalParams()).execute())
					.setNeutralButton(R.string.action_advanced, (DialogInterface dialogInterface, int which) -> {
						dialogInterface.dismiss();
						
						//Creating the dialog manager
						AdvancedSyncDialogManager _dialogManager = new AdvancedSyncDialogManager(getLayoutInflater());
						
						//Creating the dialog
						AlertDialog _dialog = new MaterialAlertDialogBuilder(getActivity())
								.setTitle(R.string.message_confirm_resyncmessages_advanced)
								.setView(_dialogManager.getView())
								.setNegativeButton(android.R.string.cancel, (DialogInterface _dialogInterface, int _which) -> _dialogInterface.dismiss())
								.setPositiveButton(R.string.action_sync, (DialogInterface _dialogInterface, int _which) -> _dialogManager.startSync())
								.create();
						
						//Setting the dialog state updater
						_dialogManager.setStateListener(state -> _dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(state));
						
						//Showing the dialog
						_dialog.show();
					})
					.create();
			
			//Showing the dialog
			dialog.show();
			
			//Setting up the button
			//dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(false);
			
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
		Preference.OnPreferenceChangeListener textIntegrationChangeListener = (preference, newValue) -> {
			//Checking if the preference is enabled
			if(((SwitchPreference) preference).isChecked()) {
				//Launching the app details screen
				startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:" + getActivity().getPackageName())));
			} else {
				if(Constants.isDefaultMessagingApp(getContext())) {
					//Requesting permissions
					requestPermissions(new String[]{Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.RECEIVE_MMS, Manifest.permission.READ_PHONE_STATE}, permissionRequestSMS);
				} else {
					//Requesting to be the default messaging app
					requestDefaultMessagingApp();
				}
			}
			
			//Returning false (to prevent the system from changing the option)
			return false;
		};
		Preference.OnPreferenceChangeListener fallbackServerChangeListener = (preference, newValue) -> {
			//Setting the value
			String newValueString = (String) newValue;
			if(newValueString.isEmpty()) {
				ConnectionManager.hostnameFallback = null;
				preference.setSummary(R.string.preference_server_serverfallback_description);
			} else {
				ConnectionManager.hostnameFallback = newValueString;
				preference.setSummary(newValueString);
			}
			
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
			}
			
			{
				//Setting the theme options based on the system version
				ListPreference themePreference = findPreference(getResources().getString(R.string.preference_appearance_theme_key));
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) themePreference.setEntries(R.array.preference_appearance_theme_entries_androidQ);
				else themePreference.setEntries(R.array.preference_appearance_theme_entries_old);
				
				//Updating the AMOLED switch option
				SwitchPreference amoledSwitch = findPreference(getResources().getString(R.string.preference_appearance_amoled_key));
				amoledSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
					//Recreating the activity
					getActivity().recreate();
					
					return true;
				});
				amoledSwitch.setEnabled(!themePreference.getValue().equals(MainApplication.darkModeLight));
				
				//Updating the text message integration option
				SwitchPreference textIntegrationSwitch = findPreference(getResources().getString(R.string.preference_textmessage_enable_key));
				textIntegrationSwitch.setOnPreferenceChangeListener(textIntegrationChangeListener);
			}
			
			//Setting the intents
			findPreference(getResources().getString(R.string.preference_server_help_key)).setIntent(new Intent(Intent.ACTION_VIEW, Constants.helpAddress));
			
			//Setting the listeners
			{
				Preference preference = findPreference(getResources().getString(R.string.preference_messagenotifications_sound_key));
				if(preference != null) {
					//Setting the preference click listener
					preference.setOnPreferenceClickListener(notificationSoundClickListener);
					
					//Setting the summary
					preference.setSummary(getRingtoneTitle(getNotificationSound(getContext())));
				}
			}
			findPreference(getResources().getString(R.string.preference_server_connectionboot_key)).setOnPreferenceChangeListener(startOnBootChangeListener);
			findPreference(getResources().getString(R.string.preference_storage_deleteattachments_key)).setOnPreferenceClickListener(deleteAttachmentsClickListener);
			findPreference(getResources().getString(R.string.preference_server_downloadmessages_key)).setOnPreferenceClickListener(syncMessagesClickListener);
			findPreference(getResources().getString(R.string.preference_appearance_theme_key)).setOnPreferenceChangeListener(themeChangeListener);
			{
				EditTextPreference fallbackServerPref = findPreference(getResources().getString(R.string.preference_server_serverfallback_key));
				fallbackServerPref.setOnPreferenceChangeListener(fallbackServerChangeListener);
				String text = fallbackServerPref.getText();
				fallbackServerPref.setSummary(text == null || text.isEmpty() ? getResources().getString(R.string.preference_server_serverfallback_description) : text);
			}
		}
		
		@Override
		public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
			super.onViewCreated(view, savedInstanceState);
			
			//Enforcing the maximum content width
			Constants.enforceContentWidthView(getResources(), getListView());
			
			//Setting the list padding
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				View recyclerView = view.findViewById(R.id.recycler_view);
				ViewCompat.setOnApplyWindowInsetsListener(recyclerView, new OnApplyWindowInsetsListener() {
					@Override
					public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
						//((ViewGroup.MarginLayoutParams) reyclerView.getLayoutParams()).bottomMargin = -insets.getSystemWindowInsetBottom();
						recyclerView.setPadding(recyclerView.getPaddingLeft(), recyclerView.getPaddingTop(), recyclerView.getPaddingRight(), insets.getSystemWindowInsetBottom());
						return insets.consumeSystemWindowInsets();
					}
				});
			}
			
			//if(Preferences.getPreferenceAMOLED(getContext())) setDarkAMOLED();
		}
		
		void setDarkAMOLEDSamsung() {
			//Configuring the list
			RecyclerView list = getListView();
			
			list.setBackgroundResource(R.drawable.background_amoledsamsung);
			list.setClipToOutline(true);
			list.invalidate();
		}
		
		@Override
		public void onRequestPermissionsResult(final int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
			//Checking if the request code is contacts access
			if(requestCode == permissionRequestLocation) {
				//Checking if the result is a success
				if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					//Enabling the toggle
					((SwitchPreference) findPreference(getResources().getString(R.string.preference_appearance_location_key))).setChecked(true);
					
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
			} else if(requestCode == permissionRequestSMS) {
				//Checking if the result is a success
				if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					//Enabling the toggle
					SwitchPreference preference = findPreference(getResources().getString(R.string.preference_textmessage_enable_key));
					preference.setChecked(true);
					
					//Starting the import service
					getActivity().startService(new Intent(getActivity(), SystemMessageImportService.class).setAction(SystemMessageImportService.selfIntentActionImport));
					
					//Showing a snackbar
					Snackbar.make(getView(), R.string.message_textmessageimport, Snackbar.LENGTH_LONG).show();
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
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
			if(requestCode == activityRequestRingtone && resultCode == RESULT_OK) {
				//Getting the selected ringtone URI
				Uri ringtoneURI = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
				
				//Saving the ringtone URI
				if(ringtoneURI == null) { //"silent" selected
					PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
							.putString(getContext().getResources().getString(R.string.preference_messagenotifications_sound_key), "")
							.apply();
				} else {
					PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
							.putString(getContext().getResources().getString(R.string.preference_messagenotifications_sound_key), ringtoneURI.toString())
							.apply();
				}
				
				//Updating the preference summary
				Preference preference = findPreference(getResources().getString(R.string.preference_messagenotifications_sound_key));
				preference.setSummary(getRingtoneTitle(ringtoneURI));
			} else if(requestCode == activityRequestDefaultMessagingApp && resultCode == RESULT_OK) {
				//Enabling the toggle
				SwitchPreference preference = findPreference(getResources().getString(R.string.preference_textmessage_enable_key));
				preference.setChecked(true);
				
				//Starting the import service
				getActivity().startService(new Intent(getActivity(), SystemMessageImportService.class).setAction(SystemMessageImportService.selfIntentActionImport));
				
				//Showing a snackbar
				Snackbar.make(getView(), R.string.message_textmessageimport, Snackbar.LENGTH_LONG).show();
			} else {
				super.onActivityResult(requestCode, resultCode, data);
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
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) updateMessageNotificationPreference(findPreference(getResources().getString(R.string.preference_messagenotifications_key)));
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
			//Creating the custom dialog fragment if a custom preference was selected
			DialogFragment dialogFragment = null;
			if(preference instanceof HostnameEditTextPreference) {
				dialogFragment = HostnameEditTextPreference.HostnameEditTextPreferenceDialog.newInstance(preference.getKey());
			}
			
			//Displaying the fragment
			if(dialogFragment != null) {
				dialogFragment.setTargetFragment(this, 0);
				dialogFragment.show(getFragmentManager(), null);
				return;
			}
			
			//Passing the request on to the superclass
			super.onDisplayPreferenceDialog(preference);
		}
		
		@RequiresApi(api = Build.VERSION_CODES.O)
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
			private static final float disabledAlpha = 0.5F;
			
			private final View view;
			private Consumer<Boolean> stateListener = null;
			
			private int currentSliderID;
			
			private final SeekBar sliderDateMessages;
			private final SeekBar sliderDateAttachments;
			
			private final SeekBar sliderAttachmentSize;
			private final TextView labelAttachmentSize;
			private final ViewGroup viewgroupAttachmentFilters;
			
			private final AdvancedSyncTime[] advancedSyncTimes = {
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
			private final long[] advancedSyncSizes = {
					1 * 1024 * 1024,
					2 * 1024 * 1024,
					4 * 1024 * 1024,
					8 * 1024 * 1024,
					16 * 1024 * 1024,
					32 * 1024 * 1024,
					64 * 1024 * 1024,
					128 * 1024 * 1024,
					256 * 1024 * 1024,
					512 * 1024 * 1024,
					1024 * 1024 * 1024
			};
			private final AdvancedSyncFilter[] advancedSyncFilters = {
					new AdvancedSyncFilter(new String[]{"image/*"}, R.drawable.gallery_outlined, R.string.message_advancedsync_type_image),
					new AdvancedSyncFilter(new String[]{"video/*"}, R.drawable.movie_outlined, R.string.message_advancedsync_type_video),
					new AdvancedSyncFilter(new String[]{"audio/*"}, R.drawable.volume_outlined, R.string.message_advancedsync_type_audio),
					new AdvancedSyncFilter(new String[]{
							"text/plain", "text/richtext", "application/rtf", "application/x-rtf",
							"application/pdf",
							"application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/vnd.openxmlformats-officedocument.wordprocessingml.template", "application/vnd.ms-word.document.macroEnabled.12", "application/vnd.ms-word.template.macroEnabled.12",
							"application/vnd.ms-excel", "pplication/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "pplication/vnd.openxmlformats-officedocument.spreadsheetml.template", "application/vnd.ms-excel.sheet.macroEnabled.12", "application/vnd.ms-excel.sheet.binary.macroEnabled.12", "application/vnd.ms-excel.template.macroEnabled.12", "application/vnd.ms-excel.addin.macroEnabled.12",
							"application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation", "application/vnd.openxmlformats-officedocument.presentationml.template", "application/vnd.openxmlformats-officedocument.presentationml.slideshow", "application/vnd.ms-powerpoint.presentation.macroEnabled.12", "application/vnd.ms-powerpoint.template.macroEnabled.12", "application/vnd.ms-powerpoint.slideshow.macroEnabled.12", "application/vnd.ms-powerpoint.addin.macroEnabled.12"},
							R.drawable.file_document_outlined, R.string.message_advancedsync_type_document),
					new AdvancedSyncFilter(null, R.drawable.file_outlined, R.string.message_advancedsync_type_other)
			};
			
			AdvancedSyncDialogManager(LayoutInflater inflater) {
				//Inflating the view
				view = inflater.inflate(R.layout.dialog_advancedsync, null);
				
				//Adding the file filters
				{
					ViewGroup group = viewgroupAttachmentFilters = view.findViewById(R.id.group_filters);
					for(AdvancedSyncFilter filter : advancedSyncFilters) {
						View itemView = filter.createView(inflater, group);
						group.addView(itemView);
					}
				}
				
				//Configuring the attachment slider
				{
					SeekBar seekBar = sliderAttachmentSize = view.findViewById(R.id.slider_attachments_size);
					TextView label = labelAttachmentSize = view.findViewById(R.id.label_attachments_size);
					seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
						@Override
						public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
							updateSyncSizeLabel(label, progress);
						}
						
						@Override
						public void onStartTrackingTouch(SeekBar seekBar) {}
						
						@Override
						public void onStopTrackingTouch(SeekBar seekBar) {}
					});
					
					//Updating the progress immediately
					updateSyncSizeLabel(label, seekBar.getProgress());
				}
				
				//Configuring the time sliders
				{
					sliderDateMessages = view.findViewById(R.id.slider_messages);
					TextView labelMessages = view.findViewById(R.id.label_messages);
					sliderDateAttachments = view.findViewById(R.id.slider_attachments);
					TextView labelAttachments = view.findViewById(R.id.label_attachments);
					sliderDateMessages.setOnSeekBarChangeListener(new SeekBarListener(sliderDateMessages, sliderDateAttachments, true, labelMessages));
					sliderDateAttachments.setOnSeekBarChangeListener(new SeekBarListener(sliderDateAttachments, sliderDateMessages, false, labelAttachments));
				}
			}
			
			private void updateSyncSizeLabel(TextView label, int progress) {
				if(progress == advancedSyncSizes.length) label.setText(R.string.message_advancedsync_anysize);
				else label.setText(getResources().getString(R.string.message_advancedsync_constraint_size, Constants.humanReadableByteCountInt(advancedSyncSizes[progress], false)));
			}
			
			private void setAttachmentSpecsEnabled(boolean state, boolean animate) {
				//Setting the input states
				sliderAttachmentSize.setEnabled(state);
				for(AdvancedSyncFilter filter : advancedSyncFilters) filter.setEnabled(state);
				//viewgroupAttachmentFilters.setClickable(state);
				
				if(animate) {
					float[] animationValues = state ? new float[]{disabledAlpha, 1} : new float[]{1, disabledAlpha};
					ValueAnimator animator = ValueAnimator.ofFloat(animationValues);
					animator.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
					animator.addUpdateListener(animation -> {
						float value = (float) animation.getAnimatedValue();
						sliderAttachmentSize.setAlpha(value);
						labelAttachmentSize.setAlpha(value);
						viewgroupAttachmentFilters.setAlpha(value);
					});
					animator.start();
				} else {
					float value = state ? 1 : disabledAlpha;
					sliderAttachmentSize.setAlpha(value);
					labelAttachmentSize.setAlpha(value);
					viewgroupAttachmentFilters.setAlpha(value);
				}
			}
			
			void setStateListener(Consumer<Boolean> listener) {
				stateListener = listener;
			}
			
			View getView() {
				return view;
			}
			
			void startSync() {
				//Ignoring the request if the service is not set up to receive an advanced mass retrieval request
				//if(!ConnectionService.staticCheckSupportsFeature(ConnectionManager.featureAdvancedSync1)) return;
				
				//Getting the parameters
				boolean restrictMessages;
				long timeSinceMessages = -1;
				{
					int progress = sliderDateMessages.getProgress();
					if(progress == 0) return; //Don't download any messages
					else if(progress - 1 == advancedSyncTimes.length) restrictMessages = false; //Download all messages
					else {
						restrictMessages = true;
						timeSinceMessages = System.currentTimeMillis() - advancedSyncTimes[progress - 1].duration;
					}
				}
				
				boolean downloadAttachments;
				boolean restrictAttachments = false;
				long timeSinceAttachments = -1;
				{
					int progress = sliderDateAttachments.getProgress();
					if(progress == 0) downloadAttachments = false; //Don't download any attachments
					else {
						downloadAttachments = true;
						if(progress - 1 == advancedSyncTimes.length) restrictAttachments = false; //Download all attachments
						else {
							restrictAttachments = true;
							timeSinceAttachments = System.currentTimeMillis() - advancedSyncTimes[progress - 1].duration;
						}
					}
				}
				
				boolean restrictAttachmentSizes;
				long attachmentSizeLimit = -1;
				{
					int progress = sliderAttachmentSize.getProgress();
					if(progress == advancedSyncSizes.length) restrictAttachmentSizes = false; //Download any size
					else {
						restrictAttachmentSizes = true;
						attachmentSizeLimit = advancedSyncSizes[progress];
					}
				}
				
				List<String> attachmentFilterWhitelist = new ArrayList<>();
				List<String> attachmentFilterBlacklist = new ArrayList<>();
				boolean attachmentFilterDLOther = false;
				for(AdvancedSyncFilter filter : advancedSyncFilters) {
					if(filter.filers == null) attachmentFilterDLOther = filter.isChecked(); //Other files
					else (filter.isChecked() ? attachmentFilterWhitelist : attachmentFilterBlacklist).addAll(Arrays.asList(filter.filers));
				}
				
				new ConversationsBase.SyncMessagesTask(getActivity().getApplicationContext(), SettingsFragment.this.getView(), new MassRetrievalParams(restrictMessages, timeSinceMessages, downloadAttachments, restrictAttachments, timeSinceAttachments, restrictAttachmentSizes, attachmentSizeLimit, attachmentFilterWhitelist, attachmentFilterBlacklist, attachmentFilterDLOther)).execute();
			}
			
			private class SeekBarListener implements SeekBar.OnSeekBarChangeListener {
				private final SeekBar otherBar;
				private final boolean isMessagesSlider;
				private final TextView descriptiveLabel;
				
				private int otherBarStartProgress;
				private boolean isActive;
				
				private boolean lastSpecState = true;
				
				@SuppressLint("ClickableViewAccessibility")
				SeekBarListener(SeekBar thisBar, SeekBar otherBar, boolean isMessagesSlider, TextView descriptiveLabel) {
					//Setting the parameters
					this.otherBar = otherBar;
					this.isMessagesSlider = isMessagesSlider;
					this.descriptiveLabel = descriptiveLabel;
					
					//Setting the touch prevention listener on the other seekbar
					otherBar.setOnTouchListener((view, event) -> isActive);
					
					//Updating the label
					updateChanges(thisBar.getProgress(), false);
				}
				
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					//Updating the label
					updateChanges(progress, true);
					
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
				
				private void updateChanges(int progress, boolean animate) {
					//Updating the label
					String text;
					if(progress == 0) text = getResources().getString(isMessagesSlider ? R.string.message_advancedsync_downloadmessages_none : R.string.message_advancedsync_downloadattachments_none);
					else if(progress - 1 == advancedSyncTimes.length) text = getResources().getString(R.string.message_advancedsync_anytime);
					else {
						AdvancedSyncTime data = advancedSyncTimes[progress - 1];
						text = getResources().getString(R.string.message_advancedsync_constraint_time, getResources().getQuantityString(data.pluralRes, data.quantity, data.quantity));
					}
					descriptiveLabel.setText(text);
					
					//Updating the attachment specs
					if(!isMessagesSlider) {
						boolean state = progress > 0;
						if(lastSpecState != state) {
							lastSpecState = state;
							setAttachmentSpecsEnabled(state, animate);
						}
					}
				}
			}
			
			private class AdvancedSyncFilter {
				private final String[] filers;
				@DrawableRes
				private final int iconRes;
				@StringRes
				private final int stringRes;
				
				private View view;
				private CheckBox viewCheckbox;
				
				private boolean isClickable = true;
				
				AdvancedSyncFilter(String[] filers, int iconRes, int stringRes) {
					this.filers = filers;
					this.iconRes = iconRes;
					this.stringRes = stringRes;
				}
				
				View createView(LayoutInflater inflater, ViewGroup parent) {
					view = inflater.inflate(R.layout.layout_advancedsync_filefilter, parent, false);
					
					((ImageView) view.findViewById(R.id.icon)).setImageResource(iconRes);
					((TextView) view.findViewById(R.id.label)).setText(stringRes);
					
					viewCheckbox = view.findViewById(R.id.checkbox);
					viewCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> handleUpdateZeroPrevention(isChecked));
					view.setOnClickListener(view -> viewCheckbox.setChecked(!viewCheckbox.isChecked()));
					
					return view;
				}
				
				void setEnabled(boolean state) {
					view.setClickable(state && isClickable);
					viewCheckbox.setEnabled(state);
				}
				
				void setClickable(boolean state) {
					isClickable = state;
					view.setClickable(state);
				}
				
				boolean isChecked() {
					return viewCheckbox.isChecked();
				}
				
				//Prevents all checkboxes from being unchecked at once
				private void handleUpdateZeroPrevention(boolean isChecked) {
					if(isChecked) {
						//Counting the amount of checked boxes
						AdvancedSyncFilter lastFilter = null;
						for(AdvancedSyncFilter filter : advancedSyncFilters) {
							if(filter == this) continue;
							if(!filter.isChecked()) continue;
							if(lastFilter != null) return; //If there were already 2 or more checked boxes, then there is no need to change anything
							lastFilter = filter;
						}
						
						//Enabling the last filter (since it was previously the only checkbox)
						lastFilter.setClickable(true);
					} else {
						//Counting the amount of checked boxes
						AdvancedSyncFilter lastFilter = null;
						for(AdvancedSyncFilter filter : advancedSyncFilters) {
							//if(filter == this) continue;
							if(!filter.isChecked()) continue;
							if(lastFilter != null) return; //If there were already 2 or more checked boxes, then there is no need to change anything
							lastFilter = filter;
						}
						
						//Disabling the last filter (since it is now the only checkbox)
						lastFilter.setClickable(false);
					}
				}
			}
			
			private class AdvancedSyncTime {
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
		
		@Override
		public Fragment getCallbackFragment() {
			return this;
		}
		
		private String getRingtoneTitle(Uri ringtoneURI) {
			//Silent ringtone
			if(ringtoneURI == null) return getContext().getResources().getString(R.string.part_none);
			
			//Getting the ringtone title
			Ringtone ringtone = RingtoneManager.getRingtone(getContext(), ringtoneURI);
			if(ringtone == null) return getContext().getResources().getString(R.string.part_unknown);
			String title = ringtone.getTitle(getContext());
			ringtone.stop();
			
			//Returning the ringtone title
			return title;
		}
		
		private void requestDefaultMessagingApp() {
			Intent intent;
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				RoleManager roleManager = getActivity().getSystemService(RoleManager.class);
				intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS);
			} else {
				intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
				intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getActivity().getPackageName());
			}
			startActivityForResult(intent, activityRequestDefaultMessagingApp);
		}
		
		/* @Override
		public boolean onPreferenceStartScreen(PreferenceFragmentCompat preferenceFragmentCompat, PreferenceScreen preferenceScreen) {
			FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
			MyPreferenceFragment fragment = new MyPreferenceFragment();
			Bundle args = new Bundle();
			args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, preferenceScreen.getKey());
			fragment.setArguments(args);
			ft.add(R.id.container, fragment, preferenceScreen.getKey());
			ft.addToBackStack(preferenceScreen.getKey());
			ft.commit();
			return true;
		} */
	}
	
	public static Uri getNotificationSound(Context context) {
		String selectedSound = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getResources().getString(R.string.preference_messagenotifications_sound_key), null);
		if(selectedSound == null) return Constants.defaultNotificationSound;
		else if(selectedSound.isEmpty()) return null;
		else return Uri.parse(selectedSound);
	}
	
	public static boolean getPreferenceAMOLED(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preference_appearance_amoled_key), false);
	}
	
	public static boolean getPreferenceReplySuggestions(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preference_features_replysuggestions_key), false);
	}
	
	public static boolean getPreferenceAdvancedColor(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preference_appearance_advancedcolor_key), false);
	}
	
	public static boolean getPreferenceShowReadReceipts(Context context) {
		return true;
		//return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preference_appearance_showreadreceipts_key), true);
	}
	
	public static boolean getPreferenceMessagePreviews(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preference_features_messagepreviews_key), true);
	}
	
	public static boolean getPreferenceMessageSounds(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preference_features_messagesounds_key), true);
	}
	
	public static boolean getPreferenceSMSDeliveryReports(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preference_textmessage_deliveryreport_key), false);
	}
	
	public static String getPreferenceFallbackServer(Context context) {
		String value = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getResources().getString(R.string.preference_server_serverfallback_key), null);
		return value != null && value.isEmpty() ? null : value;
	}
	
	public static boolean getPreferenceTextMessageIntegration(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preference_textmessage_enable_key), false);
	}
	
	public static void setPreferenceTextMessageIntegration(Context context, boolean value) {
		PreferenceManager.getDefaultSharedPreferences(context).edit()
				.putBoolean(context.getResources().getString(R.string.preference_textmessage_enable_key), value)
				.apply();
	}
	
	public static boolean isTextMessageIntegrationActive(Context context) {
		return Constants.isDefaultMessagingApp(context) &&
			   context.checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
			   context.checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
			   context.checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
			   context.checkSelfPermission(Manifest.permission.RECEIVE_MMS) == PackageManager.PERMISSION_GRANTED &&
			   context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
			   getPreferenceTextMessageIntegration(context);
	}
}