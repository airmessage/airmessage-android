package me.tagavari.airmessage.receiver;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import me.tagavari.airmessage.helper.NotificationHelper;

//Broadcast receiver for handling the dismissal of message notifications
public class MessageNotificationDeleteReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		//Dismiss the summary notification when all individual notifications are removed
		NotificationHelper.tryDismissMessageSummaryNotification((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE));
	}
}