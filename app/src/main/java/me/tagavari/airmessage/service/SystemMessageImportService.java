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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.ContactsContract;
import android.provider.Telephony;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.mms.pdu_alt.EncodedStringValue;
import com.google.android.mms.pdu_alt.PduHeaders;
import com.google.android.mms.pdu_alt.PduPersister;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.activity.ConversationsBase;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.messaging.AttachmentInfo;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.ConversationItem;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.util.Constants;
import me.tagavari.airmessage.util.ConversationUtils;

public class SystemMessageImportService extends Service {
	public static final String selfIntentActionImport = "import";
	public static final String selfIntentActionDelete = "delete";
	
	private static final long notificationProgressMinUpdateInterval = 1000;
	
	private static final int notificationID = -3;
	
	private Thread currentThread = null;
	
	public static final String[] smsMixedColumnProjection = {Telephony.BaseMmsColumns._ID, Telephony.Mms.MESSAGE_BOX, Telephony.Sms.TYPE, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.ERROR_CODE, Telephony.Sms.STATUS};
	public static final String[] smsColumnProjection = {Telephony.Sms.TYPE, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.ERROR_CODE, Telephony.Sms.STATUS};
	public static final String[] mmsColumnProjection = {Telephony.Mms._ID, Telephony.Mms.DATE, Telephony.Mms.MESSAGE_BOX, Telephony.Mms.SUBJECT};
	public static final String[] mmsPartColumnProjection = {Telephony.Mms.Part._ID, Telephony.Mms.Part.CONTENT_TYPE, Telephony.Mms.Part.FILENAME, Telephony.Mms.Part._DATA, Telephony.Mms.Part.TEXT};
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		//Getting the intent action
		String intentAction = intent == null ? null : intent.getAction();
		
		//Cancelling the current thread
		if(currentThread != null && currentThread.isAlive()) currentThread.interrupt();
		
