package me.tagavari.airmessage.messaging.viewholder

import android.content.Context
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import me.tagavari.airmessage.util.DisposableViewHolder
import me.tagavari.airmessage.util.Union
import java.io.File

/**
 * A view holder to handle different types of attachment tiles in the queue
 * @param itemView The base item view
 * @param buttonRemove The button to remove this item from the queue
 * @param contentContainer The container for the content of this queued item
 * @param content The primary content of this attachment tile
 */
class VHAttachmentQueued(
	itemView: View,
	val buttonRemove: ImageButton,
	val contentContainer: ViewGroup,
	val content: VHAttachmentTileContent
) : DisposableViewHolder(itemView) {
	/**
	 * Binds a file to this view
	 * @param context The context to use
	 * @param source The file, either a [File] or [Uri]
	 * @param fileName The name of the file
	 * @param fileType The MIME type of the file
	 * @param fileSize The size of the file
	 * @param draftID The file's draft ID (or -1 if unavailable)
	 * @param dateModified The file's media provider modification date (or -1 if unavailable)
	 */
	fun bind(
		context: Context,
		source: Union<File, Uri>,
		fileName: String,
		fileType: String,
		fileSize: Long,
		draftID: Long,
		dateModified: Long
	) {
		content.bind(context, compositeDisposable, source, fileName, fileType, fileSize, draftID, dateModified)
	}
	
	/**
	 * Updates the view to represent whether this attachment has been successfully processed
	 * @param isProcessed Whether this attachment is processed
	 * @param animate Whether to animate the change
	 */
	fun setAppearanceState(isProcessed: Boolean, animate: Boolean) {
		if(animate) {
			contentContainer.animate().alpha(if(isProcessed) 1F else 0.5F)
		} else {
			contentContainer.alpha = if(isProcessed) 1F else 0.5F
		}
	}
}