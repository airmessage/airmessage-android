package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.R
import me.tagavari.airmessage.helper.FileHelper
import me.tagavari.airmessage.messaging.QueuedFile
import java.io.File

@Composable
fun AttachmentQueueEntry(
	queuedFile: QueuedFile,
	onClick: () -> Unit,
	onRemove: () -> Unit
) {
	if(FileHelper.compareMimeTypes(queuedFile.fileType, "image/*")
		|| FileHelper.compareMimeTypes(queuedFile.fileType, "video/*")) {
		Box(
			modifier = Modifier.fillMaxHeight()
		) {
			AttachmentQueueEntryMedia(queuedFile = queuedFile, onClick = onClick)
			
			IconButton(
				modifier = Modifier
					.align(Alignment.TopEnd)
					.offset(
						x = (8 * if(LocalLayoutDirection.current === LayoutDirection.Ltr) 1 else -1).dp,
						y = (-8).dp
					),
				onClick = onRemove
			) {
				Icon(
					painter = painterResource(id = R.drawable.draft_close_circle_border),
					contentDescription = stringResource(id = R.string.action_back),
					tint = Color.Unspecified
				)
			}
		}
	}
}
