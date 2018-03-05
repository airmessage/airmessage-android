package me.tagavari.airmessage;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.pascalwelsch.compositeandroid.activity.ActivityPlugin;

public class PluginThemeUpdater extends ActivityPlugin {
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
		if(currentNightMode != getCurrentNightMode()) getActivity().recreate();
	}
	
	private int getCurrentNightMode() {
		return getActivity().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
	}
}