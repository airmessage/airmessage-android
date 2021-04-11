package me.tagavari.airmessage.util;

import me.tagavari.airmessage.enums.TrackableRequestCategory;

/**
 * Helper interface for finding requests based on a category and key value
 * @param <T> The type of the value
 */
public interface TrackableRequest<T> {
	@TrackableRequestCategory
	int getCategory();
	T getValue();
}