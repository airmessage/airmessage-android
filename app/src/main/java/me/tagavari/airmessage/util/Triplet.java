package me.tagavari.airmessage.util;

import java.util.Objects;

/** Container to ease passing around a tuple of three objects. This object provides a sensible
 * implementation of equals(), returning true if equals() is true on each of the contained
 * objects.
 */
public class Triplet<F, S, T> {
	public final F first;
	public final S second;
	public final T third;
	
	/**
	 * Constructor for a Triplet.
	 *
	 * @param first the first object in the triplet
	 * @param second the second object in the triplet
	 * @param third the third object in the triplet
	 */
	public Triplet(F first, S second, T third) {
		this.first = first;
		this.second = second;
		this.third = third;
	}
	
	/**
	 * Checks the two objects for equality by delegating to their respective
	 * {@link Object#equals(Object)} methods.
	 *
	 * @param o the {@link Triplet} to which this one is to be checked for equality
	 * @return true if the underlying objects of the Pair are both considered
	 *         equal
	 */
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof Triplet)) return false;
		Triplet<?, ?, ?> p = (Triplet<?, ?, ?>) o;
		return Objects.equals(p.first, first) && Objects.equals(p.second, second) && Objects.equals(p.third, third);
	}
	
	/**
	 * Compute a hash code using the hash codes of the underlying objects
	 *
	 * @return a hashcode of the Pair
	 */
	@Override
	public int hashCode() {
		return (first == null ? 0 : first.hashCode()) ^ (second == null ? 0 : second.hashCode()) ^ (third == null ? 0 : third.hashCode());
	}
	
	@Override
	public String toString() {
		return "Triplet{" + first + " " + second + " " + third + "}";
	}
	
	/**
	 * Convenience method for creating an appropriately typed triplet.
	 * @param a the first object in the triplet
	 * @param b the second object in the triplet
	 * @param c the third object in the triplet
	 * @return a Triplet that is templatized with the types of a and b and c
	 */
	public static <A, B, C> Triplet<A, B, C> create(A a, B b, C c) {
		return new Triplet<>(a, b, c);
	}
}