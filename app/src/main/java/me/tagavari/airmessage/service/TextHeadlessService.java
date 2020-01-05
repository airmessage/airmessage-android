package me.tagavari.airmessage.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Telephony;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.klinker.android.send_message.Message;
import com.klinker.android.send_message.Transaction;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import me.tagavari.airmessage.activity.ConversationsBase;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.receiver.TextSMSReceivedReceiver;
import me.tagavari.airmessage.util.Constants;
import me.tagavari.airmessage.util.ConversationUtils;
import me.tagavari.airmessage.util.NotificationUtils;

public class TextHeadlessService extends IntentService {
	public TextHeadlessService() {
		super(TextHeadlessService.class.getName());
		
		setIntentRedelivery(true);
	}
	
	@Override
	protected void onHandleIntent(@Nullable Intent intent) {
		//Ignoring bad actions
		if(!TelephonyManager.ACTION_RESPOND_VIA_MESSAGE.equals(intent.getAction())) return;
		
		//Getting the intent data
		Bundle extras = intent.getExtras();
		if(extras == null) return;
		
		String message = extras.getString(Intent.EXTRA_TEXT);
		Uri intentUri = intent.getData();
		String recipients = getRecipients(intentUri);
		
		//Ignoring if there is missing information
		if(TextUtils.isEmpty(recipients) || TextUtils.isEmpty(message)) return;
		
		String[] participants = TextUtils.split(recipients, ";");
		
		//Getting the Android thread ID
		long threadID = Telephony.Threads.getOrCreateThreadId(getApplicationContext(), new HashSet<>(Arrays.asList(participants)));
		
		//Running on the main thread
		new Handler(getApplicationContext().getMainLooper()).post(() -> {
			new SaveMessageTask(getApplicationContext(), message, threadID, participants, ConversationUtils.findConversationInfoExternalID(threadID, ConversationInfo.serviceHandlerSystemMessaging, ConversationInfo.serviceTypeSystemMMSSMS)).execute();
		});
	}
	
	private String getRecipients(Uri uri) {
		String base = uri.getSchemeSpecificPart();
		int pos = base.indexOf('?');
		return (pos == -1) ? base : base.substring(0, pos);
	}
	
	private static class GetAndroidThreadID extends AsyncTask<Void, Void, Long> {
		private final WeakReference<Context> contextReference;
		private final String[] participants;
		private final Consumer<Long> finishListener;
		
		public GetAndroidThreadID(Context context, String[] participants, Consumer<Long> finishListener) {
			contextReference = new WeakReference<>(context);
			this.participants = participants;
			this.finishListener = finishListener;
		}
		
		@Override
		protected Long doInBackground(Void... voids) {
			//Getting the context
			Context context = contextReference.get();
			if(context == null) return null;
			
			//Finding or creating a matching conversation in Android's message database
			return Telephony.Threads.getOrCreateThreadId(context, new HashSet<>(Arrays.asList(participants)));
		}
		
		@Override
		protected void onPostExecute(Long externalID) {
			//Calling the listener
			finishListener.accept(externalID);
		}
	}
	
	private static class SaveMessageTask extends AsyncTask<Void, Void, SaveMessageTaskResult> {
		private final WeakReference<Context> contextReference;
		private final String message;
		private final long messageDate;
		private long threadID;
		private String[] participants;
		private ConversationInfo conversationInfo;
		
		SaveMessageTask(Context context, String message, long threadID, String[] participants, ConversationInfo conversationInfo) {
			contextReference = new WeakReference<>(context);
			this.message = message;
			this.messageDate = System.currentTimeMillis();
			this.threadID = threadID;
			this.participants = participants;
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
					int conversationColor = ConversationInfo.getDefaultConversationColor(messageDate);
					conversationInfo = new ConversationInfo(-1, null, ConversationInfo.ConversationState.READY, ConversationInfo.serviceHandlerSystemMessaging, ConversationInfo.serviceTypeSystemMMSSMS, new ArrayList<>(), null, 0, conversationColor, null, new ArrayList<>(), -1);
					conversationInfo.setExternalID(threadID);
					conversationInfo.setConversationMembersCreateColors(participants);
					
					//Writing the conversation to disk
					boolean result = DatabaseManager.getInstance().addReadyConversationInfo(conversationInfo);
					if(!result) return null;
					
					conversationNew = true;
				}
			}
			
			//Creating and saving the message
			MessageInfo messageInfo = new MessageInfo(-1, -1, null, conversationInfo, null, message, null, false, messageDate, Constants.messageStateCodeGhost, Constants.messageErrorCodeOK, false, -1);
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
			
			//Checking if there is a new conversation to be added
			if(result.getNewConversationInfo() != null) {
				//Adding the conversation in memory
				ConversationUtils.addConversation(result.getNewConversationInfo());
			}
			
			//Getting the data
			MessageInfo messageInfo = result.getMessageInfo();
			ConversationInfo conversationInfo = messageInfo.getConversationInfo();
			
			//Adding the message to the conversation in memory
			boolean addItemResult = conversationInfo.addConversationItems(context, Collections.singletonList(messageInfo));
			//Setting the last item if the conversation items couldn't be added
			if(!addItemResult) conversationInfo.trySetLastItemUpdate(context, messageInfo, false);
			
			//Updating the conversation activity list
			LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ConversationsBase.localBCConversationUpdate));
			
			//Sending the message
			messageInfo.sendMessage(context);
			/* Transaction transaction = Constants.getMMSSMSTransaction(context, messageInfo.getLocalID());
			Message message = new Message(messageInfo.getMessageText(), participants);
			transaction.sendNewMessage(message, threadID); */
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
}