package me.tagavari.airmessage.common.compose.remember

import androidx.compose.runtime.*

/**
 * Mirrors the provided value, as long as shouldCache returns true
 */
@Composable
fun <T> deriveCachedValue(value: T, shouldCache: Boolean = value != null): State<T> {
	val cachedValue = remember { mutableStateOf(value) }
	LaunchedEffect(value, shouldCache) {
		if(shouldCache) {
			cachedValue.value = value
		}
	}
	return cachedValue
}
