package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
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
	Box{
		Column {
			content()
		}
	}
}