package me.tagavari.airmessage.helper;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CollectionHelper {
	/**
	 * Returns a {@link LinkedHashMap} sorted in descending order based on a comparable value
	 * @param map The map to sort
	 * @param <K> The key of the map
	 * @param <V> The value of the map
	 * @return The sorted map
	 */
	public static <K, V extends Comparable<? super V>> LinkedHashMap<K, V> sortMapByValueDesc(Map<K, V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
		Collections.sort(list, (o1, o2) -> -o1.getValue().compareTo(o2.getValue()));
		
		LinkedHashMap<K, V> result = new LinkedHashMap<>();
		for(Map.Entry<K, V> entry : list) result.put(entry.getKey(), entry.getValue());
		return result;
	}
}