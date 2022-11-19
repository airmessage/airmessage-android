package me.tagavari.airmessage.common.helper

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.tagavari.airmessage.activity.OnboardingCompose
import me.tagavari.airmessage.common.data.SharedPreferencesManager.setConnectionConfigured
import me.tagavari.airmessage.flavor.FirebaseAuthBridge
import me.tagavari.airmessage.service.ConnectionService

object ConfigurationHelper {
	/**
	 * Resets the configuration of the current user,
	 * and opens the onboarding activity
	 */
	@OptIn(DelicateCoroutinesApi::class)
	fun resetConfiguration(context: Context) {
		GlobalScope.launch {
			//Set the server as not confirmed
			setConnectionConfigured(context, false)
			
			//Stop the connection service
			context.stopService(Intent(context, ConnectionService::class.java))
			
			//Sign out
			FirebaseAuthBridge.signOut(context)
			
			//Open the onboarding activity
			context.startActivity(
				Intent(context, OnboardingCompose::class.java)
					.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
			)
		}
	}
}
