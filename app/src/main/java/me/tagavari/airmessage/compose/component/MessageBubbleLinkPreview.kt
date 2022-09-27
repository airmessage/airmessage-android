package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.enums.MessagePreviewType
import me.tagavari.airmessage.messaging.MessagePreviewInfo
import me.tagavari.airmessage.util.MessagePartFlow

/**
 * A message bubble that displays a link preview
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleLinkPreview(
	flow: MessagePartFlow,
	preview: MessagePreviewInfo,
	onClick: () -> Unit,
	onSetSelected: (Boolean) -> Unit
) {
	val colors = flow.colors
	
	Surface(
		modifier = Modifier
			.combinedClickable(
				onClick = {
					if(flow.isSelected) {
						onSetSelected(false)
					} else {
						onClick()
					}
				},
				onLongClick = {
					onSetSelected(!flow.isSelected)
				}
			),
		color = colors.background,
		shape = flow.bubbleShape,
		contentColor = colors.foreground
	) {
		Column(
			modifier = Modifier.width(256.dp)
		) {
			preview.data?.let { imageBytes ->
				AsyncImage(
					model = imageBytes,
					contentDescription = null
				)
			}
			
			Column(
				modifier = Modifier.padding(8.dp)
			) {
				Text(
					text = preview.title,
					style = MaterialTheme.typography.labelMedium
				)
				
				Text(
					text = preview.caption,
					style = MaterialTheme.typography.labelSmall
				)
			}
		}
	}
}

@Composable
@Preview
private fun MessageBubbleLinkPreviewPreview() {
	AirMessageAndroidTheme {
		MessageBubbleLinkPreview(
			flow = MessagePartFlow(
				isOutgoing = false,
				isSelected = false,
				anchorBottom = false,
				anchorTop = false,
				tintRatio = 0F
			),
			preview = MessagePreviewInfo(
				type = MessagePreviewType.link,
				localID = -1,
				data = null,
				target = "https://airmessage.org",
				title = "AirMessage",
				subtitle = "iMessage, across all your devices",
				caption = "airmessage.org"
			),
			onClick = {},
			onSetSelected = {}
		)
	}
}