package me.tagavari.airmessage.util

import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Function

/**
 * A class that either contains an object of one type or the other
 */
class Union<A, B> private constructor(private val itemA: A?, private val itemB: B?) {
	/**
	 * Gets item A from this union
	 */
	val a: A
		get() = itemA!!
	
	/**
	 * Gets item B from this union
	 */
	val b: B
		get() = itemB!!
	
	/**
	 * Gets item A from this union, or
	 * null if the item is B
	 */
	val nullableA: A?
		get() = itemA
	
	/**
	 * Gets item B from this union, or
	 * null if the item is A
	 */
	val nullableB: B?
		get() = itemB
	
	/**
	 * Gets the item that this union holds
	 */
	val either: Any
		get() = itemA ?: itemB!!
	
	/**
	 * Gets if this union is of type A
	 */
	val isA get() = itemA != null
	
	/**
	 * Gets if this union is of type B
	 */
	val isB get() = itemB != null
	
	/**
	 * Runs a given runnable, depending on the type in this union
	 * @param runnableA The runnable to run if the type is A
	 * @param runnableB The runnable to run if the item is B
	 */
	fun run(runnableA: Runnable, runnableB: Runnable) {
		if(isA) runnableA.run() else runnableB.run()
	}
	
	/**
	 * Runs a given consumer, depending on the type in this union
	 * @param consumerA The consumer to run if the type is A
	 * @param consumerB The consumer to run if the item is B
	 */
	fun accept(consumerA: Consumer<A>, consumerB: Consumer<B>) {
		if(isA) consumerA.accept(a) else consumerB.accept(b)
	}
	
	/**
	 * Applies a given transformation to an item, depending on this union's type, and returns it
	 * @param functionA The function to run if the type is A
	 * @param functionB The function to run if the item is B
	 * @param <I> The input type
	 * @param <R> The return type
	</R></I> */
	fun <I, R> apply(input: I, functionA: BiFunction<I, A, R>, functionB: BiFunction<I, B, R>): R {
		return if(isA) functionA.apply(input, a) else functionB.apply(input, b)
	}
	
	/**
	 * Maps this union type's contents to a single type
	 * @param mapA The function used to map item A
	 * @param mapB The function used to map item B
	 * @param <R> The return type
	 * @return The mapped value
	</R> */
	fun <R> map(mapA: Function<A, R>, mapB: Function<B, R>): R {
		return if(isA) mapA.apply(a) else mapB.apply(b)
	}
	
	override fun equals(other: Any?): Boolean {
		if(this === other) return true
		if(other == null) return false
		if(other !is Union<*, *>) return false
		return either == other.either
	}
	
	override fun hashCode(): Int {
		return either.hashCode()
	}
	
	override fun toString(): String {
		return either.toString()
	}
	
	companion object {
		/**
		 * Creates a union of item A
		 * @param item The item to add
		 * @param <A> This union type's item A
		 * @param <B> This union type's item B
		 * @return A union of type A/B containing A */
		@kotlin.jvm.JvmStatic
		fun <A, B> ofA(item: A): Union<A, B> {
			return Union(item, null)
		}
		
		/**
		 * Creates a union of item B
		 * @param item The item to add
		 * @param <A> This union type's item A
		 * @param <B> This union type's item B
		 * @return A union of type A/B containing B */
		@kotlin.jvm.JvmStatic
		fun <A, B> ofB(item: B): Union<A, B> {
			return Union(null, item)
		}
	}
}