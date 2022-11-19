package me.tagavari.airmessage.compose.interop

import android.view.MotionEvent

/**
 * A listener for [GestureTracker]. Receives a [MotionEvent], and returns
 * whether the event should be consumed.
 */
typealias GestureTracker = (MotionEvent) -> Boolean

/**
 * An object that others can listen for gesture events on
 */
interface GestureTrackable {
	fun addGestureTracker(gestureTracker: GestureTracker)
	fun removeGestureTracker(gestureTracker: GestureTracker)
}
