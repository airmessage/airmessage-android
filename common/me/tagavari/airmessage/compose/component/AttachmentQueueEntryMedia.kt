package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import me.tagavari.airmessage.R
import me.tagavari.airmessage.helper.FileHelper
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
		success = {
			SubcomposeAsyncImageContent()
			
			//Show play icon for video files
			if(FileHelper.compareMimeTypes(queuedFile.fileType, "video/*")) {
				Icon(
					modifier = Modifier
						.size(48.dp)
						.align(Alignment.Center),
					painter = painterResource(id = R.drawable.play_circle_rounded),
					tint = Color(0xFFFFFFFF),
					contentDescription = null
				)
			}
		},
		error = { fallback() }
	)
}
