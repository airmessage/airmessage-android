package me.tagavari.airmessage.container

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.otaliastudios.transcoder.source.DataSource
import com.otaliastudios.transcoder.source.FilePathDataSource
import me.tagavari.airmessage.helper.AttachmentStorageHelper
import me.tagavari.airmessage.helper.FileHelper
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * A [ReadableBlob] representation of a [LocalFile].
 */
class ReadableBlobLocalFile(
	private val file: LocalFile,
	private val deleteOnInvalidate: Boolean = false
) : ReadableBlob {
	private var isInvalidated = false
	
	override fun openInputStream(): InputStream {
		if(isInvalidated) throw IllegalStateException("Readable blob has been invalidated!")
		return file.file.inputStream()
	}
	
	override suspend fun getData(): ReadableBlob.Data {
		if(isInvalidated) throw IllegalStateException("Readable blob has been invalidated!")
		
		return ReadableBlob.Data(
			name = file.fileName,
			type = file.fileType,
			size = file.fileSize
		)
	}
	
	override fun getOpenIntent(context: Context): Intent {
		if(isInvalidated) throw IllegalStateException("Readable blob has been invalidated!")
		
		val content = FileProvider.getUriForFile(context, AttachmentStorageHelper.getFileAuthority(context), file.file)
		
		return Intent().apply {
			action = Intent.ACTION_VIEW
			setDataAndType(content, file.fileType)
			flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
		}
	}
	
	override fun asTranscoderSource(): DataSource {
		if(isInvalidated) throw IllegalStateException("Readable blob has been invalidated!")
		return FilePathDataSource(file.file.path)
	}
	
	override fun invalidate() {
		if(deleteOnInvalidate && !file.deleteFile()) {
			throw IOException("Failed to delete file")
		}
		
		isInvalidated = true
	}
}