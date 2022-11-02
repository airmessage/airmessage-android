package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import ezvcard.Ezvcard
import ezvcard.VCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.tagavari.airmessage.compose.R
import me.tagavari.airmessage.util.MessagePartFlow
import java.io.File
import java.io.IOException

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleContact(
	flow: MessagePartFlow,
	file: File,
	onClick: () -> Unit,
	onSetSelected: (Boolean) -> Unit
) {
	val haptic = LocalHapticFeedback.current
	val colors = flow.colors
	
	val vcard by produceState<VCard?>(null, file) {
		withContext(Dispatchers.IO) {
			try {
				Ezvcard.parse(file).first()
			} catch(exception: IOException) {
				exception.printStackTrace()
				null
			}
		}?.let {
			value = it
		}
	}
	
	Surface(
		color = colors.background,
		shape = flow.bubbleShape,
		contentColor = colors.foreground
	) {
		Row(
			modifier = Modifier
				.combinedClickable(
					onClick = {
						if(flow.isSelected) {
							onSetSelected(false)
						} else {
							onClick()
						}
					},
					onLongClick = {
						haptic.performHapticFeedback(HapticFeedbackType.LongPress)
						onSetSelected(!flow.isSelected)
					}
				)
				.width(200.dp)
				.padding(16.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				modifier = Modifier.weight(1F),
				text = vcard?.formattedName?.value ?: stringResource(R.string.part_content_contact),
				style = MaterialTheme.typography.bodyLarge
			)
			
			SubcomposeAsyncImage(
				modifier = Modifier
					.size(40.dp)
					.clip(CircleShape),
				model = file,
				loading = { FallbackImage(color = colors.foreground) },
				error = { FallbackImage(color = colors.foreground) },
				contentDescription = null
			)
		}
	}
}

@Composable
private fun FallbackImage(color: Color) {
	Image(
		painter = painterResource(id = R.drawable.user),
		contentDescription = null,
		colorFilter = ColorFilter.tint(color, BlendMode.Multiply)
	)
}
