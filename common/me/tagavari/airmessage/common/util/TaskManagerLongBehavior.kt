package me.tagavari.airmessage.common.util

import androidx.collection.LongSparseArray
import androidx.core.util.Supplier
import io.reactivex.rxjava3.annotations.CheckReturnValue
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject

/**
 * A helper class that manages [Observable] running tasks and caching their results in the form of a [BehaviorSubject<T>]
 * @param <T> The result type of this manager's tasks
 */
class TaskManagerLongBehavior<T : Any> {
	private val requestMap = LongSparseArray<BehaviorSubject<T>>()
	
	/**
	 * Requests the specified task, first by checking for a cached task based on the ID, and afterwards creating a new task from the supplier
	 * @param id The ID to remember this request
	 * @param taskSupplier A supplier that creates a new task
	 * @return A single to represent this task
	 */
	@CheckReturnValue
	fun run(id: Long, taskSupplier: Supplier<Observable<T>>): BehaviorSubject<T> {
		//If we already have a request with a matching key, return that
		requestMap[id]?.let { subject ->
			if(subject.throwable != null) {
				return subject
			}
		}
		
		//Otherwise, create a new request
		val subject = BehaviorSubject.create<T>()
		requestMap.put(id, subject)
		taskSupplier.get().subscribe(subject)
		return subject
	}
	
	/**
	 * Gets a request from the cache, returning NULL if the request is not available
	 * @param id The ID of the request
	 * @return The request task, or NULL if unavailable
	 */
	@CheckReturnValue
	operator fun get(id: Long): BehaviorSubject<T>? {
		return requestMap[id]
	}
	
	/**
	 * Adds a result sourced from an external location to the local cache
	 * @param id The ID of the request
	 * @param value The request value
	 */
	fun add(id: Long, value: T) {
		val subject = BehaviorSubject.create<T>()
		subject.onNext(value)
		subject.onComplete()
		requestMap.put(id, subject)
	}
	
	/**
	 * Removes a completed result from the cache
	 * @param id The ID of the request
	 */
	fun remove(id: Long) {
		requestMap.remove(id)
	}
	
	/**
	 * Clears all results from the cache
	 */
	fun clear() {
		requestMap.clear()
	}
}