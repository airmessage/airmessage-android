package me.tagavari.airmessage.redux;

import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.PublishSubject;

public final class ReduxEmitterNetwork {
	private static final BehaviorSubject<ReduxEventConnection> connectionStateSubject = BehaviorSubject.create();
	private static final PublishSubject<Boolean> connectionConfigurationSubject = PublishSubject.create();
	private static final PublishSubject<ReduxEventMessaging> messageUpdateSubject = PublishSubject.create();
	private static final BehaviorSubject<ReduxEventMassRetrieval> massRetrievalUpdateSubject = BehaviorSubject.create();
	private static final BehaviorSubject<ReduxEventTextImport> textImportUpdateSubject = BehaviorSubject.create();
	
	public static BehaviorSubject<ReduxEventConnection> getConnectionStateSubject() {
		return connectionStateSubject;
	}
	
	public static PublishSubject<Boolean> getConnectionConfigurationSubject() {
		return connectionConfigurationSubject;
	}
	
	public static PublishSubject<ReduxEventMessaging> getMessageUpdateSubject() {
		return messageUpdateSubject;
	}
	
	public static BehaviorSubject<ReduxEventMassRetrieval> getMassRetrievalUpdateSubject() {
		return massRetrievalUpdateSubject;
	}
	
	public static BehaviorSubject<ReduxEventTextImport> getTextImportUpdateSubject() {
		return textImportUpdateSubject;
	}
}