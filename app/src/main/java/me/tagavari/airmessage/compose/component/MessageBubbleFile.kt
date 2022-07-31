package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.helper.LanguageHelper
import me.tagavari.airmessage.util.MessagePartFlow
import java.io.File

/**
 * A message bubble that displays a generic attachment file
 */
@Composable
fun MessageBubbleFile(
	flow: MessagePartFlow,
	name: String,
	onClick: () -> Unit
) {
	val colors = flow.colors
	
	Surface(
		modifier = Modifier.clickable(onClick = onClick),
		color = colors.background,
		shape = flow.bubbleShape,
		contentColor = colors.foreground
	) {
		Column(
			modifier = Modifier.padding(all = 12.dp),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Box(
				modifier = Modifier.size(48.dp),
				contentAlignment = Alignment.Center
			) {
				Icon(Icons.Default.InsertDriveFile, contentDescription = "")
			}
			
			Spacer(modifier = Modifier.height(8.dp))
			
			val typography = MaterialTheme.typography.bodyLarge
			
			Text(
				text = name,
				style = typography
			)
		}
	}
}

@Preview
@Composable
private fun PreviewMessageBubbleFile() {
	AirMessageAndroidTheme {
		MessageBubbleFile(
			flow = MessagePartFlow(
				isOutgoing = false,
				anchorBottom = false,
				anchorTop = false,
				tintRatio = 0F
			),
			name = "image.png",
			onClick = {}
		)
	}
}