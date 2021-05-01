package me.tagavari.airmessage.messaging.viewholder

import android.content.Context
import android.content.res.ColorStateList
import android.net.Uri
import android.text.format.Formatter
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import io.reactivex.rxjava3.disposables.CompositeDisposable
import me.tagavari.airmessage.R
import me.tagavari.airmessage.enums.AttachmentType
import me.tagavari.airmessage.helper.ThemeHelper.isNightMode
import me.tagavari.airmessage.util.Union
import java.io.File

/**
 * An attachment tile that holds a generic document file
 * @param itemView The tile view group
 * @param documentName The name of the document
 * @param documentIcon An icon to represent the document
 * @param documentSize The file size of the document
 */
class VHAttachmentTileContentDocument(
	val itemView: View,
	val documentName: TextView,
	val documentIcon: ImageView,
	val documentSize: TextView
) : VHAttachmentTileContent() {
	override val attachmentType = AttachmentType.document
	
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
		//Getting the type-based details
		val iconResource: Int
		val viewColorResBG: Int
		val viewColorResFG: Int
		when(fileType) {
			"application/zip",
			"application/x-tar",
			"application/x-rar-compressed",
			"application/x-7z-compressed",
			"application/x-bzip",
			"application/x-bzip2" -> {
				iconResource = R.drawable.file_zip
				viewColorResBG = R.color.tile_brown_bg
				viewColorResFG = R.color.tile_brown_fg
			}
			"application/pdf" -> {
				iconResource = R.drawable.file_pdf
				viewColorResBG = R.color.tile_red_bg
				viewColorResFG = R.color.tile_red_fg
			}
			"text/xml",
			"application/xml",
			"text/html" -> {
				iconResource = R.drawable.file_xml
				viewColorResBG = R.color.tile_orange_bg
				viewColorResFG = R.color.tile_orange_fg
			}
			"text/vcard" -> {
				iconResource = R.drawable.file_user
				viewColorResBG = R.color.tile_cyan_bg
				viewColorResFG = R.color.tile_cyan_fg
			}
			"application/msword",
			"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
			"application/vnd.openxmlformats-officedocument.wordprocessingml.template",
			"application/vnd.ms-word.document.macroEnabled.12",
			"application/vnd.ms-word.template.macroEnabled.12" -> {
				iconResource = R.drawable.file_msword
				viewColorResBG = R.color.tile_blue_bg
				viewColorResFG = R.color.tile_blue_fg
			}
			"application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
			"application/vnd.openxmlformats-officedocument.spreadsheetml.template",
			"application/vnd.ms-excel.sheet.macroEnabled.12",
			"application/vnd.ms-excel.sheet.binary.macroEnabled.12",
			"application/vnd.ms-excel.template.macroEnabled.12",
			"application/vnd.ms-excel.addin.macroEnabled.12" -> {
				iconResource = R.drawable.file_msexcel
				viewColorResBG = R.color.tile_green_bg
				viewColorResFG = R.color.tile_green_fg
			}
			"application/vnd.ms-powerpoint",
			"application/vnd.openxmlformats-officedocument.presentationml.presentation",
			"application/vnd.openxmlformats-officedocument.presentationml.template",
			"application/vnd.openxmlformats-officedocument.presentationml.slideshow",
			"application/vnd.ms-powerpoint.addin.macroEnabled.12",
			"application/vnd.ms-powerpoint.presentation.macroEnabled.12",
			"application/vnd.ms-powerpoint.template.macroEnabled.12",
			"application/vnd.ms-powerpoint.slideshow.macroEnabled.12" -> {
				iconResource = R.drawable.file_mspowerpoint
				viewColorResBG = R.color.tile_yellow_bg
				viewColorResFG = R.color.tile_yellow_fg
			}
			else -> if(fileType.split("/").toTypedArray()[0].startsWith("text")) {
				iconResource = R.drawable.file_document
				viewColorResBG = R.color.tile_indigo_bg
				viewColorResFG = R.color.tile_indigo_fg
			} else {
				iconResource = R.drawable.file
				viewColorResBG = R.color.tile_grey_bg
				viewColorResFG = R.color.tile_grey_fg
			}
		}
		
		//Resolving the color resources
		var viewColorBG = context.resources.getColor(viewColorResBG, null)
		var viewColorFG = context.resources.getColor(viewColorResFG, null)
		if(isNightMode(context.resources)) {
			//Invert colors
			viewColorBG = viewColorFG.also { viewColorFG = viewColorBG }
		}
		
		//Filling in the view data
		documentName.text = fileName
		documentName.setTextColor(viewColorResFG)
		
		documentIcon.setImageResource(iconResource)
		documentIcon.imageTintList = ColorStateList.valueOf(viewColorResFG)
		
		documentSize.text = Formatter.formatFileSize(context, fileSize)
		documentSize.setTextColor(viewColorResFG)
		
		itemView.backgroundTintList = ColorStateList.valueOf(viewColorResBG)
	}
}