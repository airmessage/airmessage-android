package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.layout.Box
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
	stickers: List<StickerInfo>,
	tapbacks: List<TapbackInfo>,
	content: @Composable () -> Unit
) {
	Box {
		content()
		
		for(sticker in stickers) {
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
}