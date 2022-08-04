package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
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

/**
 * A message bubble that displays a downloadable attachment
 */
@Composable
fun MessageBubbleDownload(
	flow: MessagePartFlow,
	name: String? = null,
	bytesTotal: Long = 0,
	bytesDownloaded: Long? = null,
	isDownloading: Boolean = false,
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
				if(isDownloading) {
					if(bytesDownloaded == null) {
						CircularProgressIndicator(
							color = colors.foreground
						)
					} else {
						CircularProgressIndicator(
							progress = bytesDownloaded.toFloat() / bytesTotal.toFloat(),
							color = colors.foreground
						)
					}
				} else {
					Icon(Icons.Default.Download, contentDescription = "")
				}
			}
			
			Spacer(modifier = Modifier.height(8.dp))
			
			val typography = MaterialTheme.typography.bodyLarge
			
			Text(
				text = if(isDownloading) "Downloading..." else "Tap to download",
				style = typography.copy(fontWeight = FontWeight.Bold)
			)
			
			Spacer(modifier = Modifier.height(8.dp))
			
			if(name != null) {
				Text(
					text = name,
					style = typography
				)
			}
			
			val bytesTotalStr = remember(bytesTotal) {
				LanguageHelper.getHumanReadableByteCountInt(bytesTotal, false)
			}
			
			if(isDownloading) {
				val bytesDownloadedStr = remember(bytesDownloaded) {
					LanguageHelper.getHumanReadableByteCountInt(bytesDownloaded ?: 0, false)
				}
				
				Text(
					text = "$bytesDownloadedStr / $bytesTotalStr",
					style = typography
				)
			} else {
				Text(
					text = bytesTotalStr,
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
			name = "image.png",
			bytesTotal = 16 * 1024,
			onClick = {}
		)
	}
}

@Preview
@Composable
private fun PreviewMessageBubbleDownloadProgress() {
	AirMessageAndroidTheme {
		MessageBubbleDownload(
			flow = MessagePartFlow(
				isOutgoing = false,
				anchorBottom = false,
				anchorTop = false,
				tintRatio = 0F
			),
			name = "image.png",
			bytesTotal = 16 * 1024,
			bytesDownloaded = 12 * 1024,
			isDownloading = true,
			onClick = {}
		)
	}
}
