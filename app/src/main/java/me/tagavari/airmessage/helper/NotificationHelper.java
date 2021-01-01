package me.tagavari.airmessage.helper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.app.RemoteInput;
import androidx.core.graphics.drawable.IconCompat;

import com.google.mlkit.nl.smartreply.TextMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.reactivex.rxjava3.schedulers.Schedulers;
import me.tagavari.airmessage.BuildConfig;
import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.activity.Conversations;
import me.tagavari.airmessage.activity.Messaging;
import me.tagavari.airmessage.activity.Preferences;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.data.UserCacheHelper;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.receiver.MessageNotificationActionReceiver;
import me.tagavari.airmessage.receiver.MessageNotificationDeleteReceiver;
import me.tagavari.airmessage.service.ConnectionService;
import me.tagavari.airmessage.util.NotificationSummaryMessage;

public class NotificationHelper {
	private static final String TAG = NotificationHelper.class.getSimpleName();
	
	public static final String notificationChannelMessage = "message";
	public static final String notificationChannelMessageError = "message_error";
	public static final String notificationChannelStatus = "status";
	public static final String notificationChannelStatusImportant = "status_important";
	
	public static final String notificationGroupMessage = "me.tagavari.airmessage.NOTIFICATION_GROUP_MESSAGE";
	
	public static final int notificationIDConnectionService = -1;
	public static final int notificationIDMessageImport = -2;
	public static final int notificationIDMessageSummary = -3;
	
	public static final String notificationTagMessage = "message";
	public static final String notificationTagMessageError = "message_error";
	//public static final String notificationTagStatus = "status";
	
	private static final String notificationMessageSummaryExtrasCount = "messagesummary_count";
	private static final String notificationMessageSummaryExtrasDescMapKey = "messagesummary_descmap_key";
	private static final String notificationMessageSummaryExtrasDescMapSender = "messagesummary_descmap_sender";
	private static final String notificationMessageSummaryExtrasDescMapBody = "messagesummary_descmap_text";
	
	private static final int pendingIntentOffsetOpenChat = 0;
	private static final int pendingIntentOffsetBubble = 1000000;
	private static final int pendingIntentOffsetMarkAsRead = 1000000;
	
