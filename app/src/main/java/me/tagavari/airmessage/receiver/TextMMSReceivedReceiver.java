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
		
		//Handling the new message
		SystemMessageImportService.handleNewMessage(context, threadID, new SystemMessageImportService.CursorMessageMapper(cursorMMS) {
			@Override
			public MessageInfo map(Context context, ConversationInfo conversationInfo) {
				return SystemMessageImportService.readSaveMMSMessage(getCursor(), context, conversationInfo);
			}
		});
	}
	
	@Override
	public void onError(Context context, String error) {
	
	}
}