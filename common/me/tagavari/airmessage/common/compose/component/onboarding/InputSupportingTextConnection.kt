package me.tagavari.airmessage.common.compose.component.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.R
import me.tagavari.airmessage.common.compose.ui.theme.AirMessageAndroidTheme

@Composable
fun ConnectionInputSupportingText(
	modifier: Modifier = Modifier,
	show: Boolean,
	connected: Boolean
) {
	Row(
		modifier = modifier
			.padding(start = 16.dp, end = 16.dp, top = 4.dp)
			.alpha(if(show) 1F else 0F),
		horizontalArrangement = Arrangement.End,
		verticalAlignment = Alignment.CenterVertically
	) {
		if(connected) {
			CompositionLocalProvider(
				LocalContentColor provides MaterialTheme.colorScheme.primary
			) {
				Icon(
					modifier = Modifier.size(16.dp),
					imageVector = Icons.Outlined.CheckCircleOutline,
					contentDescription = null
				)
				
				Spacer(modifier = Modifier.width(2.dp))
				
				Text(
					text = stringResource(R.string.message_endpoint_connected),
					style = MaterialTheme.typography.bodySmall
				)
			}
		} else {
			CompositionLocalProvider(
				LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
			) {
				Icon(
					modifier = Modifier.size(16.dp),
					imageVector = Icons.Outlined.RemoveCircleOutline,
					contentDescription = null
				)
				
				Spacer(modifier = Modifier.width(2.dp))
				
				Text(
					text = stringResource(R.string.message_endpoint_notreachable),
					style = MaterialTheme.typography.bodySmall
				)
			}
		}
	}
}

@Composable
@Preview(name = "Connected", widthDp = 256)
private fun PreviewInputSupportingTextConnectionConnected() {
	AirMessageAndroidTheme {
		Surface {
			ConnectionInputSupportingText(
				show = true,
				connected = true
			)
		}
	}
}

@Composable
@Preview(name = "Disconnected", widthDp = 256)
private fun PreviewInputSupportingTextConnectionDisconnected() {
	AirMessageAndroidTheme {
		Surface {
			ConnectionInputSupportingText(
				show = true,
				connected = false
			)
		}
	}
}