		if(selfIntentActionImport.equals(intentAction)) {
			//Launching the import thread
			currentThread = new ImportThread(this, this);
			currentThread.start();
			
			//Posting the progress notification
			startForeground(notificationID, getImportNotification(this));
		} else if(selfIntentActionDelete.equals(intentAction)) {
			//Launching the delete thread
			currentThread = new DeleteThread(this, this);
			currentThread.start();
			
			//Posting the progress notification
			startForeground(notificationID, getDeleteNotification(this));
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
			if(context == null) return;
			
			//Indexing the conversations
			List<ConversationInfo> conversationInfoList = new ArrayList<>();
			
			Cursor cursorConversation = context.getContentResolver().query(
					Uri.parse("content://mms-sms/conversations?simple=true"),
					new String[]{Telephony.Threads._ID, Telephony.Threads.ARCHIVED, Telephony.Threads.RECIPIENT_IDS, Telephony.Threads.DATE, Telephony.Threads.MESSAGE_COUNT},
					null, null, null);
			if(cursorConversation == null) return;
			
			//Getting the column indices
			int iThreadID = cursorConversation.getColumnIndexOrThrow(Telephony.Threads._ID);
			int iArchived = cursorConversation.getColumnIndexOrThrow(Telephony.Threads.ARCHIVED);
			int iRecipientIDs = cursorConversation.getColumnIndexOrThrow(Telephony.Threads.RECIPIENT_IDS);
			int iDate = cursorConversation.getColumnIndexOrThrow(Telephony.Threads.DATE);
			int iMessageCount = cursorConversation.getColumnIndexOrThrow(Telephony.Threads.MESSAGE_COUNT);
			
			int conversationCount = cursorConversation.getCount();
			long lastNotificationUpdateTime = System.currentTimeMillis();
			
			//Starting a database transaction
			DatabaseManager.getInstance().getWritableDatabase().beginTransaction();
			
			try {
				while(cursorConversation.moveToNext()) {
					//Adding to the counter and updating the notification
					{
						long currentTime = System.currentTimeMillis();
						if(currentTime - lastNotificationUpdateTime >= notificationProgressMinUpdateInterval) {
							postNotification(context, getImportNotification(context, cursorConversation.getPosition(), conversationCount));
							lastNotificationUpdateTime = currentTime;
						}
					}
					
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
						conversationInfo.setExternalID(threadID);
						conversationInfo.setConversationMembersCreateColors(getAddressFromRecipientID(context, recipientIDs));
						conversationInfo.setArchived(archived);
					}
					
					//Writing the conversation to disk
					boolean result = DatabaseManager.getInstance().addReadyConversationInfo(conversationInfo);
					if(!result) continue;
					
					//Adding the conversation to the list
					conversationInfoList.add(conversationInfo);
					
					//Querying for the conversation's messages
					Cursor cursorMessage = context.getContentResolver().query(ContentUris.withAppendedId(Telephony.Threads.CONTENT_URI, threadID), smsMixedColumnProjection, null, null, null);
					if(cursorMessage == null) continue;
					
					//Getting the messages columns
					int mMessageID = cursorMessage.getColumnIndexOrThrow(Telephony.BaseMmsColumns._ID);
					int mMMSMessageBox = cursorMessage.getColumnIndexOrThrow(Telephony.Mms.MESSAGE_BOX);
					
					ConversationItem lastConversationItem = null;
					while(cursorMessage.moveToNext()) {
						long messageID = cursorMessage.getLong(mMessageID);
						
						//Used to discern if this is an MMS message or not
						if(cursorMessage.getString(mMMSMessageBox) != null) {
							//MMS message
							try(Cursor cursorMMS = context.getContentResolver().query(ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, messageID), mmsColumnProjection, null, null, null)) {
								if(cursorMMS == null || !cursorMMS.moveToFirst()) continue;
								
								//Reading and saving the message
								MessageInfo messageInfo = readSaveMMSMessage(cursorMMS, context, conversationInfo);
								if(messageInfo == null) continue;
								
								//Setting the last item
								lastConversationItem = messageInfo;
							}
						} else {
							//SMS message
							
							//Getting the message status information
							int type = cursorMessage.getInt(cursorMessage.getColumnIndexOrThrow(Telephony.Sms.TYPE));
							int statusCode = cursorMessage.getInt(cursorMessage.getColumnIndexOrThrow(Telephony.Sms.STATUS));
							
							//Figuring out the message state (thanks to Pulse SMS)
							int messageState;
							boolean isOutgoing = true;
							if(statusCode == Telephony.Sms.STATUS_NONE || type == Telephony.Sms.MESSAGE_TYPE_INBOX) {
								switch(type) {
									case Telephony.Sms.MESSAGE_TYPE_INBOX:
										isOutgoing = false;
										messageState = Constants.messageStateCodeSent;
										break;
									case Telephony.Sms.MESSAGE_TYPE_FAILED:
									case Telephony.Sms.MESSAGE_TYPE_OUTBOX:
										messageState = Constants.messageStateCodeGhost; //Sending
										break;
									case Telephony.Sms.MESSAGE_TYPE_SENT:
									default:
										messageState = Constants.messageStateCodeSent;
										break;
								}
							} else {
								switch(statusCode) {
									case Telephony.Sms.STATUS_COMPLETE:
										messageState = Constants.messageStateCodeDelivered;
										break;
									case Telephony.Sms.STATUS_PENDING:
									default:
										messageState = Constants.messageStateCodeSent;
										break;
									case Telephony.Sms.STATUS_FAILED:
										messageState = Constants.messageStateCodeGhost;
										break;
								}
							}
							
							String sender = isOutgoing ? null : cursorMessage.getString(cursorMessage.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
							String message = cursorMessage.getString(cursorMessage.getColumnIndexOrThrow(Telephony.Sms.BODY));
							long date = cursorMessage.getLong(cursorMessage.getColumnIndexOrThrow(Telephony.Sms.DATE));
							int errorCode = cursorMessage.getInt(cursorMessage.getColumnIndexOrThrow(Telephony.Sms.ERROR_CODE));
							
							//Mapping the error code
							int messageErrorCode = Constants.messageErrorCodeOK;
							if(messageState == Constants.messageStateCodeGhost) {
								messageErrorCode = Constants.messageErrorCodeServerUnknown;
							}
							
							//Creating the message
							MessageInfo messageInfo = new MessageInfo(-1, -1, null, conversationInfo, sender, message, null, false, date, messageState, messageErrorCode, false, -1);
							if(messageErrorCode != Constants.messageErrorCodeOK) {
								messageInfo.setErrorDetails("SMS error code " + errorCode);
							}
							
							//Writing the message to disk
							DatabaseManager.getInstance().addConversationItem(messageInfo, false);
							
							//Setting the last item
							lastConversationItem = messageInfo;
						}
					}
					
					//Setting the conversation's last item
					if(lastConversationItem != null) conversationInfo.trySetLastItem(lastConversationItem.toLightConversationItemSync(context), false);
					
					//Closing the message cursor
					cursorMessage.close();
				}
				
				cursorConversation.close();
				
				//Marking the transaction as successful
				DatabaseManager.getInstance().getWritableDatabase().setTransactionSuccessful();
			} catch(SQLiteException exception) {
				exception.printStackTrace();
			} finally {
				//Finishing the transaction
				DatabaseManager.getInstance().getWritableDatabase().endTransaction();
			}
			
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
		
