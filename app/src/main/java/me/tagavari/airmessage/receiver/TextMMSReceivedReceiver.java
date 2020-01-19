package me.tagavari.airmessage.receiver;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.Telephony;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.klinker.android.send_message.MmsReceivedReceiver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import me.tagavari.airmessage.activity.ConversationsBase;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.service.SystemMessageImportService;
import me.tagavari.airmessage.util.ConversationUtils;
import me.tagavari.airmessage.util.NotificationUtils;

public class TextMMSReceivedReceiver extends MmsReceivedReceiver {
	@Override
	public void onMessageReceived(Context context, Uri messageUri) {
		//Getting the standard projection with the thread ID
		String[] projection = Arrays.copyOf(SystemMessageImportService.mmsColumnProjection, SystemMessageImportService.mmsColumnProjection.length + 1);
		projection[projection.length - 1] = Telephony.Mms.THREAD_ID;
		
		//Querying for message information
		Cursor cursorMMS = context.getContentResolver().query(messageUri, projection, null, null, null);
		
		//Returning if there are no results
		if(cursorMMS == null) return;
		
		if(!cursorMMS.moveToFirst()) {
			cursorMMS.close();
			return;
		}
		
		//Getting the thread ID
		long threadID = cursorMMS.getLong(cursorMMS.getColumnIndexOrThrow(Telephony.Mms.THREAD_ID));
		
		//Running on the main thread
		new Handler(context.getMainLooper()).post(() -> {
			//Searching for the conversation in memory
			final ConversationInfo conversationInfoMem = ConversationUtils.findConversationInfoExternalID(threadID, ConversationInfo.serviceHandlerSystemMessaging, ConversationInfo.serviceTypeSystemMMSSMS);
			
			//Returning to a worker thread
			new Thread(() -> {
				ConversationInfo conversationInfo = conversationInfoMem;
				boolean conversationNew = false;
				//Fetching a matching conversation (if one was not found in memory)
				if(conversationInfo == null) {
					conversationInfo = DatabaseManager.getInstance().findConversationByExternalID(context, threadID, ConversationInfo.serviceHandlerSystemMessaging, ConversationInfo.serviceTypeSystemMMSSMS);
					
					//Creating a new conversation if no existing conversation was found
					if(conversationInfo == null) {
						//Getting the conversation participants
						String recipientIDs;
						try(Cursor cursorConversation = context.getContentResolver().query(Uri.parse("content://mms-sms/conversations?simple=true"), new String[]{"*"},
								Telephony.Threads._ID + " = ?", new String[]{Long.toString(threadID)},
								null)) {
							if(cursorConversation == null || !cursorConversation.moveToFirst()) return;
							
							recipientIDs = cursorConversation.getString(cursorConversation.getColumnIndexOrThrow(Telephony.Threads.RECIPIENT_IDS));
						}
						
						//Creating the conversation
						int conversationColor = ConversationInfo.getDefaultConversationColor(threadID);
						conversationInfo = new ConversationInfo(-1, null, ConversationInfo.ConversationState.READY, ConversationInfo.serviceHandlerSystemMessaging, ConversationInfo.serviceTypeSystemMMSSMS, new ArrayList<>(), null, 0, conversationColor, null, new ArrayList<>(), -1);
						conversationInfo.setExternalID(threadID);
						conversationInfo.setConversationMembersCreateColors(SystemMessageImportService.getAddressFromRecipientID(context, recipientIDs));
						
						//Writing the conversation to disk
						boolean result = DatabaseManager.getInstance().addReadyConversationInfo(conversationInfo);
						if(!result) {
							cursorMMS.close();
							return;
						}
						
						conversationNew = true;
					}
				}
				
				//Reading and saving the message
				MessageInfo messageInfo = SystemMessageImportService.readSaveMMSMessage(cursorMMS, context, conversationInfo);
				cursorMMS.close();
				if(messageInfo == null) return;
				
				//Running on the main thread
				final SaveMessageTaskResult result = new SaveMessageTaskResult(messageInfo, conversationNew ? conversationInfo : null);
				new Handler(context.getMainLooper()).post(new Runnable() {
					@Override
					public void run() {
						//Getting the data
						MessageInfo messageInfo = result.getMessageInfo();
						ConversationInfo conversationInfo = messageInfo.getConversationInfo();
						
						//Adding the message to the conversation in memory
						boolean addItemResult = conversationInfo.addConversationItems(context, Collections.singletonList(result.getMessageInfo()));
						//Setting the last item if the conversation items couldn't be added
						if(!addItemResult) conversationInfo.trySetLastItemUpdate(context, messageInfo, false);
						
						//Incrementing the conversation's unread count
						if(!messageInfo.isOutgoing()) {
							conversationInfo.setUnreadMessageCount(conversationInfo.getUnreadMessageCount() + 1);
							conversationInfo.updateUnreadStatus(context);
						}
						
						//Checking if there is a new conversation to be added
						if(result.getNewConversationInfo() != null) {
							//Adding the conversation in memory
							ConversationUtils.addConversation(result.getNewConversationInfo());
						} else {
							//Re-sorting the conversation
							ConversationUtils.sortConversation(conversationInfo);
						}
						
						//Updating the conversation activity list
						LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ConversationsBase.localBCConversationUpdate));
						
						//Sending a notification
						NotificationUtils.sendNotification(context, result.getMessageInfo());
					}
				});
			}).start();
		});
	}
	
	@Override
	public void onError(Context context, String error) {
	
	}
	
	private static class SaveMessageTaskResult {
		private final MessageInfo messageInfo;
		private final ConversationInfo newConversationInfo;
		
		/**
		 * Struct used to hold the result of a save message task
		 * @param messageInfo The message info resulting from this new message
		 * @param newConversationInfo The conversation associated with this message, only assigned if this message is the first of a new thread. Otherwise, this argument should be NULL.
		 */
		public SaveMessageTaskResult(MessageInfo messageInfo, ConversationInfo newConversationInfo) {
			this.messageInfo = messageInfo;
			this.newConversationInfo = newConversationInfo;
		}
		
		public MessageInfo getMessageInfo() {
			return messageInfo;
		}
		
		public ConversationInfo getNewConversationInfo() {
			return newConversationInfo;
		}
	}
}