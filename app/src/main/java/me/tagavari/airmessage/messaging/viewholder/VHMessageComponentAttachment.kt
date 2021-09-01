package me.tagavari.airmessage.messaging.viewholder

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import me.tagavari.airmessage.helper.ColorMathHelper.multiplyColorRaw
import me.tagavari.airmessage.helper.MessageShapeHelper.createRoundedMessageDrawable

abstract class VHMessageComponentAttachment(
	itemView: View,
	groupContainer: ViewGroup,
	stickerContainer: ViewGroup,
	tapbackContainer: ViewGroup,
	
	val groupPrompt: ViewGroup,
	val labelPromptSize: TextView,
	val labelPromptType: TextView,
	val iconPrompt: ImageView,
	
	val groupProgress: ViewGroup,
	val progressProgress: ProgressBar,
	val iconProgress: ImageView,
	
	val groupOpen: ViewGroup,
	val labelOpen: TextView,
	
	val groupContentFrame: ViewGroup
) : VHMessageComponent(itemView, groupContainer, stickerContainer, tapbackContainer) {
	/**
	 * Updates the rounded edges of this view
	 * @param context The context to use
	 * @param anchoredTop Whether this component is anchored to a component above
	 * @param anchoredBottom Whether this component is anchored to a component below
	 * @param alignToRight Whether this component is aligned to the right side of the screen (false for the left side)
	 */
	abstract fun updateContentViewEdges(context: Context, anchoredTop: Boolean, anchoredBottom: Boolean, alignToRight: Boolean)
	
	/**
	 * Updates the color values of this attachment's view
	 * @param context The context to use
	 * @param colorTextPrimary The color to use for primary text and foreground elements
	 * @param colorTextSecondary The color to use for secondary foreground elements
	 * @param colorBackground The color to use for background elements
	 */
	abstract fun updateContentViewColoring(context: Context, colorTextPrimary: Int, colorTextSecondary: Int, colorBackground: Int)
	
	override fun updateViewEdges(context: Context, anchoredTop: Boolean, anchoredBottom: Boolean, alignToRight: Boolean) {
		//Creating the drawable
		val drawable: Drawable = createRoundedMessageDrawable(context.resources, anchoredTop, anchoredBottom, alignToRight)
		
		//Assigning the drawable
		groupPrompt.background = drawable
		groupProgress.background = drawable
		groupOpen.background = drawable
		
		//Updating the content view's edges
		updateContentViewEdges(context, anchoredTop, anchoredBottom, alignToRight)
	}
	
	override fun updateViewColoring(context: Context, colorTextPrimary: Int, colorTextSecondary: Int, colorBackground: Int) {
		val cslPrimary = ColorStateList.valueOf(colorTextPrimary)
		val cslSecondary = ColorStateList.valueOf(colorTextSecondary)
		val cslBackground = ColorStateList.valueOf(colorBackground)
		
		//Coloring the views
		groupPrompt.backgroundTintList = cslBackground
		groupPrompt.invalidate()
		labelPromptSize.setTextColor(cslSecondary)
		labelPromptType.setTextColor(cslPrimary)
		iconPrompt.imageTintList = cslPrimary
		
		groupProgress.backgroundTintList = cslBackground
		groupProgress.invalidate()
		progressProgress.progressTintList = cslPrimary
		val cslProgressBG = ColorStateList.valueOf(multiplyColorRaw(colorBackground, 0.9f))
		progressProgress.indeterminateTintList = cslProgressBG
		progressProgress.progressBackgroundTintList = cslProgressBG
		iconProgress.imageTintList = cslPrimary
		
		groupOpen.backgroundTintList = cslBackground
		groupOpen.invalidate()
		labelOpen.setTextColor(cslPrimary)
		labelOpen.compoundDrawableTintList = cslPrimary
		
		//Updating the content view's coloring
		updateContentViewColoring(context, colorTextPrimary, colorTextSecondary, colorBackground)
	}
}