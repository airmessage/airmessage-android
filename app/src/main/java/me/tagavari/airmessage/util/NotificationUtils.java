package me.tagavari.airmessage.util;

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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;

import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;
import androidx.core.app.RemoteInput;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.util.Consumer;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.smartreply.FirebaseTextMessage;
import com.google.firebase.ml.naturallanguage.smartreply.SmartReplySuggestionResult;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.activity.Conversations;
import me.tagavari.airmessage.activity.Messaging;
import me.tagavari.airmessage.activity.Preferences;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.connection.request.MessageResponseManager;
import me.tagavari.airmessage.data.BitmapCacheHelper;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.data.UserCacheHelper;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.receiver.TextSMSSentReceiver;
import me.tagavari.airmessage.service.ConnectionService;

public class NotificationUtils {
	public static final String notificationTagMessage = "message";
	public static final String notificationTagMessageError = "message_error";
	//public static final String notificationTagStatus = "status";
	
	private static final String notificationMessageSummaryExtrasCount = "messagesummary_count";
	private static final String notificationMessageSummaryExtrasDescMapKey = "messagesummary_descmap_key";
	private static final String notificationMessageSummaryExtrasDescMapSender = "messagesummary_descmap_sender";
	private static final String notificationMessageSummaryExtrasDescMapBody = "messagesummary_descmap_text";
	
	private static final int pendingIntentOffsetThread = 0;
	private static final int pendingIntentOffsetBubble = 1000000;
	private static final int pendingIntentOffsetMarkAsRead = 2000000;
	
