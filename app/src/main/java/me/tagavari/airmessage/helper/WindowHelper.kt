package me.tagavari.airmessage.helper

import android.app.Activity
import android.content.res.Resources
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import me.tagavari.airmessage.R

object WindowHelper {
	/**
	 * Gets the maximum width the primary content should be displayed at
	 */
	@JvmStatic
	fun getMaxContentWidth(resources: Resources) =
			resources.getDimensionPixelSize(R.dimen.contentwidth_max)
	
	/**
	 * Helper method for calculatePaddingContentWidth() that handles finding the width
	 * @param resources Android resources, to fetch the maximum content width
	 * @param view The view to limit
	 * @return padding (in pixels) that should be applied to the view
	 */
	@JvmStatic
	fun calculatePaddingContentWidth(resources: Resources, view: View): Int {
		return calculatePaddingContentWidth(getMaxContentWidth(resources), view)
	}
	
	/**
	 * Calculates the size that should be allocated to padding in order to keep the view a reasonable width
	 * @param maxContentWidth The size to limit the view to
	 * @param view The view to limit
	 * @return padding (in pixels) that should be applied to the view
	 */
	@JvmStatic
	fun calculatePaddingContentWidth(maxContentWidth: Int, view: View): Int {
		//Getting the width
		val width = view.width
		
		//Returning if the view is already below the width
		if(width <= maxContentWidth) return 0
		
		//Returning the padding
		return (width - maxContentWidth) / 2
	}
	
	/**
	 * Friendly method to quickly enforce the maximum content width on a specified view
	 * @param resources Android resources, to fetch the maximum content width
	 * @param view The view to limit
	 */
	@JvmStatic
	fun enforceContentWidthView(resources: Resources, view: View) {
		view.post { enforceContentWidthImmediate(getMaxContentWidth(resources), view) }
	}
	
	/**
	 * Adds padding to either side of the view to prevent it from expanding to take up the entire display, if the screen is large enough
	 * @param maxContentWidth The size to limit the view to
	 * @param view The view to limit
	 */
	@JvmStatic
	fun enforceContentWidthImmediate(maxContentWidth: Int, view: View) {
		//Calculating and applying the margin
		val padding = calculatePaddingContentWidth(maxContentWidth, view)
		view.layoutParams = (view.layoutParams as MarginLayoutParams).apply {
			leftMargin = padding
			rightMargin = padding
		}
	}
	
	/**
	 * Gets the maximum width a message bubble should occupy
	 */
	@JvmStatic
	fun getMaxMessageWidth(resources: Resources): Int {
		return (getMaxContentWidth(resources) * .7F).coerceAtMost(resources.displayMetrics.widthPixels * 0.7F).toInt()
	}
	
	/**
	 * Gets the height of the current app window
	 */
	@JvmStatic
	fun getWindowHeight(activity: Activity): Int {
		return activity.window.decorView.height
	}
}