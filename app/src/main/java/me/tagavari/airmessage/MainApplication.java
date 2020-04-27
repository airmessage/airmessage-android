package me.tagavari.airmessage;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Process;
import android.provider.ContactsContract;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import io.fabric.sdk.android.Fabric;
import me.tagavari.airmessage.activity.CrashReport;
import me.tagavari.airmessage.activity.Preferences;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.data.BitmapCacheHelper;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.data.UserCacheHelper;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.receiver.StartBootReceiver;
import me.tagavari.airmessage.service.ConnectionService;
import me.tagavari.airmessage.service.SystemMessageImportService;
import me.tagavari.airmessage.util.Constants;

public class MainApplication extends Application {
	//Creating the reference values
	public static final String notificationChannelMessage = "message";
	public static final String notificationChannelMessageError = "message_error";
	public static final String notificationChannelStatus = "status";
	public static final String notificationChannelStatusImportant = "status_important";
	
	public static final String notificationGroupMessage = "me.tagavari.airmessage.NOTIFICATION_GROUP_MESSAGE";
	
	public static final int notificationIDConnectionService = -1;
	public static final int notificationIDConnectionServiceAlert = -2;
	public static final int notificationIDMessageImport = -3;
	public static final int notificationIDMessageSummary = -4;
	
	public static final String dirNameDownload = "downloads";
	public static final String dirNameUpload = "uploads";
	public static final String dirNameDraft = "draft";
	
	private static final String sharedPreferencesConnectivityFile = "connectivity";
	public static final String sharedPreferencesConnectivityKeyAccountType = "account_type"; //The account type that the user is logged in with (direct connection or AM Connect)
	public static final String sharedPreferencesConnectivityKeyConnectServerConfirmed = "connect_server_confirmed"; //TRUE if this account has confirmed its connection with the server
	public static final String sharedPreferencesConnectivityKeyLastSyncInstallationID = "last_sync_installation_id"; //The installation ID recorded when messages were last synced (or cleared), used for tracking when the user should be prompted to re-sync their messages
	public static final String sharedPreferencesConnectivityKeyHostname = "hostname";
	public static final String sharedPreferencesConnectivityKeyHostnameFallback = "hostname_fallback";
	public static final String sharedPreferencesConnectivityKeyPassword = "password";
	public static final String sharedPreferencesConnectivityKeyLastConnectionTime = "last_connection_time";
	public static final String sharedPreferencesConnectivityKeyLastConnectionInstallationID = "last_connection_installation_id";
	public static final String sharedPreferencesConnectivityKeyLastConnectionHostname = "last_connection_hostname"; //Legacy, protocol 4
	public static final String sharedPreferencesConnectivityKeyTextMessageConversationsInstalled = "text_message_conversations_installed";
	
	private static final int sharedPreferencesSchemaVersion = 2;
	public static final String sharedPreferencesDefaultKeySchemaVersion = "default_schemaversion";
	public static final String sharedPreferencesDefaultKeyInstallationID = "default_installationID";
	
	public static final String localBCContactUpdate = "LocalMSG-Main-ContactUpdate";
	
	private final ContentObserver contentObserver = new ContentObserver(null) {
		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			new Handler(getMainLooper()).post(() -> {
				userCacheHelper.clearCache();
				bitmapCacheHelper.clearUserCache();
				LocalBroadcastManager.getInstance(MainApplication.this).sendBroadcast(new Intent(localBCContactUpdate));
			});
		}
		
