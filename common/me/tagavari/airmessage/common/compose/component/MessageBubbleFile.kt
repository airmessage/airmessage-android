package me.tagavari.airmessage.common.compose.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.common.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.common.util.MessagePartFlow

/**
 * A message bubble that displays a generic attachment file
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleFile(
	flow: MessagePartFlow,
	name: String,
	onClick: () -> Unit,
	onSetSelected: (Boolean) -> Unit
) {
	val haptic = LocalHapticFeedback.current
	val colors = flow.colors
	
	Surface(
		color = colors.background,
		shape = flow.bubbleShape,
		contentColor = colors.foreground,
	) {
		Column(
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
						haptic.performHapticFeedback(HapticFeedbackType.LongPress)
						onSetSelected(!flow.isSelected)
					}
				)
				.widthIn(max = 256.dp)
				.padding(all = 12.dp),
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
				style = typography,
				textAlign = TextAlign.Center
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
				isSelected = false,
				anchorBottom = false,
				anchorTop = false,
				tintRatio = 0F
			),
			name = "image.png",
			onClick = {},
			onSetSelected = {}
		)
	}
}