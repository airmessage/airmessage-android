package me.tagavari.airmessage.receiver;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Pair;

import com.klinker.android.send_message.DeliveredReceiver;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.reactivex.rxjava3.schedulers.Schedulers;
import me.tagavari.airmessage.constants.SMSReceiverConstants;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.enums.MessageSendErrorCode;
import me.tagavari.airmessage.enums.MessageState;
import me.tagavari.airmessage.helper.NotificationHelper;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.ConversationItem;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.task.MessageActionTask;

public class TextSMSDeliveredReceiver extends DeliveredReceiver {
	@Override
	public void onMessageStatusUpdated(Context context, Intent intent, int resultCode) {
		//Getting the parameter data
		long messageID = intent.getLongExtra(SMSReceiverConstants.messageID, -1);
		
		//Running on a worker thread
		Single.create((SingleEmitter<Pair<ConversationItem, ConversationInfo>> emitter) -> {
			emitter.onSuccess(DatabaseManager.getInstance().loadConversationItemWithChat(context, messageID));
		}).subscribeOn(Schedulers.single())
				.observeOn(AndroidSchedulers.mainThread())
				.flatMapCompletable(pair -> {
					ConversationInfo conversationInfo = pair.second;
					MessageInfo messageInfo = (MessageInfo) pair.first;
					
					//Updating the message
					if(resultCode == Activity.RESULT_OK) {
						return MessageActionTask.updateMessageState(conversationInfo, messageInfo, MessageState.delivered);
					} else {
						//Sending a notification
						NotificationHelper.sendErrorNotification(context, conversationInfo);
						
						return MessageActionTask.updateMessageErrorCode(conversationInfo, messageInfo, MessageSendErrorCode.localUnknown, null);
					}
				}).subscribe();
	}
}