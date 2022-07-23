package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.util.MessagePartFlow

/**
 * A message bubble that displays a downloadable attachment
 */
@Composable
fun MessageBubbleDownload(
	flow: MessagePartFlow,
	name: String? = null
) {
	val colors = flow.colors
	
	Surface(
		color = colors.background,
		shape = flow.bubbleShape,
		contentColor = colors.foreground
	) {
		Column(
			modifier = Modifier.padding(all = 12.dp),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Icon(Icons.Default.Download, contentDescription = "")
			
			Spacer(modifier = Modifier.height(8.dp))
			
			val typography = MaterialTheme.typography.bodyLarge
			
			Text(
				text = "Tap to download",
				style = typography.copy(fontWeight = FontWeight.Bold)
			)
			
			Spacer(modifier = Modifier.height(8.dp))
			
			if(name != null) {
				Text(
					text = name,
					style = typography
				)
			}
		}
	}
}

@Preview
@Composable
private fun PreviewMessageBubbleDownload() {
	AirMessageAndroidTheme {
		MessageBubbleDownload(
			flow = MessagePartFlow(
				isOutgoing = false,
				anchorBottom = false,
				anchorTop = false,
				tintRatio = 0F
			),
			name = "image.png"
		)
	}
}