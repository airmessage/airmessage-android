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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.smartreply.SmartReplySuggestion;
import com.google.firebase.ml.naturallanguage.smartreply.SmartReplySuggestionResult;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;
import androidx.core.app.RemoteInput;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.util.Consumer;

class NotificationUtils {
	static void sendNotification(Context context, ConversationManager.MessageInfo messageInfo) {
		//Returning message is outgoing or the message's conversation is loaded
		if(messageInfo.isOutgoing() || Messaging.getForegroundConversations().contains(messageInfo.getConversationInfo().getLocalID()) || ConnectionService.getInstance() != null && ConnectionService.getInstance().isMassRetrievalInProgress()) return;
		
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
	
	private static NotificationCompat.Builder getBaseMessageNotification(Context context, ConversationManager.ConversationInfo conversationInfo, String userUri, Bitmap userIcon, boolean isOutgoing, String[] replySuggestions) {
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
		PendingIntent clickPendingIntent = clickStackBuilder.getPendingIntent((int) conversationInfo.getLocalID(), 0);
		//PendingIntent clickPendingIntent = PendingIntent.getActivity(context.getApplicationContext(), (int) conversationInfo.getLocalID(), new Intent(context, Messaging.class).putExtra(Constants.intentParamTargetID, conversationInfo.getLocalID()), 0);
		
		//Creating the notification builder
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, MainApplication.notificationChannelMessage)
				//Setting the icon
				.setSmallIcon(R.drawable.message)
				//Setting the intent
				.setContentIntent(clickPendingIntent)
				//Setting the color
				.setColor(context.getResources().getColor(R.color.colorPrimary, null))
				//Setting the group
				.setGroup(MainApplication.notificationGroupMessage)
				//Setting the category
				.setCategory(Notification.CATEGORY_MESSAGE)
				//Adding the person
				.addPerson(userUri);
		
		//Checking if the Android version is below Oreo (on API 26 and above, notification alert details are handled by the system's notification channels)
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			//Setting the sound
			notificationBuilder.setSound(Uri.parse(PreferenceManager.getDefaultSharedPreferences(context).getString(context.getResources().getString(R.string.preference_messagenotifications_sound_key), Constants.defaultNotificationSound)));
			
			//Adding vibration if it is enabled in the preferences
			if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preference_messagenotifications_vibrate_key), false)) notificationBuilder.setVibrate(new long[]{0, 250, 250, 250});
			
			//Setting the priority
			notificationBuilder.setPriority(Notification.PRIORITY_HIGH);
		}
		
		//Disabling alerts if a sound shouldn't be played
		notificationBuilder.setOnlyAlertOnce(isOutgoing);
		
		//Setting the user icon if it is needed and it is valid
		if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1 &&
				!conversationInfo.isGroupChat() &&
				userIcon != null)
			notificationBuilder.setLargeIcon(userIcon);
		
		{
			//Creating the "mark as read" notification action
			Intent intent = new Intent(context, NotificationBroadcastReceiver.class);
			intent.setAction(NotificationBroadcastReceiver.intentActionMarkRead);
			intent.putExtra(Constants.intentParamTargetID, conversationInfo.getLocalID());
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), -(int) conversationInfo.getLocalID(), intent, 0);
			
			NotificationCompat.Action action =
					new NotificationCompat.Action.Builder(R.drawable.check_circle, context.getResources().getString(R.string.action_markread), pendingIntent)
							.setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
							.build();
			
			//Adding the action
			notificationBuilder.addAction(action);
		}
		
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { //Remote input is not supported on Android versions below Nougat
			//Creating the remote input
			RemoteInput remoteInput = new RemoteInput.Builder(Constants.notificationReplyKey)
					.setLabel(context.getResources().getString(R.string.action_reply))
					.setChoices(replySuggestions)
					.build();
			
			//Creating the reply intent
			Intent intent = new Intent(context, NotificationBroadcastReceiver.class);
			intent.setAction(NotificationBroadcastReceiver.intentActionReply);
			intent.putExtra(Constants.intentParamData, conversationInfo);
			//replyIntent.putExtra(Constants.intentParamTargetID, conversationInfo.getLocalID());
			//replyIntent.putExtra(Constants.intentParamGuid, conversationInfo.getGuid());
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), (int) conversationInfo.getLocalID(), intent, 0);
			
			//Creating the "reply" notification action from the remote input
			NotificationCompat.Action action =
					new NotificationCompat.Action.Builder(R.drawable.reply, context.getResources().getString(R.string.action_reply), pendingIntent)
							.setAllowGeneratedReplies(true)
							.addRemoteInput(remoteInput)
							.setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
							.build();
			
			//Adding the action
			notificationBuilder.addAction(action);
		}
		
		//Returning the notification
		return notificationBuilder;
	}
	
	private static void addMessageToNotification(Context context, ConversationManager.ConversationInfo conversationInfo, String message, String sender, long timestamp) {
		//Getting the conversation title
		conversationInfo.buildTitle(context, (conversationTitle, wasTasked) -> {
			//Checking if the sender is the user
			if(sender == null) {
				//Checking if the system version is at or below 8.1 Oreo (where MessagingStyle does not support user icons, and large icons are to be used instead) and the conversation is one-on-one
				if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1 && !conversationInfo.isGroupChat() && !conversationInfo.getConversationMembers().isEmpty()) {
					String otherUser = conversationInfo.getConversationMembers().get(0).getName();
					
					MainApplication.getInstance().getUserCacheHelper().getUserInfo(context, otherUser, new UserCacheHelper.UserFetchResult() {
						@Override
						void onUserFetched(UserCacheHelper.UserInfo userInfo, boolean wasTasked) {
							//Sending the notification without user information if the user is invalid
							if(userInfo == null) addMessageToNotificationPrepared(context, conversationInfo, conversationTitle, message, null, null, null, timestamp, null);
								//Otherwise fetching the user's icon
							else MainApplication.getInstance().getBitmapCacheHelper().getBitmapFromContact(context, otherUser, otherUser, new BitmapCacheHelper.ImageDecodeResult() {
								@Override
								void onImageMeasured(int width, int height) {}
								
								@Override
								void onImageDecoded(Bitmap result, boolean wasTasked) {
									//Sending the notification
									addMessageToNotificationPrepared(context, conversationInfo, conversationTitle, message, result == null ? null : getCroppedBitmap(result), userInfo.getContactLookupUri().toString(), null, timestamp, null);
								}
							});
						}
					});
				} else {
					//Sending the notification without an icon
					addMessageToNotificationPrepared(context, conversationInfo, conversationTitle, message, null, null, null, timestamp, null);
				}
			} else {
				Consumer<String[]> suggestionResultListener = (String[] suggestions) -> {
					//Getting the user info
					MainApplication.getInstance().getUserCacheHelper().getUserInfo(context, sender, new UserCacheHelper.UserFetchResult() {
						@Override
						void onUserFetched(UserCacheHelper.UserInfo userInfo, boolean wasTasked) {
							//Sending the notification without user information if the user is invalid
							if(userInfo == null) addMessageToNotificationPrepared(context, conversationInfo, conversationTitle, message, null, null, sender, timestamp, suggestions);
								//Otherwise fetching the user's icon
							else MainApplication.getInstance().getBitmapCacheHelper().getBitmapFromContact(context, sender, sender, new BitmapCacheHelper.ImageDecodeResult() {
								@Override
								void onImageMeasured(int width, int height) {}
								
								@Override
								void onImageDecoded(Bitmap result, boolean wasTasked) {
									//Sending the notification
									addMessageToNotificationPrepared(context, conversationInfo, conversationTitle, message, result == null ? null : getCroppedBitmap(result), userInfo.getContactLookupUri().toString(), userInfo.getContactName() == null ? sender : userInfo.getContactName(), timestamp, suggestions);
								}
							});
						}
					});
				};
				
				//Requesting smart reply
				if(Preferences.getPreferenceReplySuggestions(context)) new SuggestionAsyncTask(conversationInfo, suggestionResultListener).execute();
				else suggestionResultListener.accept(null);
			}
		});
	}
	
	private static class SuggestionAsyncTask extends AsyncTask<Void, Void, List<ConversationManager.MessageInfo>> {
		private final ConversationManager.ConversationInfo conversationInfo;
		private final Consumer<String[]> resultListener;
		
		SuggestionAsyncTask(ConversationManager.ConversationInfo conversationInfo, Consumer<String[]> resultListener) {
			this.conversationInfo = conversationInfo;
			this.resultListener = resultListener;
		}
		
		@Override
		protected List<ConversationManager.MessageInfo> doInBackground(Void... params) {
			//Fetching the items
			List<ConversationManager.MessageInfo> list = DatabaseManager.getInstance().loadConversationHistoryBit(conversationInfo);
			if(list == null) return null;
			
			//Returning a reversed list (since items should be added in chronological order, and the database loads latest items first)
			//Collections.reverse(list);
			
			//Sorting the list by date
			Collections.sort(list, (item1, item2) -> Long.compare(item1.getDate(), item2.getDate()));
			
			return list;
		}
		
		@Override
		protected void onPostExecute(List<ConversationManager.MessageInfo> list) {
			//Returning a failed result if the items couldn't be loaded or there were none of them
			if(list == null || list.isEmpty()) {
				resultListener.accept(null);
				return;
			}
			
			FirebaseNaturalLanguage.getInstance().getSmartReply().suggestReplies(Constants.messageToFirebaseMessageList(list))
					.addOnSuccessListener(result -> {
						if(result.getStatus() != SmartReplySuggestionResult.STATUS_SUCCESS) {
							//Returning a failed result
							resultListener.accept(null);
						} else {
							//Mapping the suggestions to a string array and returning the result
							String[] suggestions = new String[result.getSuggestions().size()];
							for(int i = 0; i < suggestions.length; i++) suggestions[i] = result.getSuggestions().get(i).getText();
							resultListener.accept(suggestions);
						}
					})
					.addOnFailureListener(exception -> resultListener.accept(null));
		}
	}
	
	private static void addMessageToNotificationPrepared(Context context, ConversationManager.ConversationInfo conversationInfo, String conversationTitle, String messageText, Bitmap chatUserIcon, String chatUserUri, String senderName, long timestamp, String[] replySuggestions) {
		//Creating the base notification
		NotificationCompat.Builder notification = getBaseMessageNotification(context, conversationInfo, chatUserUri, chatUserIcon, senderName == null, replySuggestions);
		
		//Creating the message
		NotificationCompat.MessagingStyle.Message message = new NotificationCompat.MessagingStyle.Message(messageText, timestamp, senderName == null ? null : new Person.Builder().setName(senderName).setIcon(senderName == null || chatUserIcon == null ? null : IconCompat.createWithBitmap(chatUserIcon)).setUri(chatUserUri).build());
		
		//Getting the notification manager
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		
		//Getting the existing notification
		Notification existingNotification = getNotification(notificationManager, (int) conversationInfo.getLocalID());
		NotificationCompat.MessagingStyle messagingStyle = null;
		
		if(existingNotification != null) {
			try {
				messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(existingNotification);
			} catch(RuntimeException exception) {
				exception.printStackTrace();
				Crashlytics.logException(exception);
			}
		}
		
		//Getting the messaging style (and checking it)
		if(existingNotification != null && messagingStyle != null) {
			//Adding the new message
			messagingStyle.addMessage(message);
			
			//Setting the messaging style to the notification
			notification.setStyle(messagingStyle);
			
			//Updating the other notification information
			//notification.setLargeIcon(Bitmap.createBitmap());
		} else {
			//Creating the messaging style
			messagingStyle = new NotificationCompat.MessagingStyle(new Person.Builder().setName(context.getResources().getString(R.string.you)).build()).addMessage(message);
			
			//Configuring the messaging style
			if(conversationInfo.isGroupChat()) {
				messagingStyle.setGroupConversation(true);
				messagingStyle.setConversationTitle(conversationTitle);
			}
			
			//Setting the messaging style to the notification
			notification.setStyle(messagingStyle);
		}
		
		//Sending the notification
		notificationManager.notify((int) conversationInfo.getLocalID(), notification.build());
	}
	
	public static class NotificationBroadcastReceiver extends BroadcastReceiver {
		//Creating the reference values
		private static final String intentActionReply = "reply";
		private static final String intentActionMarkRead = "mark_read";
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction() == null) return;
			switch(intent.getAction()) {
				case intentActionReply:
					handleReply(context, intent);
					break;
				case intentActionMarkRead:
					handleMarkRead(context, intent);
					break;
			}
		}
		
		private void handleReply(Context context, Intent intent) {
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
				void onFail(int responseCode, String details) {
					//Refreshing the notification
					Notification existingNotification = getNotification(notificationManager, (int) conversationInfo.getLocalID());
					if(existingNotification != null) notificationManager.notify((int) conversationInfo.getLocalID(), existingNotification);
				}
			});
			
			//Marking the conversation as read
			markConversationRead(context, conversationInfo.getLocalID(), false);
		}
		
		private void handleMarkRead(Context context, Intent intent) {
			//Getting the conversation ID
			long conversationID = intent.getLongExtra(Constants.intentParamTargetID, -1);
			if(conversationID == -1) return;
			
			//Marking the conversation as read
			markConversationRead(context, conversationID, true);
		}
		
		private void markConversationRead(Context context, long conversationID, boolean dismissNotification) {
			//Updating the conversation in memory
			ConversationManager.ConversationInfo conversationInfo = ConversationManager.findConversationInfo(conversationID);
			if(conversationInfo != null) {
				conversationInfo.setUnreadMessageCount(0);
				conversationInfo.updateUnreadStatus(context);
			}
			
			//Updating the conversation on disk
			new MarkReadAsyncTask().execute(conversationID);
			
			//Dismissing the notification
			if(dismissNotification) {
				NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
				notificationManager.cancel((int) conversationID);
			}
		}
		
		private CharSequence getMessage(Intent intent) {
			Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
			if(remoteInput == null) return null;
			else return remoteInput.getCharSequence(Constants.notificationReplyKey);
		}
		
		private static class MarkReadAsyncTask extends AsyncTask<Long, Void, Void> {
			@Override
			protected Void doInBackground(Long... identifiers) {
				for(long id : identifiers) DatabaseManager.getInstance().setUnreadMessageCount(id, 0);
				return null;
			}
		}
	}
	
	private static Notification getNotification(NotificationManager notificationManager, int identifier) {
		try {
			//Getting the existing notification
			for(StatusBarNotification statusBarNotification : notificationManager.getActiveNotifications()) {
				//Skipping the remainder of the iteration if the ID does not match
				if(statusBarNotification.getId() != identifier) continue;
				
				//Returning the notification
				return statusBarNotification.getNotification();
			}
		} catch(RuntimeException exception) {
			exception.printStackTrace();
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