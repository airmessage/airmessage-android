package me.tagavari.airmessage.receiver;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.klinker.android.send_message.SentReceiver;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.data.SMSIDParcelable;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.ConversationItem;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.util.Constants;
import me.tagavari.airmessage.util.ConversationUtils;
import me.tagavari.airmessage.util.NotificationUtils;

public class TextSMSSentReceiver extends SentReceiver {
	private static final List<SMSSentListener> sentListenerList = new ArrayList<>();
	
	@Override
	public void onMessageStatusUpdated(Context context, Intent intent, int resultCode) {
		//Getting the parcel data
		Bundle bundle = intent.getBundleExtra(Constants.intentParamData);
		SMSIDParcelable parcelData = bundle.getParcelable(Constants.intentParamData);
		
		boolean resultOK = resultCode == Activity.RESULT_OK;
		
		//Running on the main UI thread
		new Handler(Looper.getMainLooper()).post(() -> {
			//Finding the message in memory
			MessageInfo messageInfo = null;
			for(ConversationInfo loadedConversation : ConversationUtils.getLoadedConversations()) {
				ConversationItem conversationItem = loadedConversation.findConversationItem(parcelData.getMessageID());
				if(conversationItem == null) continue;
				
				if(conversationItem instanceof MessageInfo) {
					messageInfo = (MessageInfo) conversationItem;
				}
				
				break;
			}
			
			if(messageInfo != null) {
				//Updating the message
				if(resultOK) {
					messageInfo.setMessageState(Constants.messageStateCodeSent);
					messageInfo.setErrorCode(Constants.messageErrorCodeOK);
				} else {
					messageInfo.setErrorCode(Constants.messageErrorCodeLocalUnknown);
					
					//Sending a notification
					NotificationUtils.sendErrorNotification(context, messageInfo.getConversationInfo());
				}
				
				messageInfo.updateViewProgressState();
				messageInfo.animateGhostStateChanges();
			}
			
			//Calling the listener
			notifyListener(parcelData.getMessageID(), resultOK);
		});
		
		//Updating the message state on disk
		if(resultOK) {
			DatabaseManager.getInstance().updateMessageState(parcelData.getMessageID(), Constants.messageStateCodeSent);
			DatabaseManager.getInstance().updateMessageErrorCode(parcelData.getMessageID(), Constants.messageErrorCodeOK, null);
		} else {
			DatabaseManager.getInstance().updateMessageErrorCode(parcelData.getMessageID(), Constants.messageErrorCodeLocalUnknown, null);
		}
	}
	
	private static void notifyListener(long messageID, boolean result) {
		for(ListIterator<SMSSentListener> iterator = sentListenerList.listIterator(); iterator.hasNext();) {
			SMSSentListener listener = iterator.next();
			if(listener.messageID != messageID) continue;
			listener.onResult(result);
			iterator.remove();
			break;
		}
	}
	
	public static void addListener(SMSSentListener listener) {
		sentListenerList.add(listener);
	}
	
	public static abstract class SMSSentListener {
		private final long messageID;
		public abstract void onResult(boolean result);
		
		public SMSSentListener(long messageID) {
			this.messageID = messageID;
		}
	}
}