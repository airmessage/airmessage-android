package me.tagavari.airmessage.receiver;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import com.klinker.android.send_message.MmsSentReceiver;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kotlin.Pair;
import me.tagavari.airmessage.common.constants.SMSReceiverConstants;
import me.tagavari.airmessage.common.data.DatabaseManager;
import me.tagavari.airmessage.common.enums.MessageSendErrorCode;
import me.tagavari.airmessage.common.enums.MessageState;
import me.tagavari.airmessage.common.helper.NotificationHelper;
import me.tagavari.airmessage.common.messaging.ConversationInfo;
import me.tagavari.airmessage.common.messaging.ConversationItem;
import me.tagavari.airmessage.common.messaging.MessageInfo;
import me.tagavari.airmessage.common.task.MessageActionTask;

public class TextMMSSentReceiver extends MmsSentReceiver {
	@Override
	public void onMessageStatusUpdated(Context context, Intent intent, int resultCode) {
		//Getting the parameter data
		long messageID = intent.getLongExtra(SMSReceiverConstants.messageID, -1);
		
		boolean resultOK = resultCode == Activity.RESULT_OK;
		
		//Running on a worker thread
		Completable.create(emitter -> {
			Pair<ConversationItem, ConversationInfo> pair = DatabaseManager.getInstance().loadConversationItemWithChat(context, messageID);
			if(pair == null) return;
			ConversationInfo conversationInfo = pair.getSecond();
			MessageInfo messageInfo = (MessageInfo) pair.getFirst();
			
			//Updating the message
			if(resultOK) {
				MessageActionTask.updateMessageState(conversationInfo, messageInfo, MessageState.sent).subscribe();
				MessageActionTask.updateMessageErrorCode(conversationInfo, messageInfo, MessageSendErrorCode.none, null).subscribe();
			} else {
				MessageActionTask.updateMessageErrorCode(conversationInfo, messageInfo, MessageSendErrorCode.localUnknown, null).subscribe();
				
				//Sending a notification
				NotificationHelper.sendErrorNotification(context, conversationInfo);
			}
		}).subscribeOn(Schedulers.single()).subscribe();
	}
}