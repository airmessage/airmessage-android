package me.tagavari.airmessage.compose.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.compose.R
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.helper.ProgressState

@Composable
fun ConversationsPaneSyncState(
	modifier: Modifier = Modifier,
	syncState: ProgressState
) {
	Column(
		modifier = modifier
	) {
		StatusCardColumn()
		
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.weight(1F),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center
		) {
			Text(
				text = stringResource(R.string.progress_sync),
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
			
			Spacer(modifier = Modifier.height(16.dp))
			
			if(syncState is ProgressState.Determinate) {
				val animatedProgress by animateFloatAsState(
					targetValue = syncState.progress,
					animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
				)
				
				LinearProgressIndicator(
					modifier = Modifier.width(150.dp),
					progress = animatedProgress
				)
			} else {
				LinearProgressIndicator(
					modifier = Modifier.width(150.dp)
				)
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun ConversationsPaneSyncStatePreview() {
	AirMessageAndroidTheme {
		ConversationsPaneSyncState(
			syncState = ProgressState.Indeterminate
		)
	}
}
