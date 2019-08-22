package me.tagavari.airmessage.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ServiceStartBoot extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		//Returning if the service is not a boot service
		if(!"android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) return;
		
		//Starting the service
		Intent serviceIntent = new Intent(context, ConnectionService.class);
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(serviceIntent);
		else context.startService(serviceIntent);
	}
}
