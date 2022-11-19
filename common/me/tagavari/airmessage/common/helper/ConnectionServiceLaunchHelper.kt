package me.tagavari.airmessage.common.helper

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import me.tagavari.airmessage.common.data.SharedPreferencesManager
import me.tagavari.airmessage.common.enums.ProxyType
import me.tagavari.airmessage.service.ConnectionService

object ConnectionServiceLaunchHelper {
	/**
	 * Launches the connection service a either a temporary or persistent foreground service, depending on the selected proxy type
	 */
	@JvmStatic
	fun launchAutomatic(context: Context) {
		when(SharedPreferencesManager.getProxyType(context)) {
			ProxyType.direct -> launchPersistent(context)
			ProxyType.connect -> launchTemporary(context)
		}
	}
	
	/**
	 * Launches the connection service as a persistent foreground service
	 */
	@JvmStatic
	fun launchPersistent(context: Context) {
		ContextCompat.startForegroundService(context,
				Intent(context, ConnectionService::class.java).apply {
					action = ConnectionService.selfIntentActionConnect
					putExtra(ConnectionService.selfIntentExtraForeground, true)
				}
		)
	}
	
	/**
	 * Launches the connection service as a temporary foreground service
	 */
	@JvmStatic
	fun launchTemporary(context: Context) {
		ContextCompat.startForegroundService(context,
				Intent(context, ConnectionService::class.java).apply {
					action = ConnectionService.selfIntentActionConnect
					putExtra(ConnectionService.selfIntentExtraTemporary, true)
					putExtra(ConnectionService.selfIntentExtraForeground, true)
				}
		)
	}
}