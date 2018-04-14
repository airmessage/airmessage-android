package me.tagavari.airmessage;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatDelegate;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.squareup.leakcanary.LeakCanary;

import java.io.File;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;

import io.fabric.sdk.android.Fabric;

public class MainApplication extends Application {
	//Creating the reference values
	static final String notificationChannelMessage = "message";
	static final String notificationChannelStatus = "status";
	
	static final String notificationGroupMessage = "message";
	
	static final String dirNameDownload = "downloads";
	static final String dirNameUpload = "uploads";
	
	private static final String sharedPreferencesConnectivityFile = "connectivity";
	static final String sharedPreferencesConnectivityKeyHostname = "hostname";
	static final String sharedPreferencesConnectivityKeyPassword = "password";
	static final String sharedPreferencesConnectivityKeyLastConnectionTime = "last_connection_time";
	static final String sharedPreferencesConnectivityKeyLastConnectionHostname = "last_connection_hostname";
	
	static final String fileAuthority = "me.tagavari.airmessage.fileprovider";
	
	//Creating the cache helpers
	private BitmapCacheHelper bitmapCacheHelper;
	private UserCacheHelper userCacheHelper;
	private SoftReference<LoadFlagArrayList<ConversationManager.ConversationInfo>> conversationReference = null;
	
	//Creating the singletons
	private static WeakReference<MainApplication> instanceReference = null;
	
	@Override
	public void onCreate() {
		//Calling the super method
		super.onCreate();
		
		if(LeakCanary.isInAnalyzerProcess(this)) {
			// This process is dedicated to LeakCanary for heap analysis.
			// You should not init your app in this process.
			return;
		}
		LeakCanary.install(this);
		
		//Configuring crash reporting
		configureCrashReporting();
		
		//Setting the instance
		instanceReference = new WeakReference<>(this);
		
		//Checking if the device is running Android Oreo (API 26)
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			//Initializing the notification channels
			NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			{
				NotificationChannel messageChannel = new NotificationChannel(notificationChannelMessage, getResources().getString(R.string.notificationchannel_message), NotificationManager.IMPORTANCE_HIGH);
				messageChannel.setDescription(getString(R.string.notificationchannel_message_desc));
				messageChannel.enableVibration(true);
				messageChannel.setVibrationPattern(new long[]{1000, 1000});
				messageChannel.setShowBadge(true);
				messageChannel.enableLights(true);
				//messageChannel.setGroup(notificationGroupMessage);
				notificationManager.createNotificationChannel(messageChannel);
			}
			{
				NotificationChannel statusChannel = new NotificationChannel(notificationChannelStatus, getResources().getString(R.string.notificationchannel_status), NotificationManager.IMPORTANCE_MIN);
				statusChannel.setDescription(getString(R.string.notificationchannel_status_desc));
				statusChannel.enableVibration(false);
				statusChannel.setShowBadge(false);
				statusChannel.enableLights(false);
				notificationManager.createNotificationChannel(statusChannel);
			}
		}
		
		//Migrating the shared preferences TODO remove on next release
		migrateSharedPreferences();
		
		//Getting the connection service information
		SharedPreferences sharedPrefs = getSharedPreferences(sharedPreferencesConnectivityFile, Context.MODE_PRIVATE);
		ConnectionService.hostname = sharedPrefs.getString(sharedPreferencesConnectivityKeyHostname, "");
		ConnectionService.password = sharedPrefs.getString(sharedPreferencesConnectivityKeyPassword, "");
		
		//Creating the cache helpers
		bitmapCacheHelper = new BitmapCacheHelper();
		userCacheHelper = new UserCacheHelper();
		
		//Creating the database manager
		DatabaseManager.createInstance(this);
		
		//Applying the dark mode
		applyDarkMode(PreferenceManager.getDefaultSharedPreferences(this).getString(getResources().getString(R.string.preference_appearance_theme_key), ""));
		
