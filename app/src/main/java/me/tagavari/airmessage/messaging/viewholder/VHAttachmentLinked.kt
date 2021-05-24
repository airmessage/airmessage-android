package me.tagavari.airmessage.messaging.viewholder

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.TextView
import me.tagavari.airmessage.R
import me.tagavari.airmessage.helper.LanguageHelper
import me.tagavari.airmessage.util.DisposableViewHolder
import me.tagavari.airmessage.util.Union
import java.io.File

private const val selectedScale = 0.85f

/**
 * A view holder to handle different types of attachment tiles in a linked picker
 */
class VHAttachmentLinked(
	itemView: View,
	private val viewStubSelection: ViewStub?,
	val content: VHAttachmentTileContent
) : DisposableViewHolder(itemView) {
	var groupSelection: ViewGroup? = null
	var labelSelection: TextView? = null
	
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
	 * Gets if this view has a selection indicator available
	 */
	val hasSelectionIndicator: Boolean
		get() = viewStubSelection != null || labelSelection != null
	
	/**
	 * Shows the selection indicator on this view
	 * @param resources The resources to use
	 * @param animate Whether to animate this change
	 * @param index The index to display on the selection indicator
	 */
	fun setSelected(resources: Resources, animate: Boolean, index: Int) {
		//Returning if the view state is already selected
		if(groupSelection.let { it != null && it.visibility == View.VISIBLE }) return
		
		//Inflating the view if it hasn't yet been
		if(groupSelection == null) {
			groupSelection = viewStubSelection!!.inflate() as ViewGroup
			labelSelection = groupSelection!!.findViewById(R.id.label_selectionindex)
		}
		val groupSelection = groupSelection!!
		val labelSelection = labelSelection!!
		
		//Showing the view
		if(animate) {
			val duration = resources.getInteger(android.R.integer.config_shortAnimTime)
			groupSelection.animate().withStartAction { groupSelection.visibility = View.VISIBLE }.alpha(1f).setDuration(duration.toLong()).start()
			ValueAnimator.ofFloat(itemView.scaleX, selectedScale).apply {
				this.duration = duration.toLong()
				addUpdateListener { animation: ValueAnimator ->
					val value = animation.animatedValue as Float
					itemView.scaleX = value
					itemView.scaleY = value
				}
			}.start()
		} else {
			groupSelection.visibility = View.VISIBLE
			groupSelection.alpha = 1F
			itemView.scaleX = selectedScale
			itemView.scaleY = selectedScale
		}
		
		labelSelection.text = LanguageHelper.intToFormattedString(resources, index)
	}
	
	/**
	 * Hides the selection indicator from this view
	 * @param resources The resources to use
	 * @param animate Whether to animate this change
	 */
	fun setDeselected(resources: Resources, animate: Boolean) {
		//Returning if the view state is already deselected
		if(groupSelection.let { it == null || it.visibility == View.GONE }) return
		
		//Inflating the view if it hasn't yet been
		if(groupSelection == null) {
			groupSelection = viewStubSelection!!.inflate() as ViewGroup
			labelSelection = groupSelection!!.findViewById(R.id.label_selectionindex)
		}
		val groupSelection = groupSelection!!
		
		//Hiding the view
		if(animate) {
			val duration = resources.getInteger(android.R.integer.config_shortAnimTime)
			groupSelection.animate().withEndAction { groupSelection.visibility = View.GONE }.alpha(0f).setDuration(duration.toLong()).start()
			ValueAnimator.ofFloat(itemView.scaleX, 1f).apply {
				this.duration = duration.toLong()
				addUpdateListener { animation: ValueAnimator ->
					val value = animation.animatedValue as Float
					itemView.scaleX = value
					itemView.scaleY = value
				}
			}.start()
		} else {
			groupSelection.visibility = View.GONE
			groupSelection.alpha = 1f
			itemView.scaleX = 1f
			itemView.scaleY = 1f
		}
	}
	
	/**
	 * Sets the index number displayed on the selection indicator
	 */
	fun setSelectionIndex(resources: Resources, index: Int) {
		//Inflating the view if it hasn't yet been
		if(groupSelection == null) {
			groupSelection = viewStubSelection!!.inflate() as ViewGroup
			labelSelection = groupSelection!!.findViewById(R.id.label_selectionindex)
		}
		labelSelection!!.text = LanguageHelper.intToFormattedString(resources, index)
	}
}