package me.tagavari.airmessage.util

import io.reactivex.rxjava3.subjects.CompletableSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.SingleSubject

/**
 * An abstract representation of a ReactiveX subject that can be cancelled
*/
abstract class RequestSubject<S, T>(
	private val subject: S,
	private val expiryException: Throwable,
	private val requestData: T
) {
	fun get(): S {
		return subject
	}
	
	abstract fun onError(error: Throwable)
	
	fun onExpire() {
		onError(expiryException)
	}
	
	fun getRequestData(): T {
		return requestData
	}
	
	/**
	 * A request subject for implementations that handles onComplete() without any parameters
	 */
	abstract class EmptyCompletable<S, T>(subject: S, expiryException: Throwable, requestData: T) : RequestSubject<S, T>(subject, expiryException, requestData) {
		abstract fun onComplete()
	}
	
	/**
	 * A request subject for [CompletableSubject]
	 */
	class Completable<T>(subject: CompletableSubject, expiryException: Throwable, requestData: T) : EmptyCompletable<CompletableSubject, T>(subject, expiryException, requestData) {
		override fun onComplete() {
			get().onComplete()
		}
		
		override fun onError(error: Throwable) {
			get().onError(error)
		}
	}
	
	/**
	 * A request subject for [SingleSubject]
	 */
	class Single<S, T>(subject: SingleSubject<S>, expiryException: Throwable, requestData: T) : RequestSubject<SingleSubject<S>, T>(subject, expiryException, requestData) {
		override fun onError(error: Throwable) {
			get().onError(error)
		}
	}
	
	/**
	 * A request subject for [PublishSubject]
	 */
	class Publish<S, T>(subject: PublishSubject<S>, expiryException: Throwable, requestData: T) : EmptyCompletable<PublishSubject<S>, T>(subject, expiryException, requestData) {
		override fun onComplete() {
			get().onComplete()
		}
		
		override fun onError(error: Throwable) {
			get().onError(error)
		}
	}
}