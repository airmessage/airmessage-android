package me.tagavari.airmessage.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.IBinder;
import android.provider.Telephony;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.data.SharedPreferencesManager;
import me.tagavari.airmessage.enums.ConversationState;
import me.tagavari.airmessage.enums.ServiceHandler;
import me.tagavari.airmessage.enums.ServiceType;
import me.tagavari.airmessage.helper.ConversationColorHelper;
import me.tagavari.airmessage.helper.MMSSMSHelper;
import me.tagavari.airmessage.helper.NotificationHelper;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.ConversationPreview;
import me.tagavari.airmessage.messaging.MemberInfo;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.redux.ReduxEmitterNetwork;
import me.tagavari.airmessage.redux.ReduxEventTextImport;

public class SystemMessageImportService extends Service {
	public static final String selfIntentActionImport = "import";
	
	private static final long notificationProgressMinUpdateInterval = 1000;
	
	private static final int notificationID = NotificationHelper.notificationIDMessageImport;
	
	private Disposable currentTask;
	private final Scheduler requestScheduler = Schedulers.from(Executors.newSingleThreadExecutor(), true);
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//Getting the intent action
		String intentAction = intent == null ? null : intent.getAction();
		
		//Cancelling the task thread
		if(currentTask != null && !currentTask.isDisposed()) currentTask.dispose();
		
		if(selfIntentActionImport.equals(intentAction)) {
			//Starting the import process
			currentTask = importMessages();
			
			//Posting the progress notification
			startForeground(notificationID, getImportNotification(this));
		} else {
			//Stopping the service
			stopSelf();
		}
		
		//Returning sticky service
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		//Clearing the notification
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(notificationID);
		
