package me.tagavari.airmessage.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.StringRes;
import androidx.core.util.Consumer;

import me.tagavari.airmessage.R;
import me.tagavari.airmessage.activity.ServerConfigStandalone;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.service.ConnectionService;

public class StateUtils {
	/**
	 * Get user-readable error details in response to an error code
	 * @param errorCode The error code
	 * @param onlyConfig TRUE to only provide buttons that should be shown during configuration - otherwise, standard 'reconnect' and 'reconfigure' buttons are also provided
	 * @return The error details with a string resource of the error description, and a button action (if applicable)
	 */
	public static ErrorDetails getErrorDetails(int errorCode, boolean onlyConfig) {
		int labelRes;
		ErrorDetails.Button button = null;
		
		switch(errorCode) {
			case ConnectionManager.connResultConnection:
				labelRes = onlyConfig ? R.string.message_connectionerror : R.string.message_serverstatus_noconnection;
				if(!onlyConfig) button = new ErrorDetails.Button(R.string.action_reconnect, StateUtils::reconnectService);
				break;
			case ConnectionManager.connResultInternet:
				labelRes = R.string.message_serverstatus_nointernet;
				if(!onlyConfig) button = new ErrorDetails.Button(R.string.action_retry, StateUtils::reconnectService);
				break;
			case ConnectionManager.connResultInternalError:
				labelRes = R.string.message_serverstatus_internalexception;
				if(!onlyConfig) button = new ErrorDetails.Button(R.string.action_retry, StateUtils::reconnectService);
				break;
			case ConnectionManager.connResultExternalError:
				labelRes = R.string.message_serverstatus_externalexception;
				if(!onlyConfig) button = new ErrorDetails.Button(R.string.action_retry, StateUtils::reconnectService);
				break;
			case ConnectionManager.connResultBadRequest:
				labelRes = R.string.message_serverstatus_badrequest;
				if(!onlyConfig) button = new ErrorDetails.Button(R.string.action_retry, StateUtils::reconnectService);
				break;
			case ConnectionManager.connResultClientOutdated:
				labelRes = R.string.message_serverstatus_clientoutdated;
				button = new ErrorDetails.Button(R.string.action_update, activity -> activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + activity.getPackageName()))));
				break;
			case ConnectionManager.connResultServerOutdated:
				labelRes = R.string.message_serverstatus_serveroutdated;
				button = new ErrorDetails.Button(R.string.screen_help, activity -> activity.startActivity(new Intent(Intent.ACTION_VIEW, Constants.serverUpdateAddress)));
				break;
			case ConnectionManager.connResultDirectUnauthorized:
				labelRes = R.string.message_serverstatus_authfail;
				if(!onlyConfig) button = new ErrorDetails.Button(R.string.action_reconfigure, activity -> activity.startActivity(new Intent(activity, ServerConfigStandalone.class)));
				break;
			case ConnectionManager.connResultConnectNoGroup:
				labelRes = R.string.message_serverstatus_nogroup;
				if(!onlyConfig) button = new ErrorDetails.Button(R.string.action_retry, StateUtils::reconnectService);
				break;
			case ConnectionManager.connResultConnectNoCapacity:
				labelRes = R.string.message_serverstatus_nocapacity;
				if(!onlyConfig) button = new ErrorDetails.Button(R.string.action_retry, StateUtils::reconnectService);
				break;
			case ConnectionManager.connResultConnectAccountValidation:
				labelRes = R.string.message_serverstatus_accoutvalidation;
				if(!onlyConfig) button = new ErrorDetails.Button(R.string.action_retry, StateUtils::reconnectService);
				break;
			case ConnectionManager.connResultConnectNoSubscription:
				labelRes = R.string.message_serverstatus_nosubscription;
				if(!onlyConfig) button = new ErrorDetails.Button(R.string.action_retry, StateUtils::reconnectService);
				break;
			case ConnectionManager.connResultConnectOtherLocation:
				labelRes = R.string.message_serverstatus_otherlocation;
				if(!onlyConfig) button = new ErrorDetails.Button(R.string.action_retry, StateUtils::reconnectService);
				break;
			default:
				labelRes = R.string.message_serverstatus_unknown;
				if(!onlyConfig) button = new ErrorDetails.Button(R.string.action_retry, StateUtils::reconnectService);
				break;
		}
		
		return new ErrorDetails(labelRes, button);
	}
	
	public static class ErrorDetails {
		@StringRes
		private final int label;
		private final Button button;
		
		public ErrorDetails(int label, Button button) {
			this.label = label;
			this.button = button;
		}
		
		public int getLabel() {
			return label;
		}
		
		public Button getButton() {
			return button;
		}
		
		public static class Button {
			@StringRes
			private final int label;
			private final Consumer<Activity> clickListener;
			
			public Button(int label, Consumer<Activity> clickListener) {
				this.label = label;
				this.clickListener = clickListener;
			}
			
			public int getLabel() {
				return label;
			}
			
			public Consumer<Activity> getClickListener() {
				return clickListener;
			}
		}
	}
	
	private static void reconnectService(Activity activity) {
		ConnectionManager connectionManager = ConnectionService.getConnectionManager();
		if(connectionManager == null) {
			//Starting the service
			activity.startService(new Intent(activity, ConnectionService.class));
		} else {
			//Reconnecting
			connectionManager.connect(activity);
		}
	}
}