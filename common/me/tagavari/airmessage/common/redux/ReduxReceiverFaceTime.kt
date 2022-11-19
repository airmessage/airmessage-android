package me.tagavari.airmessage.common.redux

import android.content.Context
import io.reactivex.rxjava3.disposables.CompositeDisposable
import me.tagavari.airmessage.common.helper.NotificationHelper
import java.util.Optional

//A receiver that handles notifying the user of FaceTime calls
class ReduxReceiverFaceTime(private val context: Context) {
	private val compositeDisposable = CompositeDisposable()
	
	fun initialize() {
		compositeDisposable.add(ReduxEmitterNetwork.faceTimeIncomingCallerSubject.subscribe { caller: Optional<String> ->
			if(caller.isPresent) {
				NotificationHelper.showFaceTimeCallNotification(context, caller.get())
			} else {
				NotificationHelper.hideFaceTimeCallNotification(context)
			}
		})
	}
	
	fun dispose() {
		compositeDisposable.clear()
	}
}