		@Override
		public boolean deliverSelfNotifications() {
			return true;
		}
	};
	
	//Creating the cache helpers
	private BitmapCacheHelper bitmapCacheHelper;
	private UserCacheHelper userCacheHelper;
	private SoftReference<LoadFlagArrayList<ConversationInfo>> conversationReference = new SoftReference<>(new LoadFlagArrayList<>(false));
	
	//Creating the references
	private static WeakReference<MainApplication> instanceReference = null;
	
	//Creating the other reference values
	private static final BouncyCastleProvider securityProvider = new BouncyCastleProvider();
	
	private Thread.UncaughtExceptionHandler defaultUEH;
	
	public MainApplication() {
		//Initializing a custom crash reporter if in debug mode
		if(BuildConfig.DEBUG) {
			//Getting the system's default uncaught exception handler
			defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
			
			Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
				//Printing the exception
				exception.printStackTrace();
				
				//Getting the exception
				StringWriter stringWriter = new StringWriter();
				PrintWriter printWriter = new PrintWriter(stringWriter);
				exception.printStackTrace(printWriter);
				
				//Launching the error activity
				Intent activityIntent = new Intent(this, CrashReport.class).putExtra(CrashReport.PARAM_STACKTRACE, stringWriter.toString()).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(activityIntent);
				Process.killProcess(Process.myPid());
			});
		}
	}
	
	@Override
	public void onCreate() {
		//Calling the super method
		super.onCreate();
		
		/* if(LeakCanary.isInAnalyzerProcess(this)) {
			// This process is dedicated to LeakCanary for heap analysis.
			// You should not init your app in this process.
			return;
		}
		LeakCanary.install(this); */
		
		//Configuring crash reporting
		configureCrashReporting();
		
		//Setting the instance
		instanceReference = new WeakReference<>(this);
		
		//Upgrading shared preferences data
		upgradeSharedPreferences();
		
		//Checking if the device is running Android Oreo (API 26)
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			//Initializing the notification channels
			NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			{
				NotificationChannel channel = new NotificationChannel(notificationChannelMessage, getResources().getString(R.string.notificationchannel_message), NotificationManager.IMPORTANCE_HIGH);
				channel.setDescription(getString(R.string.notificationchannel_message_desc));
				channel.enableVibration(true);
				channel.setShowBadge(true);
				channel.enableLights(true);
				channel.setSound(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.notification_ding),
						new AudioAttributes.Builder()
								.setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
								.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
								.setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
								.build());
				//messageChannel.setGroup(notificationGroupMessage);
				notificationManager.createNotificationChannel(channel);
			}
			{
				NotificationChannel channel = new NotificationChannel(notificationChannelMessageError, getResources().getString(R.string.notificationchannel_messageerror), NotificationManager.IMPORTANCE_HIGH);
				channel.setDescription(getString(R.string.notificationchannel_messageerror_desc));
				channel.enableVibration(true);
				channel.setShowBadge(true);
				channel.enableLights(true);
				//messageChannel.setGroup(notificationGroupMessage);
				notificationManager.createNotificationChannel(channel);
			}
			{
				NotificationChannel channel = new NotificationChannel(notificationChannelStatus, getResources().getString(R.string.notificationchannel_status), NotificationManager.IMPORTANCE_MIN);
				channel.setDescription(getString(R.string.notificationchannel_status_desc));
				channel.enableVibration(false);
				channel.setShowBadge(false);
				channel.enableLights(false);
				notificationManager.createNotificationChannel(channel);
			}
			{
				NotificationChannel channel = new NotificationChannel(notificationChannelStatusImportant, getResources().getString(R.string.notificationchannel_statusimportant), NotificationManager.IMPORTANCE_LOW);
				channel.setDescription(getString(R.string.notificationchannel_statusimportant_desc));
				channel.enableVibration(true);
				channel.setShowBadge(false);
				channel.enableLights(false);
				notificationManager.createNotificationChannel(channel);
			}
		}
		
		//Getting the connection service information
		SharedPreferences sharedPrefs = getSharedPreferences(sharedPreferencesConnectivityFile, Context.MODE_PRIVATE);
		ConnectionManager.hostname = sharedPrefs.getString(sharedPreferencesConnectivityKeyHostname, null);
		ConnectionManager.hostnameFallback = sharedPrefs.getString(sharedPreferencesConnectivityKeyHostnameFallback, null);
		ConnectionManager.password = sharedPrefs.getString(sharedPreferencesConnectivityKeyPassword, null);
		
		//Creating the cache helpers
		bitmapCacheHelper = new BitmapCacheHelper();
		userCacheHelper = new UserCacheHelper();
		
		//Creating the database manager
		DatabaseManager.createInstance(this);
		
		//Applying the dark mode
		applyDarkMode(PreferenceManager.getDefaultSharedPreferences(this).getString(getResources().getString(R.string.preference_appearance_theme_key), ""));
		
		//Enabling / disabling the service on boot as per the shared preference
		getPackageManager().setComponentEnabledSetting(new ComponentName(this, StartBootReceiver.class),
				PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getResources().getString(R.string.preference_server_connectionboot_key), true) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
				PackageManager.DONT_KILL_APP);
		
		//Registering the content observer
		if(canUseContacts(this)) registerContactsListener();
		
		//Checking if text message integration is not permitted
		if(!Preferences.isTextMessageIntegrationActive(this)) {
			//Checking if the toggle is enabled (creating an invalid state)
			if(Preferences.getPreferenceTextMessageIntegration(this)) {
				//Disabling text message integration
				Preferences.setPreferenceTextMessageIntegration(this, false);
				
				//Clearing the database of text messages
				Intent serviceIntent = new Intent(this, SystemMessageImportService.class).setAction(SystemMessageImportService.selfIntentActionDelete);
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent);
				else startService(serviceIntent);
			}
			//Clearing the database of text messages if there are still text messages in the database (in the case that the application is killed before it can clear all its messages)
			else if(getConnectivitySharedPrefs().getBoolean(sharedPreferencesConnectivityKeyTextMessageConversationsInstalled, false)) {
				Intent serviceIntent = new Intent(this, SystemMessageImportService.class).setAction(SystemMessageImportService.selfIntentActionDelete);
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent);
				else startService(serviceIntent);
			}
		}
	}
	
	public static MainApplication getInstance() {
		return instanceReference == null ? null : instanceReference.get();
	}
	
	private void configureCrashReporting() {
		//Initializing crashlytics, disabled in debug mode
		//CrashlyticsCore core = new CrashlyticsCore.Builder().build();
		CrashlyticsCore core = new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build();
		Fabric.with(this, new Crashlytics.Builder().core(core).build());
	}
	
	/* private static final String oldSharedPrefsName = "me.tagavari.airmessage.MAIN_PREFERENCES";
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
	} */
	
	public static String getFileAuthority(Context context) {
		return context.getPackageName() + ".fileprovider";
	}
	
	public static File getAttachmentDirectory(Context context) {
		//Getting the media directory
		File file = new File(context.getFilesDir(), "attachments");
		
		//Preparing the media directory
		prepareDirectory(file);
		
		//Preparing the subdirectories
		prepareDirectory(new File(file, dirNameDownload));
		prepareDirectory(new File(file, dirNameUpload));
		prepareDirectory(new File(file, dirNameDraft));
		
		//Returning the media directory
		return file;
	}
	
	public static File getDownloadDirectory(Context context) {
		return new File(getAttachmentDirectory(context), dirNameDownload);
	}
	
	public static File getUploadDirectory(Context context) {
		return new File(getAttachmentDirectory(context), dirNameUpload);
	}
	
	public static File getUploadTarget(Context context, String fileName) {
		File directory = Constants.findFreeFile(getUploadDirectory(context), Long.toString(System.currentTimeMillis()), false);
		prepareDirectory(directory);
		return new File(directory, fileName);
	}
	
	public static File getDraftDirectory(Context context) {
		return new File(getAttachmentDirectory(context), dirNameDraft);
	}
	
	public static File getDraftTarget(Context context, long conversationID, String fileName) {
		File conversationDir = new File(getDraftDirectory(context), Long.toString(conversationID));
		prepareDirectory(conversationDir);
		File collisionDir = Constants.findFreeFile(conversationDir, false); //Collision-avoidance directory: creates a directory, starting at index 0 in the conversation directory, each to host 1 file
		prepareDirectory(collisionDir);
		return new File(collisionDir, fileName);
	}
	
	public static File findUploadFileTarget(Context context, String fileName) {
		//Finding a free directory and assigning the file to it
		return new File(Constants.findFreeFile(getUploadDirectory(context), Long.toString(System.currentTimeMillis()), false), fileName);
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
	
	public static void clearAttachmentsDirectory(Context context) {
		for(File childFiles : MainApplication.getAttachmentDirectory(context).listFiles()) Constants.recursiveDelete(childFiles);
	}
	
	public BitmapCacheHelper getBitmapCacheHelper() {
		return bitmapCacheHelper;
	}
	
	public UserCacheHelper getUserCacheHelper() {
		return userCacheHelper;
	}
	
	public void registerContactsListener() {
		getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contentObserver);
	}
	
	public static boolean canUseContacts(Context context) {
		//Returning if the permission has been granted
		return ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
	}
	
	public void setConversations(LoadFlagArrayList<ConversationInfo> conversations) {
		conversationReference = new SoftReference<>(conversations);
	}
	
	public LoadFlagArrayList<ConversationInfo> getConversations() {
		if(conversationReference == null) return null;
		return conversationReference.get();
	}
	
	public static class LoadFlagArrayList<E> extends ArrayList<E> {
		private boolean isLoaded;
		
		public boolean isLoaded() {
			return isLoaded;
		}
		
		public void setLoaded(boolean loaded) {
			isLoaded = loaded;
		}
		
		public LoadFlagArrayList(int initialCapacity, boolean isLoaded) {
			super(initialCapacity);
			this.isLoaded = isLoaded;
		}
		
		public LoadFlagArrayList(boolean isLoaded) {
			this.isLoaded = isLoaded;
		}
		
		public LoadFlagArrayList(@NonNull Collection<? extends E> c, boolean isLoaded) {
			super(c);
			this.isLoaded = isLoaded;
		}
	}
	
	public SharedPreferences getConnectivitySharedPrefs() {
		return getSharedPreferences(sharedPreferencesConnectivityFile, Context.MODE_PRIVATE);
	}
	
	public boolean isServerConfigured() {
		return getConnectivitySharedPrefs().getBoolean(sharedPreferencesConnectivityKeyConnectServerConfirmed, false);
	}
	
	public void startConnectionService() {
		startService(new Intent(this, ConnectionService.class));
	}
	
	public static BouncyCastleProvider getSecurityProvider() {
		return securityProvider;
	}
	
	@Override
	public void onTrimMemory(int level) {
		super.onTrimMemory(level);
		
		if(level >= TRIM_MEMORY_MODERATE) {
			//Clearing the messages
			if(conversationReference != null) conversationReference.clear();
		}
		
		if(level >= TRIM_MEMORY_BACKGROUND) {
			//Clearing the caches
			bitmapCacheHelper.clearCache();
			userCacheHelper.clearCache();
		}
	}
	
	public static final String darkModeFollowSystem = "follow_system";
	//public static final String darkModeAutomatic = "auto";
	public static final String darkModeLight = "off";
	public static final String darkModeDark = "on";
	public void applyDarkMode(String method) {
		switch(method) {
			case darkModeFollowSystem: //Follow system
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); //On Android Q and above, the app should follow the system's dark mode setting
				else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY); //On older versions of Android, "automatic" should mean to follow the battery saver setting
				break;
			case darkModeLight: //Always light
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
				break;
			case darkModeDark: //Always dark
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
				break;
		}
	}
	
	private void upgradeSharedPreferences() {
		//Reading the current schema version
		SharedPreferences defaultSP = PreferenceManager.getDefaultSharedPreferences(this);
		int schemaVer = defaultSP.getInt(sharedPreferencesDefaultKeySchemaVersion, -1);
		
		//Fresh installation or upgrade from pre-0.6
		if(schemaVer == -1) {
			//Editing the shared preferences
			SharedPreferences.Editor defaultEditor = defaultSP.edit();
			
			//Migrate the fallback address from "preferences" shared preferences to "connectivity" shared preferences
			String fallbackKey = "pref_key_server_fallbackaddress";
			if(PreferenceManager.getDefaultSharedPreferences(this).contains(fallbackKey)) {
				//Getting the fallback
				String fallback = defaultSP.getString(fallbackKey, null);
				
				//Removing the fallback from "preferences" shared preferences
				defaultEditor.remove(fallbackKey);
				
				//Adding the fallback to "connectivity" shared preferences
				getConnectivitySharedPrefs().edit().putString(sharedPreferencesConnectivityKeyHostnameFallback, fallback).commit();
			}
			
			//Setting a notrigger hostname to prevent the sync prompt from showing up after updating
			if(getConnectivitySharedPrefs().contains(sharedPreferencesConnectivityKeyLastConnectionHostname)) {
				getConnectivitySharedPrefs().edit()
						.putString(sharedPreferencesConnectivityKeyLastSyncInstallationID, "notrigger")
						.commit();
			}
			
			//Saving the new schema version
			defaultEditor.putInt(sharedPreferencesDefaultKeySchemaVersion, sharedPreferencesSchemaVersion);
			defaultEditor.commit();
			return;
		}
		
		/* //Returning if the schema doesn't need to be updated
		if(schemaVer >= sharedPreferencesSchemaVersion) return;
		
		//Editing the shared preferences
		SharedPreferences.Editor defaultEditor = defaultSP.edit();
		
		switch(schemaVer) {
			case 1: {
			
			}
		}
		
		//Updating the new shared preferences version
		defaultEditor.putInt(sharedPreferencesDefaultKeySchemaVersion, sharedPreferencesSchemaVersion);
		
		defaultEditor.commit(); */
	}
	
	public String getInstallationID() {
		SharedPreferences defaultSP = PreferenceManager.getDefaultSharedPreferences(this);
		
		//Reading and returning the UUID
		String installationID = defaultSP.getString(sharedPreferencesDefaultKeyInstallationID, null);
		if(installationID != null) return installationID;
		
		//Generating, saving and returning a new UUID if an existing one doesn't exist
		installationID = UUID.randomUUID().toString();
		defaultSP.edit()
				.putString(sharedPreferencesDefaultKeyInstallationID, installationID)
				.apply();
		return installationID;
	}
}