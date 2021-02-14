package me.tagavari.airmessage.util;

public class IndexedItem<T> {
	public final int index;
	public final T item;
	
	public IndexedItem(int index, T item) {
		this.index = index;
		this.item = item;
	}
}