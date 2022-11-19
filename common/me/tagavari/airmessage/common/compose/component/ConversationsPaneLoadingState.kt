package me.tagavari.airmessage.common.compose.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import me.tagavari.airmessage.common.compose.ui.theme.AirMessageAndroidTheme

@Composable
fun ConversationsPaneLoadingState(
	modifier: Modifier = Modifier
) {
	Box(
		modifier = modifier
	) {
		CircularProgressIndicator(
			modifier = Modifier.align(Alignment.Center)
		)
	}
}

@Preview(showBackground = true)
@Composable
private fun ConversationsPaneLoadingStatePreview() {
	AirMessageAndroidTheme {
		ConversationsPaneLoadingState()
	}
}
