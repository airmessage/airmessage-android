package me.tagavari.airmessage.flavor

import android.content.Context
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import me.tagavari.airmessage.common.data.SharedPreferencesManager
import me.tagavari.airmessage.common.enums.ProxyType

object CrashlyticsBridge {
	private val TAG = CrashlyticsBridge::class.java.simpleName
	
	@JvmStatic
	fun configure(context: Context) {
		//Setting the user identifier
		FirebaseCrashlytics.getInstance().setUserId(SharedPreferencesManager.getInstallationID(context))
		
		//Disable Crashlytics in debug mode
		FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!me.tagavari.airmessage.BuildConfig.DEBUG)
		
		if(SharedPreferencesManager.isConnectionConfigured(context)) {
			FirebaseCrashlytics.getInstance().setCustomKey("proxy_type", if(SharedPreferencesManager.getProxyType(context) == ProxyType.direct) "direct" else "connect")
		}
	}
	
	@JvmStatic
	fun recordException(throwable: Throwable) = FirebaseCrashlytics.getInstance().recordException(throwable)
	
	@JvmStatic
	fun log(message: String) {
		Log.i(TAG, "Crashlytics log message: $message")
		FirebaseCrashlytics.getInstance().log(message)
	}
}