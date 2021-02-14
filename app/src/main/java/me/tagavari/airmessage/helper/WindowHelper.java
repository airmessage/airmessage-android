package me.tagavari.airmessage.helper;

import android.app.Activity;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;

import me.tagavari.airmessage.R;

public class WindowHelper {
	/**
	 * Gets the maximum width the primary content should be displayed at
	 */
	public static int getMaxContentWidth(Resources resources) {
		return resources.getDimensionPixelSize(R.dimen.contentwidth_max);
	}
	
	/**
	 * Helper method for calculatePaddingContentWidth() that handles finding the width
	 * @param resources Android resources, to fetch the maximum content width
	 * @param view The view to limit
	 * @return padding (in pixels) that should be applied to the view
	 */
	public static int calculatePaddingContentWidth(Resources resources, View view) {
		return calculatePaddingContentWidth(getMaxContentWidth(resources), view);
	}
	
	/**
	 * Calculates the size that should be allocated to padding in order to keep the view a reasonable width
	 * @param maxContentWidth The size to limit the view to
	 * @param view The view to limit
	 * @return padding (in pixels) that should be applied to the view
	 */
	public static int calculatePaddingContentWidth(int maxContentWidth, View view) {
		//Getting the width
		int width = view.getWidth();
		
		//Returning if the view is already below the width
		if(width <= maxContentWidth) return 0;
		
		//Returning the padding
		return (width - maxContentWidth) / 2;
	}
	
	/**
	 * Friendly method to quickly enforce the maximum content width on a specified view
	 * @param resources Android resources, to fetch the maximum content width
	 * @param view The view to limit
	 */
	public static void enforceContentWidthView(Resources resources, View view) {
		view.post(() -> enforceContentWidthImmediate(getMaxContentWidth(resources), view));
	}
	
	/**
	 * Adds padding to either side of the view to prevent it from expanding to take up the entire display, if the screen is large enough
	 * @param maxContentWidth The size to limit the view to
	 * @param view The view to limit
	 */
	public static void enforceContentWidthImmediate(int maxContentWidth, View view) {
		//Calculating and applying the margin
		int padding = calculatePaddingContentWidth(maxContentWidth, view);
		//view.setPadding(padding, view.getPaddingTop(), padding, view.getPaddingBottom());
		ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
		layoutParams.leftMargin = padding;
		layoutParams.rightMargin = padding;
		view.setLayoutParams(layoutParams);
	}
	
	/**
	 * Gets the maximum width a message bubble should occupy
	 */
	public static int getMaxMessageWidth(Resources resources) {
		return (int) Math.min(WindowHelper.getMaxContentWidth(resources) * .7F, resources.getDisplayMetrics().widthPixels * 0.7F);
	}
	
	/**
	 * Gets the height of the current app window
	 */
	public static int getWindowHeight(Activity activity) {
		return activity.getWindow().getDecorView().getHeight();
	}
}