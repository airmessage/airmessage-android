package me.tagavari.airmessage.common.container

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.otaliastudios.transcoder.source.DataSource
import com.otaliastudios.transcoder.source.FilePathDataSource
import me.tagavari.airmessage.common.helper.AttachmentStorageHelper
import me.tagavari.airmessage.common.helper.FileHelper
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * A [ReadableBlob] representation of a Java [File].
 */
class ReadableBlobFile @JvmOverloads constructor(
	private val file: File,
	private val deleteOnInvalidate: Boolean = false
) : ReadableBlob {
	private var isInvalidated = false
	
	override fun openInputStream(): InputStream {
		if(isInvalidated) throw IllegalStateException("Readable blob has been invalidated!")
		return file.inputStream()
	}
	
	override suspend fun getData(): ReadableBlob.Data {
		if(isInvalidated) throw IllegalStateException("Readable blob has been invalidated!")
		
		return ReadableBlob.Data(
			name = file.name,
			type = FileHelper.getMimeType(file),
			size = file.length()
		)
	}
	
	override fun getOpenIntent(context: Context): Intent {
		if(isInvalidated) throw IllegalStateException("Readable blob has been invalidated!")
		
		val content = FileProvider.getUriForFile(context, AttachmentStorageHelper.getFileAuthority(context), file)
		
		return Intent().apply {
			action = Intent.ACTION_VIEW
			setDataAndType(content, FileHelper.getMimeType(file))
			flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
		}
	}
	
	override fun asTranscoderSource(): DataSource {
		if(isInvalidated) throw IllegalStateException("Readable blob has been invalidated!")
		return FilePathDataSource(file.path)
	}
	
	override fun invalidate() {
		if(deleteOnInvalidate && !file.delete()) {
			throw IOException("Failed to delete file")
		}
		
		isInvalidated = true
	}
}