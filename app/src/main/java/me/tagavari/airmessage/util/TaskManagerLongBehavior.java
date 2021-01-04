package me.tagavari.airmessage.util;

import androidx.collection.LongSparseArray;
import androidx.core.util.Supplier;

import io.reactivex.rxjava3.annotations.CheckReturnValue;
import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.SingleSubject;

/**
 * A helper class that manages {@link Observable} running tasks and caching their results in the form of a {@link BehaviorSubject<T>}
 * @param <T> The result type of this manager's tasks
 */
public class TaskManagerLongBehavior<T> {
	private final LongSparseArray<BehaviorSubject<T>> requestMap = new LongSparseArray<>();
	
	/**
	 * Requests the specified task, first by checking for a cached task based on the ID, and afterwards creating a new task from the supplier
	 * @param id The ID to remember this request
	 * @param taskSupplier A supplier that creates a new task
	 * @return A single to represent this task
	 */
	@CheckReturnValue
	public BehaviorSubject<T> run(long id, Supplier<Observable<T>> taskSupplier) {
		//If we already have a request with a matching key, return that
		BehaviorSubject<T> subject = requestMap.get(id);
		//If our request errored out, overwrite it with a new request instead of returning the errored one
		if(subject != null && subject.getThrowable() == null) return subject;
		
		//Otherwise, create a new request
		subject = BehaviorSubject.create();
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
	public BehaviorSubject<T> get(long id) {
		return requestMap.get(id);
	}
	
	/**
	 * Adds a result sourced from an external location to the local cache
	 * @param id The ID of the request
	 * @param value The request value
	 */
	public void add(long id, T value) {
		BehaviorSubject<T> subject = BehaviorSubject.create();
		subject.onNext(value);
		subject.onComplete();
		requestMap.put(id, subject);
	}
	
	/**
	 * Removes a completed result from the cache
	 * @param id The ID of the request
	 */
	public void remove(long id) {
		requestMap.remove(id);
	}
	
	/**
	 * Clears all results from the cache
	 */
	public void clear() {
		requestMap.clear();
	}
}