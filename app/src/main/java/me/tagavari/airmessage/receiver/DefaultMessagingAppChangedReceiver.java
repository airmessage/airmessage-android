package me.tagavari.airmessage.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;

import androidx.annotation.RequiresApi;

import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.activity.Preferences;
import me.tagavari.airmessage.data.SharedPreferencesManager;
import me.tagavari.airmessage.service.SystemMessageImportService;

@RequiresApi(api = Build.VERSION_CODES.N)
public class DefaultMessagingAppChangedReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		//Getting if this is the new default messaging app
		boolean isDefaultApp = intent.getBooleanExtra(Telephony.Sms.Intents.EXTRA_IS_DEFAULT_SMS_APP, false);
		
		//Getting if text message conversations are installed in the database
		boolean conversationsInstalled = SharedPreferencesManager.getTextMessageConversationsInstalled(context);
		
		//Checking if this is the default app
		if(isDefaultApp) {
			//Checking if text message integration is currently disabled
			if(!conversationsInstalled) {
				//Enabling text message integration
				Preferences.setPreferenceTextMessageIntegration(context, true);
				
				//Starting the import service
				context.startService(new Intent(context, SystemMessageImportService.class).setAction(SystemMessageImportService.selfIntentActionImport));
			}
		} else {
			//Checking if text message integration is in an invalid state (enabled, but permissions are missing)
			if(conversationsInstalled) {
				//Disabling text message integration
				Preferences.setPreferenceTextMessageIntegration(context, false);
				
				//Clearing the database of text messages
				context.startService(new Intent(context, SystemMessageImportService.class).setAction(SystemMessageImportService.selfIntentActionDelete));
			}
		}
	}
}