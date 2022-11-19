package me.tagavari.airmessage.common.helper

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import me.tagavari.airmessage.R

object ResourceHelper {
	/**
	 * Resolves an attribute to a typed value
	 */
	@JvmStatic
	fun resolveThemeAttr(context: Context, @AttrRes attrRes: Int): TypedValue {
		val typedValue = TypedValue()
		context.theme.resolveAttribute(attrRes, typedValue, true)
		return typedValue
	}
	
	/**
	 * Resolves a color attribute to a color value
	 */
	@JvmStatic
	@ColorInt
	fun resolveColorAttr(context: Context, @AttrRes colorAttr: Int): Int {
		val resolvedAttr = resolveThemeAttr(context, colorAttr)
		// resourceId is used if it's a ColorStateList, and data if it's a color reference or a hex color
		val colorRes = if(resolvedAttr.resourceId != 0) resolvedAttr.resourceId else resolvedAttr.data
		return ContextCompat.getColor(context, colorRes)
	}
	
	/**
	 * Resolves a float attribute to a float value
	 */
	@JvmStatic
	fun resolveFloatAttr(context: Context, @AttrRes floatAttr: Int): Float {
		val typedArray = context.obtainStyledAttributes(TypedValue().data, intArrayOf(floatAttr))
		val value = typedArray.getFloat(0, -1f)
		typedArray.recycle()
		return value
	}
	
	/**
	 * Resolves a drawable attribute to a drawable value
	 */
	fun resolveDrawableAttr(context: Context, @AttrRes floatAttr: Int): Drawable? {
		val typedArray = context.obtainStyledAttributes(TypedValue().data, intArrayOf(floatAttr))
		val value = typedArray.getDrawable(0)
		typedArray.recycle()
		return value
	}
	
	/**
	 * Converts a value from density-independent pixels to device pixels
	 */
	@JvmStatic
	fun dpToPx(dp: Float): Int {
		return (dp * Resources.getSystem().displayMetrics.density).toInt()
	}
	
	/**
	 * Converts a value from device pixels to density-independent pixels
	 */
	@JvmStatic
	fun pxToDp(px: Int): Float {
		return px / Resources.getSystem().displayMetrics.density
	}
	
	/**
	 * Gets if the user is using left-to-right display
	 */
	@JvmStatic
	fun isLTR(resources: Resources): Boolean {
		return resources.getBoolean(R.bool.is_left_to_right)
	}
}