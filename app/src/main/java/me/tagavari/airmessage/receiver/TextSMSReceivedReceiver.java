package me.tagavari.airmessage.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import androidx.core.util.Consumer;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;

import me.tagavari.airmessage.activity.ConversationsBase;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.util.Constants;
import me.tagavari.airmessage.util.ConversationUtils;
import me.tagavari.airmessage.util.NotificationUtils;

public class TextSMSReceivedReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		Bundle bundle = intent.getExtras();
		Object[] pdus = (Object[]) bundle.get("pdus");
		String format = bundle.getString("format");
		
		for(int i = 0; i < pdus.length; i++) {
			//Getting the message
			SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdus[i], format);
			
			//Saving the message
			new GetAndroidThreadID(context, smsMessage.getOriginatingAddress(), threadID -> new SaveMessageTask(context, smsMessage, threadID, ConversationUtils.findConversationInfoExternalID(threadID, ConversationInfo.serviceHandlerSystemMessaging, ConversationInfo.serviceTypeSystemMMSSMS)).execute()).execute();
		}
	}
	
	private static class GetAndroidThreadID extends AsyncTask<Void, Void, Long> {
		private final WeakReference<Context> contextReference;
		private final String sender;
		private final Consumer<Long> finishListener;
		
		public GetAndroidThreadID(Context context, String sender, Consumer<Long> finishListener) {
			contextReference = new WeakReference<>(context);
			this.sender = sender;
			this.finishListener = finishListener;
		}
		
		@Override
		protected Long doInBackground(Void... voids) {
			//Getting the context
			Context context = contextReference.get();
			if(context == null) return null;
			
			//Finding or creating a matching conversation in Android's message database
			return Telephony.Threads.getOrCreateThreadId(context, sender);
		}
		
		@Override
		protected void onPostExecute(Long externalID) {
			//Calling the listener
			finishListener.accept(externalID);
		}
	}
	
	private static class SaveMessageTask extends AsyncTask<Void, Void, SaveMessageTaskResult> {
		private final WeakReference<Context> contextReference;
		private final SmsMessage smsMessage;
		private long threadID;
		private ConversationInfo conversationInfo;
		
		SaveMessageTask(Context context, SmsMessage smsMessage, long threadID, ConversationInfo conversationInfo) {
			contextReference = new WeakReference<>(context);
			this.smsMessage = smsMessage;
			this.threadID = threadID;
			this.conversationInfo = conversationInfo;
		}
		
		@Override
		protected SaveMessageTaskResult doInBackground(Void... voids) {
			//Getting the context
			Context context = contextReference.get();
			if(context == null) return null;
			
			//Fetching a matching conversation (if one was not found in memory)
			boolean conversationNew = false;
			if(conversationInfo == null) {
				conversationInfo = DatabaseManager.getInstance().findConversationByExternalID(context, threadID, ConversationInfo.serviceHandlerSystemMessaging, ConversationInfo.serviceTypeSystemMMSSMS);
				
				//Creating a new conversation if no existing conversation was found
				if(conversationInfo == null) {
					//Creating the conversation
					int conversationColor = ConversationInfo.getDefaultConversationColor(smsMessage.getTimestampMillis());
					conversationInfo = new ConversationInfo(-1, null, ConversationInfo.ConversationState.READY, ConversationInfo.serviceHandlerSystemMessaging, ConversationInfo.serviceTypeSystemMMSSMS, new ArrayList<>(), null, 0, conversationColor, null, new ArrayList<>(), -1);
					conversationInfo.setExternalID(threadID);
					conversationInfo.setConversationMembersCreateColors(new String[]{smsMessage.getOriginatingAddress()});
					
					//Writing the conversation to disk
					boolean result = DatabaseManager.getInstance().addReadyConversationInfo(conversationInfo);
					if(!result) return null;
					
					conversationNew = true;
				}
			}
			
			//Creating and saving the message
			MessageInfo messageInfo = createMessageInfo(smsMessage, conversationInfo);
			DatabaseManager.getInstance().addConversationItem(messageInfo, false);
			
			//Returning the result
			return new SaveMessageTaskResult(messageInfo, conversationNew ? conversationInfo : null);
		}
		
		@Override
		protected void onPostExecute(SaveMessageTaskResult result) {
			//Returning if the operation was unsuccessful
			if(result == null) return;
			
			//Getting the context
			Context context = contextReference.get();
			if(context == null) return;
			
			//Getting the data
			MessageInfo messageInfo = result.getMessageInfo();
			ConversationInfo conversationInfo = messageInfo.getConversationInfo();
			
			//Adding the message to the conversation in memory
			boolean addItemResult = conversationInfo.addConversationItems(context, Collections.singletonList(messageInfo));
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
			}
			
			//Updating the conversation activity list
			LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ConversationsBase.localBCConversationUpdate));
			
			//Sending a notification
			NotificationUtils.sendNotification(context, result.getMessageInfo());
		}
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
	
	private static MessageInfo createMessageInfo(SmsMessage smsMessage, ConversationInfo conversation) {
		return new MessageInfo(-1, -1, null, conversation, smsMessage.getOriginatingAddress(), smsMessage.getMessageBody(), null, false, smsMessage.getTimestampMillis(), Constants.messageStateCodeSent, Constants.messageErrorCodeOK, false, -1);
	}
}