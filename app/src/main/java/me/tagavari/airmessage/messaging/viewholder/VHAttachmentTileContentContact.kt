package me.tagavari.airmessage.messaging.viewholder

import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import io.reactivex.rxjava3.disposables.CompositeDisposable
import me.tagavari.airmessage.R
import me.tagavari.airmessage.enums.AttachmentType
import me.tagavari.airmessage.messaging.FileDisplayMetadata.Contact
import me.tagavari.airmessage.util.Union
import java.io.File

/**
 * An attachment tile that holds a contact file
 * @param iconPlaceholder An icon to use as a placeholder when no profile picture is present
 * @param iconProfile The contact's profile picture
 * @param labelName The contact's name
 */
class VHAttachmentTileContentContact(
	val iconPlaceholder: ImageView,
	val iconProfile: ImageView,
	val labelName: TextView
) : VHAttachmentTileContent() {
	override val attachmentType = AttachmentType.contact
	
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
		labelName.setText(R.string.part_content_contact)
		iconPlaceholder.visibility = View.VISIBLE
		iconProfile.visibility = View.GONE
	}
	
	/**
	 * Applies contact metadata to this bound view
	 * @param metadata The metadata to apply
	 */
	fun applyMetadata(metadata: Contact) {
		//Setting the contact name label
		if(metadata.contactName != null) labelName.text = metadata.contactName
		else labelName.setText(R.string.part_content_contact)
		
		//Setting the contact's picture
		if(metadata.contactIcon == null) {
			iconPlaceholder.visibility = View.VISIBLE
			iconProfile.visibility = View.GONE
		} else {
			iconPlaceholder.visibility = View.GONE
			iconProfile.visibility = View.VISIBLE
			iconProfile.setImageBitmap(metadata.contactIcon)
		}
	}
}