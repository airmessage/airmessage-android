package me.tagavari.airmessage.messaging.viewholder

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.MaterialShapeDrawable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.SingleSubject
import me.tagavari.airmessage.R
import me.tagavari.airmessage.enums.MessageComponentType
import me.tagavari.airmessage.helper.MessageShapeHelper.createRoundedMessageAppearance
import me.tagavari.airmessage.helper.MessageShapeHelper.createStandardRadiusArrayTop
import me.tagavari.airmessage.view.RoundedFrameLayout

class VHMessageComponentLocation(
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
	val groupContent: MaterialCardView,
	val viewBorder: View,
	val mapContainer: RoundedFrameLayout,
	val mapView: MapView,
	val labelTitle: TextView,
	val labelAddress: TextView
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
	override val componentType = MessageComponentType.attachmentLocation
	
	val googleMap: SingleSubject<GoogleMap> = SingleSubject.create()
	
	init {
		//Initializing the map
		mapView.onCreate(null)
		mapView.getMapAsync(googleMap::onSuccess)
	}
	
	override fun updateContentViewEdges(context: Context, anchoredTop: Boolean, anchoredBottom: Boolean, alignToRight: Boolean) {
		//Creating the message shape
		val messageAppearance =
			createRoundedMessageAppearance(context.resources, anchoredTop, anchoredBottom, alignToRight)
		
		//Assigning the border shape
		val borderDrawable = MaterialShapeDrawable(messageAppearance)
		borderDrawable.fillColor = ColorStateList.valueOf(0)
		borderDrawable.setStroke(2f, context.getColor(R.color.colorDivider))
		viewBorder.background = borderDrawable
		
		//Assigning the map shape
		mapContainer.setRadiiRaw(createStandardRadiusArrayTop(context.resources, anchoredTop, alignToRight))
		
		//Assigning the card shape
		groupContent.shapeAppearanceModel = messageAppearance
	}
	
	override fun updateContentViewColoring(context: Context, colorTextPrimary: Int, colorTextSecondary: Int, colorBackground: Int) = Unit
}