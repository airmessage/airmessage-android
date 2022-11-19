package me.tagavari.airmessage.compose.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import me.tagavari.airmessage.messaging.StickerInfo
import me.tagavari.airmessage.messaging.TapbackInfo

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
		modifier = if(tapbacks.isEmpty()) Modifier else Modifier.padding(top = TapbackIndicator.tapbackOffset)
	) {
		content()
		
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
			AnimatedVisibility(
				visible = !hideStickers,
				enter = fadeIn(),
				exit = fadeOut()
			) {
				for(sticker in stickers) {
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
		
		//Display the last 3 tapbacks
		tapbacks.takeLast(tapbackShowCount).forEachIndexed { index, tapback ->
			//Show the most recent (last) tapback on top,
			//apply an offset of 20% for tapbacks on the bottom
			TapbackIndicator(
				modifier = Modifier
					.align(if(isOutgoing) Alignment.TopStart else Alignment.TopEnd)
					.offset(
						x = (TapbackIndicator.tapbackOffset * (1F + (tapbackShowCount - 1 - index) * 0.2F) * ((if(isOutgoing) -1 else 1))),
						y = -TapbackIndicator.tapbackOffset
					),
				tapbackCode = tapback.code,
				isOutgoing = tapback.sender == null
			)
		}
	}
}

//Show up to 3 tapback indicators on a message
private const val tapbackShowCount = 3
