package me.tagavari.airmessage.compose.component

import android.net.Uri
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.R
import me.tagavari.airmessage.flavor.MessageBubbleLocationMap
import me.tagavari.airmessage.helper.LocationAttachmentData
import me.tagavari.airmessage.util.MessagePartFlow
import java.io.File
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageBubbleLocation(
	flow: MessagePartFlow,
	file: File,
	date: Date,
	onClick: (Uri) -> Unit
) {
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
		contentColor = colors.foreground,
		onClick = {
			locationData?.let {
				onClick(it.uri)
			}
		}
	) {
		Column(
			modifier = Modifier.width(256.dp),
		) {
			locationData?.let { data ->
				MessageBubbleLocationMap(
					modifier = Modifier.height(200.dp),
					coords = data.coords
				)
			}
			
			Text(
				modifier = Modifier.padding(8.dp),
				text = stringResource(R.string.message_locationfrom, dateFormatted),
				style = MaterialTheme.typography.labelMedium
			)
		}
	}
}
