package me.tagavari.airmessage.messaging.viewholder

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import me.tagavari.airmessage.enums.MessageComponentType

class VHMessageComponentDocument(
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
	groupContentFrame: ViewGroup
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
	override val componentType = MessageComponentType.attachmentDocument
	
	override fun updateContentViewEdges(context: Context, anchoredTop: Boolean, anchoredBottom: Boolean, alignToRight: Boolean) = Unit
	
	override fun updateContentViewColoring(context: Context, colorTextPrimary: Int, colorTextSecondary: Int, colorBackground: Int) = Unit
}