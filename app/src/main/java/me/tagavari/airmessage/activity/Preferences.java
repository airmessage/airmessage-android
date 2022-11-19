package me.tagavari.airmessage.activity;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.PluralsRes;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.util.Consumer;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.composite.AppCompatCompositeActivity;
import me.tagavari.airmessage.compositeplugin.PluginConnectionService;
import me.tagavari.airmessage.common.connection.MassRetrievalParams;
import me.tagavari.airmessage.common.constants.ColorConstants;
import me.tagavari.airmessage.contract.ContractDefaultMessagingApp;
import me.tagavari.airmessage.contract.ContractNotificationRingtoneSelector;
import me.tagavari.airmessage.common.data.DatabaseManager;
import me.tagavari.airmessage.common.helper.MessagesDataHelper;
import me.tagavari.airmessage.common.data.SharedPreferencesManager;
import me.tagavari.airmessage.common.enums.ProxyType;
import me.tagavari.airmessage.flavor.FirebaseAuthBridge;
import me.tagavari.airmessage.common.helper.ConfigurationHelper;
import me.tagavari.airmessage.common.helper.LanguageHelper;
import me.tagavari.airmessage.common.helper.MMSSMSHelper;
import me.tagavari.airmessage.common.helper.NotificationHelper;
import me.tagavari.airmessage.common.helper.ThemeHelper;
import me.tagavari.airmessage.common.helper.WindowHelper;
import me.tagavari.airmessage.receiver.StartBootReceiver;

