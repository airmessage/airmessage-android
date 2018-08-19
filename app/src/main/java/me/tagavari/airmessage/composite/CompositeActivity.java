package me.tagavari.airmessage.composite;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

@SuppressLint("Registered")
public class CompositeActivity extends Activity {
	private final List<ActivityPlugin> pluginList = new ArrayList<>();
	
	public void addPlugin(ActivityPlugin activityPlugin) {
		pluginList.add(activityPlugin);
		activityPlugin.setActivity(this);
	}
	
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		for(ActivityPlugin plugin : pluginList) plugin.onCreate(savedInstanceState);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		for(ActivityPlugin plugin : pluginList) plugin.onStart();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		for(ActivityPlugin plugin : pluginList) plugin.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		for(ActivityPlugin plugin : pluginList) plugin.onPause();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		for(ActivityPlugin plugin : pluginList) plugin.onStart();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		for(ActivityPlugin plugin : pluginList) plugin.onDestroy();
	}
}