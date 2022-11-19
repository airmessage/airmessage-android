package me.tagavari.airmessage.common.helper

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import me.tagavari.airmessage.R

object PlatformHelper {
	/**
	 * Gets if the current operating system is Chrome OS
	 */
	@JvmStatic
	fun isChromeOS(context: Context): Boolean {
		return context.packageManager.hasSystemFeature("org.chromium.arc.device_management")
	}
	
	/**
	 * Update the color of the status bar to match Chrome OS
	 */
	@JvmStatic
	fun updateChromeOSStatusBar(activity: AppCompatActivity) {
		//Ignoring if not running on a Chrome OS device
		if(!isChromeOS(activity)) return
		
		//Setting the statusbar color
		activity.window.statusBarColor = activity.resources.getColor(R.color.colorSubBackground, null)
	}
}