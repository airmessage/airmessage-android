package me.tagavari.airmessage.messaging.viewholder

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.MaterialShapeDrawable
import me.tagavari.airmessage.R
import me.tagavari.airmessage.helper.MessageShapeHelper
import me.tagavari.airmessage.helper.ResourceHelper

class VHMessagePreviewLink(
	itemView: View,
	val viewBorder: View,
	val imageHeader: ImageView,
	val labelTitle: TextView,
	val labelDescription: TextView,
	val labelAddress: TextView
) : VHMessagePreview(itemView) {
	override fun updateViewEdges(context: Context, anchoredBottom: Boolean, alignToRight: Boolean) {
		//Creating the message shape
		val messageAppearance = MessageShapeHelper.createRoundedMessageAppearanceBottom(context.resources, anchoredBottom, alignToRight)
		
		//Assigning the border shape
		val borderDrawable = MaterialShapeDrawable(messageAppearance)
		borderDrawable.fillColor = ColorStateList.valueOf(0)
		borderDrawable.setStroke(ResourceHelper.dpToPx(1f).toFloat(), context.getColor(R.color.colorDivider))
		viewBorder.background = borderDrawable
		
		//Updating the card shape
		(itemView as MaterialCardView).shapeAppearanceModel = messageAppearance
	}
}