	/**
	 * Sends a notification concerning a new message
	 * @param context The context to use
	 * @param messageInfo The message to notify the user about
	 */
	public static void sendNotification(Context context, MessageInfo messageInfo) {
		//Returning if the message is outgoing or the message's conversation is loaded
		if(messageInfo.isOutgoing() || Conversations.isForeground() || Messaging.getForegroundConversations().contains(messageInfo.getConversationInfo().getLocalID()) || (ConnectionService.getConnectionManager() != null && ConnectionService.getConnectionManager().isMassRetrievalInProgress())) return;
		
		//Returning if notifications are disabled or the conversation is muted
		if((Build.VERSION.SDK_INT < Build.VERSION_CODES.O && !PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preference_messagenotifications_getnotifications_key), false)) || messageInfo.getConversationInfo().isMuted()) return;
		
		//Adding the message
		addMessageToNotification(context, messageInfo.getConversationInfo(), messageInfo.getSummary(context), messageInfo.getSender(), messageInfo.getDate(), messageInfo.getSendStyle());
	}
	
	/**
	 * Sends a notification containing a raw string for the user
	 * @param context The context to use
	 * @param message The text message to notify the user about
	 * @param sender The user who sent the message
	 * @param timestamp the date the message was sent
	 * @param conversationInfo The conversation this message is from
	 */
	public static void sendNotification(Context context, String message, String sender, long timestamp, ConversationInfo conversationInfo) {
		//Returning message is outgoing or the message's conversation is loaded
		if(sender == null || Messaging.getForegroundConversations().contains(conversationInfo.getLocalID()) || (ConnectionService.getConnectionManager() != null && ConnectionService.getConnectionManager().isMassRetrievalInProgress())) return;
		
		//Returning if notifications are disabled or the conversation is muted
		if((Build.VERSION.SDK_INT < Build.VERSION_CODES.O && !PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preference_messagenotifications_getnotifications_key), false)) || conversationInfo.isMuted()) return;
		
		//Adding the message
		addMessageToNotification(context, conversationInfo, message, sender, timestamp, null);
	}
	
	public static void sendErrorNotification(Context context, ConversationInfo conversationInfo) {
		//Returning if the message's conversation is loaded
		if(Messaging.getForegroundConversations().contains(conversationInfo.getLocalID())) return;
		
		//Building the conversation title
		conversationInfo.buildTitle(context, (title, wasTasked) -> {
			//Getting the notification manager
			NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			
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
			PendingIntent clickPendingIntent = clickStackBuilder.getPendingIntent(pendingIntentOffsetThread + (int) conversationInfo.getLocalID(), 0);
			
			//Creating the notification
			NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MainApplication.notificationChannelMessageError)
					.setSmallIcon(R.drawable.message_alert)
					.setContentTitle(context.getResources().getString(R.string.message_senderrornotify))
					.setContentText(context.getResources().getString(R.string.message_senderrornotify_desc, title))
					.setContentIntent(clickPendingIntent)
					.setColor(context.getResources().getColor(R.color.colorError, null))
					.setCategory(Notification.CATEGORY_ERROR);
			
			//Checking if the Android version is below Oreo (on API 26 and above, notification alert details are handled by the system's notification channels)
			if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
				builder.setDefaults(Notification.DEFAULT_SOUND);
				builder.setPriority(Notification.PRIORITY_HIGH);
			}
			
			//Sending the notification
			notificationManager.notify(notificationTagMessageError, (int) conversationInfo.getLocalID(), builder.build());
		});
	}
	
	/**
	 * Get a summary notification for legacy system versions that don't bundle notifications by default (below Android 7)
	 * @param context The context to use
	 * @param newConversationID The conversation ID of this latest message
	 * @param newMessageConversation The conversation title of this latest message
	 * @param newMessageText The body of this latest message
	 * @return The notification to display
	 */
	private static Notification getSummaryNotificationLegacy(Context context, int newConversationID, String newMessageConversation, String newMessageText) {
		//Creating the click intent
		Intent clickIntent = new Intent(context, Conversations.class);
		
		//Getting the pending intent
		PendingIntent clickPendingIntent = PendingIntent.getActivity(context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		//Creating the notification builder
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, MainApplication.notificationChannelMessage)
				.setSmallIcon(R.drawable.message_push_group)
				.setColor(context.getResources().getColor(R.color.colorPrimary, null))
				.setContentIntent(clickPendingIntent)
				//Setting the group
				.setGroup(MainApplication.notificationGroupMessage)
				.setGroupSummary(true)
				//Setting the sound
				.setSound(Preferences.getNotificationSound(context))
				//Setting the priority
				.setPriority(Notification.PRIORITY_HIGH);
		
		//Adding vibration if it is enabled in the preferences
		if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preference_messagenotifications_vibrate_key), false)) notificationBuilder.setVibrate(new long[]{0, 250, 250, 250});
		
		//Getting the notification manager
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		
		//Preparing notification data
		NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
		int messageCount;
		LinkedHashMap<Integer, SummaryMessage> descMap;
		
		//Attempting to find an existing summary notification
		Notification notification = getNotification(notificationManager, null, MainApplication.notificationIDMessageSummary);
		if(notification == null) {
			//Setting the values to empty
			messageCount = 0;
			descMap = new LinkedHashMap<>();
			
			//Counting existing messages
			for(StatusBarNotification statusBarNotification : notificationManager.getActiveNotifications()) {
				//Skipping the remainder of the iteration if the notification is not a message notification
				if(!Objects.equals(statusBarNotification.getTag(), notificationTagMessage)) continue;
				
				NotificationCompat.MessagingStyle messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(statusBarNotification.getNotification());
				if(messagingStyle != null && !messagingStyle.getMessages().isEmpty()) {
					//Adding the message count
					messageCount += messagingStyle.getMessages().size();
					
					//Adding the last message
					NotificationCompat.MessagingStyle.Message message = messagingStyle.getMessages().get(messagingStyle.getMessages().size() - 1);
					String title = (String) messagingStyle.getConversationTitle();
					if(title == null) title = (String) message.getPerson().getName();
					descMap.put(statusBarNotification.getId(), new SummaryMessage(title, message.getText().toString()));
				}
			}
		} else {
			//Reading in existing notification metadata
			messageCount = notification.extras.getInt(notificationMessageSummaryExtrasCount);
			List<Integer> descMapKeys = (List<Integer>) notification.extras.getSerializable(notificationMessageSummaryExtrasDescMapKey);
			List<String> descMapSenders = (List<String>) notification.extras.getSerializable(notificationMessageSummaryExtrasDescMapSender);
			List<String> descMapBodies = (List<String>) notification.extras.getSerializable(notificationMessageSummaryExtrasDescMapBody);
			descMap = new LinkedHashMap<>(descMapKeys.size());
			for(ListIterator<Integer> iterator = descMapKeys.listIterator(); iterator.hasNext();) {
				int index = iterator.nextIndex();
				descMap.put(iterator.next(), new SummaryMessage(descMapSenders.get(index), descMapBodies.get(index)));
			}
		}
		
		//Adding the new message
		messageCount++;
		descMap.remove(newConversationID); //If the conversation doesn't exist, this does nothing. If it does exist, it will be removed, and the new item will be added to the bottom of the list.
		descMap.put(newConversationID, new SummaryMessage(newMessageConversation, newMessageText));
		
		//Setting the title
		if(messageCount > 0) notificationBuilder.setContentTitle(context.getResources().getQuantityString(R.plurals.message_newmessages, messageCount, messageCount));
		else notificationBuilder.setContentTitle(context.getResources().getString(R.string.message_newmessages_nocount));
		
		//Setting the description
		{
			//Getting the recipients
			String[] recipients = new String[descMap.size()];
			for(ListIterator<SummaryMessage> iterator = new ArrayList<>(descMap.values()).listIterator(descMap.size()); iterator.hasPrevious();) {
				int index = iterator.previousIndex();
				recipients[recipients.length - 1 - index] = iterator.previous().getConversation();
			}
			
			//Creating a list with the recipients
			String list = Constants.createLocalizedList(recipients, context.getResources());
			
			//Setting the notification description
			notificationBuilder.setContentText(list);
		}
		
		//Building the items
		for(ListIterator<SummaryMessage> iterator = new ArrayList<>(descMap.values()).listIterator(descMap.size()); iterator.hasPrevious();) {
			SummaryMessage message = iterator.previous();
			
			SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
			{
				SpannableString title = new SpannableString(message.getConversation());
				title.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, title.length(),0);
				spannableStringBuilder.append(title);
			}
			spannableStringBuilder.append(' ');
			spannableStringBuilder.append(message.getBody());
			inboxStyle.addLine(spannableStringBuilder);
		}
		
		//Setting the notification style
		notificationBuilder.setStyle(inboxStyle);
		
		//Writing notification metadata
		{
			Bundle bundle = new Bundle();
			bundle.putInt(notificationMessageSummaryExtrasCount, messageCount);
			bundle.putSerializable(notificationMessageSummaryExtrasDescMapKey, new ArrayList<>(descMap.keySet()));
			ArrayList<String> senderList = new ArrayList<>(descMap.size());
			ArrayList<String> bodyList = new ArrayList<>(descMap.size());
			for(SummaryMessage summaryMessage : descMap.values()) {
				senderList.add(summaryMessage.getConversation());
				bodyList.add(summaryMessage.getBody());
			}
			bundle.putSerializable(notificationMessageSummaryExtrasDescMapSender, senderList);
			bundle.putSerializable(notificationMessageSummaryExtrasDescMapBody, bodyList);
			
			notificationBuilder.addExtras(bundle);
		}
		
		//Returning the notification
		return notificationBuilder.build();
	}
	
	private static Notification getSummaryNotification(Context context) {
		//Creating the click intent
		Intent clickIntent = new Intent(context, Conversations.class);
		
		//Getting the pending intent
		PendingIntent clickPendingIntent = PendingIntent.getActivity(context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		//Creating the notification builder
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, MainApplication.notificationChannelMessage)
				.setSmallIcon(R.drawable.message_push_group)
				.setColor(context.getResources().getColor(R.color.colorPrimary, null))
				.setContentIntent(clickPendingIntent)
				//Setting the group
				.setGroup(MainApplication.notificationGroupMessage)
				.setGroupSummary(true)
				//Disabling group notifications
				.setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN);
		
		//Returning the notification
		return notificationBuilder.build();
	}
	
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
	
	private static NotificationCompat.Builder getBaseMessageNotification(Context context, ConversationInfo conversationInfo, IconCompat conversationIcon, String userUri, Bitmap userIcon, boolean isOutgoing, String[] replySuggestions) {
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
		PendingIntent clickPendingIntent = clickStackBuilder.getPendingIntent(pendingIntentOffsetThread + (int) conversationInfo.getLocalID(), 0);
		//PendingIntent clickPendingIntent = PendingIntent.getActivity(context.getApplicationContext(), (int) conversationInfo.getLocalID(), new Intent(context, Messaging.class).putExtra(Constants.intentParamTargetID, conversationInfo.getLocalID()), 0);
		
		//Creating the notification builder
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, MainApplication.notificationChannelMessage)
				//Setting the icon
				.setSmallIcon(R.drawable.message_push)
				//Setting the intent
				.setContentIntent(clickPendingIntent)
				//Setting the color
				.setColor(context.getResources().getColor(R.color.colorPrimary, null))
				//Setting the group
				.setGroup(MainApplication.notificationGroupMessage)
				//Setting the delete listener
				.setDeleteIntent(PendingIntent.getBroadcast(context, 0, new Intent(context, MessageNotificationDeleteReceiver.class), PendingIntent.FLAG_IMMUTABLE))
				//Setting the category
				.setCategory(Notification.CATEGORY_MESSAGE)
				//Adding the person
				.addPerson(userUri);
		
		//Adding the shortcut
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
			notificationBuilder.setShortcutId(ShortcutUtils.conversationToShortcutID(conversationInfo));
		}
		
		//Checking if the Android version is below Oreo (on API 26 and above, notification alert details are handled by the system's notification channels)
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			//Setting the sound
			notificationBuilder.setSound(Preferences.getNotificationSound(context));
			
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
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), pendingIntentOffsetMarkAsRead + (int) conversationInfo.getLocalID(), intent, 0);
			
			NotificationCompat.Action action =
					new NotificationCompat.Action.Builder(R.drawable.check_circle, context.getResources().getString(R.string.action_markread), pendingIntent)
							.setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
							.setShowsUserInterface(false)
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
							.setShowsUserInterface(false)
							.build();
			
			//Adding the action
			notificationBuilder.addAction(action);
		}
		
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			if(conversationIcon != null) {
				//Creating the click intent
				Intent bubbleIntent = new Intent(context, Messaging.class);
				bubbleIntent.putExtra(Constants.intentParamTargetID, conversationInfo.getLocalID());
				bubbleIntent.putExtra(Constants.intentParamBubble, true);
				PendingIntent bubblePendingIntent = PendingIntent.getActivity(context, pendingIntentOffsetBubble + (int) conversationInfo.getLocalID(), bubbleIntent, PendingIntent.FLAG_UPDATE_CURRENT);
				
				//Adding the bubble metadata
				notificationBuilder.setBubbleMetadata(
						new NotificationCompat.BubbleMetadata.Builder()
								.setIcon(conversationIcon)
								.setIntent(bubblePendingIntent)
								.setDesiredHeight(600)
								.build()
				);
			}
		}
		
		//Returning the notification
		return notificationBuilder;
	}
	
	private static void addMessageToNotification(Context context, ConversationInfo conversationInfo, String message, String sender, long timestamp, String sendStyle) {
		//Updating the message based on the effect
		String displayMessage;
		if(Constants.appleSendStyleBubbleInvisibleInk.equals(sendStyle)) displayMessage = context.getResources().getString(R.string.message_messageeffect_invisibleink);
		else displayMessage = message;
		
		//Getting the conversation title
		conversationInfo.buildTitle(context, (conversationTitle, wasConversationTitleTasked) -> {
			//Building the conversation icon
			conversationInfo.generateShortcutIcon(context, (conversationBitmap, wasConversationIconTasked) -> {
				//Creating the conversation icon
				IconCompat conversationIcon = conversationBitmap == null ? null : IconCompat.createWithAdaptiveBitmap(conversationBitmap);
				
				//Checking if the sender is the user
				if(sender == null) {
					//Checking if the system version is at or below 8.1 Oreo (where MessagingStyle does not support user icons, and large icons are to be used instead) and the conversation is one-on-one
					if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1 && !conversationInfo.isGroupChat() && !conversationInfo.getConversationMembers().isEmpty()) {
						String otherUser = conversationInfo.getConversationMembers().get(0).getName();
						
						MainApplication.getInstance().getUserCacheHelper().getUserInfo(context, otherUser, new UserCacheHelper.UserFetchResult() {
							@Override
							public void onUserFetched(UserCacheHelper.UserInfo userInfo, boolean wasTasked) {
								//Sending the notification without user information if the user is invalid
								if(userInfo == null) addMessageToNotificationPrepared(context, conversationInfo, conversationTitle, conversationIcon, displayMessage, null, null, null, timestamp, null);
									//Otherwise fetching the user's icon
								else MainApplication.getInstance().getBitmapCacheHelper().getBitmapFromContact(context, otherUser, otherUser, new BitmapCacheHelper.ImageDecodeResult() {
									@Override
									public void onImageMeasured(int width, int height) {}
									
									@Override
									public void onImageDecoded(Bitmap result, boolean wasTasked) {
										//Sending the notification
										addMessageToNotificationPrepared(context, conversationInfo, conversationTitle, conversationIcon, displayMessage, result == null ? null : getCroppedBitmap(result), userInfo.getContactLookupUri().toString(), null, timestamp, null);
									}
								});
							}
						});
					} else {
						//Sending the notification without an icon
						addMessageToNotificationPrepared(context, conversationInfo, conversationTitle, conversationIcon, displayMessage, null, null, null, timestamp, null);
					}
				} else {
					Consumer<String[]> suggestionResultListener = (String[] suggestions) -> {
						//Getting the user info
						MainApplication.getInstance().getUserCacheHelper().getUserInfo(context, sender, new UserCacheHelper.UserFetchResult() {
							@Override
							public void onUserFetched(UserCacheHelper.UserInfo userInfo, boolean wasTasked) {
								//Sending the notification without user information if the user is invalid
								if(userInfo == null) addMessageToNotificationPrepared(context, conversationInfo, conversationTitle, conversationIcon, displayMessage, null, null, Constants.formatAddress(sender), timestamp, suggestions);
									//Otherwise fetching the user's icon
								else MainApplication.getInstance().getBitmapCacheHelper().getBitmapFromContact(context, sender, sender, new BitmapCacheHelper.ImageDecodeResult() {
									@Override
									public void onImageMeasured(int width, int height) {}
									
									@Override
									public void onImageDecoded(Bitmap result, boolean wasTasked) {
										//Sending the notification
										addMessageToNotificationPrepared(context, conversationInfo, conversationTitle, conversationIcon, displayMessage, result == null ? null : getCroppedBitmap(result), userInfo.getContactLookupUri().toString(), userInfo.getContactName() == null ? Constants.formatAddress(sender) : userInfo.getContactName(), timestamp, suggestions);
									}
								});
							}
						});
					};
					
					//Requesting smart reply
					if(Preferences.getPreferenceReplySuggestions(context) && message != null && !Constants.appleSendStyleBubbleInvisibleInk.equals(sendStyle)) new SuggestionAsyncTask(conversationInfo.getLocalID(), suggestionResultListener).execute();
					else suggestionResultListener.accept(null);
				}
			});
		});
	}
	
	private static class SuggestionAsyncTask extends AsyncTask<Void, Void, List<FirebaseTextMessage>> {
		private final long conversationID;
		private final Consumer<String[]> resultListener;
		
		SuggestionAsyncTask(long conversationID, Consumer<String[]> resultListener) {
			this.conversationID = conversationID;
			this.resultListener = resultListener;
		}
		
		@Override
		protected List<FirebaseTextMessage> doInBackground(Void... params) {
			//Fetching the items
			return DatabaseManager.getInstance().loadConversationForFirebase(conversationID);
		}
		
		@Override
		protected void onPostExecute(List<FirebaseTextMessage> list) {
			//Returning a failed result if the items couldn't be loaded or there were none of them
			if(list == null || list.isEmpty()) {
				resultListener.accept(null);
				return;
			}
			
			FirebaseNaturalLanguage.getInstance().getSmartReply().suggestReplies(list)
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
	
	private static void addMessageToNotificationPrepared(Context context, ConversationInfo conversationInfo, String conversationTitle, IconCompat conversationIcon, String messageText, Bitmap chatUserIcon, String chatUserUri, String senderName, long timestamp, String[] replySuggestions) {
		//Getting the notification manager
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		
		//Getting the notification ID
		int notificationID = (int) conversationInfo.getLocalID();
		
		//Checking if the summary notification requires app control
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
			boolean shouldUseSummary = false;
			for(StatusBarNotification statusBarNotification : notificationManager.getActiveNotifications()) {
				//There are 2 cases for when a summary notification should be used
				if((Objects.equals(statusBarNotification.getTag(), notificationTagMessage) && statusBarNotification.getId() != notificationID) || //1: A notification from a different conversation already exists - both will be bundled into the summary
						statusBarNotification.getId() == MainApplication.notificationIDMessageSummary) { //2: The summary notification already exists - it will be updated with the new message
					shouldUseSummary = true;
					break;
				}
			}
			
			if(shouldUseSummary) {
				//Updating the summary notification
				notificationManager.notify(MainApplication.notificationIDMessageSummary, getSummaryNotificationLegacy(context, notificationID, conversationTitle != null ? conversationTitle : senderName, messageText));
			} else {
				//Sending the notification
				sendUpdateMessageNotification(context, conversationInfo, conversationTitle, conversationIcon, messageText, chatUserIcon, chatUserUri, senderName, timestamp, replySuggestions);
			}
		} else {
			//Sending the notification
			sendUpdateMessageNotification(context, conversationInfo, conversationTitle, conversationIcon, messageText, chatUserIcon, chatUserUri, senderName, timestamp, replySuggestions);
			
			//The system will handle hiding the summary notification automatically
			notificationManager.notify(MainApplication.notificationIDMessageSummary, getSummaryNotification(context));
		}
	}
	
	private static void sendUpdateMessageNotification(Context context, ConversationInfo conversationInfo, String conversationTitle, IconCompat conversationIcon, String messageText, Bitmap chatUserIcon, String chatUserUri, String senderName, long timestamp, String[] replySuggestions) {
		//Creating the base notification
		NotificationCompat.Builder notification = getBaseMessageNotification(context, conversationInfo, conversationIcon, chatUserUri, chatUserIcon, senderName == null, replySuggestions);
		
		//Creating the message
		NotificationCompat.MessagingStyle.Message message = new NotificationCompat.MessagingStyle.Message(messageText, timestamp, senderName == null ? null : new Person.Builder().setName(senderName).setIcon(senderName == null || chatUserIcon == null ? null : IconCompat.createWithBitmap(chatUserIcon)).setUri(chatUserUri).build());
		
		//Getting the notification manager
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		
		//Getting the existing notification
		Notification existingNotification = getNotification(notificationManager, notificationTagMessage, (int) conversationInfo.getLocalID());
		NotificationCompat.MessagingStyle messagingStyle = null;
		
		if(existingNotification != null) {
			try {
				messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(existingNotification);
			} catch(RuntimeException exception) {
				exception.printStackTrace();
				FirebaseCrashlytics.getInstance().recordException(exception);
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
			messagingStyle = new NotificationCompat.MessagingStyle(new Person.Builder().setName(context.getResources().getString(R.string.part_you)).build()).addMessage(message);
			
			//Configuring the messaging style
			if(conversationInfo.isGroupChat()) {
				messagingStyle.setGroupConversation(true);
				messagingStyle.setConversationTitle(conversationTitle);
			}
			
			//Setting the messaging style to the notification
			notification.setStyle(messagingStyle);
		}
		
		//Sending the notification
		notificationManager.notify(notificationTagMessage, (int) conversationInfo.getLocalID(), notification.build());
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
			final ConversationInfo conversationInfo = (ConversationInfo) intent.getSerializableExtra(Constants.intentParamData);
			
			//Getting the response
			final CharSequence responseMessage = getMessage(intent);
			
			//Checking if the response is invalid
			if(responseMessage == null) {
				//Refreshing the notification
				NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
				Notification existingNotification = getNotification(notificationManager, notificationTagMessage, (int) conversationInfo.getLocalID());
				if(existingNotification != null) notificationManager.notify(notificationTagMessage, (int) conversationInfo.getLocalID(), existingNotification);
				
				//Returning
				return;
			}
			
			//Checking if the service handler is AirMessage Bridge
			if(conversationInfo.getServiceHandler() == ConversationInfo.serviceHandlerAMBridge) {
				sendMessageAMBridge(context, conversationInfo, responseMessage.toString());
			}
			//Otherwise checking if the service handler is system messaging
			else if(conversationInfo.getServiceHandler() == ConversationInfo.serviceHandlerSystemMessaging) {
				if(ConversationInfo.serviceTypeSystemMMSSMS.equals(conversationInfo.getService())) sendMessageSMS(context, conversationInfo, responseMessage.toString());
			}
			
			//Marking the conversation as read
			markConversationRead(context, conversationInfo.getLocalID(), false);
		}
		
		private void sendMessageAMBridge(Context context, ConversationInfo conversationInfo, String responseMessage) {
			//Getting the connection manager
			ConnectionManager connectionManager = ConnectionService.getConnectionManager();
			
			//Checking if the service isn't running
			if(connectionManager == null) {
				//Starting the service
				context.startService(new Intent(context, ConnectionService.class));
				
				//Refreshing the notification
				NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
				Notification existingNotification = getNotification(notificationManager, notificationTagMessage, (int) conversationInfo.getLocalID());
				if(existingNotification != null) notificationManager.notify(notificationTagMessage, (int) conversationInfo.getLocalID(), existingNotification);
				
				//Returning
				return;
			}
			
			//Getting the notification manager
			final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			
			//Sending the message
			connectionManager.sendMessage(conversationInfo.getGuid(), responseMessage, new MessageResponseManager(new ConnectionManager.MessageResponseManagerDeregistrationListener(connectionManager)) {
				@Override
				public void onSuccess() {
					//Adding the message
					addMessageToNotification(context, conversationInfo, responseMessage, null, System.currentTimeMillis() / 1000L, null);
				}
				
				@Override
				public void onFail(int responseCode, String details) {
					//Refreshing the notification
					Notification existingNotification = getNotification(notificationManager, notificationTagMessage, (int) conversationInfo.getLocalID());
					if(existingNotification != null) notificationManager.notify(notificationTagMessage, (int) conversationInfo.getLocalID(), existingNotification);
				}
			});
		}
		
		private void sendMessageSMS(Context context, ConversationInfo conversationInfo, String responseMessage) {
			//Creating the message
			MessageInfo messageInfo = new MessageInfo(-1, -1, null, conversationInfo, null, responseMessage, null, null, false, System.currentTimeMillis(), Constants.messageStateCodeGhost, Constants.messageErrorCodeOK, false, -1);
			
			//Saving the message to disk and sending it
			new SaveSendMessage(context, messageInfo, result -> {
				if(result) {
					//Adding the message
					addMessageToNotification(context, conversationInfo, responseMessage, null, System.currentTimeMillis() / 1000L, null);
				} else {
					//Refreshing the notification
					NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
					Notification existingNotification = getNotification(notificationManager, notificationTagMessage, (int) conversationInfo.getLocalID());
					if(existingNotification != null) notificationManager.notify(notificationTagMessage, (int) conversationInfo.getLocalID(), existingNotification);
				}
			}).execute();
		}
		
		private static class SaveSendMessage extends AsyncTask<Void, Void, Void> {
			private final WeakReference<Context> contextReference;
			private final MessageInfo messageInfo;
			private final Consumer<Boolean> resultListener;
			
			private SaveSendMessage(Context context, MessageInfo messageInfo, Consumer<Boolean> resultListener) {
				contextReference = new WeakReference<>(context);
				this.messageInfo = messageInfo;
				this.resultListener = resultListener;
			}
			
			@Override
			protected Void doInBackground(Void... args) {
				DatabaseManager.getInstance().addConversationItem(messageInfo, messageInfo.getConversationInfo().getServiceHandler() == ConversationInfo.serviceHandlerAMBridge);
				return null;
			}
			
			@Override
			protected void onPostExecute(Void result) {
				//Getting the context
				Context context = contextReference.get();
				if(context == null) return;
				
				//Adding the message in memory
				ConversationInfo conversationInfo = ConversationUtils.findConversationInfo(messageInfo.getConversationInfo().getLocalID());
				if(conversationInfo != null) conversationInfo.addConversationItems(context, Collections.singletonList(messageInfo));
				
				//Setting the listener
				TextSMSSentReceiver.addListener(new SMSResultListener(messageInfo.getLocalID(), resultListener));
				
				//Sending the message
				messageInfo.sendMessage(context);
			}
			
			private static class SMSResultListener extends TextSMSSentReceiver.SMSSentListener {
				private final Consumer<Boolean> resultListener;
				private SMSResultListener(long messageID, Consumer<Boolean> resultListener) {
					super(messageID);
					this.resultListener = resultListener;
				}
				
				@Override
				public void onResult(boolean result) {
					resultListener.accept(result);
				}
			}
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
			ConversationInfo conversationInfo = ConversationUtils.findConversationInfo(conversationID);
			if(conversationInfo != null) {
				conversationInfo.setUnreadMessageCount(0);
				conversationInfo.updateUnreadStatus(context);
			}
			
			//Updating the conversation on disk
			new MarkReadAsyncTask().execute(conversationID);
			
			//Dismissing the notification
			if(dismissNotification) {
				cancelMessageNotification((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE), (int) conversationID);
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
	
	public static class MessageNotificationDeleteReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			tryDismissMessageSummaryNotification((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE));
		}
	}
	
	private static Notification getNotification(NotificationManager notificationManager, String tag, int identifier) {
		try {
			//Getting the existing notification
			for(StatusBarNotification statusBarNotification : notificationManager.getActiveNotifications()) {
				//Skipping the remainder of the iteration if the tag or ID does not match
				if(!Objects.equals(statusBarNotification.getTag(), tag) || statusBarNotification.getId() != identifier) continue;
				
				//Returning the notification
				return statusBarNotification.getNotification();
			}
		} catch(RuntimeException exception) {
			exception.printStackTrace();
		}
		
		//Returning null
		return null;
	}
	
	public static void cancelMessageNotification(NotificationManager notificationManager, int notificationID) {
		//Cancelling the notification
		notificationManager.cancel(notificationTagMessage, notificationID);
		
		//Returning if there are any more message notifications left
		for(StatusBarNotification statusBarNotification : notificationManager.getActiveNotifications()) {
			if(notificationTagMessage.equals(statusBarNotification.getTag()) && statusBarNotification.getId() != notificationID) {
				return;
			}
		}
		
		//Dismissing the group notification
		notificationManager.cancel(MainApplication.notificationIDMessageSummary);
	}
	
	public static void tryDismissMessageSummaryNotification(NotificationManager notificationManager) {
		//Returning if there are any more message notifications left
		for(StatusBarNotification statusBarNotification : notificationManager.getActiveNotifications()) {
			if(notificationTagMessage.equals(statusBarNotification.getTag())) {
				return;
			}
		}
		
		//Dismissing the group notification
		notificationManager.cancel(MainApplication.notificationIDMessageSummary);
	}
	
	public static class SummaryMessage {
		private final String conversation;
		private final String body;
		
		SummaryMessage(String conversation, String body) {
			this.conversation = conversation;
			this.body = body;
		}
		
		String getConversation() {
			return conversation;
		}
		
		String getBody() {
			return body;
		}
	}
}