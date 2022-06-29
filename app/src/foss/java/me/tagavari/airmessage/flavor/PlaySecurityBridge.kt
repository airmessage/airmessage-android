package me.tagavari.airmessage.flavor

import android.app.Activity
import android.content.Context
import java.util.function.Consumer

object PlaySecurityBridge {
	val isSupported = false
	
	@JvmStatic
	fun showDialog(activity: Activity, errorCode: Int, resultCode: Int) = Unit
	
	@JvmStatic
	fun update(context: Context, onError: Consumer<Int>) = Unit
}