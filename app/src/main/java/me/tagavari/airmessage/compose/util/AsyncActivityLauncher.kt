package me.tagavari.airmessage.compose.util

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.*
import androidx.core.app.ActivityOptionsCompat
import kotlinx.coroutines.CompletableDeferred

interface AsyncActivityResultLauncher<I, O> {
	suspend fun launch(input: I, options: ActivityOptionsCompat? = null): O
}

/**
 * A wrapper around [rememberLauncherForActivityResult] that exposes its launch
 * function as a suspending function rather than a callback
 */
@Composable
fun <I, O> rememberAsyncLauncherForActivityResult(contract: ActivityResultContract<I, O>): AsyncActivityResultLauncher<I, O> {
	var launcherDeferred by remember { mutableStateOf<CompletableDeferred<O>?>(null) }
	val launcher = rememberLauncherForActivityResult(contract = contract) { result ->
		launcherDeferred?.complete(result)
	}
	
	return object : AsyncActivityResultLauncher<I, O> {
		override suspend fun launch(input: I, options: ActivityOptionsCompat?): O {
			val deferred = CompletableDeferred<O>()
			launcherDeferred = deferred
			launcher.launch(input, options)
			return deferred.await()
		}
	}
}
