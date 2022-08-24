package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.memory.MemoryCache
import coil.request.ImageRequest
import me.tagavari.airmessage.messaging.QueuedFile

@Composable
fun AttachmentQueueEntryMedia(
	queuedFile: QueuedFile
) {
	AsyncImage(
		model = ImageRequest.Builder(LocalContext.current)
			.data(queuedFile.file.either)
			.crossfade(true)
			.memoryCacheKey(MemoryCache.Key("mediaQueue", mapOf("id" to (queuedFile.localID?.toString() ?: ""))))
			.build(),
		modifier = Modifier
			.fillMaxHeight()
			.clip(RoundedCornerShape(12.dp)),
		contentDescription = null
	)
}
