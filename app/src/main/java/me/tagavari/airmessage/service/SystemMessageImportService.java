package me.tagavari.airmessage.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Telephony;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.activity.ConversationsBase;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.ConversationItem;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.util.Constants;
import me.tagavari.airmessage.util.ConversationUtils;

public class SystemMessageImportService extends Service {
	public static final String selfIntentActionImport = "import";
	public static final String selfIntentActionDelete = "delete";
	
	private static final int notificationID = -2;
	
	private Thread currentThread = null;
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		//Getting the intent action
		String intentAction = intent == null ? null : intent.getAction();
		
		//Cancelling the current thread
		if(currentThread != null && currentThread.isAlive()) currentThread.interrupt();
		
		if(selfIntentActionImport.equals(intentAction)) {
			//Launching the import thread
			currentThread = new ImportThread(this, this);
			currentThread.start();
			
			//Posting the import notification
			postImportNotification();
		} else {
			//Stopping the service
			stopSelf();
		}
		
		//Returning sticky service
		return START_STICKY;
	}
	
	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	private static class ImportThread extends Thread {
		private final WeakReference<SystemMessageImportService> serviceReference;
		private final WeakReference<Context> contextReference;
		
		ImportThread(SystemMessageImportService service, Context context) {
			serviceReference = new WeakReference<>(service);
			contextReference = new WeakReference<>(context);
		}
		
		@Override
		public void run() {
			//Getting the context
			Context context = contextReference.get();
			
			//Indexing the conversations
			List<ConversationInfo> conversationInfoList = new ArrayList<>();
			
			Cursor cursorConversation = context.getContentResolver().query(Uri.parse("content://mms-sms/conversations?simple=true"), new String[]{"*"}, null, null, null);
			if(cursorConversation == null) return;
			
			//Getting the column indices
			int iThreadID = cursorConversation.getColumnIndexOrThrow(Telephony.Threads._ID);
			int iArchived = cursorConversation.getColumnIndexOrThrow(Telephony.Threads.ARCHIVED);
			int iRecipientIDs = cursorConversation.getColumnIndexOrThrow(Telephony.Threads.RECIPIENT_IDS);
			int iDate = cursorConversation.getColumnIndexOrThrow(Telephony.Threads.DATE);
			int iMessageCount = cursorConversation.getColumnIndexOrThrow(Telephony.Threads.MESSAGE_COUNT);
			
			while(cursorConversation.moveToNext()) {
				//Ignoring empty conversations
				if(cursorConversation.getInt(iMessageCount) == 0) continue;
				
				//Getting the conversation thread ID
				long threadID = cursorConversation.getLong(iThreadID);
				
				ConversationInfo conversationInfo;
				{
					//Getting the conversation data
					boolean archived = cursorConversation.getInt(iArchived) == 1;
					String recipientIDs = cursorConversation.getString(iRecipientIDs);
					long date = cursorConversation.getLong(iDate);
					int conversationColor = ConversationInfo.getDefaultConversationColor(date);
					
					//Creating the conversation
					conversationInfo = new ConversationInfo(-1, null, ConversationInfo.ConversationState.READY, ConversationInfo.serviceHandlerSystemMessaging, ConversationInfo.serviceTypeSystemMMSSMS, new ArrayList<>(), null, 0, conversationColor, null, new ArrayList<>(), -1);
					conversationInfo.setConversationMembersCreateColors(getAddressFromRecipientId(context, recipientIDs));
					conversationInfo.setArchived(archived);
				}
				
				//Writing the conversation to disk
				boolean result = DatabaseManager.getInstance().addReadyConversationInfo(conversationInfo);
				if(!result) continue;
				
				//Adding the conversation to the list
				conversationInfoList.add(conversationInfo);
				
				//Querying for the conversation's messages
				Cursor cursorMessage = context.getContentResolver().query(ContentUris.withAppendedId(Telephony.Threads.CONTENT_URI, threadID), new String[]{Telephony.BaseMmsColumns._ID, Telephony.BaseMmsColumns.CONTENT_TYPE}, null, null, null);
				if(cursorMessage == null) continue;
				
				//Getting the messages columns
				int mMessageID = cursorMessage.getColumnIndexOrThrow(Telephony.BaseMmsColumns._ID);
				int mContentType = cursorMessage.getColumnIndexOrThrow(Telephony.BaseMmsColumns.CONTENT_TYPE);
				
				ConversationItem lastConversationItem = null;
				while(cursorMessage.moveToNext()) {
					long messageID = cursorMessage.getLong(mMessageID);
					if("application/vnd.wap.multipart.related".equals(cursorMessage.getString(mContentType))) {
						//MMS message
					} else {
						//SMS message
						Cursor cursorSMS = context.getContentResolver().query(Telephony.Sms.CONTENT_URI, null, Telephony.Sms._ID + " = ?", new String[]{Long.toString(messageID)}, null);
						if(cursorSMS == null || !cursorSMS.moveToFirst()) continue;
						
						int type = cursorSMS.getInt(cursorSMS.getColumnIndexOrThrow(Telephony.Sms.TYPE));
						String sender;
						if(type == Telephony.Sms.MESSAGE_TYPE_SENT) sender = null;
						else sender = cursorSMS.getString(cursorSMS.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
						String message = cursorSMS.getString(cursorSMS.getColumnIndexOrThrow(Telephony.Sms.BODY));
						long date = cursorSMS.getLong(cursorSMS.getColumnIndexOrThrow(Telephony.Sms.DATE));
						int errorCode = cursorSMS.getInt(cursorSMS.getColumnIndexOrThrow(Telephony.Sms.ERROR_CODE));
						int statusCode = cursorSMS.getInt(cursorSMS.getColumnIndexOrThrow(Telephony.Sms.STATUS));
						
						//Mapping the status code
						int messageState;
						if(statusCode != Telephony.Sms.STATUS_COMPLETE) {
							messageState = Constants.messageStateCodeGhost;
						} else {
							messageState = Constants.messageStateCodeSent;
						}
						
						cursorSMS.close();
						
						//Creating the message
						MessageInfo messageInfo = new MessageInfo(-1, -1, null, conversationInfo, sender, message, null, false, date, messageState, errorCode, false, -1);
						
						//Writing the message to disk
						DatabaseManager.getInstance().addConversationItem(messageInfo);
						
						//Setting the last item
						lastConversationItem = messageInfo;
					}
				}
				
				//Setting the conversation's last item
				if(lastConversationItem != null) conversationInfo.trySetLastItem(lastConversationItem.toLightConversationItemSync(context), false);
				
				cursorMessage.close();
			}
			
			cursorConversation.close();
			
			//Running on the main thread
			new Handler(Looper.getMainLooper()).post(() -> {
				//Saving the conversations to memory
				for(ConversationInfo conversationInfo : conversationInfoList) {
					ConversationUtils.addConversation(conversationInfo);
				}
				
				//Updating the conversation activity list
				LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ConversationsBase.localBCConversationUpdate));
				
				//Telling the service that the task has been completed
				SystemMessageImportService service = serviceReference.get();
				if(service != null) {
					service.onFinish();
				}
			});
		}
		
		private static String[] getAddressFromRecipientId(Context context, String recipientIDs) {
			//Getting the target URI
			Uri addressUri = Uri.parse("content://mms-sms/canonical-address");
			
			//Splitting the recipient IDs
			String[] recipientIDArray = recipientIDs.split(" ");
			
			//Creating the list
			String[] recipientAddressArray = new String[recipientIDArray.length];
			
			//Iterating over each recipient
			for(int i = 0; i < recipientIDArray.length; i++) {
				//Querying for the recipient data
				Cursor cursor = context.getContentResolver().query(ContentUris.withAppendedId(addressUri, Long.parseLong(recipientIDArray[i])), new String[]{Telephony.CanonicalAddressesColumns.ADDRESS}, null, null, null);
				//Ignoring invalid or empty results
				if(cursor == null || !cursor.moveToNext()) continue;
				
				//Adding the address to the array
				String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.CanonicalAddressesColumns.ADDRESS));
				recipientAddressArray[i] = address;
				
				cursor.close();
			}
			
			//Returning the array
			return recipientAddressArray;
		}
	}
	
	private void onFinish() {
		//Clearing the notification
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(notificationID);
		
		//Stopping the service
		stopSelf();
	}
	
	private void postImportNotification() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(notificationID, getImportNotification());
	}
	
	private Notification getImportNotification() {
		//Building the notification
		Notification notification = new NotificationCompat.Builder(this, MainApplication.notificationChannelStatus)
				.setSmallIcon(R.drawable.message_download)
				.setContentTitle(getResources().getString(R.string.progress_importtextmessages))
				.setProgress(0, 0, true)
				.setPriority(Notification.PRIORITY_MIN)
				.setShowWhen(false)
				.build();
		
		//Setting the notification as ongoing
		notification.flags = Notification.FLAG_ONGOING_EVENT;
		
		//Returning the notification
		return notification;
	}
}