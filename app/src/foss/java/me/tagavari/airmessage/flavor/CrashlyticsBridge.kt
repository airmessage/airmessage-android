package me.tagavari.airmessage.flavor

import android.content.Context
import android.util.Log

object CrashlyticsBridge {
	private val TAG = CrashlyticsBridge::class.java.simpleName
	
	@JvmStatic
	fun configure(context: Context) = Unit
	
	@JvmStatic
	fun recordException(throwable: Throwable) = Unit
	
	@JvmStatic
	fun log(message: String) {
		Log.i(TAG, "Crashlytics log message: $message")
	}
}
