package me.tagavari.airmessage.util

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

private val radiusUnanchored = 20.dp
private val radiusAnchored = 5.dp

/**
 * A message's position in the thread in accordance with other nearby messages
 */
data class MessageFlow(
	//Whether this message should be anchored to the message above
	val anchorTop: Boolean,
	
	//Whether this message should be anchored to the message below
	val anchorBottom: Boolean
)

/**
 * A message's position in the thread in accordance with other nearby messages
 */
data class MessagePartFlow(
	//Whether this message is outgoing
	val isOutgoing: Boolean,
	
	//Whether this message should be anchored to the message above
	val anchorTop: Boolean,
	
	//Whether this message should be anchored to the message below
	val anchorBottom: Boolean
) {
	val bubbleShape: Shape
		get() = if(isOutgoing) {
			RoundedCornerShape(
				radiusUnanchored,
				if(anchorTop) radiusAnchored else radiusUnanchored,
				if(anchorBottom) radiusAnchored else radiusUnanchored,
				radiusUnanchored
			)
		} else {
			RoundedCornerShape(
				if(anchorTop) radiusAnchored else radiusUnanchored,
				radiusUnanchored,
				radiusUnanchored,
				if(anchorBottom) radiusAnchored else radiusUnanchored
			)
		}
}