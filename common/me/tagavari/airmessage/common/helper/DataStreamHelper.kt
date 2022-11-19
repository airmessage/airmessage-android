package me.tagavari.airmessage.common.helper

import android.os.Build
import android.os.FileUtils
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Helper class for manipulating streams of data
 */
object DataStreamHelper {
	const val standardBuffer = 8192 //8 kB
	
	/**
	 * Copies from one stream to another
	 * @param inputStream The stream to read from
	 * @param outputStream The stream to write to
	 * @return The total number of bytes transferred
	 */
	@JvmStatic
	@Throws(IOException::class)
	fun copyStream(inputStream: InputStream, outputStream: OutputStream): Long {
		return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			//Using Android Q's optimized solution
			FileUtils.copy(inputStream, outputStream)
		} else {
			//Copying the stream manually
			val buf = ByteArray(standardBuffer)
			var len: Int
			var totalLength: Long = 0
			while(inputStream.read(buf).also { len = it } > 0) {
				outputStream.write(buf, 0, len)
				totalLength += len.toLong()
			}
			totalLength
		}
	}
}