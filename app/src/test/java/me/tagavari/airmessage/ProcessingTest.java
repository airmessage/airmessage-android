package me.tagavari.airmessage;

import android.graphics.Color;
import android.support.v4.graphics.ColorUtils;

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
	
	@Test
	public void testColorModifiers() {
		final int[] presetColors = {
				0xFFFFFF,
				0x000000,
				0x808080
		};
		final int[] randomColors = new int[100];
		{
			Random random = new Random(0);
			for(int i = 0; i < randomColors.length; i++) randomColors[i] = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256));
		}
		
		for(int color : presetColors) {
			float[] hsl = new float[3];
			ColorUtils.colorToHSL(color, hsl);
			float colorLightness = hsl[2];
			
			{
				int result = ColorHelper.lightenColor(color);
				if(colorLightness == 1) Assert.assertEquals(color, result);
				else {
					float[] resultHsl = new float[3];
					ColorUtils.colorToHSL(result, resultHsl);
					float resultLightness = resultHsl[2];
					Assert.assertTrue(resultLightness + " (" + Integer.toHexString(result) + ") is not greater than " + colorLightness + " (" + Integer.toHexString(color) + ")", colorLightness < resultLightness);
				}
			}
			
			{
				int result = ColorHelper.darkenColor(color);
				if(colorLightness == 0) Assert.assertEquals(color, result);
				else {
					float[] resultHsl = new float[3];
					ColorUtils.colorToHSL(result, resultHsl);
					float resultLightness = resultHsl[2];
					Assert.assertTrue(resultLightness + " (" + Integer.toHexString(result) + ") is not less than " + colorLightness + " (" + Integer.toHexString(color) + ")", colorLightness > resultLightness);
				}
			}
		}
		for(int color : randomColors) {
			float[] hsl = new float[3];
			ColorUtils.colorToHSL(color, hsl);
			float colorLightness = hsl[2];
			
			{
				int result = ColorHelper.lightenColor(color);
				if(colorLightness == 1) Assert.assertEquals(color, result);
				else {
					float[] resultHsl = new float[3];
					ColorUtils.colorToHSL(result, resultHsl);
					float resultLightness = resultHsl[2];
					Assert.assertTrue(result + "(" + resultLightness + ") is not greater than " + color + " (" + colorLightness + ")", colorLightness < resultLightness);
				}
			}
			
			{
				int result = ColorHelper.darkenColor(color);
				if(colorLightness == 0) Assert.assertEquals(color, result);
				else {
					float[] resultHsl = new float[3];
					ColorUtils.colorToHSL(result, resultHsl);
					float resultLightness = resultHsl[2];
					Assert.assertTrue(result + "(" + resultLightness + ") is not less than " + color + " (" + colorLightness + ")", colorLightness > resultLightness);
				}
			}
		}
	}
}