package me.tagavari.airmessage.compose.util

import androidx.compose.runtime.Immutable
import kotlin.reflect.KProperty

/**
 * A value wrapper that marks itself as @Immutable
 */
@Immutable
class ImmutableHolder<T>(val item: T) {
	operator fun component1(): T = item
	operator fun getValue(thisRef: Any?, property: KProperty<*>) = item
}

/**
 * Convenience function for wrapping the invoked value in an immutable holder
 */
fun <T> T.wrapImmutableHolder(): ImmutableHolder<T> = ImmutableHolder(this)
