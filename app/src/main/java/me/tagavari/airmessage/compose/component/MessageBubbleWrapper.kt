package me.tagavari.airmessage.compose.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import me.tagavari.airmessage.enums.TapbackType
import me.tagavari.airmessage.messaging.StickerInfo
import me.tagavari.airmessage.messaging.TapbackInfo

private val tapbackSize = 28.dp
private val tapbackOffset = 14.dp

/**
 * A wrapper around a bubble component that
 * layers tapbacks and stickers
 */
@Composable
fun MessageBubbleWrapper(
	isOutgoing: Boolean,
	stickers: List<StickerInfo>,
	tapbacks: List<TapbackInfo>,
	hideStickers: Boolean,
	content: @Composable () -> Unit
) {
	Box(
		modifier = if(tapbacks.isEmpty()) Modifier else Modifier.padding(top = tapbackOffset)
	) {
		content()
		
		AnimatedVisibility(
			visible = !hideStickers,
			enter = fadeIn(),
			exit = fadeOut()
		) {
			for(sticker in stickers) {
				//Create a box with a size of 0
				Box(
					modifier = Modifier
						.align(Alignment.Center)
						.layout { measurable, constraints ->
							val placeable = measurable.measure(constraints)
							layout(0, 0) {
								placeable.place(-placeable.width / 2, -placeable.height / 2)
							}
						}
				) {
					AsyncImage(
						modifier = Modifier
							.sizeIn(
								maxWidth = 128.dp,
								maxHeight = 128.dp
							),
						model = sticker.file,
						contentDescription = null
					)
				}
			}
		}
		
		tapbacks.firstOrNull()?.let { tapback ->
			Surface(
				modifier = Modifier
					.size(tapbackSize)
					.align(if(isOutgoing) Alignment.TopStart else Alignment.TopEnd)
					.offset(
						x = tapbackOffset * ((if(isOutgoing) -1 else 1)),
						y = -tapbackOffset
					),
				shape = CircleShape,
				tonalElevation = 2.dp,
				shadowElevation = 2.dp
			) {
				Box(contentAlignment = Alignment.Center) {
					Text(
						text = when(tapback.code) {
							TapbackType.heart -> "\u2764"
							TapbackType.like -> "\uD83D\uDC4D"
							TapbackType.dislike -> "\uD83D\uDC4E"
							TapbackType.laugh -> "\uD83D\uDE02"
							TapbackType.exclamation -> "\u203C\uFE0F"
							TapbackType.question -> "\u2753"
							else -> ""
						}
					)
				}
			}
		}
	}
}