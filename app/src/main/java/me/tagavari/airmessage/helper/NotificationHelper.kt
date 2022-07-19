package me.tagavari.airmessage.helper

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import androidx.preference.PreferenceManager
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import me.tagavari.airmessage.BuildConfig
import me.tagavari.airmessage.MainApplication
import me.tagavari.airmessage.R
import me.tagavari.airmessage.activity.Conversations
import me.tagavari.airmessage.activity.FaceTimeCall
import me.tagavari.airmessage.activity.Messaging
import me.tagavari.airmessage.activity.Preferences
import me.tagavari.airmessage.data.DatabaseManager
import me.tagavari.airmessage.data.UserCacheHelper
import me.tagavari.airmessage.helper.AddressHelper.formatAddress
import me.tagavari.airmessage.helper.AddressHelper.normalizeAddress
import me.tagavari.airmessage.helper.BitmapHelper.loadBitmapCircular
import me.tagavari.airmessage.helper.ContactHelper.getContactImageURI
import me.tagavari.airmessage.helper.ConversationBuildHelper.buildConversationTitle
import me.tagavari.airmessage.helper.ConversationBuildHelper.generateShortcutIcon
import me.tagavari.airmessage.helper.LanguageHelper.createLocalizedList
import me.tagavari.airmessage.helper.LanguageHelper.messageToString
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.messaging.MessageInfo
import me.tagavari.airmessage.receiver.MessageNotificationActionReceiver
import me.tagavari.airmessage.receiver.MessageNotificationDeleteReceiver
import me.tagavari.airmessage.service.ConnectionService
import me.tagavari.airmessage.util.NotificationSummaryMessage
import java.util.*

object NotificationHelper {
	private val TAG = NotificationHelper::class.simpleName!!
	const val notificationChannelMessage = "message"
	const val notificationChannelMessageError = "message_error"
	const val notificationChannelMessageReceiveError = "message_receive_error"
	const val notificationChannelStatus = "status"
	const val notificationChannelStatusImportant = "status_important"
	const val notificationChannelIncomingCall = "incoming_call"
	
	const val notificationGroupMessage = "me.tagavari.airmessage.NOTIFICATION_GROUP_MESSAGE"
	
	const val notificationIDConnectionService = -1
	const val notificationIDMessageImport = -2
	const val notificationIDMessageSummary = -3
	const val notificationIDWarningDecrypt = -4
	const val notificationIDIncomingCall = -5
	
	const val notificationTagMessage = "message"
	const val notificationTagMessageError = "message_error"
	
	private const val notificationExtrasMessageSummaryCount = "message_summary_count"
	private const val notificationExtrasMessageSummaryConversationID = "message_summary_conversation_id"
	private const val notificationExtrasMessageSummaryTitle = "message_summary_title"
	private const val notificationExtrasMessageSummaryDescription = "message_summary_description"
	private const val pendingIntentOffsetOpenChat = 0
	private const val pendingIntentOffsetBubble = 1000000
	private const val pendingIntentOffsetMarkAsRead = 1000000
	
