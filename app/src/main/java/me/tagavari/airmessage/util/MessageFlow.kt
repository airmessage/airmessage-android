package me.tagavari.airmessage.util

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils

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
	val anchorBottom: Boolean,
	
	//How tinted this element should be, depending on its position on the screen
	val tintRatio: Float
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
	
	val colors: MessageColors
		@Composable
		@ReadOnlyComposable
		get() {
			val colorScheme = MaterialTheme.colorScheme
			
			return if(isOutgoing) {
				val background = ColorUtils.blendARGB(
					colorScheme.secondary.toArgb(),
					colorScheme.primary.toArgb(),
					tintRatio
				).let { Color(it) }
				
				val foreground = ColorUtils.blendARGB(
					colorScheme.onSecondary.toArgb(),
					colorScheme.onPrimary.toArgb(),
					tintRatio
				).let { Color(it) }
				
				MessageColors(background, foreground)
			} else {
				MessageColors(colorScheme.surfaceVariant, colorScheme.onSurfaceVariant)
			}
		}
}

enum class MessageFlowSpacing(val padding: Dp) {
	NONE(0.dp),
	RELATED(2.dp),
	GAP(10.dp)
}

data class MessageColors(
	val background: Color,
	val foreground: Color
)