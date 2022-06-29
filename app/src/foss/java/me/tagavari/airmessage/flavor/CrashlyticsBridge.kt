package me.tagavari.airmessage.flavor

import android.content.Context

object CrashlyticsBridge {
	@JvmStatic
	fun configure(context: Context) = Unit
	
	@JvmStatic
	fun recordException(throwable: Throwable) = Unit
	
	@JvmStatic
	fun log(message: String) = Unit
}