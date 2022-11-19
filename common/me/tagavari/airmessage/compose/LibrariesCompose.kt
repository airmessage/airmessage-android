package me.tagavari.airmessage.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import me.tagavari.airmessage.compose.component.LibrariesPane
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme

class LibrariesCompose : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		WindowCompat.setDecorFitsSystemWindows(window, false)
		
		setContent {
			AirMessageAndroidTheme {
				LibrariesPane(onBack = ::finish)
			}
		}
	}
}
