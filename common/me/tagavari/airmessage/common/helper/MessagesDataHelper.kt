package me.tagavari.airmessage.common.helper

import android.content.Context
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import me.tagavari.airmessage.common.data.DatabaseManager
import me.tagavari.airmessage.common.enums.ServiceHandler
import me.tagavari.airmessage.common.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.common.redux.ReduxEventMessaging.ConversationServiceHandlerDelete
import java.util.*
import java.util.stream.Collectors

object MessagesDataHelper {
	/**
	 * Deletes all locally saved attachment files under AirMessage bridge
	 */
	@JvmStatic
	fun deleteAMBAttachments(context: Context): Completable {
		//Clearing the attachment files from AM bridge
		return Completable.fromAction { DatabaseManager.getInstance().clearDeleteAttachmentFilesAMBridge(context) }
			.subscribeOn(Schedulers.single()).observeOn(AndroidSchedulers.mainThread())
	}
	
	/**
	 * Deletes all messages and conversations under AirMessage bridge
	 */
	@JvmStatic
	fun deleteAMBMessages(context: Context): Completable {
		//Removing the messages from the database
		return Single.fromCallable {
			DatabaseManager.getInstance().deleteConversationsByServiceHandler(context, ServiceHandler.appleBridge)
		}
			.subscribeOn(Schedulers.single())
			.observeOn(AndroidSchedulers.mainThread())
			.doOnSuccess { deletedIDs: LongArray ->
				//Emitting an update
				ReduxEmitterNetwork.messageUpdateSubject.onNext(ConversationServiceHandlerDelete(ServiceHandler.appleBridge, Arrays.stream(deletedIDs).boxed().collect(Collectors.toList())))
			}
			.ignoreElement()
	}
}