	/**
	 * Sets up Android Oreo notification channels
	 */
	@RequiresApi(api = Build.VERSION_CODES.O)
	public static void initializeChannels(Context context) {
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		{
			NotificationChannel channel = new NotificationChannel(notificationChannelMessage, context.getResources().getString(R.string.notificationchannel_message), NotificationManager.IMPORTANCE_HIGH);
			channel.setDescription(context.getString(R.string.notificationchannel_message_desc));
			channel.enableVibration(true);
			channel.setShowBadge(true);
			channel.enableLights(true);
			channel.setSound(Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.notification_ding),
					new AudioAttributes.Builder()
							.setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
							.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
							.setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
							.build());
			//messageChannel.setGroup(notificationGroupMessage);
			notificationManager.createNotificationChannel(channel);
		}
		{
			NotificationChannel channel = new NotificationChannel(notificationChannelMessageError, context.getResources().getString(R.string.notificationchannel_messageerror), NotificationManager.IMPORTANCE_HIGH);
			channel.setDescription(context.getString(R.string.notificationchannel_messageerror_desc));
			channel.enableVibration(true);
			channel.setShowBadge(true);
			channel.enableLights(true);
			//messageChannel.setGroup(notificationGroupMessage);
			notificationManager.createNotificationChannel(channel);
		}
		{
			NotificationChannel channel = new NotificationChannel(notificationChannelStatus, context.getResources().getString(R.string.notificationchannel_status), NotificationManager.IMPORTANCE_MIN);
			channel.setDescription(context.getString(R.string.notificationchannel_status_desc));
			channel.enableVibration(false);
			channel.setShowBadge(false);
			channel.enableLights(false);
			notificationManager.createNotificationChannel(channel);
		}
		{
			NotificationChannel channel = new NotificationChannel(notificationChannelStatusImportant, context.getResources().getString(R.string.notificationchannel_statusimportant), NotificationManager.IMPORTANCE_DEFAULT);
			channel.setDescription(context.getString(R.string.notificationchannel_statusimportant_desc));
			channel.enableVibration(true);
			channel.setShowBadge(false);
			channel.enableLights(false);
			notificationManager.createNotificationChannel(channel);
		}
	}
	
	/**
	 * Sends a notification concerning a new message
	 * @param context The context to use
	 * @param conversationInfo The conversation of the message
	 * @param messageInfo The message to notify the user about
	 */
	public static void sendNotification(Context context, ConversationInfo conversationInfo, MessageInfo messageInfo) {
		//Returning if notifications are disabled or the conversation is muted
		if((Build.VERSION.SDK_INT < Build.VERSION_CODES.O && !PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preference_messagenotifications_getnotifications_key), false)) || conversationInfo.isMuted()) return;
		
		//Adding the message
		addMessageToNotification(context, conversationInfo, LanguageHelper.messageToString(context.getResources(), messageInfo), messageInfo.getSender(), messageInfo.getDate(), messageInfo.getSendStyle());
	}
	
	/**
	 * Sends a notification containing a raw string for the user
	 * @param context The context to use
	 * @param message The message text to notify the user about
	 * @param sender The user who sent the message
	 * @param timestamp The date the message was sent
	 * @param conversationInfo The conversation this message is from
	 */
	public static void sendNotification(Context context, String message, String sender, long timestamp, ConversationInfo conversationInfo) {
		//Ignoring if conversations are in the foreground, the message is outgoing, the message's conversation is loaded, or a mass retrieval is happening
		if(Conversations.isForeground() || sender == null || Messaging.getForegroundConversations().contains(conversationInfo.getLocalID()) || (ConnectionService.getConnectionManager() != null && ConnectionService.getConnectionManager().isMassRetrievalInProgress())) return;
		
		//Returning if notifications are disabled or the conversation is muted
		if((Build.VERSION.SDK_INT < Build.VERSION_CODES.O && !PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preference_messagenotifications_getnotifications_key), false)) || conversationInfo.isMuted()) return;
		
		//Adding the message
		addMessageToNotification(context, conversationInfo, message, sender, timestamp, null);
	}
	
	/**
	 * Uses the parameters to asynchronously fetch required notification information and then sends the notification
	 * @param context The context to use
	 * @param conversationInfo The conversation of the message
	 * @param message The message text to notify the user about
	 * @param sender The user who sent the message
	 * @param timestamp The date the message was sent
	 * @param sendStyle The send style associated with this message
	 */
	public static void addMessageToNotification(Context context, ConversationInfo conversationInfo, String message, @Nullable String sender, long timestamp, @Nullable String sendStyle) {
		//Changing the message based on the effect
		String displayMessage;
		if(SendStyleHelper.appleSendStyleBubbleInvisibleInk.equals(sendStyle)) displayMessage = context.getResources().getString(R.string.message_messageeffect_invisibleink);
		else displayMessage = message;
		
		//Generate the conversation title
		Single<String> singleTitle = ConversationBuildHelper.buildConversationTitle(context, conversationInfo);
		
		//Used for conversation shortcuts, only available on Android 11
		Single<Optional<IconCompat>> singleShortcutIcon;
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			singleShortcutIcon = ConversationBuildHelper.generateShortcutIcon(context, conversationInfo).map(bitmap -> Optional.of(IconCompat.createWithAdaptiveBitmap(bitmap))).onErrorReturnItem(Optional.empty());
		} else {
			singleShortcutIcon = Single.just(Optional.empty());
		}
		
		//Generate the message sender info
		Single<Optional<UserCacheHelper.UserInfo>> singleMemberInfo;
		if(conversationInfo.getMembers().isEmpty() || (conversationInfo.isGroupChat() && sender == null)) {
			singleMemberInfo = Single.just(Optional.empty());
		} else {
			/*
			 * If we're in a group chat, get the icon of the sender
			 * Otherwise, get the icon of the other member in the one-on-one chat
			 */
			String memberAddress = conversationInfo.isGroupChat() ? sender : conversationInfo.getMembers().get(0).getAddress();
			singleMemberInfo = MainApplication.getInstance().getUserCacheHelper().getUserInfo(context, memberAddress)
					.map(Optional::of).onErrorReturnItem(Optional.empty()).cache();
		}
		
		//Generate the user icon
		Single<Optional<Bitmap>> singleMemberIcon = singleMemberInfo.flatMap(user -> {
			if(!user.isPresent()) return Single.just(Optional.empty());
			
			return BitmapHelper.loadBitmapCircular(context, ContactHelper.getContactImageURI(user.get().getContactID()))
					.map(Optional::of)
					.doOnError(error -> Log.w(TAG, "Failed to load user icon", error))
					.onErrorReturnItem(Optional.empty());
		});
		
		/*
		 * Skip generating smart replies if the local user is the sender, or the user has disabled the option in preferences
		 *
		 * If we're on Android 10 or later, we'll let the system handle generating smart replies.
		 * The Android system is able to add contextual actions to notifications as well, which isn't accessible through the API.
		 */
		Single<String[]> singleSuggestions;
		if(sender == null || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || !Preferences.getPreferenceReplySuggestions(context)) {
			singleSuggestions = Single.just(new String[0]);
		} else {
			singleSuggestions = Single.create((SingleEmitter<List<TextMessage>> emitter) -> emitter.onSuccess(DatabaseManager.getInstance().loadConversationForMLKit(conversationInfo.getLocalID())))
					.subscribeOn(Schedulers.single()).observeOn(AndroidSchedulers.mainThread()).flatMap(SmartReplyHelper::generateResponsesMLKit);
		}
		
		Single.zip(Arrays.asList(singleTitle, singleShortcutIcon, singleMemberInfo, singleMemberIcon, singleSuggestions), Arrays::asList).subscribe(result -> {
			String resultTitle = (String) result.get(0);
			Optional<IconCompat> resultShortcutIcon = (Optional<IconCompat>) result.get(1);
			Optional<UserCacheHelper.UserInfo> resultMemberInfo = (Optional<UserCacheHelper.UserInfo>) result.get(2);
			Optional<Bitmap> resultMemberIcon = (Optional<Bitmap>) result.get(3);
			String[] resultSuggestions = (String[]) result.get(4);
			
			//Sending the notification
			notifyMessageNotificationWithSummary(context,
					buildMessageNotification(
							context,
							buildBaseMessageNotification(
									context,
									conversationInfo,
									sender == null,
									resultMemberIcon.orElse(null),
									resultShortcutIcon.orElse(null),
									resultMemberInfo.map(UserCacheHelper.UserInfo::getContactLookupUri).orElse(null),
									resultSuggestions.length > 0 ? resultSuggestions : null
							),
							conversationInfo,
							resultTitle,
							displayMessage,
							timestamp,
							sender,
							resultMemberInfo.orElse(null),
							resultMemberIcon.map(IconCompat::createWithBitmap).orElse(null)
					),
					conversationInfo,
					resultTitle,
					displayMessage
			);
		});
	}
	
	/**
	 * Gets a summary notification for legacy system versions that don't bundle notifications by default (below Android 7)
	 * This function returns a notification that contains the provided message notification information, as well as previous messages
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
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, notificationChannelMessage)
				.setSmallIcon(R.drawable.message_push_group)
				.setColor(context.getResources().getColor(R.color.colorPrimary, null))
				.setContentIntent(clickPendingIntent)
				//Setting the group
				.setGroup(notificationGroupMessage)
				.setGroupSummary(true)
				//Setting the sound
				.setSound(Preferences.getNotificationSound(context))
				//Setting the priority
				.setPriority(Notification.PRIORITY_HIGH);
		
		//Adding vibration if it is enabled in the preferences
		if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preference_messagenotifications_vibrate_key), false)) notificationBuilder.setVibrate(new long[]{0, 250, 250, 250});
		
		//Preparing notification data
		NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
		int messageCount;
		LinkedHashMap<Integer, NotificationSummaryMessage> descMap;
		
		//Getting the notification manager
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		
		//Attempting to find an existing summary notification
		Notification notification = getNotification(notificationManager, null, notificationIDMessageSummary);
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
					descMap.put(statusBarNotification.getId(), new NotificationSummaryMessage(title, message.getText().toString()));
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
				descMap.put(iterator.next(), new NotificationSummaryMessage(descMapSenders.get(index), descMapBodies.get(index)));
			}
		}
		
		//Adding the new message
		messageCount++;
		descMap.remove(newConversationID); //If the conversation doesn't exist, this does nothing. If it does exist, it will be removed, and the new item will be added to the bottom of the list.
		descMap.put(newConversationID, new NotificationSummaryMessage(newMessageConversation, newMessageText));
		
		//Setting the title
		if(messageCount > 0) notificationBuilder.setContentTitle(context.getResources().getQuantityString(R.plurals.message_newmessages, messageCount, messageCount));
		else notificationBuilder.setContentTitle(context.getResources().getString(R.string.message_newmessages_nocount));
		
		//Setting the description
		{
			//Getting the recipients
			String[] recipients = new String[descMap.size()];
			for(ListIterator<NotificationSummaryMessage> iterator = new ArrayList<>(descMap.values()).listIterator(descMap.size()); iterator.hasPrevious();) {
				int index = iterator.previousIndex();
				recipients[recipients.length - 1 - index] = iterator.previous().getConversation();
			}
			
			//Creating a list with the recipients
			String list = LanguageHelper.createLocalizedList(context.getResources(), recipients);
			
			//Setting the notification description
			notificationBuilder.setContentText(list);
		}
		
		//Building the items
		for(ListIterator<NotificationSummaryMessage> iterator = new ArrayList<>(descMap.values()).listIterator(descMap.size()); iterator.hasPrevious();) {
			NotificationSummaryMessage message = iterator.previous();
			
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
			for(NotificationSummaryMessage summaryMessage : descMap.values()) {
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
	
	/**
	 * Gets a summary notification for system versions that automatically help bundle similar notifications (Android 7+)
	 * @param context The context to use
	 * @return The summary notification to display
	 */
	private static Notification getSummaryNotification(Context context) {
		//Creating the click intent
		Intent clickIntent = new Intent(context, Conversations.class);
		
		//Getting the pending intent
		PendingIntent clickPendingIntent = PendingIntent.getActivity(context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		//Creating the notification builder
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, notificationChannelMessage)
				.setSmallIcon(R.drawable.message_push_group)
				.setColor(context.getResources().getColor(R.color.colorPrimary, null))
				.setContentIntent(clickPendingIntent)
				//Setting the group
				.setGroup(notificationGroupMessage)
				.setGroupSummary(true)
				//Disabling group notifications
				.setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN);
		
		//Returning the notification
		return notificationBuilder.build();
	}
	
	/**
	 * Gets a notification builder for building messaging notifications on top of
	 * @param context The context to use
	 * @param conversationInfo The conversation this message is from
	 * @param isOutgoing True if this is a notification for an outgoing message
	 * @param largeIcon The large icon to display on older versions of Android
	 * @param shortcutIcon The adaptive icon to use for conversation shortcuts
	 * @param memberURI The URI of the other user involved in this conversation (for one-on-one conversations on older versions of Android)
	 * @param replySuggestions An array of generated quick reply suggestions
	 * @return The base notification
	 */
	private static NotificationCompat.Builder buildBaseMessageNotification(@NonNull Context context, @NonNull ConversationInfo conversationInfo, boolean isOutgoing, @Nullable Bitmap largeIcon, @Nullable IconCompat shortcutIcon, @Nullable Uri memberURI, @Nullable String[] replySuggestions) {
		//Creating the click intent
		Intent clickIntent = new Intent(context, Messaging.class)
				.putExtra(Messaging.intentParamTargetID, conversationInfo.getLocalID());
		
		//Creating the task stack builder
		TaskStackBuilder clickStackBuilder = TaskStackBuilder.create(context);
		
		//Adding the back stack
		clickStackBuilder.addParentStack(Messaging.class);
		
		//Setting the result intent
		clickStackBuilder.addNextIntent(clickIntent);
		
		//Getting the pending intent
		PendingIntent clickPendingIntent = clickStackBuilder.getPendingIntent(pendingIntentOffsetOpenChat + (int) conversationInfo.getLocalID(), 0);
		
		//Creating the notification builder
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, notificationChannelMessage)
				//Setting the icon
				.setSmallIcon(R.drawable.message_push)
				//Setting the intent
				.setContentIntent(clickPendingIntent)
				//Setting the color
				.setColor(context.getResources().getColor(R.color.colorPrimary, null))
				//Setting the group
				.setGroup(notificationGroupMessage)
				//Setting the delete listener
				.setDeleteIntent(PendingIntent.getBroadcast(context, 0, new Intent(context, MessageNotificationDeleteReceiver.class), PendingIntent.FLAG_IMMUTABLE))
				//Setting the category
				.setCategory(Notification.CATEGORY_MESSAGE)
				//Adding the person
				.addPerson(memberURI == null ? null : memberURI.toString());
		
		//Adding the shortcut
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
			notificationBuilder.setShortcutId(ShortcutHelper.conversationToShortcutID(conversationInfo));
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
		
		//Setting the user icon for older versions of Android
		if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1 && !conversationInfo.isGroupChat() && largeIcon != null) {
			notificationBuilder.setLargeIcon(largeIcon);
		}
		
		{
			//Creating the "mark as read" notification action
			Intent intent = new Intent(context, MessageNotificationActionReceiver.class)
					.setAction(MessageNotificationActionReceiver.intentActionMarkRead)
					.putExtra(MessageNotificationActionReceiver.intentParamConversationID, conversationInfo.getLocalID());
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), pendingIntentOffsetMarkAsRead + (int) conversationInfo.getLocalID(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
			
			NotificationCompat.Action action =
					new NotificationCompat.Action.Builder(R.drawable.check_circle, context.getResources().getString(R.string.action_markread), pendingIntent)
							.setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
							.setShowsUserInterface(false)
							.build();
			
			//Adding the action
			notificationBuilder.addAction(action);
		}
		
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { //Remote input is not supported on Android versions below Nougat
			boolean enableReplySuggestions = Preferences.getPreferenceReplySuggestions(context);
			
			RemoteInput.Builder remoteInputBuilder = new RemoteInput.Builder(MessageNotificationActionReceiver.remoteInputID)
					.setLabel(context.getResources().getString(R.string.action_reply));
			
			//Checking if reply suggestions should be shown
			if(enableReplySuggestions) {
				//If we're on Android 10 or later, let the system handle it
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
					notificationBuilder.setAllowSystemGeneratedContextualActions(true);
				} else {
					//Use our own reply suggestions
					remoteInputBuilder.setChoices(replySuggestions);
				}
			}
			
			//Creating the remote input
			RemoteInput remoteInput = remoteInputBuilder.build();
			
			//Creating the reply intent
			Intent intent = new Intent(context, MessageNotificationActionReceiver.class)
					.setAction(MessageNotificationActionReceiver.intentActionReply)
					.putExtra(MessageNotificationActionReceiver.intentParamConversationID, conversationInfo.getLocalID());
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), (int) conversationInfo.getLocalID(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
			
			//Creating the "reply" notification action from the remote input
			NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.drawable.reply, context.getResources().getString(R.string.action_reply), pendingIntent)
							//Let the system handle generated replies on Android 10 or later
							.setAllowGeneratedReplies(enableReplySuggestions && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
							.addRemoteInput(remoteInput)
							.setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
							.setShowsUserInterface(false)
							.build();
			
			//Adding the action
			notificationBuilder.addAction(action);
		}
		
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			if(shortcutIcon != null) {
				//Creating the click intent
				Intent bubbleIntent = new Intent(context, Messaging.class)
						.putExtra(Messaging.intentParamTargetID, conversationInfo.getLocalID())
						.putExtra(Messaging.intentParamBubble, true);
				PendingIntent bubblePendingIntent = PendingIntent.getActivity(context, pendingIntentOffsetBubble + (int) conversationInfo.getLocalID(), bubbleIntent, PendingIntent.FLAG_UPDATE_CURRENT);
				
				//Adding the bubble metadata
				notificationBuilder.setBubbleMetadata(
						new NotificationCompat.BubbleMetadata.Builder()
								.setIcon(shortcutIcon)
								.setIntent(bubblePendingIntent)
								.setDesiredHeight(600)
								.build()
				);
			}
		}
		
		//Returning the notification
		return notificationBuilder;
	}
	
	/**
	 * Sends a message notification while also handling the summary notification
	 * @param context The context to use
	 * @param messageNotification The message notification to send
	 * @param conversationInfo The conversation this message is from
	 * @param conversationTitle The generated title of the conversation
	 * @param messageText The text to notify the user of
	 */
	private static void notifyMessageNotificationWithSummary(@NonNull Context context, @NonNull Notification messageNotification, @NonNull ConversationInfo conversationInfo, @NonNull String conversationTitle, @NonNull String messageText) {
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
						statusBarNotification.getId() == notificationIDMessageSummary) { //2: The summary notification already exists - it will be updated with the new message
					shouldUseSummary = true;
					break;
				}
			}
			
			if(shouldUseSummary) {
				//Updating the summary notification
				notificationManager.notify(notificationIDMessageSummary, getSummaryNotificationLegacy(context, notificationID, conversationTitle, messageText));
			} else {
				//Sending the notification
				notificationManager.notify(notificationTagMessage, (int) conversationInfo.getLocalID(), messageNotification);
			}
		} else {
			//Sending the notification
			notificationManager.notify(notificationTagMessage, (int) conversationInfo.getLocalID(), messageNotification);
			
			//The system will handle hiding the summary notification automatically
			notificationManager.notify(notificationIDMessageSummary, getSummaryNotification(context));
		}
	}
	
	/**
	 * Builds a message notification that is ready to be displayed to the user
	 * @param context The context to use
	 * @param baseNotificationBuilder A notification builder to build upon
	 * @param conversationInfo The conversation this message is from
	 * @param title The generated title of the conversation
	 * @param messageText The text to notify the user of
	 * @param messageTimestamp The time this message was received
	 * @param senderAddress The address of the sender, or NULL if this message is outgoing
	 * @param senderInfo Extra information regarding the sender
	 * @param senderIcon The icon of the sender
	 * @return A message notification to display to the user
	 */
	private static Notification buildMessageNotification(Context context, @NonNull NotificationCompat.Builder baseNotificationBuilder, @NonNull ConversationInfo conversationInfo, @Nullable String title, @NonNull String messageText, long messageTimestamp, @Nullable String senderAddress, @Nullable UserCacheHelper.UserInfo senderInfo, @Nullable IconCompat senderIcon) {
		//Creating the messaging style
		Person person;
		if(senderAddress == null) {
			//This message is outgoing
			person = null;
		} else {
			if(senderInfo == null) {
				person = new Person.Builder()
						.setName(AddressHelper.formatAddress(senderAddress))
						.setKey(AddressHelper.normalizeAddress(senderAddress))
						.build();
			} else {
				person = senderInfo == null ? null : new Person.Builder()
						.setName(senderInfo.getContactName())
						.setUri(senderInfo.getContactLookupUri().toString())
						.setIcon(senderIcon)
						.setKey(AddressHelper.normalizeAddress(senderAddress))
						.build();
			}
		}
		NotificationCompat.MessagingStyle.Message message = new NotificationCompat.MessagingStyle.Message(messageText, messageTimestamp, person);
		
		//Getting the notification manager
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		
		//Attempting to re-use an existing MessagingStyle
		Notification existingNotification = getNotification(notificationManager, notificationTagMessage, (int) conversationInfo.getLocalID());
		NotificationCompat.MessagingStyle messagingStyle;
		if(existingNotification != null && (messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(existingNotification)) != null) {
			//Adding the new message
			messagingStyle.addMessage(message);
		} else {
			//Creating a new messaging style
			messagingStyle = new NotificationCompat.MessagingStyle(new Person.Builder().setName(context.getResources().getString(R.string.part_you)).build()).addMessage(message);
			
			//Configuring the messaging style
			if(conversationInfo.isGroupChat()) {
				messagingStyle.setGroupConversation(true);
				messagingStyle.setConversationTitle(title);
			}
		}
		
		//Setting the messaging style to the notification
		baseNotificationBuilder.setStyle(messagingStyle);
		
		//Returning the notification
		return baseNotificationBuilder.build();
	}
	
	/**
	 * Sends a notification to alert the user of a failed message in a conversation
	 * @param context The context to use
	 * @param conversationInfo The conversation with the failed message
	 */
	public static void sendErrorNotification(Context context, ConversationInfo conversationInfo) {
		//Returning if the message's conversation is loaded
		if(Messaging.getForegroundConversations().contains(conversationInfo.getLocalID())) return;
		
		//Building the conversation title
		ConversationBuildHelper.buildConversationTitle(context, conversationInfo).subscribe(title -> {
			//Getting the notification manager
			NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
			
			//Creating the click intent
			Intent clickIntent = new Intent(context, Messaging.class)
					.putExtra(Messaging.intentParamTargetID, conversationInfo.getLocalID());
			
			//Creating the task stack builder
			TaskStackBuilder clickStackBuilder = TaskStackBuilder.create(context);
			
			//Adding the back stack
			clickStackBuilder.addParentStack(Messaging.class);
			
			//Setting the result intent
			clickStackBuilder.addNextIntent(clickIntent);
			
			//Getting the pending intent
			PendingIntent clickPendingIntent = clickStackBuilder.getPendingIntent(pendingIntentOffsetOpenChat + (int) conversationInfo.getLocalID(), 0);
			
			//Creating the notification
			Notification notification = new NotificationCompat.Builder(context, notificationChannelMessageError)
					.setSmallIcon(R.drawable.message_alert)
					.setContentTitle(context.getResources().getString(R.string.message_senderrornotify))
					.setContentText(context.getResources().getString(R.string.message_senderrornotify_desc, title))
					.setContentIntent(clickPendingIntent)
					.setColor(context.getResources().getColor(R.color.colorError, null))
					.setCategory(Notification.CATEGORY_ERROR)
					.setDefaults(Notification.DEFAULT_ALL) //API 23-25
					.setPriority(Notification.PRIORITY_HIGH).build(); //API 23-25
			
			//Sending the notification
			notificationManager.notify(notificationTagMessageError, (int) conversationInfo.getLocalID(), notification);
		});
	}
	
	/**
	 * Dismisses a message notification, automatically handling the dismissal of the group notification
	 * @param notificationManager The notification manager to use
	 * @param notificationID The ID of the message notification to dismiss
	 */
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
		notificationManager.cancel(notificationIDMessageSummary);
	}
	
	/**
	 * Dismisses the summary notification if there are no more notifications left
	 */
	public static void tryDismissMessageSummaryNotification(NotificationManager notificationManager) {
		//Returning if there are any more message notifications left
		for(StatusBarNotification statusBarNotification : notificationManager.getActiveNotifications()) {
			if(notificationTagMessage.equals(statusBarNotification.getTag())) {
				return;
			}
		}
		
		//Dismissing the group notification
		notificationManager.cancel(notificationIDMessageSummary);
	}
	
	/**
	 * Finds an active notification matching the provided tag and ID
	 * @param notificationManager The notification manager to use
	 * @param tag The notification tag to match
	 * @param id The notification ID to match
	 * @return The notification if it was found, or NULL otherwise
	 */
	public static Notification getNotification(NotificationManager notificationManager, String tag, int id) {
		return Arrays.stream(notificationManager.getActiveNotifications())
				.filter(notification -> Objects.equals(notification.getTag(), tag) && notification.getId() == id)
				.findAny()
				.map(StatusBarNotification::getNotification)
				.orElse(null);
	}
	
	private static NotificationCompat.Action getConnectionQuitAction(Context context) {
		return new NotificationCompat.Action.Builder(
				R.drawable.close_circle,
				context.getResources().getString(R.string.action_quit),
				PendingIntent.getService(
						context,
						0,
						new Intent(context, ConnectionService.class).setAction(ConnectionService.selfIntentActionStop),
						PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_CANCEL_CURRENT
				)
		).build();
	}
	
	public static Notification getConnectionConfigurationNotification(Context context) {
		return new NotificationCompat.Builder(context, notificationChannelStatus)
				.setSmallIcon(R.drawable.push)
				.setContentTitle(context.getResources().getString(R.string.message_manualconfigurationstatus))
				.addAction(getConnectionQuitAction(context))
				.setShowWhen(false)
				.setPriority(Notification.PRIORITY_MIN)
				.setOngoing(true)
				.build();
	}
	
	public static Notification getTemporaryModeNotification(Context context) {
		return new NotificationCompat.Builder(context, notificationChannelStatus)
				.setSmallIcon(R.drawable.push)
				.setContentTitle(context.getResources().getString(R.string.message_temporarymodestatus))
				.addAction(getConnectionQuitAction(context))
				.setShowWhen(false)
				.setPriority(Notification.PRIORITY_MIN)
				.setOngoing(true)
				.build();
	}
	
	public static Notification getConnectionBackgroundNotification(Context context, boolean isConnected, boolean isFallback) {
		//Building the notification
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context, notificationChannelStatus)
				.setSmallIcon(R.drawable.push)
				.setContentTitle(context.getResources().getString(isConnected ? (isFallback ? R.string.message_connection_connectedfallback : R.string.message_connection_connected) : R.string.progress_connectingtoserver))
				.setContentText(context.getResources().getString(R.string.imperative_tapopenapp))
				.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, Conversations.class), PendingIntent.FLAG_UPDATE_CURRENT));
		
		//Disconnect (only available in debug)
		if(BuildConfig.DEBUG) builder.addAction(R.drawable.wifi_off, context.getResources().getString(R.string.action_disconnect), PendingIntent.getService(context, 0, new Intent(context, ConnectionService.class).setAction(ConnectionService.selfIntentActionDisconnect), PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_CANCEL_CURRENT));
		
		return builder
				.addAction(getConnectionQuitAction(context))
				.setShowWhen(false)
				.setPriority(Notification.PRIORITY_MIN)
				.setOngoing(true)
				.setOnlyAlertOnce(true)
				.build();
	}
	
	public static Notification getConnectionOfflineNotification(Context context, boolean silent) {
		//Building the notification
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context, notificationChannelStatusImportant)
				.setSmallIcon(R.drawable.warning)
				.setContentTitle(context.getResources().getString(R.string.message_connection_disconnected))
				.setContentText(context.getResources().getString(R.string.imperative_tapopenapp))
				.setColor(context.getResources().getColor(R.color.colorServerDisconnected, null))
				.setContentText(context.getResources().getString(R.string.imperative_tapopenapp))
				.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, Conversations.class), PendingIntent.FLAG_UPDATE_CURRENT));
		
		//Adding the reconnect action
		builder.addAction(R.drawable.wifi, context.getResources().getString(R.string.action_reconnect), PendingIntent.getService(context, 0, new Intent(context, ConnectionService.class).setAction(ConnectionService.selfIntentActionConnect), PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_CANCEL_CURRENT));
		
		//Completing and returning the notification
		return builder
				.addAction(getConnectionQuitAction(context))
				.setShowWhen(false)
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.setOnlyAlertOnce(silent)
				.build();
	}
	
	/**
	 * Rounds the corners of a bitmap
	 */
	private static Bitmap getRoundBitmap(Bitmap bitmap) {
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(output);
		
		final int color = 0xFF424242;
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
}