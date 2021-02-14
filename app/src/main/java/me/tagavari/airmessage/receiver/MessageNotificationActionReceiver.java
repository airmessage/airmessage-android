package me.tagavari.airmessage.receiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.RemoteInput;

import java.util.Collections;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.CheckReturnValue;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.connection.exception.AMRequestException;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.enums.ServiceHandler;
import me.tagavari.airmessage.enums.ServiceType;
import me.tagavari.airmessage.helper.ConnectionServiceLaunchHelper;
import me.tagavari.airmessage.helper.MessageSendHelper;
import me.tagavari.airmessage.helper.NotificationHelper;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.service.ConnectionService;
import me.tagavari.airmessage.task.ConversationActionTask;
import me.tagavari.airmessage.task.MessageActionTask;
import me.tagavari.airmessage.util.ConversationTarget;

//Broadcast receiver for handling actions on message notifications
public class MessageNotificationActionReceiver extends BroadcastReceiver {
	private static final String TAG = MessageNotificationActionReceiver.class.getSimpleName();
	
	//Creating the reference values
	public static final String remoteInputID = "reply";
	
	public static final String intentActionReply = "reply";
	public static final String intentActionMarkRead = "mark_read";
	
	public static final String intentParamConversationID = "conversationID";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if(intent.getAction() == null) return;
		switch(intent.getAction()) {
			case intentActionReply:
				handleReply(context, intent);
				break;
			case intentActionMarkRead:
				handleMarkRead(context, intent);
				break;
		}
	}
	
	/**
	 * Handles the initial action of replying to a message
	 */
	private void handleReply(Context context, Intent intent) {
		//Getting the conversation info
		long conversationID = intent.getLongExtra(intentParamConversationID, -1);
		if(conversationID == -1) return;
		Single.fromCallable(() -> DatabaseManager.getInstance().fetchConversationInfo(context, conversationID))
				.subscribeOn(Schedulers.single())
				.observeOn(AndroidSchedulers.mainThread())
				.flatMapCompletable(conversationInfo -> {
					//Getting the response
					final CharSequence responseMessageCS = getMessage(intent);
					
					//Checking if the response is invalid
					if(responseMessageCS == null) {
						//Refreshing the notification
						NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
						Notification existingNotification = NotificationHelper.getNotification(notificationManager, NotificationHelper.notificationTagMessage, (int) conversationInfo.getLocalID());
						if(existingNotification != null) notificationManager.notify(NotificationHelper.notificationTagMessage, (int) conversationInfo.getLocalID(), existingNotification);
						
						//Returning
						return Completable.error(new Throwable("No message text"));
					}
					
					String responseMessage = responseMessageCS.toString();
					
					Completable completable = Completable.error(new Throwable("No service match"));
					
					//Checking if the service handler is AirMessage Bridge
					if(conversationInfo.getServiceHandler() == ServiceHandler.appleBridge) {
						completable = sendMessageAMBridge(context, conversationInfo, responseMessage);
					}
					//Otherwise checking if the service handler is system messaging
					else if(conversationInfo.getServiceHandler() == ServiceHandler.systemMessaging) {
						if(ServiceType.systemSMS.equals(conversationInfo.getServiceType())) {
							completable = sendMessageSMS(context, conversationInfo, responseMessage);
						}
					}
					
					return completable.doOnComplete(() -> {
						//Updating the notification
						NotificationHelper.addMessageToNotification(context, conversationInfo, responseMessage, null, System.currentTimeMillis(), null);
					}).doOnError(error -> {
						//Logging the error
						Log.w(TAG, "Failed to send message from notification", error);
						
						//Refreshing the notification
						NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
						Notification existingNotification = NotificationHelper.getNotification(notificationManager, NotificationHelper.notificationTagMessage, (int) conversationInfo.getLocalID());
						if(existingNotification != null) notificationManager.notify(NotificationHelper.notificationTagMessage, (int) conversationInfo.getLocalID(), existingNotification);
					}).andThen(ConversationActionTask.unreadConversations(Collections.singleton(conversationInfo), 0)); //Marking the conversation as read
				}).subscribe();
	}
	
	@CheckReturnValue
	private Completable sendMessageAMBridge(Context context, ConversationInfo conversationInfo, String responseMessage) {
		return MessageActionTask.writeMessages(conversationInfo, Collections.singletonList(MessageInfo.blankFromText(responseMessage)))
				.map(list -> list.get(0))
				.subscribeOn(Schedulers.single()).observeOn(AndroidSchedulers.mainThread())
				.flatMapCompletable(messageInfo -> {
					//Getting the connection manager
					ConnectionManager connectionManager = ConnectionService.getConnectionManager();
					
					//Sending the message
					Completable completable;
					if(connectionManager != null && connectionManager.isConnected()) {
						completable = connectionManager.sendMessage(new ConversationTarget.AppleLinked(conversationInfo.getGUID()), responseMessage);
						ConnectionService.getInstance().refreshTemporaryMode();
					} else {
						completable = ConnectionService.addPendingMessage(responseMessage, conversationInfo.getGUID());
						ConnectionServiceLaunchHelper.launchAutomatic(context);
					}
					
					//Updating the message state on fail
					return completable.doOnError(error -> {
						AMRequestException amError = (AMRequestException) error;
						MessageActionTask.updateMessageErrorCode(conversationInfo, messageInfo, amError.getErrorCode(), amError.getErrorDetails()).onErrorComplete().subscribe();
					});
				});
	}
	
	@CheckReturnValue
	private Completable sendMessageSMS(Context context, ConversationInfo conversationInfo, String responseMessage) {
		//Creating the message and saving it to disk
		return MessageActionTask.writeMessages(conversationInfo, Collections.singletonList(MessageInfo.blankFromText(responseMessage)))
				.map(list -> list.get(0))
				.subscribeOn(Schedulers.single()).observeOn(AndroidSchedulers.mainThread())
				.flatMapCompletable(messageInfo -> MessageSendHelper.sendMessageMMSSMS(context, conversationInfo, messageInfo));
	}
	
	/**
	 * Handles the initial action of marking a conversation as read
	 */
	private void handleMarkRead(Context context, Intent intent) {
		//Getting the conversation ID
		long conversationID = intent.getLongExtra(intentParamConversationID, -1);
		if(conversationID == -1) return;
		
		//Marking the conversation as read
		markConversationRead(context, conversationID);
		
		//Dismissing the notification
		NotificationHelper.cancelMessageNotification((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE), (int) conversationID);
	}
	
	/**
	 * Marks a conversation as read
	 */
	private void markConversationRead(Context context, long conversationID) {
		//Updating the conversation
		Single.create((SingleEmitter<ConversationInfo> emitter) -> emitter.onSuccess(DatabaseManager.getInstance().fetchConversationInfo(context, conversationID)))
				.subscribeOn(Schedulers.single()).flatMapCompletable(conversation -> ConversationActionTask.unreadConversations(Collections.singleton(conversation), 0)).subscribe();
	}
	
	/**
	 * Gets the remote input value from an action intent
	 * @param intent The intent to use
	 * @return A char sequence of the entered text, or NULL if unavailable
	 */
	@Nullable
	private CharSequence getMessage(Intent intent) {
		Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
		if(remoteInput == null) return null;
		else return remoteInput.getCharSequence(remoteInputID);
	}
}