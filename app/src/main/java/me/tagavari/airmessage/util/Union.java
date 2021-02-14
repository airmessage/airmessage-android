package me.tagavari.airmessage.util;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A class that either contains a {@link File} or {@link Uri}
 */
public class Union<A, B> {
	private final A itemA;
	private final B itemB;
	
	private Union(A itemA, B itemB) {
		this.itemA = itemA;
		this.itemB = itemB;
	}
	
	/**
	 * Creates a union of item A
	 * @param item The item to add
	 * @param <A> This union type's item A
	 * @param <B> This union type's item B
	 * @return A union of type A/B containing A
	 */
	public static <A, B> Union<A, B> ofA(@NonNull A item) {
		return new Union<>(item, null);
	}
	
	/**
	 * Creates a union of item B
	 * @param item The item to add
	 * @param <A> This union type's item A
	 * @param <B> This union type's item B
	 * @return A union of type A/B containing B
	 */
	public static <A, B> Union<A, B> ofB(@NonNull B item) {
		return new Union<>(null, item);
	}
	
	/**
	 * Gets item A from this union
	 */
	@Nullable
	public A getA() {
		return itemA;
	}
	
	/**
	 * Gets item B from this union
	 */
	@Nullable
	public B getB() {
		return itemB;
	}
	
	/**
	 * Gets if this union is of type A
	 */
	public boolean isA() {
		return itemA != null;
	}
	
	/**
	 * Gets if this union is of type B
	 */
	public boolean isB() {
		return itemB != null;
	}
	
	/**
	 * Runs a given runnable, depending on the type in this union
	 * @param runnableA The runnable to run if the type is A
	 * @param runnableB The runnable to run if the item is B
	 */
	public void run(Runnable runnableA, Runnable runnableB) {
		if(itemA != null) runnableA.run();
		else runnableB.run();
	}
	
	/**
	 * Runs a given consumer, depending on the type in this union
	 * @param consumerA The consumer to run if the type is A
	 * @param consumerB The consumer to run if the item is B
	 */
	public void accept(Consumer<A> consumerA, Consumer<B> consumerB) {
		if(itemA != null) consumerA.accept(itemA);
		else consumerB.accept(itemB);
	}
	
	/**
	 * Applies a given transformation to an item, depending on this union's type, and returns it
	 * @param functionA The function to run if the type is A
	 * @param functionB The function to run if the item is B
	 * @param <I> The input type
	 * @param <R> The return type
	 */
	public <I, R> R apply(I input, BiFunction<I, A, R> functionA, BiFunction<I, B, R> functionB) {
		if(itemA != null) return functionA.apply(input, itemA);
		else return functionB.apply(input, itemB);
	}
	
	/**
	 * Maps this union type's contents to a single type
	 * @param mapA The function used to map item A
	 * @param mapB The function used to map item B
	 * @param <R> The return type
	 * @return The mapped value
	 */
	public <R> R map(Function<A, R> mapA, Function<B, R> mapB) {
		if(itemA != null) return mapA.apply(itemA);
		else return mapB.apply(itemB);
	}
}