package me.tagavari.airmessage.worker

import android.content.Context
import android.util.Log
import androidx.work.WorkerParameters
import androidx.work.rxjava3.RxWorker
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import me.tagavari.airmessage.data.DatabaseManager
import me.tagavari.airmessage.data.SharedPreferencesManager
import me.tagavari.airmessage.enums.ServiceHandler
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxEventMessaging

class SystemMessageCleanupWorker(appContext: Context, workerParams: WorkerParameters): RxWorker(appContext, workerParams) {
    override fun createWork(): Single<Result> {
        Log.i(TAG, "Starting system message cleanup worker")

        return Single.fromCallable {
            DatabaseManager.getInstance().deleteConversationsByServiceHandler(applicationContext, ServiceHandler.systemMessaging)
        }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { deletedIDs: LongArray ->
                //Sending an update
                ReduxEmitterNetwork.messageUpdateSubject.onNext(ReduxEventMessaging.ConversationServiceHandlerDelete(ServiceHandler.systemMessaging, deletedIDs.toList()))

                //Updating the shared preferences value
                SharedPreferencesManager.setTextMessageConversationsInstalled(applicationContext, false)
            }
            .map { Result.success() }
    }

    companion object {
        private val TAG = SystemMessageCleanupWorker::class.java.simpleName
        const val workName = "SystemMessageCleanupWorker"
    }
}