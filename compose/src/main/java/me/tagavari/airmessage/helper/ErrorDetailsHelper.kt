package me.tagavari.airmessage.helper

import androidx.annotation.StringRes
import me.tagavari.airmessage.compose.R
import me.tagavari.airmessage.enums.ConnectionErrorCode

object ErrorDetailsHelper {
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
				if(!onlyConfig) button = ErrorDetails.Button(R.string.action_reconnect, ErrorDetailsAction.RECONNECT)
			}
			ConnectionErrorCode.internet -> {
				labelRes = R.string.message_serverstatus_nointernet
				if(!onlyConfig) button = ErrorDetails.Button(R.string.action_retry, ErrorDetailsAction.RECONNECT)
			}
			ConnectionErrorCode.internalError -> {
				labelRes = R.string.message_serverstatus_internalexception
				if(!onlyConfig) button = ErrorDetails.Button(R.string.action_retry, ErrorDetailsAction.RECONNECT)
			}
			ConnectionErrorCode.externalError -> {
				labelRes = R.string.message_serverstatus_externalexception
				if(!onlyConfig) button = ErrorDetails.Button(R.string.action_retry, ErrorDetailsAction.RECONNECT)
			}
			ConnectionErrorCode.badRequest -> {
				labelRes = R.string.message_serverstatus_badrequest
				if(!onlyConfig) button = ErrorDetails.Button(R.string.action_retry, ErrorDetailsAction.RECONNECT)
			}
			ConnectionErrorCode.clientOutdated -> {
				labelRes = R.string.message_serverstatus_clientoutdated
				button = ErrorDetails.Button(R.string.action_update, ErrorDetailsAction.UPDATE_APP)
			}
			ConnectionErrorCode.serverOutdated -> {
				labelRes = R.string.message_serverstatus_serveroutdated
				button = ErrorDetails.Button(R.string.screen_help, ErrorDetailsAction.UPDATE_SERVER)
			}
			ConnectionErrorCode.unauthorized -> {
				labelRes = R.string.message_serverstatus_authfail
				if(!onlyConfig) {
					button = ErrorDetails.Button(R.string.action_reconfigure, ErrorDetailsAction.CHANGE_PASSWORD)
				}
			}
			ConnectionErrorCode.connectNoGroup -> {
				labelRes = R.string.message_serverstatus_nogroup
				if(!onlyConfig) button = ErrorDetails.Button(R.string.action_retry, ErrorDetailsAction.RECONNECT)
			}
			ConnectionErrorCode.connectNoCapacity -> {
				labelRes = R.string.message_serverstatus_nocapacity
				if(!onlyConfig) button = ErrorDetails.Button(R.string.action_retry, ErrorDetailsAction.RECONNECT)
			}
			ConnectionErrorCode.connectAccountValidation -> {
				labelRes = R.string.message_serverstatus_accountvalidation
				if(!onlyConfig) button = ErrorDetails.Button(R.string.action_retry, ErrorDetailsAction.RECONNECT)
			}
			ConnectionErrorCode.connectNoActivation -> {
				labelRes = R.string.message_serverstatus_noactivation
				if(!onlyConfig) button = ErrorDetails.Button(R.string.action_retry, ErrorDetailsAction.RECONNECT)
			}
			ConnectionErrorCode.connectOtherLocation -> {
				labelRes = R.string.message_serverstatus_otherlocation
				if(!onlyConfig) button = ErrorDetails.Button(R.string.action_retry, ErrorDetailsAction.RECONNECT)
			}
			else -> {
				labelRes = R.string.message_serverstatus_unknown
				if(!onlyConfig) button = ErrorDetails.Button(R.string.action_retry, ErrorDetailsAction.RECONNECT)
			}
		}
		return ErrorDetails(labelRes, button)
	}
	
	/**
	 * Represents error details to show to the user, including a description label and an optional button action
	 */
	data class ErrorDetails(@StringRes val label: Int, val button: Button?) {
		data class Button(@StringRes val label: Int, val action: ErrorDetailsAction)
	}
}

enum class ErrorDetailsAction {
	RECONNECT,
	UPDATE_APP,
	UPDATE_SERVER,
	CHANGE_PASSWORD
}
