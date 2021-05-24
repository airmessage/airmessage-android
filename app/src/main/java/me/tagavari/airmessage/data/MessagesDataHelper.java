package me.tagavari.airmessage.data;

import android.content.Context;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import me.tagavari.airmessage.enums.ServiceHandler;
import me.tagavari.airmessage.redux.ReduxEmitterNetwork;
import me.tagavari.airmessage.redux.ReduxEventMessaging;

import java.util.Arrays;
import java.util.stream.Collectors;

public class MessagesDataHelper {
	/**
	 * Deletes all locally saved attachment files under AirMessage bridge
	 */
	public static Completable deleteAMBAttachments(Context context) {
		//Clearing the attachment files from AM bridge
		return Completable.fromAction(() -> DatabaseManager.getInstance().clearDeleteAttachmentFilesAMBridge(context))
				.subscribeOn(Schedulers.single()).observeOn(AndroidSchedulers.mainThread());
	}
	
	/**
	 * Deletes all messages and conversations under AirMessage bridge
	 */
	public static Completable deleteAMBMessages(Context context) {
		//Removing the messages from the database
		return Single.fromCallable(() -> DatabaseManager.getInstance().deleteConversationsByServiceHandler(context, ServiceHandler.appleBridge))
				.subscribeOn(Schedulers.single())
				.observeOn(AndroidSchedulers.mainThread()).doOnSuccess(deletedIDs -> {
					//Emitting an update
					ReduxEmitterNetwork.getMessageUpdateSubject().onNext(new ReduxEventMessaging.ConversationServiceHandlerDelete(ServiceHandler.appleBridge, Arrays.stream(deletedIDs).boxed().collect(Collectors.toList())));
				}).ignoreElement();
	}
}