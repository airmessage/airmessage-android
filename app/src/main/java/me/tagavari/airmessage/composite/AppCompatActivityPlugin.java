package me.tagavari.airmessage.composite;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

public abstract class AppCompatActivityPlugin {
	private AppCompatActivity activity;
	
	protected void onCreate(@Nullable Bundle savedInstanceState) {};
	protected void onStart() {};
	protected void onResume() {};
	protected void onPause() {};
	protected void onStop() {};
	protected void onDestroy() {};
	
	void setActivity(AppCompatActivity activity) {
		this.activity = activity;
	}
	
	protected AppCompatActivity getActivity() {
		return activity;
	}
}