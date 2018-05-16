package me.tagavari.airmessage;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;

import me.tagavari.airmessage.composite.AppCompatActivityPlugin;

public class PluginThemeUpdater extends AppCompatActivityPlugin {
	private int currentNightMode;
	
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Recording the current night mode
		currentNightMode = getCurrentNightMode();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		//Recreating the activity if night mode has changed
		if(currentNightMode != getCurrentNightMode()) {
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				((AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE)).set(AlarmManager.RTC, System.currentTimeMillis() + 100, PendingIntent.getActivity(getActivity(), 123456, getActivity().getIntent(), PendingIntent.FLAG_CANCEL_CURRENT));
				System.exit(0);
			} else getActivity().recreate();
		}
	}
	
	private int getCurrentNightMode() {
		return getActivity().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
	}
}