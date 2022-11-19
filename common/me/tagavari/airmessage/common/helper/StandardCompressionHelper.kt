package me.tagavari.airmessage.common.helper

import me.tagavari.airmessage.common.helper.DataStreamHelper.copyStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.InflaterInputStream

/**
 * A set of utilities for compressing and decompressing arbitrary data
 */
object StandardCompressionHelper {
	/**
	 * GZIP-compresses a byte array
	 * @param data The data to compress
	 * @param length The length of the data to read and compress
	 * @return The compressed data
	 * @throws IOException If an I/O error has occurred
	 */
	@JvmStatic
	@JvmOverloads
	@Throws(IOException::class)
	fun compressGZIP(data: ByteArray, length: Int = data.size): ByteArray {
		ByteArrayOutputStream().use { fin ->
			GZIPOutputStream(fin).use { out ->
				out.write(data, 0, length)
				out.close()
				return fin.toByteArray()
			}
		}
	}

	/**
	 * Deflates a byte array
	 * @param data The data to compress
	 * @param length The length of the data to read and compress
	 * @return The compressed data
	 * @throws IOException If an I/O error has occurred
	 */
	@JvmStatic
	@JvmOverloads
	@Throws(IOException::class)
	fun decompressGZIP(data: ByteArray, length: Int = data.size): ByteArray {
		GZIPInputStream(data.inputStream(0, length)).use { inputStream ->
			ByteArrayOutputStream().use { out ->
				copyStream(inputStream, out)
				return out.toByteArray()
			}
		}
	}

	/**
	 * Deflates a byte array
	 * @param data The data to compress
	 * @return The compressed data
	 * @throws IOException If an I/O error has occurred
	 */
	@JvmStatic
	@JvmOverloads
	@Throws(IOException::class)
	fun compressDeflate(data: ByteArray, length: Int = data.size): ByteArray {
		ByteArrayOutputStream().use { fin ->
			DeflaterOutputStream(fin).use { out ->
				out.write(data, 0, length)
				out.close()
				return fin.toByteArray()
			}
		}
	}

	/**
	 * Inflates a byte array
	 * @param data The data to decompress
	 * @param length The length of the data to read and decompress
	 * @return The decompressed data
	 * @throws IOException If an I/O error has occurred
	 */
	@JvmStatic
	@JvmOverloads
	@Throws(IOException::class)
	fun decompressDeflate(data: ByteArray, length: Int = data.size): ByteArray {
		InflaterInputStream(data.inputStream(0, length)).use { inputStream ->
			ByteArrayOutputStream().use { out ->
				copyStream(inputStream, out)
				return out.toByteArray()
			}
		}
	}
}