package me.tagavari.airmessage.container

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.core.view.ContentInfoCompat
import com.otaliastudios.transcoder.source.DataSource
import com.otaliastudios.transcoder.source.UriDataSource
import me.tagavari.airmessage.MainApplication
import java.io.IOException
import java.util.*

/**
 * A [ReadableBlob] representation of an Android [Uri].
 */
class ReadableBlobUri(
	private val uri: Uri
) : ReadableBlob {
	private val context: Context
		get() = MainApplication.instance
	
	override fun openInputStream() =
		context.contentResolver.openInputStream(uri)
			?: throw IOException("Content resolver crashed")
	
	override suspend fun getData(): ReadableBlob.Data {
		//Resolve the mime type
		val mimeType = if(uri.scheme == ContentResolver.SCHEME_CONTENT) {
			context.contentResolver.getType(uri)
		} else {
			val fileExtension: String = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
			MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.lowercase(Locale.getDefault()))
		}
		
		//Query the URI for file size and file name
		val (fileName: String?, fileSize: Long?) = context.contentResolver.query(uri, null, null, null, null, null).use { cursor ->
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
		return Intent(Intent.ACTION_VIEW, uri)
	}
	
	override fun asTranscoderSource() = UriDataSource(context, uri)
	
	override fun invalidate() = Unit
}