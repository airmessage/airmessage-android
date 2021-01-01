package me.tagavari.airmessage.receiver;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import java.util.ArrayList;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.reactivex.rxjava3.schedulers.Schedulers;
import me.tagavari.airmessage.enums.MessageSendErrorCode;
import me.tagavari.airmessage.enums.MessageState;
import me.tagavari.airmessage.helper.AddressHelper;
import me.tagavari.airmessage.helper.MMSSMSHelper;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.util.Triplet;

public class TextSMSReceivedReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		Bundle bundle = intent.getExtras();
		Object[] pdus = (Object[]) bundle.get("pdus");
		String format = bundle.getString("format");
		
		//Getting the message
		StringBuilder messageBody = new StringBuilder();
		String messageSender = null;
		//long messageTimestamp;
		for(Object o : pdus) {
			//Getting the SMS message
			SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) o, format);
			
			//Appending the information
			messageBody.append(smsMessage.getMessageBody());
			if(smsMessage.getOriginatingAddress() != null) {
				messageSender = AddressHelper.normalizeAddress(smsMessage.getOriginatingAddress());
			}
			//timestamp = smsMessage.getTimestampMillis();
		}
		long timestamp = System.currentTimeMillis();
		
		if(messageSender == null) return;
		String finalMessageSender = messageSender;
		
		//Running on a worker thread
		Single.create((SingleEmitter<Triplet<Boolean, ConversationInfo, MessageInfo>> emitter) -> {
			//Writing the message to Android's database
			insertInternalSMS(context, messageBody.toString(), finalMessageSender, timestamp);
		}).subscribeOn(Schedulers.single()).subscribe();
		
		//Adding the message to the conversation
		MMSSMSHelper.updateTextConversationMessage(context, new String[]{finalMessageSender}, new MessageInfo(-1, -1, null, timestamp, finalMessageSender, messageBody.toString(), null, new ArrayList<>(), null, false, -1, MessageState.sent, MessageSendErrorCode.none, false)).subscribe();
	}
	
	/**
	 * Inserts a message into Android's internal SMS database
	 * @param context The context to use
	 * @param sender The sender of the message
	 * @param body The text content of the message
	 * @param timestamp The date the message was sent
	 */
	private static void insertInternalSMS(Context context, String sender, String body, long timestamp) {
		ContentValues contentValues = new ContentValues();
		contentValues.put(Telephony.Sms.ADDRESS, sender);
		contentValues.put(Telephony.Sms.BODY, body);
		contentValues.put(Telephony.Sms.DATE, System.currentTimeMillis());
		contentValues.put(Telephony.Sms.READ, "1");
		contentValues.put(Telephony.Sms.DATE_SENT, timestamp);
		
		try {
			context.getContentResolver().insert(Telephony.Sms.Inbox.CONTENT_URI, contentValues);
		} catch(Exception exception) {
			exception.printStackTrace();
		}
	}
}