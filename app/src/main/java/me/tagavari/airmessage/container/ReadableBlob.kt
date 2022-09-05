package me.tagavari.airmessage.container

import android.content.Context
import android.content.Intent
import com.otaliastudios.transcoder.source.DataSource
import java.io.InputStream

/**
 * A streamable blob with associated metadata
 */
interface ReadableBlob {
	/**
	 * Opens an [InputStream] to this blob
	 */
	fun openInputStream(): InputStream
	
	/**
	 * Gets [Data] metadata about this blob
	 */
	suspend fun getData(): Data
	
	/**
	 * An Android [Intent] that can be used to open this file
	 */
	fun getOpenIntent(context: Context): Intent?
	
	/**
	 * Gets this blob as a [DataSource], or returns NULL
	 * if the blob cannot be properly converted
	 */
	fun asTranscoderSource(): DataSource?
	
	/**
	 * Invalidates the data behind this blob
	 */
	fun invalidate()
	
	data class Data(
		//The file name of this blob
		val name: String?,
		//The MIME type of this blob
		val type: String?,
		//The size in bytes of this blob
		val size: Long?
	)
}
