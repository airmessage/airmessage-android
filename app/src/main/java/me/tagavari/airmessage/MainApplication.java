package me.tagavari.airmessage;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Handler;
import android.os.Process;
import android.provider.ContactsContract;
import android.webkit.WebView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;

import com.google.android.gms.maps.MapsInitializer;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.security.Security;

import me.tagavari.airmessage.activity.CrashReport;
import me.tagavari.airmessage.activity.Preferences;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.data.SharedPreferencesManager;
import me.tagavari.airmessage.data.UserCacheHelper;
import me.tagavari.airmessage.enums.ProxyType;
import me.tagavari.airmessage.helper.NotificationHelper;
import me.tagavari.airmessage.helper.ThemeHelper;
import me.tagavari.airmessage.redux.ReduxEmitterNetwork;
import me.tagavari.airmessage.redux.ReduxReceiverFaceTime;
import me.tagavari.airmessage.redux.ReduxReceiverNotification;
import me.tagavari.airmessage.redux.ReduxReceiverShortcut;
import me.tagavari.airmessage.worker.SystemMessageCleanupWorker;

public class MainApplication extends Application {
	//Creating the reference values
	public static final String localBCContactUpdate = "LocalMSG-Main-ContactUpdate";
	
	private final ContentObserver contentObserver = new ContentObserver(null) {
		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			new Handler(getMainLooper()).post(() -> {
				userCacheHelper.clearCache();
				LocalBroadcastManager.getInstance(MainApplication.this).sendBroadcast(new Intent(localBCContactUpdate));
			});
		}
		
		@Override
		public boolean deliverSelfNotifications() {
			return true;
		}
	};
	
	//Creating the cache helpers
	private UserCacheHelper userCacheHelper;
	
	//Creating the references
	private static WeakReference<MainApplication> instanceReference = null;
	
	public MainApplication() {
		//Initializing a custom crash reporter if in debug mode
		if(BuildConfig.DEBUG) {
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
		
		//Configuring crash reporting
		configureCrashReporting();
		
		//Setting the instance
		instanceReference = new WeakReference<>(this);
		
		//Upgrading shared preferences data
		SharedPreferencesManager.upgradeSharedPreferences(this);
		
		//Checking if the device is running Android Oreo (API 26)
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			//Initializing the notification channels
			NotificationHelper.initializeChannels(this);
		}
		
		//Creating the cache helpers
		userCacheHelper = new UserCacheHelper();
		
		//Creating the database manager
		DatabaseManager.createInstance(this);
		
		//Applying the dark mode
		ThemeHelper.applyDarkMode(PreferenceManager.getDefaultSharedPreferences(this).getString(getResources().getString(R.string.preference_appearance_theme_key), ""));
		
		//Registering the content observer
		if(canUseContacts(this)) registerContactsListener();

		//Load the initial FaceTime support state
		ReduxEmitterNetwork.getServerFaceTimeSupportSubject().onNext(SharedPreferencesManager.getServerSupportsFaceTime(this));
		
		//Listening for content changes
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
			new ReduxReceiverShortcut(this).initialize();
		}
		new ReduxReceiverNotification(this).initialize();
		new ReduxReceiverFaceTime(this).initialize();
		
		//Checking if text message integration is not permitted
		if(!Preferences.isTextMessageIntegrationActive(this)) {
			boolean cleanUpMessages = false;
			//Checking if the toggle is enabled (creating an invalid state)
			if(Preferences.getPreferenceTextMessageIntegration(this)) {
				//Disabling text message integration
				Preferences.setPreferenceTextMessageIntegration(this, false);
				
				//Clearing the database of text messages
				cleanUpMessages = true;
			}
			//Clearing the database of text messages if there are still text messages in the database (in the case that the application is killed before it can clear all its messages)
			else if(SharedPreferencesManager.getTextMessageConversationsInstalled(this)) {
				cleanUpMessages = true;
			}

			if(cleanUpMessages) {
				OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SystemMessageCleanupWorker.class)
						.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
						.build();

				//If this work is already enqueued (likely from our broadcast listener), replace it with our expedited request
				WorkManager.getInstance(this).enqueueUniqueWork(SystemMessageCleanupWorker.workName, ExistingWorkPolicy.REPLACE, workRequest);
			}
		}
		
		//Registering BouncyCastle as a security provider on older versions
		//Otherwise, Android provides everything we need by default, so we'll just stick with that
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			Security.removeProvider("BC");
			int insertionIndex = Security.insertProviderAt(new BouncyCastleProvider(), 1);
		}

		//Initializing Google Maps
		MapsInitializer.initialize(getApplicationContext(), MapsInitializer.Renderer.LATEST, null);
		
		//Enable WebView debugging
		if(0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
			WebView.setWebContentsDebuggingEnabled(true);
		}
	}
	
	public static MainApplication getInstance() {
		return instanceReference == null ? null : instanceReference.get();
	}
	
	private void configureCrashReporting() {
		//Setting the user identifier
		FirebaseCrashlytics.getInstance().setUserId(SharedPreferencesManager.getInstallationID(this));
		
		//Disable Crashlytics in debug mode
		FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG);
		
		if(SharedPreferencesManager.isConnectionConfigured(this)) {
			FirebaseCrashlytics.getInstance().setCustomKey("proxy_type", SharedPreferencesManager.getProxyType(this) == ProxyType.direct ? "direct" : "connect");
		}
	}
	
	public UserCacheHelper getUserCacheHelper() {
		return userCacheHelper;
	}
	
	public void registerContactsListener() {
		getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contentObserver);
	}
	
	public static boolean canUseContacts(Context context) {
		//Returning if the permission has been granted
		return context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
	}
	
	@Override
	public void onTrimMemory(int level) {
		super.onTrimMemory(level);
		
		if(level >= TRIM_MEMORY_BACKGROUND) {
			//Clearing the caches
			userCacheHelper.clearCache();
		}
	}
}