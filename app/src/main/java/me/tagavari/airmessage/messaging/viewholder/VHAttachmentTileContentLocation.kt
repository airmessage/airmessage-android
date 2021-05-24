package me.tagavari.airmessage.messaging.viewholder

import android.content.Context
import android.net.Uri
import android.widget.TextView
import io.reactivex.rxjava3.disposables.CompositeDisposable
import me.tagavari.airmessage.R
import me.tagavari.airmessage.enums.AttachmentType
import me.tagavari.airmessage.messaging.FileDisplayMetadata.LocationSimple
import me.tagavari.airmessage.util.Union
import java.io.File

/**
 * An attachment tile that holds a location file
 * @param labelName The name of the location
 */
class VHAttachmentTileContentLocation(private val labelName: TextView) : VHAttachmentTileContent() {
	override val attachmentType = AttachmentType.location
	
	override fun bind(
		context: Context,
		compositeDisposable: CompositeDisposable,
		source: Union<File, Uri>,
		fileName: String,
		fileType: String,
		fileSize: Long,
		draftID: Long,
		dateModified: Long
	) {
		//Resetting the view
		labelName.setText(R.string.part_content_location)
	}
	
	/**
	 * Applies contact metadata to this bound view
	 * @param metadata The metadata to apply
	 */
	fun applyMetadata(metadata: LocationSimple) {
		labelName.text = metadata.locationName
	}
}