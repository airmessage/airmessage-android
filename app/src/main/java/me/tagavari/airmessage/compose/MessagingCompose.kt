package me.tagavari.airmessage.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.component.MessagingScreen
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme

class MessagingCompose : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		WindowCompat.setDecorFitsSystemWindows(window, false)
		
		setContent {
			AirMessageAndroidTheme {
				MessagingScreen(
					navigationIcon = {
						IconButton(onClick = { finish() }) {
							Icon(
								imageVector = Icons.Filled.ArrowBack,
								contentDescription = stringResource(id = R.string.action_back)
							)
						}
					}
				)
			}
		}
	}
}