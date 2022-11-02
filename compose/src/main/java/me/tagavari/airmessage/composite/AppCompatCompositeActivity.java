package me.tagavari.airmessage.composite;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class AppCompatCompositeActivity extends AppCompatActivity {
	private final List<AppCompatActivityPlugin> pluginList = new ArrayList<>();
	
	public void addPlugin(AppCompatActivityPlugin activityPlugin) {
		pluginList.add(activityPlugin);
		activityPlugin.setActivity(this);
	}
	
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		for(AppCompatActivityPlugin plugin : pluginList) plugin.onCreate(savedInstanceState);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		for(AppCompatActivityPlugin plugin : pluginList) plugin.onStart();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		for(AppCompatActivityPlugin plugin : pluginList) plugin.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		for(AppCompatActivityPlugin plugin : pluginList) plugin.onPause();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		for(AppCompatActivityPlugin plugin : pluginList) plugin.onStop();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		for(AppCompatActivityPlugin plugin : pluginList) plugin.onDestroy();
	}
}