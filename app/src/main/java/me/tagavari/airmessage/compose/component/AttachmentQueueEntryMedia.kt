package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import me.tagavari.airmessage.messaging.QueuedFile

@Composable
fun AttachmentQueueEntryMedia(
	queuedFile: QueuedFile,
	onClick: () -> Unit,
	fallback: @Composable () -> Unit
) {
	SubcomposeAsyncImage(
		modifier = Modifier
			.fillMaxHeight()
			.clip(RoundedCornerShape(12.dp))
			.clickable(onClick = onClick),
		model = ImageRequest.Builder(LocalContext.current)
			.data(queuedFile.file.either)
			.crossfade(true)
			.build(),
		contentDescription = null,
		error = { fallback() }
	)
}
