package me.tagavari.airmessage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

public class LaunchActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Calling the super method
		super.onCreate(savedInstanceState);
		
		//Setting the layout
		//setContentView(R.layout.activity_splash);
		
		//Getting the connection service information
		SharedPreferences sharedPrefs = getSharedPreferences(MainApplication.sharedPreferencesFile, Context.MODE_PRIVATE);
		String hostname = sharedPrefs.getString(MainApplication.sharedPreferencesKeyHostname, "");
		String password = sharedPrefs.getString(MainApplication.sharedPreferencesKeyPassword, "");
		
		//Checking if there is no hostname
		if(hostname.isEmpty()) {
			//Creating the intent
			Intent launchServerSetup = new Intent(this, ServerSetup.class);
			
			//Setting the change as required
			launchServerSetup.putExtra(ServerSetup.intentExtraRequired, true);
			
			//Launching the intent
			startActivity(launchServerSetup);
		} else {
			//Starting the connection service
			Intent serviceIntent = new Intent(this, ConnectionService.class);
			startService(serviceIntent);
			
			//Launching the conversations activity
			startActivity(new Intent(this, Conversations.class));
		}
	}
}