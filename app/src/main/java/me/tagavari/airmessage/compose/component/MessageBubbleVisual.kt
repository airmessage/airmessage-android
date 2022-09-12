package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.imageLoader
import coil.memory.MemoryCache
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Dimension
import coil.size.Scale
import coil.size.Size
import me.tagavari.airmessage.R
import me.tagavari.airmessage.constants.MIMEConstants
import me.tagavari.airmessage.helper.ColorMathHelper.calculateBrightness
import me.tagavari.airmessage.helper.FileHelper
import me.tagavari.airmessage.util.MessagePartFlow
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleVisual(
	flow: MessagePartFlow,
	file: File,
	type: String?,
	onClick: () -> Unit,
	onSetSelected: (Boolean) -> Unit
) {
	Box {
		val isVideo = remember(type) {
			FileHelper.compareMimeTypes(type, MIMEConstants.mimeTypeVideo)
		}
		
		val context = LocalContext.current
		
		AsyncImage(
			modifier = Modifier
				.width(200.dp)
				.clip(flow.bubbleShape)
				.combinedClickable(
					onClick = {
						if(flow.isSelected) {
							onSetSelected(false)
						} else {
							onClick()
						}
					},
					onLongClick = {
						onSetSelected(!flow.isSelected)
					}
				),
			model = ImageRequest.Builder(context)
				.data(file)
				.size(Size(
					width = with(LocalDensity.current) { 200.dp.roundToPx() },
					height = Dimension.Undefined
				))
				.crossfade(true)
				.build(),
			contentDescription = null,
			colorFilter = if(flow.isSelected) ColorFilter.tint(flow.colors.background.copy(alpha = 0.8F), blendMode = BlendMode.Multiply) else null,
			contentScale = ContentScale.FillWidth
		)
		
		if(isVideo) {
			val isContentLight by produceState(initialValue = false) {
				//Create the image request
				val request = ImageRequest.Builder(context)
					.data(file)
					// We scale the image to cover 16px x 16px (i.e. min dimension == 16px)
					.size(16).scale(Scale.FILL)
					// Disable hardware bitmaps, since Palette uses Bitmap.getPixels()
					.allowHardware(false)
					// Set a custom memory cache key to avoid overwriting the displayed image in the cache
					.memoryCacheKey(MemoryCache.Key(file.path, mapOf("palette" to "true")))
					.build()
				
				//Load the bitmap
				val bitmap = when(val result = context.imageLoader.execute(request)) {
					is SuccessResult -> result.drawable.toBitmap()
					else -> null
				}
				
				//Calculate the brightness value
				value = bitmap?.let { calculateBrightness(it, 1) > 200 } ?: false
			}
			
			Icon(
				modifier = Modifier
					.size(48.dp)
					.align(Alignment.Center),
				painter = painterResource(id = R.drawable.play_circle_rounded),
				tint = if(isContentLight) Color(0xFF212121) else Color(0xFFFFFFFF),
				contentDescription = null
			)
		}
	}
}
