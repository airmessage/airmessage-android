package me.tagavari.airmessage.messaging.viewholder

import android.content.Context
import android.net.Uri
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.MediaStoreSignature
import com.bumptech.glide.signature.ObjectKey
import io.reactivex.rxjava3.disposables.CompositeDisposable
import me.tagavari.airmessage.constants.MIMEConstants
import me.tagavari.airmessage.enums.AttachmentType
import me.tagavari.airmessage.helper.FileHelper.compareMimeTypes
import me.tagavari.airmessage.messaging.FileDisplayMetadata
import me.tagavari.airmessage.util.Union
import java.io.File

/**
 * An attachment tile that holds a visual media file
 * @param imageThumbnail The main thumbnail image view
 * @param imageFlagGIF An icon indicator if this attachment is a GIF
 * @param groupVideo The view group to show if this attachment is a video
 * @param labelVideo The time label if this attachment is a video
 */
class VHAttachmentTileContentMedia(
	val imageThumbnail: ImageView,
	val imageFlagGIF: ImageView,
	val groupVideo: ViewGroup,
	val labelVideo: TextView
) : VHAttachmentTileContent() {
	override val attachmentType = AttachmentType.media
	
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
		Glide.with(context).run {
			//Loading the file
			if(source.isA) load(source.a)
			else load(source.b)
		}.run {
			//Applying the signature
			when {
				draftID != -1L -> signature(ObjectKey(draftID))
				dateModified != -1L -> signature(MediaStoreSignature(fileType, dateModified, 0))
				else -> this
			}
		}.apply(RequestOptions.centerCropTransform())
			.transition(DrawableTransitionOptions.withCrossFade())
			.into(imageThumbnail)
		
		//Setting the image flags
		when {
			compareMimeTypes(fileType, MIMEConstants.mimeTypeGIF) -> {
				imageFlagGIF.visibility = View.VISIBLE
				groupVideo.visibility = View.GONE
			}
			compareMimeTypes(fileType, MIMEConstants.mimeTypeVideo) -> {
				imageFlagGIF.visibility = View.GONE
				groupVideo.visibility = View.VISIBLE
				labelVideo.visibility = View.GONE
			}
			else -> {
				imageFlagGIF.visibility = View.GONE
				groupVideo.visibility = View.GONE
			}
		}
	}
	
	/**
	 * Applies contact metadata to this bound view
	 * @param metadata The metadata to apply
	 */
	fun applyMetadata(metadata: FileDisplayMetadata.Media) {
		labelVideo.text = DateUtils.formatElapsedTime(metadata.mediaDuration / 1000)
		labelVideo.visibility = View.VISIBLE
	}
}