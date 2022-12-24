package me.tagavari.airmessage.compose.util

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx3.asFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.tagavari.airmessage.enums.ConnectionState
import me.tagavari.airmessage.redux.ReduxEmitterNetwork

/**
 * Runs the provided block while the app
 * is connected to the server
 */
suspend fun repeatWhileConnected(
	block: suspend CoroutineScope.() -> Unit
) {
	// This scope is required to preserve context before we move to Dispatchers.Main
	coroutineScope {
		withContext(Dispatchers.Main.immediate) {
			// Instance of the running repeating coroutine
			var launchedJob: Job? = null
			val mutex = Mutex()
			
			ReduxEmitterNetwork.connectionStateSubject.asFlow()
				.map { it.state }
				.distinctUntilChanged()
				.collect { state ->
					if(state == ConnectionState.connected) {
						// Launch the repeating work preserving the calling context
						launchedJob = this@coroutineScope.launch {
							// Mutex makes invocations run serially,
							// coroutineScope ensures all child coroutines finish
							mutex.withLock {
								coroutineScope {
									block()
								}
							}
						}
					} else {
						launchedJob?.cancel()
						launchedJob = null
					}
				}
		}
	}
}
