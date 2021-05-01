package me.tagavari.airmessage.messaging.viewholder

import android.content.Context
import android.view.View
import android.view.ViewGroup
import me.tagavari.airmessage.enums.MessageComponentType
import me.tagavari.airmessage.util.DisposableViewHolder

/**
 * A view holder for a user-generated message component that can be displayed in the conversations list
 * @param itemView The base item view
 * @param groupContainer The container of the entire group
 * @param stickerContainer The container for sticker items
 * @param tapbackContainer The container for tapback items
 */
abstract class VHMessageComponent(
	itemView: View,
	val groupContainer: ViewGroup,
	val stickerContainer: ViewGroup,
	val tapbackContainer: ViewGroup
) : DisposableViewHolder(itemView) {
	@get:MessageComponentType
	abstract val componentType: Int
	
	/**
	 * Updates the rounded corners of this component
	 * @param context The context to use
	 * @param anchoredTop Whether this view is anchored to a message above
	 * @param anchoredBottom Whether this view is anchored to a message below
	 * @param alignToRight Whether this view is aligned to the right edge of the screen
	 */
	abstract fun updateViewEdges(context: Context, anchoredTop: Boolean, anchoredBottom: Boolean, alignToRight: Boolean)
	
	/**
	 * Updates the colors of the view
	 * @param context The context to use
	 * @param colorTextPrimary The color to use for primary text and foreground elements
	 * @param colorTextSecondary The color to use for secondary foreground elements
	 * @param colorBackground The color to use for background elements
	 */
	abstract fun updateViewColoring(context: Context, colorTextPrimary: Int, colorTextSecondary: Int, colorBackground: Int)
}