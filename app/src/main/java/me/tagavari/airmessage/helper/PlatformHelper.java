package me.tagavari.airmessage.helper;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

import me.tagavari.airmessage.R;

public class PlatformHelper {
	/**
	 * Gets if the current operating system is Chrome OS
	 */
	public static boolean isChromeOS(Context context) {
		return context.getPackageManager().hasSystemFeature("org.chromium.arc.device_management");
	}
	
	/**
	 * Update the color of the status bar to match Chrome OS
	 */
	public static void updateChromeOSStatusBar(AppCompatActivity activity) {
		//Ignoring if not running on a Chrome OS device
		if(!isChromeOS(activity)) return;
		
		//Setting the statusbar color
		activity.getWindow().setStatusBarColor(activity.getResources().getColor(R.color.colorSubBackground, null));
	}
}