	/**
	 * Sets up Android Oreo notification channels
	 */
	@JvmStatic
	@RequiresApi(api = Build.VERSION_CODES.O)
	fun initializeChannels(context: Context) {
		val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		
		notificationManager.createNotificationChannel(
			NotificationChannel(notificationChannelMessage, context.resources.getString(R.string.notificationchannel_message), NotificationManager.IMPORTANCE_HIGH).apply {
				description = context.getString(R.string.notificationchannel_message_desc)
				enableVibration(true)
				setShowBadge(true)
				enableLights(true)
				setSound(Uri.parse("android.resource://" + context.packageName + "/" + R.raw.notification_ding),
					AudioAttributes.Builder()
						.setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
						.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
						.setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
						.build())
			}
		)
		
		notificationManager.createNotificationChannel(
			NotificationChannel(notificationChannelMessageError, context.resources.getString(R.string.notificationchannel_messageerror), NotificationManager.IMPORTANCE_HIGH).apply {
				description = context.getString(R.string.notificationchannel_messageerror_desc)
				enableVibration(true)
				setShowBadge(true)
				enableLights(true)
			}
		)
		
		notificationManager.createNotificationChannel(
			NotificationChannel(notificationChannelMessageReceiveError, context.resources.getString(R.string.notificationchannel_messageerrorreception), NotificationManager.IMPORTANCE_HIGH).apply {
				description = context.getString(R.string.notificationchannel_messageerrorreception_desc)
				enableVibration(true)
				setShowBadge(true)
				enableLights(true)
			}
		)
		
		notificationManager.createNotificationChannel(
			NotificationChannel(notificationChannelStatus, context.resources.getString(R.string.notificationchannel_status), NotificationManager.IMPORTANCE_MIN).apply {
				description = context.getString(R.string.notificationchannel_status_desc)
				enableVibration(false)
				setShowBadge(false)
				enableLights(false)
			}
		)
		
		notificationManager.createNotificationChannel(
			NotificationChannel(notificationChannelStatusImportant, context.resources.getString(R.string.notificationchannel_statusimportant), NotificationManager.IMPORTANCE_DEFAULT).apply {
				description = context.getString(R.string.notificationchannel_statusimportant_desc)
				enableVibration(true)
				setShowBadge(false)
				enableLights(false)
			}
		)
		
		notificationManager.createNotificationChannel(
			NotificationChannel(notificationChannelIncomingCall, context.resources.getString(R.string.notificationchannel_incomingcall), NotificationManager.IMPORTANCE_HIGH).apply {
				description = context.getString(R.string.notificationchannel_incomingcall_desc)
				enableVibration(true)
				setShowBadge(false)
				enableLights(false)
				setSound(RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE),
					AudioAttributes.Builder()
						.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
						.setLegacyStreamType(AudioManager.STREAM_RING)
						.setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
						.build())
			}
		)
	}
	
	/**
	 * Sends a notification concerning a new message
	 * @param context The context to use
	 * @param conversationInfo The conversation of the message
	 * @param messageInfo The message to notify the user about
	 */
	@JvmStatic
	fun sendNotification(context: Context, conversationInfo: ConversationInfo, messageInfo: MessageInfo) {
		//Returning if notifications are disabled or the conversation is muted
		if((Build.VERSION.SDK_INT < Build.VERSION_CODES.O && !PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.resources.getString(R.string.preference_messagenotifications_getnotifications_key), true)) || conversationInfo.isMuted) return
		
		//Adding the message
		addMessageToNotification(context, conversationInfo, messageToString(context.resources, messageInfo), messageInfo.sender, messageInfo.date, messageInfo.sendStyle)
	}
	
	/**
	 * Sends a notification containing a raw string for the user
	 * @param context The context to use
	 * @param message The message text to notify the user about
	 * @param sender The user who sent the message
	 * @param timestamp The date the message was sent
	 * @param conversationInfo The conversation this message is from
	 */
	@JvmStatic
	fun sendNotification(context: Context, message: String, sender: String?, timestamp: Long, conversationInfo: ConversationInfo) {
		//Ignoring if conversations are in the foreground, the message is outgoing, the message's conversation is loaded, or a mass retrieval is happening
		if(Conversations.isForeground() || sender == null || Messaging.getForegroundConversations().contains(conversationInfo.localID) || ConnectionService.getConnectionManager().let {it != null && it.isMassRetrievalInProgress}) return
		
		//Returning if notifications are disabled or the conversation is muted
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O && !PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.resources.getString(R.string.preference_messagenotifications_getnotifications_key), false) || conversationInfo.isMuted) return
		
		//Adding the message
		addMessageToNotification(context, conversationInfo, message, sender, timestamp, null)
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
	@JvmStatic
	fun addMessageToNotification(context: Context, conversationInfo: ConversationInfo, message: String, sender: String?, timestamp: Long, sendStyle: String?) {
		//Changing the message based on the effect
		val displayMessage = if(SendStyleHelper.appleSendStyleBubbleInvisibleInk == sendStyle) context.resources.getString(R.string.message_messageeffect_invisibleink) else message
		
		//Generate the conversation title
		val singleTitle = buildConversationTitle(context, conversationInfo)
		
		//Used for conversation shortcuts, only available on Android 11
		val singleShortcutIcon: Single<Optional<IconCompat>> = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			generateShortcutIcon(context, conversationInfo).map { bitmap: Bitmap? -> Optional.of(IconCompat.createWithAdaptiveBitmap(bitmap)) }.onErrorReturnItem(Optional.empty())
		} else {
			Single.just(Optional.empty())
		}
		
		//Generate the message sender info
		val singleMemberInfo: Single<Optional<UserCacheHelper.UserInfo>> = if(conversationInfo.members.isEmpty() || (conversationInfo.isGroupChat && sender == null)) {
			Single.just(Optional.empty())
		} else {
			/*
			 * If we're in a group chat, get the icon of the sender
			 * Otherwise, get the icon of the other member in the one-on-one chat
			 */
			val memberAddress: String = if(conversationInfo.isGroupChat) sender!! else conversationInfo.members[0].address
			MainApplication.getInstance().userCacheHelper.getUserInfo(context, memberAddress)
					.map { Optional.of(it) }.onErrorReturnItem(Optional.empty()).cache()
		}
		
		//Generate the user icon
		val singleMemberIcon: Single<Optional<Bitmap>> = singleMemberInfo.flatMap { user: Optional<UserCacheHelper.UserInfo> ->
			return@flatMap if(!user.isPresent) {
				Single.just(Optional.empty<Bitmap>())
			} else {
				loadBitmapCircular(context, getContactImageURI(user.get().contactID))
						.map { Optional.of(it) }
						.doOnError { error -> Log.w(TAG, "Failed to load user icon", error) }
						.onErrorReturnItem(Optional.empty())
			}
		}
		
		/*
		 * Skip generating smart replies if the local user is the sender, or the user has disabled the option in preferences
		 *
		 * If we're on Android 10 or later, we'll let the system handle generating smart replies.
		 * The Android system is able to add contextual actions to notifications as well, which isn't accessible through the API.
		 */
		val singleSuggestions: Single<List<String>> = if(sender == null || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || !Preferences.getPreferenceReplySuggestions(context)) {
			Single.just(emptyList())
		} else {
			Single.fromCallable {
				DatabaseManager.getInstance().loadConversationForMLKit(conversationInfo.localID)
			}.subscribeOn(Schedulers.single()).observeOn(AndroidSchedulers.mainThread())
					.flatMap { messages -> SmartReplyHelper.generateResponsesMLKit(messages) }
		}
		
		Single.zip(singleTitle, singleShortcutIcon, singleMemberInfo, singleMemberIcon, singleSuggestions, ::NotificationFutureData)
				.subscribe { (resultTitle, resultShortcutIcon, resultMemberInfo, resultMemberIcon, resultSuggestions) ->
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
											resultMemberInfo.orElse(null),
											resultSuggestions.ifEmpty { null }
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
					)
				}
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
	private fun getSummaryNotificationLegacy(context: Context, newConversationID: Int, newMessageConversation: String, newMessageText: String): Notification {
		//Creating the click intent
		val clickIntent = Intent(context, Conversations::class.java)
		
		//Getting the pending intent
		val clickPendingIntent = PendingIntent.getActivity(context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
		
		//Creating the notification builder
		val notificationBuilder = NotificationCompat.Builder(context, notificationChannelMessage)
				.setSmallIcon(R.drawable.message_push_group)
				.setColor(context.resources.getColor(R.color.colorPrimary, null))
				.setContentIntent(clickPendingIntent)
				//Setting the group
				.setGroup(notificationGroupMessage)
				.setGroupSummary(true)
				//Setting the sound
				.setSound(Preferences.getNotificationSound(context))
				//Setting the priority
				.setPriority(NotificationCompat.PRIORITY_HIGH)
		
		//Adding vibration if it is enabled in the preferences
		if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.resources.getString(R.string.preference_messagenotifications_vibrate_key), false)) {
			notificationBuilder.setVibrate(longArrayOf(0, 250, 250, 250))
		}
		
		//Preparing notification data
		val summaryMessages: MutableList<NotificationSummaryMessage> //A map of notification IDs to their title and message
		
		//Getting the notification manager
		val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		
		//Attempting to find an existing summary notification
		val notification = getNotification(notificationManager, null, notificationIDMessageSummary)
		if(notification == null) {
			//Counting existing messages
			summaryMessages = notificationManager.activeNotifications.mapNotNull { statusBarNotification ->
				//Ignoring if the notification is not a message notification
				if(statusBarNotification.tag != notificationTagMessage) return@mapNotNull null
				
				val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(statusBarNotification.notification)
				if(messagingStyle == null || messagingStyle.messages.isEmpty()) return@mapNotNull null
				
				//Adding the last message
				val message = messagingStyle.messages.last()
				val title = messagingStyle.conversationTitle as String? ?: message.person!!.name as String
				return@mapNotNull NotificationSummaryMessage(statusBarNotification.id, title, message.text.toString())
			}.toMutableList()
		} else {
			//Reading in existing notification metadata
			summaryMessages = notification.extras.run {
				val size = getInt(notificationExtrasMessageSummaryCount)
				
				val arrIDs = getIntArray(notificationExtrasMessageSummaryConversationID)!!
				val arrTitles = getStringArray(notificationExtrasMessageSummaryTitle)!!
				val arrDescriptions = getStringArray(notificationExtrasMessageSummaryDescription)!!
				(0 until size).map { i -> NotificationSummaryMessage(arrIDs[i], arrTitles[i], arrDescriptions[i]) }
			}.toMutableList()
		}
		
		//Adding the new message
		summaryMessages.removeIf { it.conversationID == newConversationID } //If the conversation doesn't exist, this does nothing. If it does exist, it will be removed, and the new item will be added to the bottom of the list.
		summaryMessages.add(NotificationSummaryMessage(newConversationID, newMessageConversation, newMessageText))
		
		//Setting the title
		val messageCount = summaryMessages.size
		notificationBuilder.setContentTitle(context.resources.getQuantityString(R.plurals.message_newmessages, messageCount, messageCount))
		
		//Setting the description
		notificationBuilder.setContentText(createLocalizedList(context.resources, summaryMessages.asReversed().map(NotificationSummaryMessage::title)))
		
		//Compiling the summaries into an InboxStyle and applying it to the notification
		notificationBuilder.setStyle(NotificationCompat.InboxStyle().also { inboxStyle ->
			summaryMessages.reversed().forEach { message ->
				inboxStyle.addLine(SpannableStringBuilder().apply {
					append(SpannableString(message.title).apply { setSpan(StyleSpan(Typeface.BOLD), 0, message.title.length, 0) })
					append(' ')
					append(message.description)
				})
			}
		})
		
		//Writing notification metadata
		notificationBuilder.setExtras(Bundle().apply {
			putInt(notificationExtrasMessageSummaryCount, summaryMessages.size)
			putIntArray(notificationExtrasMessageSummaryConversationID, summaryMessages.map { it.conversationID }.toIntArray())
			putStringArray(notificationExtrasMessageSummaryTitle, summaryMessages.map { it.title }.toTypedArray())
			putStringArray(notificationExtrasMessageSummaryDescription, summaryMessages.map { it.description }.toTypedArray())
		})
		
		//Returning the notification
		return notificationBuilder.build()
	}
	
	/**
	 * Gets a summary notification for system versions that automatically help bundle similar notifications (Android 7+)
	 * @param context The context to use
	 * @return The summary notification to display
	 */
	private fun getSummaryNotification(context: Context): Notification {
		return NotificationCompat.Builder(context, notificationChannelMessage).apply {
			setSmallIcon(R.drawable.message_push_group)
			setColor(context.resources.getColor(R.color.colorPrimary, null))
			setContentIntent(
					Intent(context, Conversations::class.java)
							.let { intent -> PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) }
			)
			
			//Setting the group
			setGroup(notificationGroupMessage)
			setGroupSummary(true)
			
			//Disabling group notifications
			setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
		}.build()
	}
	
	/**
	 * Gets a notification builder for building messaging notifications on top of
	 * @param context The context to use
	 * @param conversationInfo The conversation this message is from
	 * @param isOutgoing True if this is a notification for an outgoing message
	 * @param largeIcon The large icon to display on older versions of Android
	 * @param shortcutIcon The adaptive icon to use for conversation shortcuts
	 * @param memberInfo The member info of the other user involved in this conversation (for one-on-one conversations on older versions of Android)
	 * @param replySuggestions An array of generated quick reply suggestions
	 * @return The base notification
	 */
	private fun buildBaseMessageNotification(context: Context, conversationInfo: ConversationInfo, isOutgoing: Boolean, largeIcon: Bitmap?, shortcutIcon: IconCompat?, memberInfo: UserCacheHelper.UserInfo?, replySuggestions: List<String>?): NotificationCompat.Builder {
		//Creating the click intent
		val clickIntent = Intent(context, Messaging::class.java).apply {
			putExtra(Messaging.intentParamTargetID, conversationInfo.localID)
		}
		
		//Creating the task stack builder
		val clickStackBuilder = TaskStackBuilder.create(context)
		
		//Adding the back stack
		clickStackBuilder.addParentStack(Messaging::class.java)
		
		//Setting the result intent
		clickStackBuilder.addNextIntent(clickIntent)
		
		//Getting the pending intent
		val clickPendingIntent = clickStackBuilder.getPendingIntent(pendingIntentOffsetOpenChat + conversationInfo.localID.toInt(), PendingIntent.FLAG_IMMUTABLE)
		
		//Creating the notification builder
		val notificationBuilder = NotificationCompat.Builder(context, notificationChannelMessage)
			//Setting the icon
			.setSmallIcon(R.drawable.message_push)
			//Setting the intent
			.setContentIntent(clickPendingIntent)
			//Setting the color
			.setColor(context.resources.getColor(R.color.colorPrimary, null))
			//Setting the group
			.setGroup(notificationGroupMessage)
			//Setting the delete listener
			.setDeleteIntent(PendingIntent.getBroadcast(context, 0, Intent(context, MessageNotificationDeleteReceiver::class.java), PendingIntent.FLAG_IMMUTABLE))
			//Setting the category
			.setCategory(NotificationCompat.CATEGORY_MESSAGE)
			//Adding the person
			.addPerson(memberInfo?.let { member -> Person.Builder().apply {
				setName(member.contactName)
				setUri(member.contactLookupUri.toString())
				setKey(member.lookupKey)
			}.build()})
		
		//Adding the shortcut
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
			notificationBuilder.setShortcutId(ShortcutHelper.conversationToShortcutID(conversationInfo))
		}
		
		//Checking if the Android version is below Oreo (on API 26 and above, notification alert details are handled by the system's notification channels)
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			//Setting the sound
			notificationBuilder.setSound(Preferences.getNotificationSound(context))
			
			//Adding vibration if it is enabled in the preferences
			if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.resources.getString(R.string.preference_messagenotifications_vibrate_key), false)) notificationBuilder.setVibrate(longArrayOf(0, 250, 250, 250))
			
			//Setting the priority
			notificationBuilder.priority = NotificationCompat.PRIORITY_HIGH
		}
		
		//Disabling alerts if a sound shouldn't be played
		notificationBuilder.setOnlyAlertOnce(isOutgoing)
		
		//Setting the user icon for older versions of Android
		if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1 && !conversationInfo.isGroupChat && largeIcon != null) {
			notificationBuilder.setLargeIcon(largeIcon)
		}
		
		//Creating the "mark as read" notification action
		notificationBuilder.addAction(run {
			val pendingIntent = Intent(context, MessageNotificationActionReceiver::class.java).apply {
				action = MessageNotificationActionReceiver.intentActionMarkRead
				putExtra(MessageNotificationActionReceiver.intentParamConversationID, conversationInfo.localID)
			}.let { intent -> PendingIntent.getBroadcast(context.applicationContext, pendingIntentOffsetMarkAsRead + conversationInfo.localID.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) }
			
			return@run NotificationCompat.Action.Builder(R.drawable.check_circle, context.resources.getString(R.string.action_markread), pendingIntent).apply {
				setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
				setShowsUserInterface(false)
			}.build()
		})
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { //Remote input is not supported on Android versions below Nougat
			val enableReplySuggestions = Preferences.getPreferenceReplySuggestions(context)
			val remoteInputBuilder = RemoteInput.Builder(MessageNotificationActionReceiver.remoteInputID).apply {
				setLabel(context.resources.getString(R.string.action_reply))
			}
			
			//Checking if reply suggestions should be shown
			if(enableReplySuggestions) {
				//If we're on Android 10 or later, let the system handle it
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
					notificationBuilder.setAllowSystemGeneratedContextualActions(true)
				} else {
					//Use our own reply suggestions
					remoteInputBuilder.setChoices(replySuggestions?.toTypedArray())
				}
			}
			
			//Creating the remote input
			val remoteInput = remoteInputBuilder.build()
			
			//Creating the reply intent
			val pendingIntent = Intent(context, MessageNotificationActionReceiver::class.java).apply {
				action = MessageNotificationActionReceiver.intentActionReply
				putExtra(MessageNotificationActionReceiver.intentParamConversationID, conversationInfo.localID)
			}.let { intent -> PendingIntent.getBroadcast(
				context.applicationContext,
				conversationInfo.localID.toInt(),
				intent,
				PendingIntent.FLAG_UPDATE_CURRENT or (if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
			) }
			
			//Creating the "reply" notification action from the remote input
			val action = NotificationCompat.Action.Builder(R.drawable.reply, context.resources.getString(R.string.action_reply), pendingIntent).apply {
				//Let the system handle generated replies on Android 10 or later
				setAllowGeneratedReplies(enableReplySuggestions && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
				
				addRemoteInput(remoteInput)
				
				setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
				setShowsUserInterface(false)
			}.build()
			
			//Adding the action
			notificationBuilder.addAction(action)
		}
		
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			if(shortcutIcon != null) {
				//Adding the bubble metadata
				notificationBuilder.bubbleMetadata =
					NotificationCompat.BubbleMetadata.Builder(
						Intent(context, Messaging::class.java).apply {
							putExtra(Messaging.intentParamTargetID, conversationInfo.localID)
							putExtra(Messaging.intentParamBubble, true)
						}.let { intent -> PendingIntent.getActivity(
							context,
							pendingIntentOffsetBubble + conversationInfo.localID.toInt(),
							intent,
							PendingIntent.FLAG_UPDATE_CURRENT or (if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
						) },
						shortcutIcon
					).apply {
						setDesiredHeight(600)
					}.build()
			}
		}
		
		//Returning the notification
		return notificationBuilder
	}
	
	/**
	 * Sends a message notification while also handling the summary notification
	 * @param context The context to use
	 * @param messageNotification The message notification to send
	 * @param conversationInfo The conversation this message is from
	 * @param conversationTitle The generated title of the conversation
	 * @param messageText The text to notify the user of
	 */
	private fun notifyMessageNotificationWithSummary(context: Context, messageNotification: Notification, conversationInfo: ConversationInfo, conversationTitle: String, messageText: String) {
		//Getting the notification manager
		val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		
		//Getting the notification ID
		val notificationID = conversationInfo.localID.toInt()
		
		//Checking if the summary notification requires app control
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
			var shouldUseSummary = false
			for(statusBarNotification in notificationManager.activeNotifications) {
				//There are 2 cases for when a summary notification should be used
				if(statusBarNotification.tag == notificationTagMessage && statusBarNotification.id != notificationID ||  //1: A notification from a different conversation already exists - both will be bundled into the summary
						statusBarNotification.id == notificationIDMessageSummary) { //2: The summary notification already exists - it will be updated with the new message
					shouldUseSummary = true
					break
				}
			}
			if(shouldUseSummary) {
				//Updating the summary notification
				notificationManager.notify(notificationIDMessageSummary, getSummaryNotificationLegacy(context, notificationID, conversationTitle, messageText))
			} else {
				//Sending the notification
				notificationManager.notify(notificationTagMessage, conversationInfo.localID.toInt(), messageNotification)
			}
		} else {
			//Sending the notification
			notificationManager.notify(notificationTagMessage, conversationInfo.localID.toInt(), messageNotification)
			
			//The system will handle hiding the summary notification automatically
			notificationManager.notify(notificationIDMessageSummary, getSummaryNotification(context))
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
	private fun buildMessageNotification(context: Context, baseNotificationBuilder: NotificationCompat.Builder, conversationInfo: ConversationInfo, title: String, messageText: String, messageTimestamp: Long, senderAddress: String?, senderInfo: UserCacheHelper.UserInfo?, senderIcon: IconCompat?): Notification {
		//Creating the messaging style
		val person: Person? = if(senderAddress == null) {
			//This message is outgoing
			null
		} else {
			if(senderInfo == null) {
				Person.Builder().apply {
					setName(formatAddress(senderAddress))
					setKey(normalizeAddress(senderAddress))
				}.build()
			} else {
				Person.Builder().apply {
					setName(senderInfo.contactName)
					setUri(senderInfo.contactLookupUri.toString())
					setIcon(senderIcon)
					setKey(normalizeAddress(senderAddress))
				}.build()
			}
		}
		val message = NotificationCompat.MessagingStyle.Message(messageText, messageTimestamp, person)
		
		//Getting the notification manager
		val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		
		//Attempting to re-use an existing MessagingStyle
		val existingNotification = getNotification(notificationManager, notificationTagMessage, conversationInfo.localID.toInt())
		var messagingStyle = existingNotification?.let(NotificationCompat.MessagingStyle::extractMessagingStyleFromNotification)
		
		if(messagingStyle != null) {
			//Adding the new message
			messagingStyle.addMessage(message)
		} else {
			//Creating a new messaging style
			messagingStyle = NotificationCompat.MessagingStyle(Person.Builder().setName(context.resources.getString(R.string.part_you)).build()).addMessage(message)
			
			//Configuring the messaging style
			if(conversationInfo.isGroupChat) {
				messagingStyle.isGroupConversation = true
				messagingStyle.conversationTitle = title
			}
		}
		
		//Setting the messaging style to the notification
		baseNotificationBuilder.setStyle(messagingStyle)
		
		//Returning the notification
		return baseNotificationBuilder.build()
	}
	
	/**
	 * Sends a notification to alert the user of a failed message in a conversation
	 * @param context The context to use
	 * @param conversationInfo The conversation with the failed message
	 */
	@JvmStatic
	fun sendErrorNotification(context: Context, conversationInfo: ConversationInfo) {
		//Returning if the message's conversation is loaded
		if(Messaging.getForegroundConversations().contains(conversationInfo.localID)) return
		
		//Building the conversation title
		buildConversationTitle(context, conversationInfo).subscribe { title ->
			//Getting the notification manager
			val notificationManager = NotificationManagerCompat.from(context)
			
			//Creating the task stack builder
			val clickStackBuilder = TaskStackBuilder.create(context).apply {
				addParentStack(Messaging::class.java)
				addNextIntent(Intent(context, Messaging::class.java).apply {
					putExtra(Messaging.intentParamTargetID, conversationInfo.localID)
				})
			}
			
			//Getting the pending intent
			val clickPendingIntent = clickStackBuilder.getPendingIntent(pendingIntentOffsetOpenChat + conversationInfo.localID.toInt(), PendingIntent.FLAG_IMMUTABLE)
			
			//Creating the notification
			val notification = NotificationCompat.Builder(context, notificationChannelMessageError)
				.setSmallIcon(R.drawable.message_alert)
				.setContentTitle(context.resources.getString(R.string.message_senderrornotify))
				.setContentText(context.resources.getString(R.string.message_senderrornotify_desc, title))
				.setContentIntent(clickPendingIntent)
				.setColor(context.resources.getColor(R.color.colorError, null))
				.setCategory(NotificationCompat.CATEGORY_ERROR)
				.setDefaults(NotificationCompat.DEFAULT_ALL) //API 23-25
				.setPriority(NotificationCompat.PRIORITY_HIGH).build() //API 23-25
			
			//Sending the notification
			notificationManager.notify(notificationTagMessageError, conversationInfo.localID.toInt(), notification)
		}
	}
	
	/**
	 * Sends a notification to alert the user that an incoming message could not be decrypted
	 * @param context The context to use
	 */
	@JvmStatic
	fun sendDecryptErrorNotification(context: Context) {
		//Getting the notification manager
		val notificationManager = NotificationManagerCompat.from(context)

		//Getting the pending intent
		val clickPendingIntent: PendingIntent = TaskStackBuilder.create(context).run {
			addNextIntentWithParentStack(Intent(context, Conversations::class.java))
			getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
		}
		
		//Creating the notification
		val notification = NotificationCompat.Builder(context, notificationChannelMessageReceiveError)
			.setSmallIcon(R.drawable.message_alert)
			.setContentTitle(context.resources.getString(R.string.message_decrypterrornotify))
			.setContentText(context.resources.getString(R.string.message_decrypterrornotify_desc))
			.setContentIntent(clickPendingIntent)
			.setColor(context.resources.getColor(R.color.colorError, null))
			.setCategory(NotificationCompat.CATEGORY_ERROR)
			.setDefaults(NotificationCompat.DEFAULT_ALL) //API 23-25
			.setPriority(NotificationCompat.PRIORITY_HIGH).build() //API 23-25
		
		//Sending the notification
		notificationManager.notify(notificationTagMessageError, notificationIDWarningDecrypt, notification)
	}
	
	/**
	 * Dismisses a message notification, automatically handling the dismissal of the group notification
	 * @param notificationManager The notification manager to use
	 * @param notificationID The ID of the message notification to dismiss
	 */
	@JvmStatic
	fun cancelMessageNotification(notificationManager: NotificationManager, notificationID: Int) {
		//Cancelling the notification
		notificationManager.cancel(notificationTagMessage, notificationID)
		
		//Returning if there are any more message notifications left
		for(statusBarNotification in notificationManager.activeNotifications) {
			if(notificationTagMessage == statusBarNotification.tag && statusBarNotification.id != notificationID) {
				return
			}
		}
		
		//Dismissing the group notification
		notificationManager.cancel(notificationIDMessageSummary)
	}
	
	/**
	 * Dismisses the summary notification if there are no more notifications left
	 */
	@JvmStatic
	fun tryDismissMessageSummaryNotification(notificationManager: NotificationManager) {
		//Returning if there are any more message notifications left
		for(statusBarNotification in notificationManager.activeNotifications) {
			if(notificationTagMessage == statusBarNotification.tag) {
				return
			}
		}
		
		//Dismissing the group notification
		notificationManager.cancel(notificationIDMessageSummary)
	}
	
	/**
	 * Finds an active notification matching the provided tag and ID
	 * @param notificationManager The notification manager to use
	 * @param tag The notification tag to match
	 * @param id The notification ID to match
	 * @return The notification if it was found, or NULL otherwise
	 */
	@JvmStatic
	fun getNotification(notificationManager: NotificationManager, tag: String?, id: Int): Notification? {
		return notificationManager.activeNotifications
				?.firstOrNull { notification: StatusBarNotification -> notification.tag == tag && notification.id == id }
				?.notification
	}
	
	private fun getConnectionQuitAction(context: Context): NotificationCompat.Action {
		return NotificationCompat.Action.Builder(
				R.drawable.close_circle,
				context.resources.getString(R.string.action_quit),
				PendingIntent.getService(
						context,
						0,
						Intent(context, ConnectionService::class.java).setAction(ConnectionService.selfIntentActionStop),
						PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
				)
		).build()
	}
	
	@JvmStatic
	fun getConnectionConfigurationNotification(context: Context): Notification {
		return NotificationCompat.Builder(context, notificationChannelStatus).apply {
			setSmallIcon(R.drawable.push)
			setContentTitle(context.resources.getString(R.string.message_manualconfigurationstatus))
			addAction(getConnectionQuitAction(context))
			setShowWhen(false)
			setPriority(NotificationCompat.PRIORITY_MIN)
			setOngoing(true)
		}.build()
	}
	
	@JvmStatic
	fun getTemporaryModeNotification(context: Context): Notification {
		return NotificationCompat.Builder(context, notificationChannelStatus).apply {
			setSmallIcon(R.drawable.push)
			setContentTitle(context.resources.getString(R.string.message_temporarymodestatus))
			addAction(getConnectionQuitAction(context))
			setShowWhen(false)
			setPriority(NotificationCompat.PRIORITY_MIN)
			setOngoing(true)
		}.build()
	}
	
	@JvmStatic
	fun getConnectionBackgroundNotification(context: Context, isConnected: Boolean, isFallback: Boolean): Notification {
		//Building the notification
		return NotificationCompat.Builder(context, notificationChannelStatus).apply {
			setSmallIcon(R.drawable.push)
			setContentTitle(context.resources.getString(
				if(isConnected) {
					if(isFallback) R.string.message_connection_connectedfallback
					else R.string.message_connection_connected
				} else {
					R.string.progress_connectingtoserver
				}
			))
			setContentText(context.resources.getString(R.string.imperative_tapopenapp))
			
			setShowWhen(false)
			setPriority(NotificationCompat.PRIORITY_MIN)
			setOngoing(true)
			setOnlyAlertOnce(true)
			
			setContentIntent(PendingIntent.getActivity(context, 0, Intent(context, Conversations::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
			
			//Disconnect (only available in debug)
			if(BuildConfig.DEBUG) {
				addAction(
						R.drawable.wifi_off,
						context.resources.getString(R.string.action_disconnect),
						PendingIntent.getService(
								context,
								1,
								Intent(context, ConnectionService::class.java).apply {
									action = ConnectionService.selfIntentActionDisconnect
								},
								PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
						)
				)
			}
		}.build()
	}
	
	@JvmStatic
	fun getConnectionOfflineNotification(context: Context, silent: Boolean): Notification {
		//Building the notification
		return NotificationCompat.Builder(context, notificationChannelStatusImportant).apply {
			setSmallIcon(R.drawable.warning)
			setContentTitle(context.resources.getString(R.string.message_connection_disconnected))
			setContentText(context.resources.getString(R.string.imperative_tapopenapp))
			setColor(context.resources.getColor(R.color.colorServerDisconnected, null))
			setContentText(context.resources.getString(R.string.imperative_tapopenapp))
			setContentIntent(PendingIntent.getActivity(context, 0, Intent(context, Conversations::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
			
			setShowWhen(false)
			setPriority(NotificationCompat.PRIORITY_HIGH)
			setOnlyAlertOnce(silent)
			
			addAction(
					R.drawable.wifi,
					context.resources.getString(R.string.action_reconnect),
					PendingIntent.getService(
							context,
							2,
							Intent(context, ConnectionService::class.java).apply {
								action = ConnectionService.selfIntentActionConnect
							},
							PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
					)
			)
		}.build()
	}
	
	@JvmStatic
	fun showFaceTimeCallNotification(context: Context, callerName: String) {
		val fullScreenIntent = Intent(context, FaceTimeCall::class.java).apply {
			putExtra(FaceTimeCall.PARAM_TYPE, FaceTimeCall.Type.incoming)
			putExtra(FaceTimeCall.PARAM_PARTICIPANTS_RAW, callerName)
		}
		val fullScreenPendingIntent = PendingIntent.getActivity(context, 0, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
		
		val notification = NotificationCompat.Builder(context, notificationChannelIncomingCall)
			.setSmallIcon(R.drawable.facetime)
			.setContentTitle(context.resources.getString(R.string.message_facetime_incoming))
			.setContentText(callerName)
			.setPriority(NotificationCompat.PRIORITY_HIGH)
			.setCategory(NotificationCompat.CATEGORY_CALL)
			.setFullScreenIntent(fullScreenPendingIntent, true)
			.setVibrate(null)
			.setOngoing(true)
			.setColor(context.resources.getColor(R.color.colorFaceTime, null))
			.build()
		
		NotificationManagerCompat.from(context)
			.notify(notificationIDIncomingCall, notification)
	}
	
	@JvmStatic
	fun hideFaceTimeCallNotification(context: Context) {
		NotificationManagerCompat.from(context)
			.cancel(notificationIDIncomingCall)
	}
	
	data class NotificationFutureData(val title: String, val shortcutIcon: Optional<IconCompat>, val memberInfo: Optional<UserCacheHelper.UserInfo>, val memberIcon: Optional<Bitmap>, val suggestions: List<String>)
}