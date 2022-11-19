package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.enums.TapbackType
import me.tagavari.airmessage.helper.LanguageHelper

/**
 * Represents a floating indicator attached to a
 * message to indicate a tapback response
 */
@Composable
fun TapbackIndicator(
	modifier: Modifier = Modifier,
	@TapbackType tapbackCode: Int,
	isOutgoing: Boolean
) {
	Surface(
		modifier = modifier.size(TapbackIndicator.tapbackSize),
		shape = CircleShape,
		tonalElevation = 2.dp,
		shadowElevation = 2.dp,
		color = if(isOutgoing) MaterialTheme.colorScheme.primary
		else MaterialTheme.colorScheme.surface
	) {
		Box(contentAlignment = Alignment.Center) {
			LanguageHelper.getTapbackEmoji(tapbackCode)?.let { tapbackString ->
				Text(
					text = tapbackString
				)
			}
		}
	}
}

object TapbackIndicator {
	val tapbackSize = 28.dp
	val tapbackOffset = 14.dp
}

@Preview
@Composable
private fun PreviewTapbackIndicator() {
	AirMessageAndroidTheme {
		TapbackIndicator(
			tapbackCode = TapbackType.heart,
			isOutgoing = false
		)
	}
}

@Preview
@Composable
private fun PreviewOutgoingTapbackIndicator() {
	AirMessageAndroidTheme {
		TapbackIndicator(
			tapbackCode = TapbackType.laugh,
			isOutgoing = true
		)
	}
}
