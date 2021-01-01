package me.tagavari.airmessage.helper;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.function.BiConsumer;

import me.tagavari.airmessage.R;
import me.tagavari.airmessage.activity.ServerConfigStandalone;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.constants.ExternalLinkConstants;
import me.tagavari.airmessage.enums.ConnectionErrorCode;

public class ErrorDetailsHelper {
	private static final BiConsumer<Activity, ConnectionManager> actionReconnectService = (activity, connectionManager) -> connectionManager.connect();
	
	/**
	 * Gets user-readable error details in response to an error code
	 * @param errorCode The error code
	 * @param onlyConfig TRUE to only provide buttons that should be shown during configuration - otherwise, standard 'reconnect' and 'reconfigure' buttons are also provided
	 * @return The error details with a string resource of the error description, and a button action (if applicable)
	 */
	public static ErrorDetails getErrorDetails(@ConnectionErrorCode int errorCode, boolean onlyConfig) {
		int labelRes;
		ErrorDetails.Button button = null;
		
		switch(errorCode) {
			case ConnectionErrorCode.user:
			case ConnectionErrorCode.connection:
				labelRes = onlyConfig ? R.string.message_connectionerror : R.string.message_serverstatus_noconnection;
				if(!onlyConfig) button = new ErrorDetails.Button(R.string.action_reconnect, actionReconnectService);
				break;
			case ConnectionErrorCode.internet:
				labelRes = R.string.message_serverstatus_nointernet;
				if(!onlyConfig) button = new ErrorDetails.Button(R.string.action_retry, actionReconnectService);
				break;
			case ConnectionErrorCode.internalError:
				labelRes = R.string.message_serverstatus_internalexception;
				if(!onlyConfig) button = new ErrorDetails.Button(R.string.action_retry, actionReconnectService);
				break;
			case ConnectionErrorCode.externalError:
				labelRes = R.string.message_serverstatus_externalexception;
				if(!onlyConfig) button = new ErrorDetails.Button(R.string.action_retry, actionReconnectService);
				break;
			case ConnectionErrorCode.badRequest:
				labelRes = R.string.message_serverstatus_badrequest;
				if(!onlyConfig) button = new ErrorDetails.Button(R.string.action_retry, actionReconnectService);
				break;
			case ConnectionErrorCode.clientOutdated:
				labelRes = R.string.message_serverstatus_clientoutdated;
				button = new ErrorDetails.Button(R.string.action_update, (activity, connectionManager) -> activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + activity.getPackageName()))));
				break;
			case ConnectionErrorCode.serverOutdated:
				labelRes = R.string.message_serverstatus_serveroutdated;
				button = new ErrorDetails.Button(R.string.screen_help, (activity, connectionManager) -> activity.startActivity(new Intent(Intent.ACTION_VIEW, ExternalLinkConstants.serverUpdateAddress)));
				break;
			case ConnectionErrorCode.directUnauthorized:
				labelRes = R.string.message_serverstatus_authfail;
				if(!onlyConfig) button = new ErrorDetails.Button(R.string.action_reconfigure, (activity, connectionManager) -> activity.startActivity(new Intent(activity, ServerConfigStandalone.class)));
				break;
			case ConnectionErrorCode.connectNoGroup:
				labelRes = R.string.message_serverstatus_nogroup;
				if(!onlyConfig) button = new ErrorDetails.Button(R.string.action_retry, actionReconnectService);
				break;
			case ConnectionErrorCode.connectNoCapacity:
				labelRes = R.string.message_serverstatus_nocapacity;
				if(!onlyConfig) button = new ErrorDetails.Button(R.string.action_retry, actionReconnectService);
				break;
			case ConnectionErrorCode.connectAccountValidation:
				labelRes = R.string.message_serverstatus_accountvalidation;
				if(!onlyConfig) button = new ErrorDetails.Button(R.string.action_retry, actionReconnectService);
				break;
			case ConnectionErrorCode.connectNoSubscription:
				labelRes = R.string.message_serverstatus_noenrollment;
				if(!onlyConfig) button = new ErrorDetails.Button(R.string.action_retry, actionReconnectService);
				break;
			case ConnectionErrorCode.connectOtherLocation:
				labelRes = R.string.message_serverstatus_otherlocation;
				if(!onlyConfig) button = new ErrorDetails.Button(R.string.action_retry, actionReconnectService);
				break;
			default:
				labelRes = R.string.message_serverstatus_unknown;
				if(!onlyConfig) button = new ErrorDetails.Button(R.string.action_retry, actionReconnectService);
				break;
		}
		
		return new ErrorDetails(labelRes, button);
	}
	
	/**
	 * Represents error details to show to the user, including a description label and an optional button action
	 */
	public static class ErrorDetails {
		@StringRes private final int label;
		@Nullable private final Button button;
		
		public ErrorDetails(int label, @Nullable Button button) {
			this.label = label;
			this.button = button;
		}
		
		public int getLabel() {
			return label;
		}
		
		@Nullable
		public Button getButton() {
			return button;
		}
		
		public static class Button {
			@StringRes private final int label;
			@NonNull private final BiConsumer<Activity, ConnectionManager> clickListener;
			
			public Button(int label, @NonNull BiConsumer<Activity, ConnectionManager> clickListener) {
				this.label = label;
				this.clickListener = clickListener;
			}
			
			public int getLabel() {
				return label;
			}
			
			@NonNull
			public BiConsumer<Activity, ConnectionManager> getClickListener() {
				return clickListener;
			}
		}
	}
}