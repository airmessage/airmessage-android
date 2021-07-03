package me.tagavari.airmessage.helper

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import me.tagavari.airmessage.R
import me.tagavari.airmessage.activity.ServerConfigStandalone
import me.tagavari.airmessage.connection.ConnectionManager
import me.tagavari.airmessage.constants.ExternalLinkConstants
import me.tagavari.airmessage.enums.ConnectionErrorCode
import me.tagavari.airmessage.helper.ConnectionServiceLaunchHelper.launchAutomatic
import java.util.function.BiConsumer

object ErrorDetailsHelper {
	private val actionReconnectService = BiConsumer { activity: Activity, connectionManager: ConnectionManager? ->
		if(connectionManager == null) {
			launchAutomatic(activity)
		} else {
			connectionManager.connect()
		}
	}
	
	/**
	 * Gets user-readable error details in response to an error code
	 * @param errorCode The error code
	 * @param onlyConfig TRUE to only provide buttons that should be shown during configuration - otherwise, standard 'reconnect' and 'reconfigure' buttons are also provided
	 * @return The error details with a string resource of the error description, and a button action (if applicable)
	 */
	@JvmStatic
	fun getErrorDetails(@ConnectionErrorCode errorCode: Int, onlyConfig: Boolean): ErrorDetails {
		val labelRes: Int
		var button: ErrorDetails.Button? = null
		when(errorCode) {
			ConnectionErrorCode.user, ConnectionErrorCode.connection -> {
				labelRes = if(onlyConfig) R.string.message_connectionerror else R.string.message_serverstatus_noconnection
				if(!onlyConfig) button = ErrorDetails.Button(R.string.action_reconnect, actionReconnectService)
			}
			ConnectionErrorCode.internet -> {
				labelRes = R.string.message_serverstatus_nointernet
				if(!onlyConfig) button = ErrorDetails.Button(R.string.action_retry, actionReconnectService)
			}
			ConnectionErrorCode.internalError -> {
				labelRes = R.string.message_serverstatus_internalexception
				if(!onlyConfig) button = ErrorDetails.Button(R.string.action_retry, actionReconnectService)
			}
			ConnectionErrorCode.externalError -> {
				labelRes = R.string.message_serverstatus_externalexception
				if(!onlyConfig) button = ErrorDetails.Button(R.string.action_retry, actionReconnectService)
			}
			ConnectionErrorCode.badRequest -> {
				labelRes = R.string.message_serverstatus_badrequest
				if(!onlyConfig) button = ErrorDetails.Button(R.string.action_retry, actionReconnectService)
			}
			ConnectionErrorCode.clientOutdated -> {
				labelRes = R.string.message_serverstatus_clientoutdated
				button = ErrorDetails.Button(R.string.action_update) { activity, _ -> activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + activity.packageName))) }
			}
			ConnectionErrorCode.serverOutdated -> {
				labelRes = R.string.message_serverstatus_serveroutdated
				button = ErrorDetails.Button(R.string.screen_help) { activity, _ -> activity.startActivity(Intent(Intent.ACTION_VIEW, ExternalLinkConstants.serverUpdateAddress)) }
			}
			ConnectionErrorCode.directUnauthorized -> {
				labelRes = R.string.message_serverstatus_authfail
				if(!onlyConfig) button = ErrorDetails.Button(R.string.action_reconfigure) { activity, _ -> activity.startActivity(Intent(activity, ServerConfigStandalone::class.java)) }
			}
			ConnectionErrorCode.connectNoGroup -> {
				labelRes = R.string.message_serverstatus_nogroup
				if(!onlyConfig) button = ErrorDetails.Button(R.string.action_retry, actionReconnectService)
			}
			ConnectionErrorCode.connectNoCapacity -> {
				labelRes = R.string.message_serverstatus_nocapacity
				if(!onlyConfig) button = ErrorDetails.Button(R.string.action_retry, actionReconnectService)
			}
			ConnectionErrorCode.connectAccountValidation -> {
				labelRes = R.string.message_serverstatus_accountvalidation
				if(!onlyConfig) button = ErrorDetails.Button(R.string.action_retry, actionReconnectService)
			}
			ConnectionErrorCode.connectNoActivation -> {
				labelRes = R.string.message_serverstatus_noactivation
				if(!onlyConfig) button = ErrorDetails.Button(R.string.action_retry, actionReconnectService)
			}
			ConnectionErrorCode.connectOtherLocation -> {
				labelRes = R.string.message_serverstatus_otherlocation
				if(!onlyConfig) button = ErrorDetails.Button(R.string.action_retry, actionReconnectService)
			}
			else -> {
				labelRes = R.string.message_serverstatus_unknown
				if(!onlyConfig) button = ErrorDetails.Button(R.string.action_retry, actionReconnectService)
			}
		}
		return ErrorDetails(labelRes, button)
	}
	
	/**
	 * Represents error details to show to the user, including a description label and an optional button action
	 */
	data class ErrorDetails(@StringRes val label: Int, val button: Button?) {
		data class Button(@StringRes val label: Int, val clickListener: BiConsumer<Activity, ConnectionManager?>)
	}
}