package me.tagavari.airmessage

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Process
import android.provider.ContactsContract
import android.webkit.WebView
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.tagavari.airmessage.activity.CrashReport
import me.tagavari.airmessage.activity.Preferences
import me.tagavari.airmessage.coil.IconFetcher
import me.tagavari.airmessage.coil.VCardFetcher
import me.tagavari.airmessage.compose.R
import me.tagavari.airmessage.data.DatabaseManager
import me.tagavari.airmessage.data.SharedPreferencesManager
import me.tagavari.airmessage.data.SharedPreferencesManager.getTextMessageConversationsInstalled
import me.tagavari.airmessage.data.UserCacheHelper
import me.tagavari.airmessage.flavor.CrashlyticsBridge
import me.tagavari.airmessage.flavor.MapsBridge
import me.tagavari.airmessage.helper.NotificationHelper
import me.tagavari.airmessage.helper.ThemeHelper
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxReceiverFaceTime
import me.tagavari.airmessage.redux.ReduxReceiverNotification
import me.tagavari.airmessage.redux.ReduxReceiverShortcut
import me.tagavari.airmessage.worker.SystemMessageCleanupWorker
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.PrintWriter
import java.io.StringWriter
import java.security.Security

class MainApplication : Application(), ImageLoaderFactory {
	lateinit var userCacheHelper: UserCacheHelper
		private set
	
	private val contentObserver = object : ContentObserver(null) {
		override fun onChange(selfChange: Boolean) {
			super.onChange(selfChange)
			
			Handler(mainLooper).post {
				userCacheHelper.clearCache()
				triggerContactsUpdate()
			}
		}
		
		override fun deliverSelfNotifications(): Boolean {
			return true
		}
	}
	
	init {
		//Initialize a custom crash reporter if in debug mode
		if(BuildConfig.DEBUG) {
			Thread.setDefaultUncaughtExceptionHandler { _: Thread, exception: Throwable ->
				//Print the exception
				exception.printStackTrace()
				
				//Get the exception
				val stringWriter = StringWriter()
				val printWriter = PrintWriter(stringWriter)
				exception.printStackTrace(printWriter)
				
				//Launch the error activity
				val activityIntent = Intent(this, CrashReport::class.java)
						.putExtra(CrashReport.PARAM_STACKTRACE, stringWriter.toString())
						.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
				startActivity(activityIntent)
				Process.killProcess(Process.myPid())
			}
		}
	}
	
	override fun onCreate() {
		super.onCreate()
		
		//Configure crash reporting
		CrashlyticsBridge.configure(this)
		
		//Set the application instance
		instance = this
		
		//Upgrade shared preferences data
		SharedPreferencesManager.upgradeSharedPreferences(this)
		
		//Initialize notification channels
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationHelper.initializeChannels(this)
		}
		
		//Create the cache helpers
		userCacheHelper = UserCacheHelper()
		
		//Create the database manager
		DatabaseManager.createInstance(this)
		
		//Apply dark mode
		PreferenceManager.getDefaultSharedPreferences(this)
			.getString(getString(R.string.preference_appearance_theme_key), null)
			?.let { ThemeHelper.applyDarkMode(it) }
		
		//Apply dynamic color
		DynamicColors.applyToActivitiesIfAvailable(this)
		
		//Register the content observer
		if(canUseContacts(this)) {
			registerContactsListener()
		}
		
		//Load the initial FaceTime support state
		ReduxEmitterNetwork.serverFaceTimeSupportSubject.onNext(SharedPreferencesManager.getServerSupportsFaceTime(this))
		
		//Listen for content changes
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
			ReduxReceiverShortcut(this).initialize()
		}
		ReduxReceiverNotification(this).initialize()
		ReduxReceiverFaceTime(this).initialize()
		
		//Check if text message integration is not permitted
		if(!Preferences.isTextMessageIntegrationActive(this)) {
			var cleanUpMessages = false
			
			//Check if the toggle is enabled (creating an invalid state)
			if(Preferences.getPreferenceTextMessageIntegration(this)) {
				//Disable text message integration
				Preferences.setPreferenceTextMessageIntegration(this, false)
				
				//Clear the database of text messages
				cleanUpMessages = true
			} else if(getTextMessageConversationsInstalled(this)) {
				cleanUpMessages = true
			}
			
			if(cleanUpMessages) {
				val workRequest = OneTimeWorkRequest.Builder(SystemMessageCleanupWorker::class.java)
					.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
					.build()
				
				//If this work is already enqueued (likely from our broadcast listener), replace it with our expedited request
				WorkManager.getInstance(this).enqueueUniqueWork(
					SystemMessageCleanupWorker.workName,
					ExistingWorkPolicy.REPLACE,
					workRequest
				)
			}
		}
		
		//Register BouncyCastle as a security provider on older versions
		//Otherwise, Android provides everything we need by default, so we'll just stick with that
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			Security.removeProvider("BC")
			Security.insertProviderAt(BouncyCastleProvider(), 1)
		}
		
		//Initialize Google Maps
		MapsBridge.initialize(this)
		
		//Enable WebView debugging
		if(BuildConfig.DEBUG) {
			WebView.setWebContentsDebuggingEnabled(true)
		}
	}
	
	/**
	 * Registers for updates to contacts
	 * @param triggerImmediate Whether to trigger an update immediately
	 */
	fun registerContactsListener(triggerImmediate: Boolean = false) {
		contentResolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contentObserver)
		
		if(triggerImmediate) {
			triggerContactsUpdate()
		}
	}
	
	@OptIn(DelicateCoroutinesApi::class)
	private fun triggerContactsUpdate() {
		GlobalScope.launch {
			ReduxEmitterNetwork.contactUpdates.emit(System.currentTimeMillis())
		}
	}
	
	override fun onTrimMemory(level: Int) {
		super.onTrimMemory(level)
		
		if(level >= TRIM_MEMORY_BACKGROUND) {
			//Clear the caches
			userCacheHelper.clearCache()
		}
	}
	
	override fun newImageLoader() = ImageLoader.Builder(this)
		.components {
			//Decode GIFs
			if(Build.VERSION.SDK_INT >= 28) {
				add(ImageDecoderDecoder.Factory())
			} else {
				add(GifDecoder.Factory())
			}
			
			//Decode videos
			add(VideoFrameDecoder.Factory())
			
			//Decode icons
			add(IconFetcher.Factory())
			
			//Decode vcard thumbnails
			add(VCardFetcher.Factory())
		}
		.build()
	
	companion object {
		@get:JvmStatic
		@get:JvmName("getInstance")
		lateinit var instance: MainApplication
			private set
		
		fun canUseContacts(context: Context): Boolean {
			return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
		}
	}
}
