package me.tagavari.airmessage.helper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterOutputStream;

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
		try(ByteArrayOutputStream fin = new ByteArrayOutputStream(); OutputStream out = new GZIPOutputStream(fin)) {
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
		return decompressGZIP(data, data.length);
	}
	
	/**
	 * GZIP-decompresses a byte array
	 * @param data The data to decompress
	 * @param length The length of the data to read and decompress
	 * @return The decompressed data
	 * @throws IOException If an I/O error has occurred
	 */
	public static byte[] decompressGZIP(byte[] data, int length) throws IOException {
		try(GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(data, 0, length)); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			DataStreamHelper.copyStream(in, out);
			return out.toByteArray();
		}
	}
	
	/**
	 * Deflates a byte array
	 * @param data The data to compress
	 * @return The compressed data
	 * @throws IOException If an I/O error has occurred
	 */
	public static byte[] compressDeflate(byte[] data) throws IOException {
		return compressDeflate(data, data.length);
	}
	
	/**
	 * Deflates a byte array
	 * @param data The data to compress
	 * @param length The length of the data to read and compress
	 * @return The compressed data
	 * @throws IOException If an I/O error has occurred
	 */
	public static byte[] compressDeflate(byte[] data, int length) throws IOException {
		try(ByteArrayOutputStream fin = new ByteArrayOutputStream(); OutputStream out = new DeflaterOutputStream(fin)) {
			out.write(data, 0, length);
			out.close();
			return fin.toByteArray();
		}
	}
	
	/**
	 * Inflates a byte array
	 * @param data The data to decompress
	 * @return The decompressed data
	 * @throws IOException If an I/O error has occurred
	 */
	public static byte[] decompressInflate(byte[] data) throws IOException {
		return decompressInflate(data, data.length);
	}
	
	/**
	 * Inflates a byte array
	 * @param data The data to decompress
	 * @param length The length of the data to read and decompress
	 * @return The decompressed data
	 * @throws IOException If an I/O error has occurred
	 */
	public static byte[] decompressInflate(byte[] data, int length) throws IOException {
		try(ByteArrayOutputStream fin = new ByteArrayOutputStream(); OutputStream out = new InflaterOutputStream(fin)) {
			out.write(data, 0, length);
			out.close();
			return fin.toByteArray();
		}
	}
}