public class Preferences extends AppCompatCompositeActivity implements PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
	private static final String TAG = Preferences.class.getSimpleName();
	
	//Creating the plugin values
	private PluginConnectionService pluginCS;
	
	public Preferences() {
		addPlugin(pluginCS = new PluginConnectionService());
	}
	
	PluginConnectionService getPluginCS() {
		return pluginCS;
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
		
		//Rendering edge-to-edge
		WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
		
		//Enabling the toolbar and up navigation
		setSupportActionBar(findViewById(R.id.toolbar));
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		//Configuring the AMOLED theme
		if(ThemeHelper.shouldUseAMOLED(this)) setDarkAMOLED();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
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
	
	private void setDarkAMOLED() {
		ThemeHelper.setActivityAMOLEDBase(this);
		AppBarLayout appBar = findViewById(R.id.appbar);
		appBar.setBackgroundColor(ColorConstants.colorAMOLED);
	}
	
	public static class SettingsFragment extends PreferenceFragmentCompat {
		static final String FRAGMENT_TAG = "preferencefragment";
		
		//Creating the callback values
		private final ActivityResultLauncher<Uri> requestRingtoneLauncher = registerForActivityResult(new ContractNotificationRingtoneSelector(), result -> {
			if(result.getCanceled()) return;
			
			//Getting the selected ringtone URI
			Uri ringtoneURI = result.getSelectedURI();
			
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
		});
		private final ActivityResultLauncher<Void> requestDefaultMessagingAppLauncher = registerForActivityResult(new ContractDefaultMessagingApp(), granted -> {
			if(!granted) return;
			
			//Enabling the toggle
			SwitchPreference preference = findPreference(getResources().getString(R.string.preference_textmessage_enable_key));
			preference.setChecked(true);
			
			//Showing a snackbar
			Snackbar.make(getView(), R.string.message_textmessageimport, Snackbar.LENGTH_LONG).show();
		});
		private final ActivityResultLauncher<String[]> requestMessagingPermissionsLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
			//Check if all permissions are granted
			if(permissions.values().stream().allMatch((granted) -> granted)) {
				//Enabling the toggle
				SwitchPreference preference = findPreference(getResources().getString(R.string.preference_textmessage_enable_key));
				preference.setChecked(true);
				
				//Starting the import service (started automatically by broadcast listener DefaultMessagingAppChangedReceiver)
				//getActivity().startService(new Intent(getActivity(), SystemMessageImportService.class).setAction(SystemMessageImportService.selfIntentActionImport));
				
				//Showing a snackbar
				Snackbar.make(getView(), R.string.message_textmessageimport, Snackbar.LENGTH_LONG).show();
			} else {
				//Showing a snackbar
				Snackbar.make(getView(), R.string.message_permissionrejected, Snackbar.LENGTH_LONG)
					.setAction(R.string.screen_settings, view -> startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:" + getActivity().getPackageName()))))
					.show();
			}
		});
		
		//Creating the subscription values
		private Disposable syncSubscription;
		
		Preference.OnPreferenceClickListener notificationSoundClickListener = preference -> {
			requestRingtoneLauncher.launch(getNotificationSound(getContext()));
			
			return true;
		};
		Preference.OnPreferenceChangeListener startOnBootChangeListener = (preference, value) -> {
			//Updating the service state
			updateConnectionServiceBootEnabled(getActivity(), (boolean) value);
			
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
					.setTitle(R.string.message_confirm_deleteattachments)
					//Setting the negative button
					.setNegativeButton(android.R.string.cancel, (DialogInterface dialogInterface, int which) -> {
						//Dismissing the dialog
						dialogInterface.dismiss();
					})
					//Setting the positive button
					.setPositiveButton(R.string.action_delete, (DialogInterface dialogInterface, int which) -> {
						//Deleting the attachment files on disk and in the database
						MessagesDataHelper.deleteAMBAttachments(getContext()).subscribe();
						
						//Displaying a snackbar
						Snackbar.make(getView(), R.string.message_confirm_deleteattachments_started, Snackbar.LENGTH_SHORT).show();
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
			PluginConnectionService pluginCS = getPluginCS();
			if(pluginCS == null || !pluginCS.isServiceBound() || !pluginCS.getConnectionManager().isConnected()) {
				//Displaying a snackbar
				Snackbar.make(getView(), R.string.message_serverstatus_noconnection, Snackbar.LENGTH_LONG).show();
				
				//Returning
				return true;
			}
			
			//Checking if there is already a mass retrieval in progress
			if(pluginCS.getConnectionManager().isMassRetrievalInProgress()) {
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
					.setPositiveButton(R.string.action_sync, (DialogInterface dialogInterface, int which) -> requestSyncMessages(new MassRetrievalParams()))
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
			
			//Returning true
			return true;
		};
		Preference.OnPreferenceChangeListener themeChangeListener = (preference, newValue) -> {
			//Applying the dark mode
			ThemeHelper.applyDarkMode((String) newValue);
			
			//Recreating the activity
			getActivity().recreate();
			
			//Accepting the change
			return true;
		};
		@RequiresApi(api = Build.VERSION_CODES.N)
		Preference.OnPreferenceChangeListener textIntegrationChangeListener = (preference, newValue) -> {
			//Checking if the preference is enabled
			if(((SwitchPreference) preference).isChecked()) {
				//Launching the app details screen
				try {
					startActivity(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)); //Manage default apps
				} catch(ActivityNotFoundException exception) {
					startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:" + getActivity().getPackageName()))); //App details page (fallback)
				}
			} else {
				if(MMSSMSHelper.isDefaultMessagingApp(getContext())) {
					//Requesting permissions
					requestMessagingPermissionsLauncher.launch(new String[]{Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.RECEIVE_MMS, Manifest.permission.READ_PHONE_STATE});
				} else {
					//Requesting to be the default messaging app
					requestDefaultMessagingAppLauncher.launch(null);
				}
			}
			
			//Returning false (to prevent the system from changing the option)
			return false;
		};
		Preference.OnPreferenceClickListener directResetClickListener = preference -> {
			//Creating a dialog
			AlertDialog dialog = new MaterialAlertDialogBuilder(getActivity())
					//Setting the name
					.setTitle(R.string.message_reset_direct)
					//Setting the negative button
					.setNegativeButton(android.R.string.cancel, null)
					//Setting the positive button
					.setPositiveButton(R.string.action_switchtoaccount, (DialogInterface dialogInterface, int which) -> {
						ConfigurationHelper.INSTANCE.resetConfiguration(requireContext());
					})
					//Creating the dialog
					.create();
			
			//Showing the dialog
			dialog.show();
			
			//Returning true
			return true;
		};
		Preference.OnPreferenceClickListener connectResetClickListener = preference -> {
			//Creating a dialog
			AlertDialog dialog = new MaterialAlertDialogBuilder(getActivity())
					//Setting the name
					.setTitle(R.string.message_reset_connect)
					//Setting the negative button
					.setNegativeButton(android.R.string.cancel, null)
					//Setting the positive button
					.setPositiveButton(R.string.action_signout, (DialogInterface dialogInterface, int which) -> {
						ConfigurationHelper.INSTANCE.resetConfiguration(requireContext());
					})
					//Creating the dialog
					.create();
			
			//Showing the dialog
			dialog.show();
			
			//Returning true
			return true;
		};
		Preference.OnPreferenceChangeListener autoDownloadAttachmentsChangeListener = (preference, newValue) -> {
			//If the user disables auto-download attachments, clear the status in the database
			if(!((boolean) newValue)) {
				Completable.fromAction(() -> DatabaseManager.getInstance().clearAutoDownloaded())
					.subscribeOn(Schedulers.single()).subscribe();
			}
			
			return true;
		};
		
		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			//Adding the preferences
			addPreferencesFromResource(R.xml.preferences_notification);
			addPreferencesFromResource(R.xml.preferences_main);
			int accountType = SharedPreferencesManager.getProxyType(getContext());
			if(accountType == ProxyType.direct) addPreferencesFromResource(R.xml.preferences_server);
			else if(accountType == ProxyType.connect) addPreferencesFromResource(R.xml.preferences_account);
			addPreferencesFromResource(R.xml.preferences_footer);
			
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				//Creating the notification channel intent
				Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
				intent.putExtra(Settings.EXTRA_CHANNEL_ID, NotificationHelper.notificationChannelMessage);
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
					//Updating the state
					ThemeHelper.INSTANCE.setAmoledMode((boolean) newValue);
					
					//Recreating the activity
					getActivity().recreate();
					
					return true;
				});
				amoledSwitch.setEnabled(!themePreference.getValue().equals(ThemeHelper.darkModeLight));
				
				//Checking if the device is running below Android 7.0 (API 24), or doesn't support telephony
				if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N || !getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
					//Removing the text message preference group
					PreferenceGroup preferenceGroup = findPreference(getResources().getString(R.string.preferencegroup_textmessage_key));
					getPreferenceScreen().removePreference(preferenceGroup);
				} else {
					//Updating the text message integration option
					SwitchPreference textIntegrationSwitch = findPreference(getResources().getString(R.string.preference_textmessage_enable_key));
					textIntegrationSwitch.setOnPreferenceChangeListener(textIntegrationChangeListener);
				}
			}
			
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
			{
				Preference preference = findPreference(getResources().getString(R.string.preference_server_connectionboot_key));
				if(preference != null) preference.setOnPreferenceChangeListener(startOnBootChangeListener);
			}
			findPreference(getResources().getString(R.string.preference_storage_deleteattachments_key)).setOnPreferenceClickListener(deleteAttachmentsClickListener);
			findPreference(getResources().getString(R.string.preference_server_downloadmessages_key)).setOnPreferenceClickListener(syncMessagesClickListener);
			findPreference(getResources().getString(R.string.preference_appearance_theme_key)).setOnPreferenceChangeListener(themeChangeListener);
			{
				Preference preference = findPreference(getResources().getString(R.string.preference_account_accountdetails_key));
				if(preference != null) {
					String summary = FirebaseAuthBridge.getUserSummary();
					preference.setSummary(summary);
				}
			}
			{
				Preference preference = findPreference(getResources().getString(R.string.preference_server_reset_key));
				if(preference != null) {
					if(FirebaseAuthBridge.isSupported()) {
						preference.setOnPreferenceClickListener(directResetClickListener);
					} else {
						getPreferenceScreen().removePreferenceRecursively(preference.getKey());
					}
				}
			}
			{
				Preference preference = findPreference(getResources().getString(R.string.preference_account_reset_key));
				if(preference != null) preference.setOnPreferenceClickListener(connectResetClickListener);
			}
		}
		
		@Override
		public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
			super.onViewCreated(view, savedInstanceState);
			
			//Enforcing the maximum content width
			WindowHelper.enforceContentWidthView(getResources(), getListView());
			
			//Setting the list padding
			ViewCompat.setOnApplyWindowInsetsListener(
					view.findViewById(R.id.recycler_view),
					(recyclerView, windowInsets) -> {
						Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
						//Top inset is handled by app bar
						recyclerView.setPadding(insets.left, 0, insets.right, insets.bottom);
						return WindowInsetsCompat.CONSUMED;
					}
			);
		}
		
		@Override
		public void onResume() {
			//Calling the super method
			super.onResume();
			
			//Updating the server URL
			{
				Preference preference = findPreference(getResources().getString(R.string.preference_server_serverdetails_key));
				if(preference != null) updateServerURL(preference);
			}
			
			//Updating the notification preference
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) updateMessageNotificationPreference(findPreference(getResources().getString(R.string.preference_messagenotifications_key)));
		}
		
		@Override
		public void onDestroyView() {
			super.onDestroyView();
			
			//Cancelling task subscriptions
			if(syncSubscription != null && !syncSubscription.isDisposed()) syncSubscription.dispose();
		}
		
		@RequiresApi(api = Build.VERSION_CODES.O)
		private void updateMessageNotificationPreference(Preference preference) {
			//Getting the summary
			String summary;
			switch(((NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE)).getNotificationChannel(NotificationHelper.notificationChannelMessage).getImportance()) {
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
			try {
				preference.setSummary(SharedPreferencesManager.getDirectConnectionAddress(getContext()));
			} catch(GeneralSecurityException | IOException exception) {
				exception.printStackTrace();
				preference.setSummary(R.string.part_unknown);
			}
		}
		
		private class AdvancedSyncDialogManager {
			private static final float disabledAlpha = 0.5F;
			
			private final View view;
			private Consumer<Boolean> stateListener = null;
			
			private int currentSliderID;
			
			private final Slider sliderDateMessages;
			private final Slider sliderDateAttachments;
			
			private final Slider sliderAttachmentSize;
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
					Slider slider = sliderAttachmentSize = view.findViewById(R.id.slider_attachments_size);
					TextView label = labelAttachmentSize = view.findViewById(R.id.label_attachments_size);
					slider.addOnChangeListener((changedSlider, value, fromUser) -> updateSyncSizeLabel(label, (int) value));
					
					//Updating the progress immediately
					updateSyncSizeLabel(label, (int) slider.getValue());
				}
				
				//Configuring the time sliders
				{
					sliderDateMessages = view.findViewById(R.id.slider_messages);
					TextView labelMessages = view.findViewById(R.id.label_messages);
					sliderDateAttachments = view.findViewById(R.id.slider_attachments);
					TextView labelAttachments = view.findViewById(R.id.label_attachments);
					
					SliderListener sliderListenerMessages = new SliderListener(sliderDateMessages, sliderDateAttachments, true, labelMessages);
					sliderDateMessages.addOnSliderTouchListener(sliderListenerMessages);
					sliderDateMessages.addOnChangeListener(sliderListenerMessages);
					
					SliderListener sliderListenerAttachments = new SliderListener(sliderDateAttachments, sliderDateMessages, false, labelAttachments);
					sliderDateAttachments.addOnSliderTouchListener(sliderListenerAttachments);
					sliderDateAttachments.addOnChangeListener(sliderListenerAttachments);
				}
			}
			
			private void updateSyncSizeLabel(TextView label, int progress) {
				if(progress == advancedSyncSizes.length) label.setText(R.string.message_advancedsync_anysize);
				else label.setText(getResources().getString(R.string.message_advancedsync_constraint_size, LanguageHelper.getHumanReadableByteCountInt(advancedSyncSizes[progress], false)));
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
					int progress = (int) sliderDateMessages.getValue();
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
					int progress = (int) sliderDateAttachments.getValue();
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
					int progress = (int) sliderAttachmentSize.getValue();
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
				
				requestSyncMessages(new MassRetrievalParams(restrictMessages, timeSinceMessages, downloadAttachments, restrictAttachments, timeSinceAttachments, restrictAttachmentSizes, attachmentSizeLimit, attachmentFilterWhitelist, attachmentFilterBlacklist, attachmentFilterDLOther));
			}
			
			private class SliderListener implements Slider.OnChangeListener, Slider.OnSliderTouchListener {
				private final Slider otherBar;
				private final boolean isMessagesSlider;
				private final TextView descriptiveLabel;
				
				private int otherBarStartValue;
				private boolean isActive;
				
				private boolean lastSpecState = true;
				
				@SuppressLint("ClickableViewAccessibility")
				SliderListener(Slider thisBar, Slider otherBar, boolean isMessagesSlider, TextView descriptiveLabel) {
					//Setting the parameters
					this.otherBar = otherBar;
					this.isMessagesSlider = isMessagesSlider;
					this.descriptiveLabel = descriptiveLabel;
					
					//Setting the touch prevention listener on the other slider
					otherBar.setOnTouchListener((view, event) -> isActive);
					
					//Updating the label
					updateChanges((int) thisBar.getValue(), false);
				}
				
				@Override
				public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
					//Updating the label
					updateChanges((int) value, true);
					
					//Updating the button state
					if(isMessagesSlider) stateListener.accept(value > 0);
					
					//Returning if this is not the active slider (to prevent both sliders from being activated at once)
					if(currentSliderID != slider.getId()) return;
					
					//Enforcing the position onto the other slider
					if(isMessagesSlider) {
						if(otherBar.getValue() > slider.getValue()) otherBar.setValue(slider.getValue());
						else if(otherBar.getValue() != otherBarStartValue) otherBar.setValue(Math.min(otherBarStartValue, slider.getValue()));
					} else {
						if(otherBar.getValue() < slider.getValue()) otherBar.setValue(slider.getValue());
						else if(otherBar.getValue() != otherBarStartValue) otherBar.setValue(Math.max(otherBarStartValue, slider.getValue()));
					}
				}
				
				@Override
				public void onStartTrackingTouch(@NonNull Slider slider) {
					currentSliderID = slider.getId();
					otherBarStartValue = (int) otherBar.getValue();
					isActive = true;
				}
				
				@Override
				public void onStopTrackingTouch(@NonNull Slider slider) {
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
		
		private void requestSyncMessages(MassRetrievalParams params) {
			//Deleting the messages
			syncSubscription = MessagesDataHelper.deleteAMBMessages(getContext())
					.toSingle(() -> {
						//Requesting a re-sync
						PluginConnectionService pluginCS = getPluginCS();
						if(pluginCS != null && pluginCS.isServiceBound() && pluginCS.getConnectionManager().isConnected()) {
							pluginCS.getConnectionManager().fetchMassConversationData(params).doOnError(error -> {
								Log.i(TAG, "Failed to sync messages", error);
							}).onErrorComplete().subscribe();
							return true;
						} else {
							return false;
						}
					})
					.subscribe(success -> {
						//Displaying a snackbar
						View view = getView();
						if(view != null) {
							if(success) Snackbar.make(view, R.string.message_confirm_resyncmessages_started, Snackbar.LENGTH_SHORT).show();
							else Snackbar.make(view, R.string.message_serverstatus_noconnection, Snackbar.LENGTH_LONG).show();
						}
			});
		}
		
		private PluginConnectionService getPluginCS() {
			Activity activity = getActivity();
			if(activity == null) return null;
			else return ((Preferences) activity).getPluginCS();
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
		if(selectedSound == null) return Settings.System.DEFAULT_NOTIFICATION_URI;
		else if(selectedSound.isEmpty()) return null;
		else return Uri.parse(selectedSound);
	}
	
	public static boolean getPreferenceAMOLED(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preference_appearance_amoled_key), false);
	}
	
	public static boolean getPreferenceReplySuggestions(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preference_features_replysuggestions_key), true);
	}
	
	public static boolean getPreferenceMessagePreviews(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preference_features_messagepreviews_key), true);
	}
	
	public static boolean getPreferenceAutoDownloadAttachments(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preference_storage_autodownload_key), true);
	}
	
	public static boolean getPreferenceSMSDeliveryReports(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preference_textmessage_deliveryreport_key), false);
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
		return MMSSMSHelper.isDefaultMessagingApp(context) &&
			   context.checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
			   context.checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
			   context.checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
			   context.checkSelfPermission(Manifest.permission.RECEIVE_MMS) == PackageManager.PERMISSION_GRANTED &&
			   context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
			   getPreferenceTextMessageIntegration(context);
	}
	
	public static boolean getPreferenceStartOnBoot(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preference_server_connectionboot_key), false);
	}
	
	public static void updateConnectionServiceBootEnabled(Context context) {
		int accountType = SharedPreferencesManager.getProxyType(context);
		updateConnectionServiceBootEnabled(context, getPreferenceStartOnBoot(context) && accountType == ProxyType.direct); //Don't start on boot if we're using Connect
	}
	
	public static void updateConnectionServiceBootEnabled(Context context, boolean enable) {
		context.getPackageManager().setComponentEnabledSetting(new ComponentName(context, StartBootReceiver.class),
				enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
				PackageManager.DONT_KILL_APP);
	}
}