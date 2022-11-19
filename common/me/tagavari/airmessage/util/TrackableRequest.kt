package me.tagavari.airmessage.util

import me.tagavari.airmessage.enums.TrackableRequestCategory

/**
 * Helper interface for finding requests based on a category and key value
 * @param <T> The type of the value
*/
interface TrackableRequest<T> {
	@get:TrackableRequestCategory val category: Int
	val value: T
}