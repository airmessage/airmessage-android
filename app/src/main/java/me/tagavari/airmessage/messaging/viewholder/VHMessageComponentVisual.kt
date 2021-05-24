package me.tagavari.airmessage.messaging.viewholder

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.imageview.ShapeableImageView
import me.tagavari.airmessage.enums.MessageComponentType
import me.tagavari.airmessage.helper.MessageShapeHelper
import me.tagavari.airmessage.view.InvisibleInkView

//Image or video
class VHMessageComponentVisual(
	itemView: View,
	groupContainer: ViewGroup,
	stickerContainer: ViewGroup,
	tapbackContainer: ViewGroup,
	groupPrompt: ViewGroup,
	labelPromptSize: TextView,
	labelPromptType: TextView,
	iconPrompt: ImageView,
	groupProgress: ViewGroup,
	progressProgress: ProgressBar,
	iconProgress: ImageView,
	groupOpen: ViewGroup,
	labelOpen: TextView,
	groupContentFrame: ViewGroup,
	val groupContent: ViewGroup,
	val imageView: ShapeableImageView,
	val inkView: InvisibleInkView,
	val playIndicator: ImageView
) : VHMessageComponentAttachment(
	itemView,
	groupContainer,
	stickerContainer,
	tapbackContainer,
	groupPrompt,
	labelPromptSize,
	labelPromptType,
	iconPrompt,
	groupProgress,
	progressProgress,
	iconProgress,
	groupOpen,
	labelOpen,
	groupContentFrame
) {
	override val componentType = MessageComponentType.attachmentVisual
	
	override fun updateContentViewEdges(context: Context, anchoredTop: Boolean, anchoredBottom: Boolean, alignToRight: Boolean) {
		imageView.shapeAppearanceModel = MessageShapeHelper.createRoundedMessageAppearance(context.resources, anchoredTop, anchoredBottom, alignToRight)
		inkView.setRadii(MessageShapeHelper.createStandardRadiusArray(context.resources, anchoredTop, anchoredBottom, alignToRight))
	}
	
	override fun updateContentViewColoring(context: Context, colorTextPrimary: Int, colorTextSecondary: Int, colorBackground: Int) = Unit
}