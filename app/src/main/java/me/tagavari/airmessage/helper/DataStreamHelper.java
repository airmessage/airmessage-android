package me.tagavari.airmessage.helper;

import android.os.Build;
import android.os.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Helper class for manipulating streams of data
 */
public class DataStreamHelper {
	public static final int standardBuffer = 8192; //8 kB
	
	/**
	 * Copies from one stream to another
	 * @param inputStream The stream to read from
	 * @param outputStream The stream to write to
	 * @return The total number of bytes transferred
	 */
	public static long copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			//Using Android Q's optimized solution
			return FileUtils.copy(inputStream, outputStream);
		} else {
			//Copying the stream manually
			byte[] buf = new byte[standardBuffer];
			int len;
			long totalLength = 0;
			while((len = inputStream.read(buf)) > 0) {
				outputStream.write(buf, 0, len);
				totalLength += len;
			}
			return totalLength;
		}
	}
}