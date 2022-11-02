package me.tagavari.airmessage.flavor

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import me.tagavari.airmessage.compose.BuildConfig
import me.tagavari.airmessage.data.SharedPreferencesManager

object CrashlyticsBridge {
	@JvmStatic
	fun configure(context: Context) {
		//Setting the user identifier
		FirebaseCrashlytics.getInstance().setUserId(SharedPreferencesManager.getInstallationID(context))
		
		//Disable Crashlytics in debug mode
		FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
		
		if(SharedPreferencesManager.isConnectionConfigured(context)) {
			FirebaseCrashlytics.getInstance().setCustomKey("proxy_type", if(SharedPreferencesManager.getProxyType(context) == me.tagavari.airmessage.enums.ProxyType.direct) "direct" else "connect")
		}
	}
	
	@JvmStatic
	fun recordException(throwable: Throwable) = FirebaseCrashlytics.getInstance().recordException(throwable)
	
	@JvmStatic
	fun log(message: String) = FirebaseCrashlytics.getInstance().log(message)
}