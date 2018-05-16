package me.tagavari.airmessage.composite;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

public abstract class ActivityPlugin {
	private Activity activity;
	
	protected void onCreate(@Nullable Bundle savedInstanceState) {};
	protected void onStart() {};
	protected void onResume() {};
	protected void onPause() {};
	protected void onStop() {};
	protected void onDestroy() {};
	
	void setActivity(Activity activity) {
		this.activity = activity;
	}
	
	protected Activity getActivity() {
		return activity;
	}
}