package me.tagavari.airmessage;

import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import me.tagavari.airmessage.common.helper.CollectionHelper;

import static com.google.common.truth.Truth.assertThat;

public class CollectionTest {
	@Test
	public void testCollection() {
		Map<String, Integer> inputMap = new HashMap<>();
		inputMap.put("one", 1);
		inputMap.put("two", 2);
		inputMap.put("three", 3);
		inputMap.put("four", 4);
		inputMap.put("five", 5);
		
		LinkedHashMap<String, Integer> sortedMap = CollectionHelper.sortMapByValueDesc(inputMap);
		
		assertThat(sortedMap).hasSize(inputMap.size());
		
		int lastValue = Integer.MAX_VALUE;
		for(Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
			Integer value = sortedMap.get(entry.getKey());
			assertThat(value).isNotNull();
			assertThat(value).isEqualTo(inputMap.get(entry.getKey()));
			assertThat(value).isLessThan(lastValue);
			lastValue = value;
		}
	}
}