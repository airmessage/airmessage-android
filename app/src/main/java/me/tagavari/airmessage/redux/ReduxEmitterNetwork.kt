package me.tagavari.airmessage.redux

import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject

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
	
}