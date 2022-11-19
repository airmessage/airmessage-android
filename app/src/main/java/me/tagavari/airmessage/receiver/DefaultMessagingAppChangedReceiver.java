package me.tagavari.airmessage.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import me.tagavari.airmessage.activity.Preferences;
import me.tagavari.airmessage.common.data.SharedPreferencesManager;
import me.tagavari.airmessage.service.SystemMessageImportService;
import me.tagavari.airmessage.worker.SystemMessageCleanupWorker;

@RequiresApi(api = Build.VERSION_CODES.N)
public class DefaultMessagingAppChangedReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if(!Telephony.Sms.Intents.ACTION_DEFAULT_SMS_PACKAGE_CHANGED.equals(intent.getAction())) return;
		
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
				ContextCompat.startForegroundService(context, new Intent(context, SystemMessageImportService.class).setAction(SystemMessageImportService.selfIntentActionImport));
			}
		} else {
			//Checking if text message integration is in an invalid state (enabled, but permissions are missing)
			if(conversationsInstalled) {
				//Disabling text message integration
				Preferences.setPreferenceTextMessageIntegration(context, false);

				//Clearing the database of text messages
				OneTimeWorkRequest workRequest = OneTimeWorkRequest.from(SystemMessageCleanupWorker.class);
				WorkManager.getInstance(context).enqueueUniqueWork(SystemMessageCleanupWorker.workName, ExistingWorkPolicy.KEEP, workRequest);
			}
		}
	}
}