package me.tagavari.airmessage.helper

import java.util.*

object CollectionHelper {
	/**
	 * Returns a [LinkedHashMap] sorted in descending order based on a comparable value
	 * @param map The map to sort
	 * @param <K> The key of the map
	 * @param <V> The value of the map
	 * @return The sorted map
	</V></K> */
	@JvmStatic
	fun <K, V : Comparable<V>> sortMapByValueDesc(map: Map<K, V>): LinkedHashMap<K, V> {
		val list: MutableList<Map.Entry<K, V>> = LinkedList(map.entries)
		list.sortWith { o1: Map.Entry<K, V>, o2: Map.Entry<K, V> -> -o1.value.compareTo(o2.value) }
		val result = LinkedHashMap<K, V>()
		for((key, value) in list) result[key] = value
		return result
	}
}