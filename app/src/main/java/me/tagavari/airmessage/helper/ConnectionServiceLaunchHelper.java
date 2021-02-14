package me.tagavari.airmessage.helper;

import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

import me.tagavari.airmessage.data.SharedPreferencesManager;
import me.tagavari.airmessage.enums.ProxyType;
import me.tagavari.airmessage.service.ConnectionService;

public class ConnectionServiceLaunchHelper {
	/**
	 * Launches the connection service a either a temporary or persistent foreground service, depending on the selected proxy type
	 */
	public static void launchAutomatic(Context context) {
		switch(SharedPreferencesManager.getProxyType(context)) {
			case ProxyType.direct:
				launchPersistent(context);
				break;
			case ProxyType.connect:
				launchTemporary(context);
				break;
		}
	}
	
	/**
	 * Launches the connection service as a persistent foreground service
	 */
	public static void launchPersistent(Context context) {
		ContextCompat.startForegroundService(context,
				new Intent(context, ConnectionService.class)
						.setAction(ConnectionService.selfIntentActionConnect)
						.putExtra(ConnectionService.selfIntentExtraForeground, true)
		);
	}
	
	/**
	 * Launches the connection service as a temporary foreground service
	 */
	public static void launchTemporary(Context context) {
		ContextCompat.startForegroundService(context,
				new Intent(context, ConnectionService.class)
						.setAction(ConnectionService.selfIntentActionConnect)
						.putExtra(ConnectionService.selfIntentExtraTemporary, true)
						.putExtra(ConnectionService.selfIntentExtraForeground, true)
		);
	}
}