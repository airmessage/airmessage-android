package me.tagavari.airmessage.task;

import android.content.Context;
import androidx.annotation.Nullable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.CheckReturnValue;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kotlin.Pair;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.enums.MessageSendErrorCode;
import me.tagavari.airmessage.enums.MessageState;
import me.tagavari.airmessage.enums.ServiceHandler;
import me.tagavari.airmessage.helper.AttachmentStorageHelper;
import me.tagavari.airmessage.messaging.AttachmentInfo;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.redux.ReduxEmitterNetwork;
import me.tagavari.airmessage.redux.ReduxEventMessaging;
import me.tagavari.airmessage.util.ReplaceInsertResult;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MessageActionTask {
	/**
	 * Writes a list of messages to the database
	 */
	@CheckReturnValue
	public static Single<List<MessageInfo>> writeMessages(ConversationInfo conversationInfo, List<MessageInfo> messages) {
		return Observable.fromIterable(messages)
				.observeOn(Schedulers.single()).map(message -> {
					//Write the items to the database
					long messageID = DatabaseManager.getInstance().addConversationItem(conversationInfo.getLocalID(), message, conversationInfo.getServiceHandler() == ServiceHandler.appleBridge);
					if(messageID == -1) throw new Exception("Failed to add message to database");
					
					MessageInfo newMessage = message.clone();
					newMessage.setLocalID(messageID);
					return newMessage;
				}).observeOn(AndroidSchedulers.mainThread()).toList().doOnSuccess(savedMessages -> {
					//Notify the emitter
					ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.Message(Collections.singletonList(new Pair<>(conversationInfo, savedMessages.stream().map(ReplaceInsertResult::createAddition).collect(Collectors.toList())))));
				});
	}
	
	/**
	 * Updates the state of message
	 */
	@CheckReturnValue
	public static Completable updateMessageState(ConversationInfo conversationInfo, MessageInfo message, @MessageState int state) {
		return Completable.create(emitter -> {
			//Write the item to the database
			DatabaseManager.getInstance().updateMessageState(message.getLocalID(), state);
			
			emitter.onComplete();
		}).observeOn(AndroidSchedulers.mainThread()).doOnComplete(() -> {
			//Notify the emitter
			ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.MessageState(message.getLocalID(), state));
		});
	}
	
	/**
	 * Updates the error code
	 */
	@CheckReturnValue
	public static Completable updateMessageErrorCode(ConversationInfo conversationInfo, MessageInfo message, @MessageSendErrorCode int errorCode, @Nullable String errorDetail) {
		//Write the item to the database
		return Completable.fromAction(() -> DatabaseManager.getInstance().updateMessageErrorCode(message.getLocalID(), errorCode, errorDetail))
				.subscribeOn(Schedulers.single()).observeOn(AndroidSchedulers.mainThread()).doOnComplete(() -> {
			//Notify the emitter
			ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.MessageError(conversationInfo, message, errorCode, errorDetail));
		});
	}
	
	/**
	 * Deletes a list of messages
	 */
	@CheckReturnValue
	public static Completable deleteMessages(Context context, ConversationInfo conversationInfo, List<MessageInfo> messages) {
		return Observable.fromIterable(messages)
				.observeOn(Schedulers.single()).doOnNext(message -> {
					//Write the items to the database
					DatabaseManager.getInstance().deleteMessage(context, message.getLocalID());
				}).observeOn(AndroidSchedulers.mainThread()).doOnNext(message -> {
					//Notify the emitter
					ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.MessageDelete(conversationInfo, message));
				}).ignoreElements();
	}
	
	/**
	 * Deletes the file of an attachment
	 */
	@CheckReturnValue
	public static Completable deleteAttachmentFile(long messageID, AttachmentInfo attachmentInfo) {
		return Completable.fromAction(() -> {
			//Deleting the attachment file
			AttachmentStorageHelper.deleteContentFile(AttachmentStorageHelper.dirNameAttachment, attachmentInfo.getFile());
			
			//Updating the attachment's database entry
			DatabaseManager.getInstance().invalidateAttachment(attachmentInfo.getLocalID());
		}).subscribeOn(Schedulers.single()).observeOn(AndroidSchedulers.mainThread()).doOnComplete(() -> {
			//Notify the emitter
			ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.AttachmentFile(messageID, attachmentInfo.getLocalID(), null));
		});
	}
}