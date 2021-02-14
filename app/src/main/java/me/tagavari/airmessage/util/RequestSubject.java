package me.tagavari.airmessage.util;

import io.reactivex.rxjava3.subjects.CompletableSubject;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.SingleSubject;

/**
 * An abstract representation of a ReactiveX subject that can be cancelled
 * @param <S>
 */
public abstract class RequestSubject<S> {
	private final S subject;
	private final Throwable expiryException;
	private final Object requestData;
	
	public RequestSubject(S subject, Throwable expiryException) {
		this.subject = subject;
		this.expiryException = expiryException;
		this.requestData = null;
	}
	
	public RequestSubject(S subject, Throwable expiryException, Object requestData) {
		this.subject = subject;
		this.expiryException = expiryException;
		this.requestData = requestData;
	}
	
	public S get() {
		return subject;
	}
	
	public abstract void onError(Throwable e);
	
	public void onExpire() {
		onError(expiryException);
	}
	
	public <T> T getRequestData() {
		return (T) requestData;
	}
	
	/**
	 * A request subject for implementations that handle onComplete() without any parameters
	 */
	public static abstract class EmptyCompletable<T> extends RequestSubject<T> {
		public EmptyCompletable(T subject, Throwable expiryException) {
			super(subject, expiryException);
		}
		
		public EmptyCompletable(T subject, Throwable expiryException, Object requestData) {
			super(subject, expiryException, requestData);
		}
		
		public abstract void onComplete();
	}
	
	/**
	 * A request subject for {@link CompletableSubject}
	 */
	public static final class Completable extends EmptyCompletable<CompletableSubject> {
		public Completable(CompletableSubject subject, Throwable expiryException) {
			super(subject, expiryException);
		}
		
		public Completable(CompletableSubject subject, Throwable expiryException, Object requestData) {
			super(subject, expiryException, requestData);
		}
		
		@Override
		public void onComplete() {
			get().onComplete();
		}
		
		@Override
		public void onError(Throwable e) {
			get().onError(e);
		}
	}
	
	/**
	 * A request subject for {@link SingleSubject<T>}
	 */
	public static final class Single<T> extends RequestSubject<SingleSubject<T>> {
		public Single(SingleSubject<T> subject, Throwable expiryException) {
			super(subject, expiryException);
		}
		
		public Single(SingleSubject<T> subject, Throwable expiryException, Object requestData) {
			super(subject, expiryException, requestData);
		}
		
		@Override
		public void onError(Throwable e) {
			get().onError(e);
		}
	}
	
	/**
	 * A request subject for {@link PublishSubject<T>}
	 */
	public static final class Publish<T> extends EmptyCompletable<PublishSubject<T>> {
		public Publish(PublishSubject<T> subject, Throwable expiryException) {
			super(subject, expiryException);
		}
		
		public Publish(PublishSubject<T> subject, Throwable expiryException, Object requestData) {
			super(subject, expiryException, requestData);
		}
		
		@Override
		public void onComplete() {
			get().onComplete();
		}
		
		@Override
		public void onError(Throwable e) {
			get().onError(e);
		}
	}
}