		private static class MMSAttachmentInfo {
			private final String contentType;
			private final long partID;
			private final String fileName;
			
			public MMSAttachmentInfo(String contentType, long partID, String fileName) {
				this.contentType = contentType;
				this.partID = partID;
				this.fileName = fileName;
			}
			
			public String getContentType() {
				return contentType;
			}
			
			public long getPartID() {
				return partID;
			}
			
			public String getFileName() {
				return fileName;
			}
		}
	}
	
	private static class DeleteThread extends Thread {
		private final WeakReference<SystemMessageImportService> serviceReference;
		private final WeakReference<Context> contextReference;
		
		DeleteThread(SystemMessageImportService service, Context context) {
			//Setting the references
			serviceReference = new WeakReference<>(service);
			contextReference = new WeakReference<>(context);
			
			//Clearing relevant conversations from memory
			ArrayList<ConversationInfo> conversations = ConversationUtils.getConversations();
			if(conversations != null) {
				for(ListIterator<ConversationInfo> iterator = conversations.listIterator(); iterator.hasNext();) {
					ConversationInfo conversationInfo = iterator.next();
					if(conversationInfo.getServiceHandler() != ConversationInfo.serviceHandlerSystemMessaging) continue;
					iterator.remove();
				}
				
				//Updating the conversation activity list
				LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ConversationsBase.localBCConversationUpdate));
			}
		}
		
		@Override
		public void run() {
			//Getting the context
			Context context = contextReference.get();
			if(context == null) return;
			
			//Deleting related conversations and messages from the database
			DatabaseManager.getInstance().deleteConversations(ConversationInfo.serviceHandlerSystemMessaging);
			
			//Running on the main thread
			new Handler(Looper.getMainLooper()).post(() -> {
				//Telling the service that the task has been completed
				SystemMessageImportService service = serviceReference.get();
				if(service != null) {
					service.onFinish();
				}
			});
		}
	}
	
	private void onFinish() {
		//Clearing the notification
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(notificationID);
		
		//Stopping the service
		stopSelf();
	}
	
	private static void postNotification(Context context, Notification notification) {
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(notificationID, notification);
	}
	
	private static Notification getImportNotification(Context context) {
		//Building the notification
		Notification notification = new NotificationCompat.Builder(context, MainApplication.notificationChannelStatus)
				.setSmallIcon(R.drawable.message_download)
				.setContentTitle(context.getResources().getString(R.string.progress_importtextmessages))
				.setProgress(0, 0, true)
				//.setColor(context.getColor(R.color.colorMessageTextMessage))
				//.setColorized(false)
				.setPriority(Notification.PRIORITY_MIN)
				.setShowWhen(false)
				.setLocalOnly(true)
				.build();
		
		//Setting the notification as ongoing
		notification.flags = Notification.FLAG_ONGOING_EVENT;
		
		//Returning the notification
		return notification;
	}
	
	private static Notification getImportNotification(Context context, int progress, int max) {
		//Building the notification
		Notification notification = new NotificationCompat.Builder(context, MainApplication.notificationChannelStatus)
				.setSmallIcon(R.drawable.message_download)
				.setContentTitle(context.getResources().getString(R.string.progress_importtextmessages))
				.setProgress(max, progress, false)
				//.setColor(context.getColor(R.color.colorMessageTextMessage))
				//.setColorized(false)
				.setPriority(Notification.PRIORITY_MIN)
				.setShowWhen(false)
				.setLocalOnly(true)
				.build();
		
		//Setting the notification as ongoing
		notification.flags = Notification.FLAG_ONGOING_EVENT;
		
		//Returning the notification
		return notification;
	}
	
	private static Notification getDeleteNotification(Context context) {
		//Building the notification
		Notification notification = new NotificationCompat.Builder(context, MainApplication.notificationChannelStatus)
				.setSmallIcon(R.drawable.message_download)
				.setContentTitle(context.getResources().getString(R.string.progress_cleantextmessages))
				.setProgress(0, 0, false)
				//.setColor(context.getColor(R.color.colorMessageTextMessage))
				//.setColorized(false)
				.setPriority(Notification.PRIORITY_MIN)
				.setShowWhen(false)
				.setLocalOnly(true)
				.build();
		
		//Setting the notification as ongoing
		notification.flags = Notification.FLAG_ONGOING_EVENT;
		
		//Returning the notification
		return notification;
	}
	
	public static MessageInfo readSaveMMSMessage(Cursor cursorMMS, Context context, ConversationInfo conversationInfo) {
		//Getting the message type
		int messageBox = cursorMMS.getInt(cursorMMS.getColumnIndexOrThrow(Telephony.Mms.MESSAGE_BOX));
		
		//Mapping the status code
		int messageState;
		int messageErrorCode = Constants.messageErrorCodeOK;
		boolean isOutgoing = true;
		switch(messageBox) {
			case Telephony.Mms.MESSAGE_BOX_INBOX:
				messageState = Constants.messageStateCodeSent;
				isOutgoing = false;
				break;
			case Telephony.Mms.MESSAGE_BOX_FAILED:
				messageState = Constants.messageStateCodeGhost;
				messageErrorCode = Constants.messageErrorCodeServerUnknown;
				break;
			case Telephony.Mms.MESSAGE_BOX_OUTBOX:
				messageState = Constants.messageStateCodeGhost; //Sending
				break;
			case Telephony.Mms.MESSAGE_BOX_SENT:
			default:
				messageState = Constants.messageStateCodeSent; //Sent
				break;
		}
		
		//Reading the common message data
		long messageID = cursorMMS.getLong(cursorMMS.getColumnIndexOrThrow(Telephony.Mms._ID));
		long date = cursorMMS.getLong(cursorMMS.getColumnIndexOrThrow(Telephony.Mms.DATE)) * 1000;
		String subject = cursorMMS.getString(cursorMMS.getColumnIndexOrThrow(Telephony.Mms.SUBJECT));
		String sender = isOutgoing ? null : getMMSSender(context, messageID);
		//long threadID = cursorMMS.getLong(cursorMMS.getColumnIndexOrThrow(Telephony.Mms.THREAD_ID));
		
		StringBuilder messageTextSB = new StringBuilder();
		List<ImportThread.MMSAttachmentInfo> messageAttachments = new ArrayList<>();
		
		//This URI has a constant at Telephony.Mms.Part.CONTENT_URI, but for some reason this is only available in Android Q, so we can't use it here
		try(Cursor cursorMMSData = context.getContentResolver().query(Uri.parse("content://mms/part"), mmsPartColumnProjection, Telephony.Mms.Part.MSG_ID + " = ?", new String[]{Long.toString(messageID)}, null)) {
			if(cursorMMSData == null || !cursorMMSData.moveToFirst()) return null;
			
			do {
				//Reading the part data
				long partID = cursorMMSData.getLong(cursorMMSData.getColumnIndex(Telephony.Mms.Part._ID));
				String contentType = cursorMMSData.getString(cursorMMSData.getColumnIndex(Telephony.Mms.Part.CONTENT_TYPE));
				String fileName = cursorMMSData.getString(cursorMMSData.getColumnIndex(Telephony.Mms.Part.FILENAME));
				if(fileName == null) fileName = "unnamed_attachment";
				
				//Checking if the part is text
				if("text/plain".equals(contentType)) {
					//Reading the text
					String data = cursorMMSData.getString(cursorMMSData.getColumnIndex(Telephony.Mms.Part._DATA));
					String body;
					if(data != null) body = getMMSTextContent(context, partID);
					else body = cursorMMSData.getString(cursorMMSData.getColumnIndex(Telephony.Mms.Part.TEXT));
					
					//Appending the text
					messageTextSB.append(body);
				}
				//Ignoring SMIL data
				else if(!"application/smil".equals(contentType)) {
					//Saving the part details
					messageAttachments.add(new ImportThread.MMSAttachmentInfo(contentType, partID, fileName));
				}
			} while(cursorMMSData.moveToNext());
		}
		
		//Getting the message text
		String messageText = messageTextSB.length() > 0 ? messageTextSB.toString() : null;
		
		//Creating the message
		MessageInfo messageInfo = new MessageInfo(-1, -1, null, conversationInfo, sender, messageText, null, false, date, messageState, messageErrorCode, false, -1);
		
		//Adding the attachments
		for(ImportThread.MMSAttachmentInfo attachment : messageAttachments) {
			AttachmentInfo attachmentInfo = ConversationUtils.createAttachmentInfoFromType(-1, null, messageInfo, Constants.cleanFileName(attachment.getFileName()), attachment.getContentType(), -1);
			messageInfo.addAttachment(attachmentInfo);
		}
		
		//Writing the message to disk
		DatabaseManager.getInstance().addConversationItem(messageInfo, false);
		
		//Copying the attachment data
		for(int i = 0; i < messageAttachments.size(); i++) {
			//Getting the attachment information
			ImportThread.MMSAttachmentInfo mmsAttachmentInfo = messageAttachments.get(i);
			AttachmentInfo attachmentInfo = messageInfo.getAttachments().get(i);
			
			//Finding a target file
			File targetFileDir = new File(MainApplication.getDownloadDirectory(context), Long.toString(attachmentInfo.getLocalID()));
			if(!targetFileDir.exists()) targetFileDir.mkdir();
			else if(targetFileDir.isFile()) {
				Constants.recursiveDelete(targetFileDir);
				targetFileDir.mkdir();
			}
			File targetFile = new File(targetFileDir, attachmentInfo.getFileName());
			
			//Writing to the file
			long totalSize = 0;
			
			//Checking if the file type is an image
			try(InputStream inputStream = context.getContentResolver().openInputStream(ContentUris.withAppendedId(Uri.parse("content://mms/part/"), mmsAttachmentInfo.getPartID()));
				FileOutputStream outputStream = new FileOutputStream(targetFile)) {
				if(inputStream == null) throw new IOException("Input stream is null");
				
				byte[] buf = new byte[1024];
				int len;
				while((len = inputStream.read(buf)) > 0) {
					outputStream.write(buf, 0, len);
					totalSize += len;
				}
			} catch(IOException exception) {
				exception.printStackTrace();
				continue;
			}
			
			//Updating the attachment information
			DatabaseManager.getInstance().updateAttachmentFile(attachmentInfo.getLocalID(), context, targetFile, totalSize);
			attachmentInfo.setFile(targetFile);
			attachmentInfo.setFileSize(totalSize);
		}
		
		//Returning the message
		return messageInfo;
	}
	
	private static String getMMSSender(Context context, long messageID) {
		//Querying for the message information
		try(Cursor cursor = context.getContentResolver().query(
				Telephony.Mms.CONTENT_URI.buildUpon().appendPath(Long.toString(messageID)).appendPath("addr").build(),
				new String[]{Telephony.Mms.Addr.ADDRESS, Telephony.Mms.Addr.CHARSET},
				Telephony.Mms.Addr.TYPE + " = " + PduHeaders.FROM, null, null, null)) {
			//Returning immediately if the cursor couldn't be opened
			if(cursor == null || !cursor.moveToFirst()) return null;
			
			//Getting the raw sender
			String sender = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Mms.Addr.ADDRESS));
			if(sender == null || sender.isEmpty()) return null;
			
			//Re-encoding and returning the sender with the correct encoding
			byte[] senderBytes = PduPersister.getBytes(sender);
			int charset = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Mms.Addr.CHARSET));
			return new EncodedStringValue(charset, senderBytes).getString();
		}
	}
	
	private static String getMMSTextContent(Context context, long partID) {
		//Creating the string builder
		StringBuilder stringBuilder = new StringBuilder();
		
		//Opening the stream
		try(InputStream inputStream = context.getContentResolver().openInputStream(ContentUris.withAppendedId(Uri.parse("content://mms/part/"), partID))) {
			//Returning immediately if the stream couldn't be opened
			if(inputStream == null) return null;
			
			try(InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
				//Reading to the string builder
				String buffer;
				do {
					buffer = bufferedReader.readLine();
					stringBuilder.append(buffer);
				} while(buffer != null);
			}
		} catch(IOException exception) {
			//Printing the stack trace
			exception.printStackTrace();
			
			//Returning null
			return null;
		}
		
		//Returning the string builder
		return stringBuilder.toString();
	}
	
	public static String[] getAddressFromRecipientID(Context context, String recipientIDs) {
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