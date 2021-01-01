package me.tagavari.airmessage.helper;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;

import me.tagavari.airmessage.R;

public class ResourceHelper {
	/**
	 * Resolves an attribute to a typed value
	 */
	public static TypedValue resolveThemeAttr(Context context, @AttrRes int attrRes) {
		Resources.Theme theme = context.getTheme();
		TypedValue typedValue = new TypedValue();
		theme.resolveAttribute(attrRes, typedValue, true);
		return typedValue;
	}
	
	/**
	 * Resolves a color attribute to a color value
	 */
	@ColorInt
	public static int resolveColorAttr(Context context, @AttrRes int colorAttr) {
		TypedValue resolvedAttr = resolveThemeAttr(context, colorAttr);
		// resourceId is used if it's a ColorStateList, and data if it's a color reference or a hex color
		int colorRes = resolvedAttr.resourceId != 0 ? resolvedAttr.resourceId : resolvedAttr.data;
		return ContextCompat.getColor(context, colorRes);
	}
	
	/**
	 * Resolves a float attribute to a float value
	 */
	public static float resolveFloatAttr(Context context, @AttrRes int floatAttr) {
		TypedArray typedArray = context.obtainStyledAttributes(new TypedValue().data, new int[]{floatAttr});
		float value = typedArray.getFloat(0, -1);
		typedArray.recycle();
		return value;
	}
	
	/**
	 * Resolves a drawable attribute to a drawable value
	 */
	public static Drawable resolveDrawableAttr(Context context, @AttrRes int floatAttr) {
		TypedArray typedArray = context.obtainStyledAttributes(new TypedValue().data, new int[]{floatAttr});
		Drawable value = typedArray.getDrawable(0);
		typedArray.recycle();
		return value;
	}
	
	/**
	 * Converts a value from density-independent pixels to device pixels
	 */
	public static int dpToPx(float dp) {
		return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
	}
	
	/**
	 * Converts a value from device pixels to density-independent pixels
	 */
	public static float pxToDp(int px) {
		return px / Resources.getSystem().getDisplayMetrics().density;
	}
	
	/**
	 * Gets if the user is using left-to-right display
	 */
	public static boolean isLTR(Resources resources) {
		return resources.getBoolean(R.bool.is_left_to_right);
	}
}