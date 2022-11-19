package me.tagavari.airmessage.common.compose.component.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.common.connection.ConnectionManager

@Composable
fun OnboardingPane(
	connectionManager: ConnectionManager?,
	onComplete: () -> Unit,
	windowSizeClass: WindowSizeClass
) {
	if(windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium
		&& windowSizeClass.heightSizeClass >= WindowHeightSizeClass.Medium) {
		//Display in a container
		Box(
			modifier = Modifier
				.background(MaterialTheme.colorScheme.inverseOnSurface)
				.fillMaxSize(),
			contentAlignment = Alignment.Center
		) {
			Card(
				modifier = Modifier
					.systemBarsPadding()
					.width(width = 512.dp)
					.heightIn(max = 712.dp)
					.fillMaxHeight()
			) {
				OnboardingNavigationPane(
					connectionManager = connectionManager,
					onComplete = onComplete
				)
			}
		}
	} else {
		//Display full-screen
		OnboardingNavigationPane(
			connectionManager = connectionManager,
			onComplete = onComplete
		)
	}
}
