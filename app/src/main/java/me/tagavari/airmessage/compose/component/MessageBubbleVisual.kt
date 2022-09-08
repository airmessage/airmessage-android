package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
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
	val isVideo = remember(type) {
		FileHelper.compareMimeTypes(type, MIMEConstants.mimeTypeVideo)
	}
	
	Box {
		val painter = rememberAsyncImagePainter(
			model = ImageRequest.Builder(LocalContext.current)
				.data(file)
				.size(Size.ORIGINAL)
				.crossfade(true)
				.allowHardware(false)
				.build()
		)
		
		val painterState = painter.state
		val isContentLight = remember(painterState) {
			if(painterState is AsyncImagePainter.State.Success) {
				val bitmap = painterState.result.drawable.toBitmap()
				calculateBrightness(bitmap, bitmap.width / 16) > 200
			} else {
				false
			}
		}
		
		Image(
			painter = painter,
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
			contentDescription = null,
			colorFilter = if(flow.isSelected) ColorFilter.tint(flow.colors.background.copy(alpha = 0.8F), blendMode = BlendMode.Multiply) else null
		)
		
		if(isVideo) {
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
