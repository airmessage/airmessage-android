package me.tagavari.airmessage.compose.component

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.R
import me.tagavari.airmessage.flavor.MessageBubbleLocationMap
import me.tagavari.airmessage.helper.LocationAttachmentData
import me.tagavari.airmessage.util.MessagePartFlow
import java.io.File
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleLocation(
	flow: MessagePartFlow,
	file: File,
	date: Date,
	onClick: (LocationAttachmentData) -> Unit,
	onSetSelected: (Boolean) -> Unit
) {
	val haptic = LocalHapticFeedback.current
	val context = LocalContext.current
	val dateFormatted = remember(date) {
		DateFormat.getDateFormat(context).format(date)
	}
	
	val locationData by produceState<LocationAttachmentData?>(initialValue = null, file) {
		value = LocationAttachmentData.fromVCard(file)
	}
	
	val colors = flow.colors
	
	Surface(
		color = colors.background,
		shape = flow.bubbleShape,
		contentColor = colors.foreground
	) {
		Column(
			modifier = Modifier
				.combinedClickable(
					onClick = {
						if(flow.isSelected) {
							onSetSelected(false)
						} else {
							locationData?.let { onClick(it) }
						}
					},
					onLongClick = {
						haptic.performHapticFeedback(HapticFeedbackType.LongPress)
						onSetSelected(!flow.isSelected)
					}
				)
				.width(256.dp),
		) {
			Box(modifier = Modifier.height(200.dp)) {
				locationData?.let { data ->
					MessageBubbleLocationMap(
						modifier = Modifier.fillMaxSize(),
						coords = data.coords,
						highlight = if(flow.isSelected) flow.colors.background.copy(alpha = 0.5F) else null
					)
				}
			}
			
			Text(
				modifier = Modifier.padding(8.dp),
				text = stringResource(R.string.message_locationfrom, dateFormatted),
				style = MaterialTheme.typography.labelMedium
			)
		}
	}
}
