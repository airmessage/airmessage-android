package me.tagavari.airmessage.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme

class Licenses : ComponentActivity() {
	@OptIn(ExperimentalMaterial3Api::class)
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		WindowCompat.setDecorFitsSystemWindows(window, false)
		
		setContent {
			AirMessageAndroidTheme {
				val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
				
				Scaffold(
					modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
					topBar = {
						SmallTopAppBar(
							title = {
								Text(text = stringResource(id = R.string.screen_licenses))
							},
							navigationIcon = {
								IconButton(onClick = { finish() }) {
									Icon(
										imageVector = Icons.Filled.ArrowBack,
										contentDescription = stringResource(id = R.string.action_back)
									)
								}
							},
							scrollBehavior = scrollBehavior
						)
					},
					content = { innerPadding ->
						LibrariesContainer(
							modifier = Modifier.fillMaxSize(),
							contentPadding = innerPadding,
							colors = LibraryDefaults.libraryColors(
								backgroundColor = MaterialTheme.colorScheme.background,
								contentColor = MaterialTheme.colorScheme.onBackground,
								badgeBackgroundColor = MaterialTheme.colorScheme.primary,
								badgeContentColor = MaterialTheme.colorScheme.onPrimary
							)
						)
					}
				)
			}
		}
	}
}
