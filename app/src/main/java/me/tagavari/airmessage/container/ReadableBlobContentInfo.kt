package me.tagavari.airmessage.container

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.core.view.ContentInfoCompat
import com.otaliastudios.transcoder.source.DataSource
import com.otaliastudios.transcoder.source.UriDataSource
import me.tagavari.airmessage.MainApplication
import java.io.IOException
import java.io.InputStream

/**
 * A [ReadableBlob] representation of a [ContentInfoCompat] URI.
 *
 * URI permissions are tied to the lifecycle of [ContentInfoCompat],
 * so this object will hold a strong reference to it.
 */
class ReadableBlobContentInfo(
	contentInfo: ContentInfoCompat,
	private val index: Int
) : ReadableBlob {
	private var contentInfo: ContentInfoCompat?
	
	init {
		this.contentInfo = contentInfo
	}
	
	private val context: Context
		get() = MainApplication.getInstance()
	
	private val ContentInfoCompat.blobUri: Uri
		get() = clip.getItemAt(index).uri
	
	override fun openInputStream(): InputStream {
		val contentInfo = contentInfo
			?: throw IllegalStateException("Readable blob has been invalidated!")
		
		return context.contentResolver.openInputStream(contentInfo.blobUri)
			?: throw IOException("Content resolver crashed")
	}
	
	override suspend fun getData(): ReadableBlob.Data {
		val contentInfo = contentInfo ?: throw IllegalStateException("Readable blob has been invalidated!")
		
		//Get the MIME type from the content info clip, then the content resolver
		val mimeType = contentInfo.clip.description.let { description ->
			if(index < description.mimeTypeCount) {
				description.getMimeType(index)
			} else {
				context.contentResolver.getType(contentInfo.blobUri)
			}
		}
		
		//Query the URI for file size and file name
		val (fileName: String?, fileSize: Long?) = context.contentResolver.query(contentInfo.blobUri, null, null, null, null, null).use { cursor ->
			if(cursor == null || !cursor.moveToFirst()) return@use Pair(null, null)
			
			Pair(
				first = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).let { columnName ->
					cursor.getStringOrNull(columnName)
				},
				second = cursor.getColumnIndex(OpenableColumns.SIZE).let { columnName ->
					cursor.getLongOrNull(columnName)
				}
			)
		}
		
		return ReadableBlob.Data(
			name = fileName,
			type = mimeType,
			size = fileSize
		)
	}
	
	override fun getOpenIntent(context: Context): Intent {
		val contentInfo = contentInfo ?: throw IllegalStateException("Readable blob has been invalidated!")
		return Intent(Intent.ACTION_VIEW, contentInfo.blobUri)
	}
	
	override fun asTranscoderSource(): DataSource {
		val contentInfo = contentInfo ?: throw IllegalStateException("Readable blob has been invalidated!")
		return UriDataSource(context, contentInfo.blobUri)
	}
	
	override fun invalidate() {
		//Release the content info to release URI permissions
		contentInfo = null
	}
}