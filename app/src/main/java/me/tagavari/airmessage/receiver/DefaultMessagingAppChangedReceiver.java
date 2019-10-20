package me.tagavari.airmessage.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;

import me.tagavari.airmessage.activity.Preferences;
import me.tagavari.airmessage.service.SystemMessageImportService;

public class DefaultMessagingAppChangedReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		//Checking if text message integration is in an invalid state (enabled, but permissions are missing)
		if(Preferences.getPreferenceTextMessageIntegration(context) && !Preferences.isTextMessageIntegrationActive(context)) {
			//Disabling text message integration
			Preferences.setPreferenceTextMessageIntegration(context, false);
			
			//Clearing the database of text messages
			context.startService(new Intent(context, SystemMessageImportService.class).setAction(SystemMessageImportService.selfIntentActionDelete));
		}
	}
}