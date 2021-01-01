package me.tagavari.airmessage.helper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A set of utilities for compressing and decompressing arbitrary data
 */
public class StandardCompressionHelper {
	/**
	 * GZIP-compresses a byte array
	 * @param data The data to compress
	 * @return The compressed data
	 * @throws IOException If an I/O error has occurred
	 */
	public static byte[] compressGZIP(byte[] data) throws IOException {
		return compressGZIP(data, data.length);
	}
	
	/**
	 * GZIP-compresses a byte array
	 * @param data The data to compress
	 * @param length The length of the data to read and compress
	 * @return The compressed data
	 * @throws IOException If an I/O error has occurred
	 */
	public static byte[] compressGZIP(byte[] data, int length) throws IOException {
		try(ByteArrayOutputStream fin = new ByteArrayOutputStream(); GZIPOutputStream out = new GZIPOutputStream(fin)) {
			out.write(data, 0, length);
			out.close();
			return fin.toByteArray();
		}
	}
	
	/**
	 * GZIP-decompresses a byte array
	 * @param data The data to decompress
	 * @return The decompressed data
	 * @throws IOException If an I/O error has occurred
	 */
	public static byte[] decompressGZIP(byte[] data) throws IOException {
		try(ByteArrayInputStream src = new ByteArrayInputStream(data); GZIPInputStream in = new GZIPInputStream(src);
			ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			DataStreamHelper.copyStream(in, out);
			return out.toByteArray();
		}
	}
}