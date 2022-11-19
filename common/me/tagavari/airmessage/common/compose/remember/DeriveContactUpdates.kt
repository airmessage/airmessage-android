package me.tagavari.airmessage.common.compose.remember

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import me.tagavari.airmessage.common.redux.ReduxEmitterNetwork

/**
 * Returns a value that represents the last
 * time a contact update occurred
 */
@Composable
fun deriveContactUpdates() =
	ReduxEmitterNetwork.contactUpdates.collectAsState(initial = 0).value
