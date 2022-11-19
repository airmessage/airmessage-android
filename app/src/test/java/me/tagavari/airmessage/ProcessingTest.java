package me.tagavari.airmessage;

import org.junit.Test;

import java.io.IOException;
import java.util.Random;

import me.tagavari.airmessage.common.helper.StandardCompressionHelper;

import static com.google.common.truth.Truth.assertThat;

public class ProcessingTest {
	@Test
	public void testCompression() throws IOException {
		final byte[][] testCompressionValues = {
				"Hello, world!".getBytes(),
				new byte[0],
				new byte[1024 * 1024]
		};
		new Random().nextBytes(testCompressionValues[2]);
		
		for(byte[] value : testCompressionValues) {
			byte[] compressed = StandardCompressionHelper.compressGZIP(value, value.length);
			byte[] decompressed = StandardCompressionHelper.decompressGZIP(compressed);
			assertThat(value).isEqualTo(decompressed);
		}
	}
}