package me.tagavari.airmessage;

import junit.framework.Assert;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class ProcessingTest {
	@Test
	public void testCompression() throws IOException {
		final byte[][] testCompressionValues = {
				"Hello, world!".getBytes(),
				new byte[0],
				new byte[1024 * 1024]
		};
		new Random(0).nextBytes(testCompressionValues[2]);
		
		for(byte[] value : testCompressionValues) {
			byte[] compressed = Constants.compressGZIP(value, value.length);
			byte[] decompressed = Constants.decompressGZIP(compressed);
			Assert.assertTrue(Arrays.equals(value, decompressed));
		}
	}
	
	@Test
	public void testAddressNormalization() {
		final ArrayList<String> inputList = new ArrayList<>();
		inputList.add("example@example.com");
		inputList.add("an_example.te24@example.com");
		inputList.add("1234567890"); //(123) 456-7890
		inputList.add("+12223334444"); //+1 (222) 333-4444
		inputList.add("+1 222 333 4444"); //+1 (222) 333-4444
		inputList.add("+10 (222) 333-4444"); //+1 (222) 333-4444
		inputList.add("+10 222.333.4444"); //+1 (222) 333-4444
		inputList.add("+10 222.333.4444"); //+1 (222) 333-4444
		inputList.add("+10 222.333.4444"); //+1 (222) 333-4444
		
		final ArrayList<String> expectedList = new ArrayList<>();
		expectedList.add("example@example.com");
		expectedList.add("an_example.te24@example.com");
		expectedList.add("1234567890");
		expectedList.add("+12223334444");
		expectedList.add("+12223334444");
		expectedList.add("+102223334444");
		expectedList.add("+102223334444");
		expectedList.add("+102223334444");
		expectedList.add("+102223334444");
		
		for(int i = 0; i < inputList.size(); i++) {
			Assert.assertEquals(expectedList.get(i), Constants.normalizeAddress(inputList.get(i)));
		}
		
		{
			List<String> outputList = Constants.normalizeAddresses(inputList);
			Iterator<String> outputIterator = outputList.iterator();
			for(String expected : expectedList) {
				Assert.assertEquals(expected, outputIterator.next());
			}
		}
	}
}