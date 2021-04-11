package me.tagavari.airmessage.helper

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import me.tagavari.airmessage.R
import me.tagavari.airmessage.activity.Preferences
import me.tagavari.airmessage.constants.ColorConstants

object ThemeHelper {
	const val darkModeFollowSystem = "follow_system"
	const val darkModeLight = "off"
	const val darkModeDark = "on"
	
	/**
	 * Gets if the app is currently being displayed in night mode
	 */
	@JvmStatic
	fun isNightMode(resources: Resources): Boolean {
		return when(resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
			Configuration.UI_MODE_NIGHT_YES -> true
			Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_UNDEFINED -> false
			else -> false
		}
	}
	
	/**
	 * Gets if an AMOLED theme should be used
	 */
	@JvmStatic
	fun shouldUseAMOLED(context: Context) =
			isNightMode(context.resources) && Preferences.getPreferenceAMOLED(context)
	
	/**
	 * Sets the app's theming method
	 */
	@JvmStatic
	fun applyDarkMode(method: String?) {
		when(method) {
			darkModeFollowSystem -> {
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) //On Android Q and above, the app should follow the system's dark mode setting
				else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY) //On older versions of Android, "automatic" should mean to follow the battery saver setting
			}
			darkModeLight -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
			darkModeDark -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
		}
	}
	
	@JvmStatic
	fun setActivityAMOLEDBase(activity: AppCompatActivity) {
		activity.findViewById<View>(android.R.id.content).rootView.setBackgroundColor(ColorConstants.colorAMOLED)
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) activity.window.navigationBarColor = ColorConstants.colorAMOLED //Leaving the transparent navigation bar on Android 10
		activity.window.statusBarColor = ColorConstants.colorAMOLED
		activity.supportActionBar!!.setBackgroundDrawable(ColorDrawable(ColorConstants.colorAMOLED))
		
		for(view in ViewHelper.getViewsByTag(activity.findViewById(android.R.id.content), activity.resources.getString(R.string.tag_amoleddivider))) {
			view.visibility = View.VISIBLE
		}
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
			//The bottom divider is only necessary for actual navigation bars
			for(view in ViewHelper.getViewsByTag(activity.findViewById(android.R.id.content), activity.resources.getString(R.string.tag_amoleddivider_bottom))) {
				view.visibility = View.VISIBLE
			}
		}
	}
}