package me.tagavari.airmessage.messaging.viewholder

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView

abstract class VHMessagePreview(itemView: View) : RecyclerView.ViewHolder(itemView) {
	/**
	 * Updates the rounded corners of this component
	 * @param context The context to use
	 * @param anchoredBottom Whether this view is anchored to a message below
	 * @param alignToRight Whether this view is aligned to the right edge of the screen
	 */
	abstract fun updateViewEdges(context: Context, anchoredBottom: Boolean, alignToRight: Boolean)
}