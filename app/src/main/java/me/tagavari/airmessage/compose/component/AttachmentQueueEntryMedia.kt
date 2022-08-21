package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
		contentScale = ContentScale.Crop,
		modifier = Modifier
			.fillMaxSize()
			.clip(RoundedCornerShape(4.dp)),
		contentDescription = null
	)
}