		//Shutting down the scheduler
		requestScheduler.shutdown();
	}
	
	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	private Disposable importMessages() {
		//Updating the shared preferences value
		SharedPreferencesManager.setTextMessageConversationsInstalled(this, true);
		
		return Single.fromCallable(() -> {
			List<ConversationInfo> conversationInfoList = new ArrayList<>();
			
			//Querying the conversations
			Cursor cursorConversation = getContentResolver().query(
					Uri.parse("content://mms-sms/conversations?simple=true"),
					new String[]{Telephony.Threads._ID,/* Telephony.Threads.ARCHIVED,*/ Telephony.Threads.RECIPIENT_IDS, Telephony.Threads.MESSAGE_COUNT},
					null, null, null);
			if(cursorConversation == null) throw new Exception("Failed to load conversations from Android database");
			
			//Getting the column indices
			int iThreadID = cursorConversation.getColumnIndexOrThrow(Telephony.Threads._ID);
			//int iArchived = cursorConversation.getColumnIndexOrThrow(Telephony.Threads.ARCHIVED);
			int iRecipientIDs = cursorConversation.getColumnIndexOrThrow(Telephony.Threads.RECIPIENT_IDS);
			int iMessageCount = cursorConversation.getColumnIndexOrThrow(Telephony.Threads.MESSAGE_COUNT);
			
			int conversationCount = cursorConversation.getCount();
			long lastNotificationUpdateTime = System.currentTimeMillis();
			
			//Emitting an update
			Completable.fromAction(() -> ReduxEmitterNetwork.getTextImportUpdateSubject().onNext(new ReduxEventTextImport.Start(conversationCount)))
					.subscribeOn(AndroidSchedulers.mainThread()).subscribe();
			
			try {
				while(cursorConversation.moveToNext()) {
					//Adding to the counter and updating the notification
					{
						long currentTime = System.currentTimeMillis();
						if(currentTime - lastNotificationUpdateTime >= notificationProgressMinUpdateInterval) {
							postNotification(this, getImportNotification(this, cursorConversation.getPosition(), conversationCount));
							lastNotificationUpdateTime = currentTime;
						}
					}
					
					//Emitting an update
					Completable.fromAction(() -> ReduxEmitterNetwork.getTextImportUpdateSubject().onNext(new ReduxEventTextImport.Progress(cursorConversation.getPosition(), conversationCount)))
							.subscribeOn(AndroidSchedulers.mainThread()).subscribe();
					
					//Ignoring empty conversations
					if(cursorConversation.getInt(iMessageCount) == 0) continue;
					
					//Getting the conversation thread ID
					long threadID = cursorConversation.getLong(iThreadID);
					
					ConversationInfo conversationInfo;
					{
						//Getting the conversation data
						//boolean archived = cursorConversation.getInt(iArchived) == 1;
						String recipientIDs = cursorConversation.getString(iRecipientIDs);
						int conversationColor = ConversationColorHelper.getDefaultConversationColor(threadID);
						List<MemberInfo> members = ConversationColorHelper.getColoredMembers(MMSSMSHelper.getAddressFromRecipientID(this, recipientIDs), conversationColor);
						
						//Creating the conversation
						conversationInfo = new ConversationInfo(-1, null, threadID, ConversationState.ready, ServiceHandler.systemMessaging, ServiceType.systemSMS, conversationColor, members, null, 0, false, false, null, null, new ArrayList<>(), -1);
					}
					
					//Writing the conversation to disk
					boolean result = DatabaseManager.getInstance().addConversationInfo(conversationInfo);
					if(!result) continue;
					
					//Adding the conversation to the list
					conversationInfoList.add(conversationInfo);
					
					//Querying for the conversation's messages
					Cursor cursorMessage = getContentResolver().query(ContentUris.withAppendedId(Telephony.Threads.CONTENT_URI, threadID), MMSSMSHelper.smsMixedColumnProjection, null, null, null);
					if(cursorMessage == null) continue;
					
					//Getting the messages columns
					int mMMSMessageBox = cursorMessage.getColumnIndexOrThrow(Telephony.Mms.MESSAGE_BOX);
					
					MessageInfo lastMessage = null;
					while(cursorMessage.moveToNext()) {
						MessageInfo messageInfo;
						
						//Used to discern if this is an MMS message or not
						if(cursorMessage.getString(mMMSMessageBox) != null) { //MMS message
							messageInfo = MMSSMSHelper.readMMSMessage(this, cursorMessage);
						} else { //SMS message
							messageInfo = MMSSMSHelper.readSMSMessage(cursorMessage);
						}
						if(messageInfo == null) continue;
						
						//Writing the message to disk
						long messageID = DatabaseManager.getInstance().addConversationItem(conversationInfo.getLocalID(), messageInfo, false);
						messageInfo.setLocalID(messageID);
						
						//Setting the last item
						lastMessage = messageInfo;
					}
					
					//Setting the conversation's preview
					if(lastMessage != null) conversationInfo.setMessagePreview(ConversationPreview.Message.fromMessage(lastMessage));
					
					//Closing the message cursor
					cursorMessage.close();
				}
				
				cursorConversation.close();
			} catch(SQLiteException exception) {
				exception.printStackTrace();
			}
			
			return conversationInfoList;
		}).subscribeOn(requestScheduler)
				.observeOn(AndroidSchedulers.mainThread()).subscribe(conversationInfoList -> {
			//Emitting an update
			ReduxEmitterNetwork.getTextImportUpdateSubject().onNext(new ReduxEventTextImport.Complete(conversationInfoList));
			
			//Finishing the service
			stopSelf();
		}, error -> {
			//Emitting an update
			ReduxEmitterNetwork.getTextImportUpdateSubject().onNext(new ReduxEventTextImport.Fail());
			
			//Finishing the service
			stopSelf();
		});
	}
	
	private static void postNotification(Context context, Notification notification) {
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(notificationID, notification);
	}
	
	private static Notification getImportNotification(Context context) {
		//Building the notification
		Notification notification = new NotificationCompat.Builder(context, NotificationHelper.notificationChannelStatus)
				.setSmallIcon(R.drawable.message_download)
				.setContentTitle(context.getResources().getString(R.string.progress_importtextmessages))
				.setProgress(0, 0, true)
				.setPriority(Notification.PRIORITY_MIN)
				.setShowWhen(false)
				.setLocalOnly(true)
				.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
				.build();
		
		//Setting the notification as ongoing
		notification.flags = Notification.FLAG_ONGOING_EVENT;
		
		//Returning the notification
		return notification;
	}
	
	private static Notification getImportNotification(Context context, int progress, int max) {
		//Building the notification
		Notification notification = new NotificationCompat.Builder(context, NotificationHelper.notificationChannelStatus)
				.setSmallIcon(R.drawable.message_download)
				.setContentTitle(context.getResources().getString(R.string.progress_importtextmessages))
				.setProgress(max, progress, false)
				.setPriority(Notification.PRIORITY_MIN)
				.setShowWhen(false)
				.setLocalOnly(true)
				.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
				.build();
		
		//Setting the notification as ongoing
		notification.flags = Notification.FLAG_ONGOING_EVENT;
		
		//Returning the notification
		return notification;
	}
}