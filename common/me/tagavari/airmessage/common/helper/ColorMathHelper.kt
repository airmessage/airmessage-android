package me.tagavari.airmessage.common.helper

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import kotlin.math.roundToInt

object ColorMathHelper {
	//Creating the reference values
	private const val lightColorRatio = 1.2F
	private const val darkColorRatio = 0.8F
	
	@JvmStatic
	fun lightenColor(color: Int): Int {
		return multiplyColorRaw(color, lightColorRatio)
	}
	
	/**
	 * Darkens a color by a set amount
	 */
	@JvmStatic
	fun darkenColor(color: Int): Int {
		return multiplyColorRaw(color, darkColorRatio)
	}
	
	/**
	 * Lightens a color to align it with dark mode standards
	 */
	@JvmStatic
	fun darkModeLightenColor(color: Int): Int {
		return multiplyColorLightness(color, 1.5f)
	}
	
	/**
	 * Multiplies the raw RGB values of a color
	 */
	@JvmStatic
	fun multiplyColorRaw(color: Int, factor: Float): Int {
		val a = Color.alpha(color)
		val r = (Color.red(color) * factor).roundToInt()
		val g = (Color.green(color) * factor).roundToInt()
		val b = (Color.blue(color) * factor).roundToInt()
		return Color.argb(a, r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
	}
	
	/**
	 * Multiplies the lightness value of a color
	 */
	@JvmStatic
	fun multiplyColorLightness(color: Int, factor: Float): Int {
		val hsl = FloatArray(3)
		ColorUtils.colorToHSL(color, hsl)
		hsl[2] = (hsl[2] * factor).coerceIn(0F, 1F)
		return ColorUtils.HSLToColor(hsl)
	}
	
	/**
	 * Adds to the lightness value of a color
	 */
	fun modifyColorAdd(color: Int, amount: Float): Int {
		val hsl = FloatArray(3)
		ColorUtils.colorToHSL(color, hsl)
		hsl[2] = (hsl[2] + amount).coerceIn(0F, 1F)
		return ColorUtils.HSLToColor(hsl)
	}
	
	/**
	 * Calculates the overall brightness of a given bitmap
	 * @param bitmap The bitmap to evaluate
	 * @param skipPixel At what interval to sample the bitmap
	 * @return A lightness value of the image, between 0 and 255
	 */
	@JvmStatic
	fun calculateBrightness(bitmap: Bitmap, skipPixel: Int): Int {
		var r = 0
		var g = 0
		var b = 0
		
		val width = bitmap.width
		val height = bitmap.height
		var n = 0
		val pixels = IntArray(width * height)
		
		bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
		
		for(i in pixels.indices step skipPixel) {
			val color = pixels[i]
			r += Color.red(color)
			g += Color.green(color)
			b += Color.blue(color)
			n++
		}
		return (r + g + b) / (n * 3)
	}
}