		//Enabling / disabling the service on boot as per the shared preference
		getPackageManager().setComponentEnabledSetting(new ComponentName(this, ConnectionService.ServiceStartBoot.class),
				PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getResources().getString(R.string.preference_server_connectionboot_key), true) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
				PackageManager.DONT_KILL_APP);
	}
	
	public static MainApplication getInstance() {
		return instanceReference == null ? null : instanceReference.get();
	}
	
	private void configureCrashReporting() {
		// Set up Crashlytics, disabled for debug builds
		Crashlytics crashlyticsKit = new Crashlytics.Builder()
				.core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
				.build();
		
		// Initialize Fabric with the debug-disabled crashlytics
		Fabric.with(this, crashlyticsKit);
	}
	
	private static final String oldSharedPrefsName = "me.tagavari.airmessage.MAIN_PREFERENCES";
	@SuppressLint("ApplySharedPref")
	private boolean migrateSharedPreferences() {
		//Returning false if the file doesn't exist
		File oldSharedPrefsFile = new File(getFilesDir().getParent(), "shared_prefs/" + oldSharedPrefsName + ".xml");
		if(!oldSharedPrefsFile.exists()) return false;
		
		//Getting the shared preferences
		SharedPreferences oldSharedPrefs = getSharedPreferences(oldSharedPrefsName, Context.MODE_PRIVATE);
		SharedPreferences newSharedPrefs = getSharedPreferences(sharedPreferencesConnectivityFile, Context.MODE_PRIVATE);
		
		//Copying the data
		newSharedPrefs.edit()
				.putString(sharedPreferencesConnectivityKeyHostname, oldSharedPrefs.getString("hostname", null))
				.putString(sharedPreferencesConnectivityKeyPassword, oldSharedPrefs.getString("password", null))
				.commit();
		
		//Deleting the old shared preferences
		oldSharedPrefsFile.delete();
		
		//Returning true
		return true;
	}
	
	static File getAttachmentDirectory(Context context) {
		//Getting the media directory
		File file = new File(context.getFilesDir(), "attachments");
		
		//Preparing the media directory
		prepareDirectory(file);
		
		//Preparing the subdirectories
		prepareDirectory(new File(file, dirNameDownload));
		prepareDirectory(new File(file, dirNameUpload));
		
		//Returning the media directory
		return file;
	}
	
	static File getDownloadDirectory(Context context) {
		return new File(getAttachmentDirectory(context), dirNameDownload);
	}
	
	static File getUploadDirectory(Context context) {
		return new File(getAttachmentDirectory(context), dirNameUpload);
	}
	
	static File findUploadFileTarget(Context context, String fileName) {
		//Finding a free directory and assigning the file to it
		return new File(Constants.findFreeFile(getUploadDirectory(context), Long.toString(System.currentTimeMillis())), fileName);
	}
	
	private static boolean prepareDirectory(File file) {
		//Creating the directory if the file doesn't exist
		if(!file.exists()) return file.mkdir();
		
		//Checking if the path is a file
		if(file.isFile()) {
			//Deleting the file
			if(!file.delete()) return false;
			
			//Creating the directory
			return file.mkdir();
		}
		
		//Returning true
		return true;
	}
	
	static void clearAttachmentsDirectory(Context context) {
		for(File childFiles : MainApplication.getAttachmentDirectory(context).listFiles()) Constants.recursiveDelete(childFiles);
	}
	
	BitmapCacheHelper getBitmapCacheHelper() {
		return bitmapCacheHelper;
	}
	
	UserCacheHelper getUserCacheHelper() {
		return userCacheHelper;
	}
	
	static boolean canUseContacts(Context context) {
		//Returning if the permission has been granted
		return ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
	}
	
	void setConversations(LoadFlagArrayList<ConversationManager.ConversationInfo> conversations) {
		conversationReference = new SoftReference<>(conversations);
	}
	
	LoadFlagArrayList<ConversationManager.ConversationInfo> getConversations() {
		if(conversationReference == null) return null;
		return conversationReference.get();
	}
	
	public static class LoadFlagArrayList<E> extends ArrayList<E> {
		private boolean isLoaded = false;
		
		boolean isLoaded() {
			return isLoaded;
		}
		
		void setLoaded(boolean loaded) {
			isLoaded = loaded;
		}
		
		public LoadFlagArrayList(int initialCapacity, boolean isLoaded) {
			super(initialCapacity);
			this.isLoaded = isLoaded;
		}
		
		LoadFlagArrayList(boolean isLoaded) {
			this.isLoaded = isLoaded;
		}
		
		public LoadFlagArrayList(@NonNull Collection<? extends E> c, boolean isLoaded) {
			super(c);
			this.isLoaded = isLoaded;
		}
	}
	
	SharedPreferences getConnectivitySharedPrefs() {
		return getSharedPreferences(sharedPreferencesConnectivityFile, Context.MODE_PRIVATE);
	}
	
	boolean isServerConfigured() {
		return !getConnectivitySharedPrefs().getString(sharedPreferencesConnectivityKeyHostname, "").isEmpty();
	}
	
	static final String darkModeFollowSystem = "follow_system";
	static final String darkModeAutomatic = "auto";
	static final String darkModeAlwaysLight = "off";
	static final String darkModeAlwaysDark = "on";
	void applyDarkMode(String method) {
		switch(method) {
			case darkModeFollowSystem: //Follow system
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
				break;
			case darkModeAutomatic: //Automatic
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
				break;
			case darkModeAlwaysLight: //Always light
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
				break;
			case darkModeAlwaysDark: //Always dark
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
				break;
		}
	}
}