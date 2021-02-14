package me.tagavari.airmessage.util;

public interface ObjIntConsumer<T> {
	/**
	 * Performs this operation on the given arguments.
	 * @param t the first input argument
	 * @param value the second input argument
	 */
	void accept(T t, int value);
}