package me.tagavari.airmessage.common.util

import android.util.SparseArray
import androidx.core.util.Supplier
import io.reactivex.rxjava3.annotations.CheckReturnValue
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.SingleSubject

/**
 * A helper class that manages running tasks and caching their results
 * @param <T> The result type of this manager's tasks
 */
class TaskManager<T> {
	private val requestMap = SparseArray<SingleSubject<T>>()
	
	/**
	 * Requests the specified task, first by checking for a cached task based on the ID, and afterwards creating a new task from the supplier
	 * @param id The ID to remember this request
	 * @param taskSupplier A supplier that creates a new task
	 * @return A single to represent this task
	 */
	@CheckReturnValue
	fun run(id: Int, taskSupplier: Supplier<Single<T>>): SingleSubject<T> {
		//If we already have a request with a matching key, return that
		var subject = requestMap[id]
		if(subject != null && subject.throwable == null) return subject
		
		//Otherwise, create a new request
		subject = SingleSubject.create()
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
	operator fun get(id: Int): SingleSubject<T>? {
		return requestMap[id]
	}
	
	/**
	 * Adds a result sourced from an external location to the local cache
	 * @param id The ID of the request
	 * @param value The request value
	 */
	fun add(id: Int, value: T) {
		val subject = SingleSubject.create<T>()
		subject.onSuccess(value)
		requestMap.put(id, subject)
	}
	
	/**
	 * Removes a completed result from the cache
	 * @param id The ID of the request
	 */
	fun remove(id: Int) {
		requestMap.remove(id)
	}
	
	/**
	 * Clears all results from the cache
	 */
	fun clear() {
		requestMap.clear()
	}
}