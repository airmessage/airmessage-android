package me.tagavari.airmessage.messaging.viewholder

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import me.tagavari.airmessage.enums.MessageComponentType
import me.tagavari.airmessage.helper.MessageShapeHelper.createRoundedMessageDrawable

class VHMessageComponentContact(
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
	val iconProfile: ImageView,
	val iconPlaceholder: ImageView,
	val labelName: TextView
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
	override val componentType = MessageComponentType.attachmentContact
	
	override fun updateContentViewEdges(context: Context, anchoredTop: Boolean, anchoredBottom: Boolean, alignToRight: Boolean) {
		groupContent.background = createRoundedMessageDrawable(context.resources, anchoredTop, anchoredBottom, alignToRight)
	}
	
	override fun updateContentViewColoring(context: Context, colorTextPrimary: Int, colorTextSecondary: Int, colorBackground: Int) {
		groupContent.backgroundTintList = ColorStateList.valueOf(colorBackground)
		labelName.setTextColor(ColorStateList.valueOf(colorTextPrimary))
	}
}