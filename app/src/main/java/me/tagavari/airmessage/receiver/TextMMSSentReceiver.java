package me.tagavari.airmessage.receiver;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Telephony;

import com.klinker.android.send_message.MmsSentReceiver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.data.SMSIDParcelable;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.ConversationItem;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.util.Constants;
import me.tagavari.airmessage.util.ConversationUtils;

public class TextMMSSentReceiver extends MmsSentReceiver {
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
				if(!(conversationItem instanceof MessageInfo)) break;
				messageInfo = (MessageInfo) conversationItem;
				break;
			}
			
			if(messageInfo != null) {
				//Updating the message
				if(resultOK) {
					messageInfo.setMessageState(Constants.messageStateCodeSent);
					messageInfo.setErrorCode(Constants.messageErrorCodeOK);
				} else {
					messageInfo.setErrorCode(Constants.messageErrorCodeLocalUnknown);
				}
				
				messageInfo.updateViewProgressState();
			}
		});
		
		//Updating the message state on disk
		if(resultOK) {
			DatabaseManager.getInstance().updateMessageState(parcelData.getMessageID(), Constants.messageStateCodeSent);
			DatabaseManager.getInstance().updateMessageErrorCode(parcelData.getMessageID(), Constants.messageErrorCodeOK, null);
		} else {
			DatabaseManager.getInstance().updateMessageErrorCode(parcelData.getMessageID(), Constants.messageErrorCodeLocalUnknown, null);
		}
	}
}