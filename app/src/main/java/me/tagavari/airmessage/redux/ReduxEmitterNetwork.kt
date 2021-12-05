package me.tagavari.airmessage.redux

import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import me.tagavari.airmessage.data.SharedPreferencesManager
import me.tagavari.airmessage.util.ServerUpdateData
import java.util.*

object ReduxEmitterNetwork {
	@JvmStatic
	val connectionStateSubject: BehaviorSubject<ReduxEventConnection> = BehaviorSubject.create()
	
	@JvmStatic
	val connectionConfigurationSubject: PublishSubject<Boolean> = PublishSubject.create()
	
	@JvmStatic
	val messageUpdateSubject: PublishSubject<ReduxEventMessaging> = PublishSubject.create()
	
	@JvmStatic
	val massRetrievalUpdateSubject: BehaviorSubject<ReduxEventMassRetrieval> = BehaviorSubject.create()
	
	@JvmStatic
	val textImportUpdateSubject: BehaviorSubject<ReduxEventTextImport> = BehaviorSubject.create()

	@JvmStatic
	val remoteUpdateSubject: BehaviorSubject<Optional<ServerUpdateData>> = BehaviorSubject.create()

	@JvmStatic
	val remoteUpdateProgressSubject: PublishSubject<ReduxEventRemoteUpdate> = PublishSubject.create()

	@JvmStatic
	val serverFaceTimeSupportSubject: BehaviorSubject<Boolean> = BehaviorSubject.create()
}