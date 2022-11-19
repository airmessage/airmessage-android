package me.tagavari.airmessage.compose.component

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme

@Composable
fun AlertCard(
	modifier: Modifier = Modifier,
	show: Boolean = true,
	icon: (@Composable () -> Unit)? = null,
	message: (@Composable () -> Unit)? = null,
	button: (@Composable () -> Unit)? = null
) {
	AnimatedVisibility(
		visible = show,
		enter = expandVertically(
			expandFrom = Alignment.Top
		) + fadeIn(),
		exit = shrinkVertically(
			shrinkTowards = Alignment.Top
		) + fadeOut()
	) {
		Card(modifier = modifier) {
			Row(modifier = Modifier.padding(16.dp)) {
				if(icon != null) {
					icon()
					
					Spacer(modifier = Modifier.width(16.dp))
				}
				
				Column(modifier = Modifier.weight(1F)) {
					if(message != null) {
						Spacer(modifier = Modifier.height(1.dp))
						message()
					}
					
					if(button != null) {
						Spacer(modifier = Modifier.height(8.dp))
						
						Box(
							modifier = Modifier.align(Alignment.End)
						) {
							button()
						}
					}
				}
			}
		}
	}
}

@Preview
@Composable
private fun PreviewAlertCard() {
	AirMessageAndroidTheme {
		AlertCard(
			icon = {
				Icon(
					imageVector = Icons.Outlined.Notifications,
					contentDescription = null
				)
			},
			message = {
				Text("Enable notifications so we can bug you")
			},
			button = {
				TextButton(onClick = {}) {
					Text("Enable notifications")
				}
			}
		)
	}
}