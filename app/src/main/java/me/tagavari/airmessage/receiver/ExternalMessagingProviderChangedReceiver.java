package me.tagavari.airmessage.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;

import java.util.Arrays;

import me.tagavari.airmessage.helper.MMSSMSHelper;
import me.tagavari.airmessage.helper.MessageSendHelper;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.service.SystemMessageImportService;

public class ExternalMessagingProviderChangedReceiver extends BroadcastReceiver {
	private static final String typeMMS = "mms";
	private static final String typeSMS = "sms";
	
	private static long lastMessageID = -1;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if(!Telephony.Sms.Intents.ACTION_EXTERNAL_PROVIDER_CHANGE.equals(intent.getAction())) return;
		
		//Getting the updated message URI from the notification
		if(intent.getData() == null) return;
		Uri messageUri = intent.getData();
		
		String type;
		if(messageUri.toString().contains(typeSMS)) type = typeSMS;
		else if(messageUri.toString().contains(typeMMS)) type = typeMMS;
		else return;
		
		//Getting the standard projection with the thread ID
		String[] projection = type.equals(typeSMS) ? MMSSMSHelper.smsColumnProjection : MMSSMSHelper.mmsColumnProjection;
		projection = Arrays.copyOf(projection, projection.length + 1);
		projection[projection.length - 1] = Telephony.Mms.THREAD_ID;
		
		//Querying the updated message
		Cursor cursor = context.getContentResolver().query(messageUri, projection, null, null, null);
		if(cursor == null) return;
		if(!cursor.moveToFirst()) {
			cursor.close();
			return;
		}
		
		//Getting the message information
		long messageID = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.BaseMmsColumns._ID));
		long threadID = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Mms.THREAD_ID));
		
		//Ignoring if this message is a duplicate
		if(lastMessageID == messageID) return;
		lastMessageID = messageID;
		
		//Reading the message
		MessageInfo messageInfo;
		if(type.equals(typeMMS)) messageInfo = MMSSMSHelper.readMMSMessage(context, cursor);
		else messageInfo = MMSSMSHelper.readSMSMessage(cursor);
		
		//Saving the message
		if(messageInfo != null) {
			MMSSMSHelper.updateTextConversationMessage(context, threadID, messageInfo).subscribe();
		}
	}
}