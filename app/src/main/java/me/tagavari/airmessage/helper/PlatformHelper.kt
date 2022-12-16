package me.tagavari.airmessage.helper

import android.app.Activity
import android.content.Context
import android.util.TypedValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import me.tagavari.airmessage.R

object PlatformHelper {
	/**
	 * Gets if the current operating system is Chrome OS
	 */
	@JvmStatic
	fun isChromeOS(context: Context): Boolean {
		val pm = context.packageManager
		return pm.hasSystemFeature("org.chromium.arc") ||
				pm.hasSystemFeature("org.chromium.arc.device_management")
	}
	
	/**
	 * Update the color of the status bar to match Chrome OS
	 */
	@JvmStatic
	fun updateChromeOSTopBar(activity: Activity) {
		//Ignore if not running on a Chrome OS device
		if(!isChromeOS(activity)) return
		
		//Set the status bar color
		val typedValue = TypedValue()
		activity.theme.resolveAttribute(R.attr.colorOnSurfaceInverse, typedValue, true)
		val color = activity.getColor(typedValue.resourceId)
		activity.window.statusBarColor = color
	}
	
	@Composable
	fun updateChromeOSTopBarCompose(activity: Activity) {
		val color = MaterialTheme.colorScheme.inverseOnSurface
		LaunchedEffect(color) {
			activity.window.statusBarColor = color.toArgb()
		}
	}
}