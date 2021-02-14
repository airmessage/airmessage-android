package me.tagavari.airmessage.helper;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.button.MaterialButton;

import me.tagavari.airmessage.R;
import me.tagavari.airmessage.activity.Preferences;
import me.tagavari.airmessage.constants.ColorConstants;

public class ThemeHelper {
	public static final String darkModeFollowSystem = "follow_system";
	//public static final String darkModeAutomatic = "auto";
	public static final String darkModeLight = "off";
	public static final String darkModeDark = "on";
	
	/**
	 * Gets if the app is currently being displayed in night mode
	 */
	public static boolean isNightMode(Resources resources) {
		switch (resources.getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
			case Configuration.UI_MODE_NIGHT_YES:
				return true;
			case Configuration.UI_MODE_NIGHT_NO:
			case Configuration.UI_MODE_NIGHT_UNDEFINED:
			default:
				return false;
		}
	}
	
	/**
	 * Gets if an AMOLED theme should be used
	 */
	public static boolean shouldUseAMOLED(Context context) {
		return isNightMode(context.getResources()) && Preferences.getPreferenceAMOLED(context);
	}
	
	/**
	 * Sets the app's theming method
	 */
	public static void applyDarkMode(String method) {
		switch(method) {
			case darkModeFollowSystem: //Follow system
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); //On Android Q and above, the app should follow the system's dark mode setting
				else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY); //On older versions of Android, "automatic" should mean to follow the battery saver setting
				break;
			case darkModeLight: //Always light
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
				break;
			case darkModeDark: //Always dark
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
				break;
		}
	}
	
	public static void setActivityAMOLEDBase(AppCompatActivity activity) {
		activity.findViewById(android.R.id.content).getRootView().setBackgroundColor(ColorConstants.colorAMOLED);
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) activity.getWindow().setNavigationBarColor(ColorConstants.colorAMOLED); //Leaving the transparent navigation bar on Android 10
		activity.getWindow().setStatusBarColor(ColorConstants.colorAMOLED);
		activity.getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ColorConstants.colorAMOLED));
		
		for(View view : ViewHelper.getViewsByTag(activity.findViewById(android.R.id.content), activity.getResources().getString(R.string.tag_amoleddivider))) {
			view.setVisibility(View.VISIBLE);
		}
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
			//The bottom divider is only necessary for actual navigation bars
			for(View view : ViewHelper.getViewsByTag(activity.findViewById(android.R.id.content), activity.getResources().getString(R.string.tag_amoleddivider_bottom))) {
				view.setVisibility(View.VISIBLE);
			}
		}
	}
}