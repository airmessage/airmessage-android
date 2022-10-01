package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme

@Composable
fun ConversationsPaneErrorState(
	modifier: Modifier = Modifier,
	onRetry: () -> Unit
) {
	Column(
		modifier = modifier,
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Text(text = stringResource(id = R.string.message_loaderror_messages))
		
		TextButton(onClick = onRetry) {
			Text(text = stringResource(id = R.string.action_retry))
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun ConversationsPaneErrorStatePreview() {
	AirMessageAndroidTheme {
		ConversationsPaneErrorState(
			onRetry = {}
		)
	}
}
