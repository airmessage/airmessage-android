package me.tagavari.airmessage.container

import android.content.Context
import android.content.Intent
import com.otaliastudios.transcoder.source.DataSource
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * A [ReadableBlob] representation of a [ByteArray]
 */
class ReadableBlobByteArray(
	byteArray: ByteArray,
	private val name: String? = null,
	private val type: String? = null
) : ReadableBlob {
	private var byteArray: ByteArray?
	
	init {
		this.byteArray = byteArray
	}
	
	override fun openInputStream(): InputStream {
		val byteArray = byteArray
			?: throw IllegalStateException("Readable blob has been invalidated!")
		return ByteArrayInputStream(byteArray)
	}
	
	override suspend fun getData(): ReadableBlob.Data {
		val byteArray = byteArray
			?: throw IllegalStateException("Readable blob has been invalidated!")
		
		return ReadableBlob.Data(
			name = name,
			type = type,
			size = byteArray.size.toLong()
		)
	}
	
	override fun getOpenIntent(context: Context): Intent? {
		return null
	}
	
	override fun asTranscoderSource(): DataSource? {
		//Not supported for in-memory sources
		return null
	}
	
	override fun invalidate() {
		//Free byte array
		byteArray = null
	}
}