package me.tagavari.airmessage.service;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

import me.tagavari.airmessage.helper.MMSSMSHelper;
import me.tagavari.airmessage.helper.MessageSendHelper;
import me.tagavari.airmessage.messaging.MessageInfo;

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
		
		List<String> participants = Arrays.asList(TextUtils.split(recipients, ";"));
		MessageInfo messageInfo = MessageInfo.blankFromText(message);
		
		//Adding the message
		MMSSMSHelper.updateTextConversationMessage(this, participants, messageInfo)
				//Sending the message
				.flatMapCompletable(pair -> MessageSendHelper.sendMessageMMSSMS(this, pair.getFirst(), pair.getSecond()))
				.subscribe();
	}
	
	private String getRecipients(Uri uri) {
		String base = uri.getSchemeSpecificPart();
		int pos = base.indexOf('?');
		return (pos == -1) ? base : base.substring(0, pos);
	}
}