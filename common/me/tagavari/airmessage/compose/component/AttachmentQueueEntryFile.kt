package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.messaging.QueuedFile
import me.tagavari.airmessage.util.Union
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentQueueEntryFile(
	queuedFile: QueuedFile,
	onClick: () -> Unit
) {
	Surface(
		modifier = Modifier
			.fillMaxHeight()
			.widthIn(max = 256.dp)
			.clip(RoundedCornerShape(12.dp)),
		onClick = onClick,
		color = MaterialTheme.colorScheme.surfaceVariant
	) {
		Column(
			modifier = Modifier.padding(all = 16.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center
		) {
			Box(
				modifier = Modifier.size(48.dp),
				contentAlignment = Alignment.Center
			) {
				Icon(Icons.Default.InsertDriveFile, contentDescription = "")
			}
			
			Spacer(modifier = Modifier.height(8.dp))
			
			Text(
				text = queuedFile.fileName,
				style = MaterialTheme.typography.bodyLarge,
				textAlign = TextAlign.Center,
				overflow = TextOverflow.Ellipsis
			)
		}
	}
}

@Preview
@Composable
private fun AttachmentQueueEntryFilePreview() {
	AirMessageAndroidTheme {
		Box(
			modifier = Modifier.height(256.dp)
		) {
			AttachmentQueueEntryFile(
				queuedFile = QueuedFile(
					file = Union.ofB(File("")),
					fileName = "a document with really cool stuff inside.pdf",
					fileSize = 1024,
					fileType = "document/pdf"
				),
				onClick = {}
			)
		}
	}
}
