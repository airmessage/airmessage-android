package me.tagavari.airmessage.receiver;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;

import com.klinker.android.send_message.MmsReceivedReceiver;

import java.util.Arrays;

import me.tagavari.airmessage.helper.MMSSMSHelper;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.service.SystemMessageImportService;

public class TextMMSReceivedReceiver extends MmsReceivedReceiver {
	@Override
	public void onMessageReceived(Context context, Uri messageUri) {
		//Getting the standard projection with the thread ID
		String[] projection = Arrays.copyOf(MMSSMSHelper.mmsColumnProjection, MMSSMSHelper.mmsColumnProjection.length + 1);
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
		
		//Reading the message
		MessageInfo messageInfo = MMSSMSHelper.readMMSMessage(context, cursorMMS);
		
		//Saving the message
		if(messageInfo != null) {
			MMSSMSHelper.updateTextConversationMessage(context, threadID, messageInfo).subscribe();
		}
	}
	
	@Override
	public void onError(Context context, String error) {
	
	}
}