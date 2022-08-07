package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.helper.LanguageHelper
import me.tagavari.airmessage.util.MessagePartFlow

/**
 * A message bubble that displays a downloadable attachment
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageBubbleDownload(
	flow: MessagePartFlow,
	name: String? = null,
	bytesTotal: Long = 0,
	bytesDownloaded: Long? = null,
	isDownloading: Boolean = false,
	onClick: () -> Unit,
	enabled: Boolean = true
) {
	val colors = flow.colors
	
	Surface(
		modifier = Modifier.widthIn(max = 256.dp),
		color = colors.background,
		shape = flow.bubbleShape,
		contentColor = colors.foreground,
		onClick = onClick,
		enabled = enabled
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
				style = typography.copy(fontWeight = FontWeight.Bold),
				textAlign = TextAlign.Center
			)
			
			Spacer(modifier = Modifier.height(8.dp))
			
			if(name != null) {
				Text(
					text = name,
					style = typography,
					textAlign = TextAlign.Center,
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
					style = typography,
					textAlign = TextAlign.Center
				)
			} else {
				Text(
					text = bytesTotalStr,
					style = typography,
					textAlign = TextAlign.Center
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
