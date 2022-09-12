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

private val radiusLarge = 20.dp
private val radiusSmall = 5.dp

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
 * A message part's position in the thread in accordance
 * with other nearby messages and the state of the app
 */
data class MessagePartFlow(
	//Whether this message is outgoing
	val isOutgoing: Boolean,
	
	//Whether this message is selected
	val isSelected: Boolean,
	
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
				topStart = radiusLarge,
				topEnd = radiusLarge,
				bottomEnd = if(anchorBottom) radiusLarge else radiusSmall,
				bottomStart = radiusLarge
			)
		} else {
			RoundedCornerShape(
				topStart = radiusLarge,
				topEnd = radiusLarge,
				bottomEnd = radiusLarge,
				bottomStart = if(anchorBottom) radiusLarge else radiusSmall
			)
		}
	
	val colors: MessageColors
		@Composable
		@ReadOnlyComposable
		get() {
			val colorScheme = MaterialTheme.colorScheme
			
			return when {
				isSelected ->
					MessageColors(
						background = colorScheme.tertiaryContainer,
						foreground = colorScheme.onTertiaryContainer
					)
				isOutgoing ->
					MessageColors(
						background = ColorUtils.blendARGB(
							colorScheme.primary.toArgb(),
							colorScheme.secondary.toArgb(),
							tintRatio
						).let { Color(it) },
						foreground = ColorUtils.blendARGB(
							colorScheme.onPrimary.toArgb(),
							colorScheme.onSecondary.toArgb(),
							tintRatio
						).let { Color(it) }
					)
				else ->
					MessageColors(
						background = colorScheme.surfaceVariant,
						foreground = colorScheme.onSurfaceVariant
					)
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