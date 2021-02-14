package me.tagavari.airmessage.util;

import android.util.SparseArray;

import androidx.collection.LongSparseArray;
import androidx.core.util.Supplier;

import io.reactivex.rxjava3.annotations.CheckReturnValue;
import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.subjects.SingleSubject;

/**
 * A helper class that manages running tasks and caching their results
 * @param <T> The result type of this manager's tasks
 */
public class TaskManager<T> {
	private final SparseArray<SingleSubject<T>> requestMap = new SparseArray<>();
	
	/**
	 * Requests the specified task, first by checking for a cached task based on the ID, and afterwards creating a new task from the supplier
	 * @param id The ID to remember this request
	 * @param taskSupplier A supplier that creates a new task
	 * @return A single to represent this task
	 */
	@CheckReturnValue
	public SingleSubject<T> run(int id, Supplier<Single<T>> taskSupplier) {
		//If we already have a request with a matching key, return that
		SingleSubject<T> subject = requestMap.get(id);
		//If our request errored out, overwrite it with a new request instead of returning the errored one
		if(subject != null && subject.getThrowable() == null) return subject;
		
		//Otherwise, create a new request
		subject = SingleSubject.create();
		requestMap.put(id, subject);
		taskSupplier.get().subscribe(subject);
		return subject;
	}
	
	/**
	 * Gets a request from the cache, returning NULL if the request is not available
	 * @param id The ID of the request
	 * @return The request task, or NULL if unavailable
	 */
	@Nullable
	@CheckReturnValue
	public SingleSubject<T> get(int id) {
		return requestMap.get(id);
	}
	
	/**
	 * Adds a result sourced from an external location to the local cache
	 * @param id The ID of the request
	 * @param value The request value
	 */
	public void add(int id, T value) {
		SingleSubject<T> subject = SingleSubject.create();
		subject.onSuccess(value);
		requestMap.put(id, subject);
	}
}