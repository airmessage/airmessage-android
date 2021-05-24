package me.tagavari.airmessage.messaging.viewholder

import android.content.Context
import android.net.Uri
import io.reactivex.rxjava3.disposables.CompositeDisposable
import me.tagavari.airmessage.enums.AttachmentType
import me.tagavari.airmessage.util.Union
import java.io.File

abstract class VHAttachmentTileContent {
	/**
	 * Binds a file to this view
	 * @param context The context to use
	 * @param compositeDisposable A composite disposable linked to the lifecycle of this view
	 * @param source The file, either a [File] or [Uri]
	 * @param fileName The name of the file
	 * @param fileType The MIME type of the file
	 * @param fileSize The size of the file
	 * @param draftID The file's draft ID (or -1 if unavailable)
	 * @param dateModified The file's media provider modification date (or -1 if unavailable)
	 */
	abstract fun bind(
		context: Context,
		compositeDisposable: CompositeDisposable,
		source: Union<File, Uri>,
		fileName: String,
		fileType: String,
		fileSize: Long,
		draftID: Long,
		dateModified: Long
	)
	
	/**
	 * Gets the attachment type that this view holder handles
	 */
	@get:AttachmentType
	abstract val attachmentType: Int
}