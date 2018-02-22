package me.tagavari.airmessage;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;

class NotificationUtils {
	static void sendNotification(Context context, ConversationManager.MessageInfo messageInfo) {
		//Returning message is outgoing or the message's conversation is loaded
		if(messageInfo.isOutgoing() || Messaging.getLoadedConversations().contains(messageInfo.getConversationInfo().getLocalID())) return;
		
		//Returning if notifications are disabled or the conversation is muted
		if((Build.VERSION.SDK_INT < Build.VERSION_CODES.O && !PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preference_messagenotifications_getnotifications_key), false)) || messageInfo.getConversationInfo().isMuted()) return;
		
		//Adding the message
		addMessageToNotification(context, messageInfo.getConversationInfo(), messageInfo.getSummary(context), messageInfo.getSender(), messageInfo.getDate());
	}
	
	/* private static Notification getSummaryNotification(Context context) {
		//Creating the click intent
		Intent clickIntent = new Intent(context, Conversations.class);
		
		//Getting the pending intent
		PendingIntent clickPendingIntent = PendingIntent.getActivity(context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		//Creating the notification builder
		return new NotificationCompat.Builder(context, MainApplication.notificationChannelMessage)
				//Setting the icon
				.setSmallIcon(R.drawable.message)
				//Setting the color
				.setColor(Color.GREEN)
				//Setting the click intent
				.setContentIntent(clickPendingIntent)
				//Setting the group
				.setGroup(MainApplication.notificationGroupMessage)
				//Setting the message as the group summary
				.setGroupSummary(true)
				//Setting the importance as high
				.setPriority(Notification.PRIORITY_HIGH)
				//Building the notification
				.build();
	} */
	
	private static Bitmap getCroppedBitmap(Bitmap bitmap) {
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(output);
		
		final int color = 0xff424242;
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		
		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		// canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
		canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2, bitmap.getWidth() / 2, paint);
		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);
		//Bitmap _bmp = Bitmap.createScaledBitmap(output, 60, 60, false);
		//return _bmp;
		return output;
	}
	
	private static NotificationCompat.Builder getBaseMessageNotification(Context context, ConversationManager.ConversationInfo conversationInfo, Bitmap userIcon, boolean playSound) {
		//Creating the click intent
		Intent clickIntent = new Intent(context, Messaging.class);
		clickIntent.putExtra(Constants.intentParamTargetID, conversationInfo.getLocalID());
		
		//Creating the task stack builder
		TaskStackBuilder clickStackBuilder = TaskStackBuilder.create(context);
		
		//Adding the back stack
		clickStackBuilder.addParentStack(Messaging.class);
		
		//Setting the result intent
		clickStackBuilder.addNextIntent(clickIntent);
		
		//Getting the pending intent
		PendingIntent clickPendingIntent = clickStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
		
		//Creating the notification builder
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, MainApplication.notificationChannelMessage)
				//Setting the icon
				.setSmallIcon(R.drawable.message)
				//Setting the intent
				.setContentIntent(clickPendingIntent)
				//Setting the color
				.setColor(context.getResources().getColor(R.color.colorNotification, null))
				//Setting and showing the timestamp
				//.setWhen(messageInfo.getDateTime().toDate().getTime())
				//.setShowWhen(true)
				//Setting the group
				.setGroup(MainApplication.notificationGroupMessage)
				//Setting the category
				.setCategory(Notification.CATEGORY_MESSAGE);
		//TODO add person
		//.addPerson();
		
		//Checking if the Android version is below Oreo
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			//Setting the sound
			notificationBuilder.setSound(Uri.parse(PreferenceManager.getDefaultSharedPreferences(context).getString(context.getResources().getString(R.string.preference_messagenotifications_sound_key), null)));
			
			//Enabling vibration if it is enabled in the preferences
			if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preference_messagenotifications_vibrate_key), false)) notificationBuilder.setVibrate(new long[]{1000, 1000});
			
			//Setting the priority
			notificationBuilder.setPriority(Notification.PRIORITY_HIGH);
		}
		
		//Disabling alerts if a sound shouldn't be played
		notificationBuilder.setOnlyAlertOnce(!playSound);
		
		//Setting the user icon if it is valid
		if(userIcon != null) notificationBuilder.setLargeIcon(getCroppedBitmap(userIcon));
		
		//Creating the remote input
		RemoteInput remoteInput = new RemoteInput.Builder(Constants.notificationReplyKey)
				.setLabel(context.getResources().getString(R.string.notification_remoteinput_reply))
				.build();
		
		//Creating the reply intent
		Intent replyIntent = new Intent(context, NotificationResponse.class);
		replyIntent.putExtra(Constants.intentParamData, conversationInfo);
		//replyIntent.putExtra(Constants.intentParamTargetID, conversationInfo.getLocalID());
		//replyIntent.putExtra(Constants.intentParamGuid, conversationInfo.getGuid());
		PendingIntent replyPendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), (int) conversationInfo.getLocalID(), replyIntent, 0);
		
		//Getting a notification action from the remote input
		NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.drawable.reply, context.getResources().getString(R.string.button_reply), replyPendingIntent).setAllowGeneratedReplies(true).addRemoteInput(remoteInput).build();
		
		//Adding the action
		notificationBuilder.addAction(action);
		
		//Returning the notification
		return notificationBuilder;
	}
	
	private static void addMessageToNotification(Context context, ConversationManager.ConversationInfo conversationInfo, String message, String sender, long timestamp) {
		//Getting the conversation title
		conversationInfo.buildTitle(context, (conversationTitle, wasTasked) -> {
			//Checking if the sender is the user
			if(sender == null) {
				//Sending the notification without an icon if the conversation is a group chat
				if(conversationInfo.isGroupChat()) addMessageToNotificationPrepared(context, conversationInfo, conversationTitle, message, null, null, timestamp);
				else {
					//Fetching the icon of the recipient member
					String member = conversationInfo.getConversationMembers().get(0).getName();
					MainApplication.getInstance().getBitmapCacheHelper().getBitmapFromContact(context, member, member, new BitmapCacheHelper.ImageDecodeResult() {
						@Override
						void onImageMeasured(int width, int height) {}
						
						@Override
						void onImageDecoded(Bitmap result, boolean wasTasked) {
							//Sending the notification
							addMessageToNotificationPrepared(context, conversationInfo, conversationTitle, message, result, null, timestamp);
						}
					});
				}
			}
			//Otherwise getting the user info
			else MainApplication.getInstance().getUserCacheHelper().getUserInfo(context, sender, new UserCacheHelper.UserFetchResult() {
				@Override
				void onUserFetched(UserCacheHelper.UserInfo userInfo, boolean wasTasked) {
					//Sending the notification without an icon if the conversation is a group chat or the user is invalid
					if(userInfo == null || conversationInfo.isGroupChat()) addMessageToNotificationPrepared(context, conversationInfo, conversationTitle, message, null, userInfo == null || userInfo.getContactName() == null ? sender : userInfo.getContactName(), timestamp);
					//else if(conversationInfo.isGroupChat()) addMessageToNotificationPrepared(context, conversationInfo, conversationTitle, message, null, userInfo.getContactName() == null ? sender, timestamp);
					//Otherwise fetching the user's icon
					else MainApplication.getInstance().getBitmapCacheHelper().getBitmapFromContact(context, sender, sender, new BitmapCacheHelper.ImageDecodeResult() {
						@Override
						void onImageMeasured(int width, int height) {}
						
						@Override
						void onImageDecoded(Bitmap result, boolean wasTasked) {
							//Sending the notification
							addMessageToNotificationPrepared(context, conversationInfo, conversationTitle, message, result, userInfo.getContactName() == null ? sender : userInfo.getContactName(), timestamp);
						}
					});
				}
			});
		});
	}
	
	private static void addMessageToNotificationPrepared(Context context, ConversationManager.ConversationInfo conversationInfo, String conversationTitle, String message, Bitmap userIcon, String senderName, long timestamp) {
		//Creating the base notification
		NotificationCompat.Builder notification = getBaseMessageNotification(context, conversationInfo, userIcon, senderName != null);
		
		//Getting the notification manager
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		
		//Getting the existing notification
		Notification existingNotification = getNotification(notificationManager, (int) conversationInfo.getLocalID());
		
		if(existingNotification != null) {
			//Extracting the messaging style
			NotificationCompat.MessagingStyle messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(existingNotification);
			
			//Adding the new message
			messagingStyle.addMessage(message, timestamp, senderName);
			
			//Setting the messaging style to the notification
			notification.setStyle(messagingStyle);
			
			//Updating the other notification information
			//notification.setLargeIcon(Bitmap.createBitmap());
		} else {
			//Creating the messaging style
			NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle(context.getResources().getString(R.string.you)).addMessage(message, timestamp, senderName);
			
			//Configuring the messaging style
			if(conversationInfo.isGroupChat()) messagingStyle.setConversationTitle(conversationTitle);
			
			//Setting the messaging style to the notification
			notification.setStyle(messagingStyle);
		}
		
		//Sending the notification
		notificationManager.notify((int) conversationInfo.getLocalID(), notification.build());
	}
	
	public static class NotificationResponse extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			//Getting the conversation info
			final ConversationManager.ConversationInfo conversationInfo = (ConversationManager.ConversationInfo) intent.getSerializableExtra(Constants.intentParamData);
			
			//Getting the response
			final CharSequence responseMessage = getMessage(intent);
			
			//Checking if the response is invalid
			if(responseMessage == null) {
				//Refreshing the notification
				NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
				Notification existingNotification = getNotification(notificationManager, (int) conversationInfo.getLocalID());
				if(existingNotification != null) notificationManager.notify((int) conversationInfo.getLocalID(), existingNotification);
				
				//Returning
				return;
			}
			
			//Getting the connection service
			ConnectionService connectionService = ConnectionService.getInstance();
			
			//Checking if the service isn't running
			if(connectionService == null) {
				//Starting the service
				context.startService(new Intent(context, ConnectionService.class));
				
				//Refreshing the notification
				NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
				Notification existingNotification = getNotification(notificationManager, (int) conversationInfo.getLocalID());
				if(existingNotification != null) notificationManager.notify((int) conversationInfo.getLocalID(), existingNotification);
				
				//Returning
				return;
			}
			
			//Getting the notification manager
			final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			
			//Sending the message
			connectionService.sendMessage(conversationInfo.getGuid(), responseMessage.toString(), new ConnectionService.MessageResponseManager() {
				@Override
				void onSuccess() {
					//Adding the message
					addMessageToNotification(context, conversationInfo, responseMessage.toString(), null, System.currentTimeMillis() / 1000L);
				}
				
				@Override
				void onFail(byte responseCode) {
					//Refreshing the notification
					Notification existingNotification = getNotification(notificationManager, (int) conversationInfo.getLocalID());
					if(existingNotification != null) notificationManager.notify((int) conversationInfo.getLocalID(), existingNotification);
				}
			});
		}
		
		private CharSequence getMessage(Intent intent) {
			Bundle remoteInput = android.app.RemoteInput.getResultsFromIntent(intent);
			if(remoteInput != null) {
				return remoteInput.getCharSequence(Constants.notificationReplyKey);
			}
			return null;
		}
	}
	
	private static Notification getNotification(NotificationManager notificationManager, int identifier) {
		//Getting the existing notification
		for(StatusBarNotification statusBarNotification : notificationManager.getActiveNotifications()) {
			//Skipping the remainder of the iteration if the ID does not match
			if(statusBarNotification.getId() != identifier) continue;
			
			//Returning the notification
			return statusBarNotification.getNotification();
		}
		
		//Returning null
		return null;
	}
	
	/* public static class IntentResponse extends Activity {
		@Override
		protected void onCreate(Bundle savedInstanceState) {
			//Calling the super method
			super.onCreate(savedInstanceState);
			
			//Getting the intent
			Intent intent = getIntent();
			
			//Returning if the intent is not the send intent
			if(!intent.getAction().equals(Intent.ACTION_SENDTO)) return;
			
			//Getting the message
			String message = intent.getStringExtra(Intent.EXTRA_TEXT);
		}
	} */
}