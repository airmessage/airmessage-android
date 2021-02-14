package me.tagavari.airmessage.helper;

import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.core.graphics.ColorUtils;

public class ColorMathHelper {
	//Creating the reference values
	private static final float lightColorRatio = 1.2F;
	private static final float darkColorRatio = 0.8F;
	private static final float lightColorDiff = 0.08F;
	private static final float darkColorDiff = -0.08F;
	
	/* static int lightenColor(int color) {
		return modifyColorAdd(color, lightColorDiff);
	}
	
	static int darkenColor(int color) {
		return modifyColorAdd(color, darkColorDiff);
	} */
	
	/**
	 * Lightens a color by a set amount
	 */
	public static int lightenColor(int color) {
		return multiplyColorRaw(color, lightColorRatio);
	}
	
	/**
	 * Darkens a color by a set amount
	 */
	public static int darkenColor(int color) {
		return multiplyColorRaw(color, darkColorRatio);
	}
	
	/**
	 * Lightens a color to align it with dark mode standards
	 */
	public static int darkModeLightenColor(int color) {
		return multiplyColorLightness(color, 1.5F);
	}
	
	/**
	 * Multiplies the raw RGB values of a color
	 */
	public static int multiplyColorRaw(int color, float factor) {
		int a = Color.alpha(color);
		int r = Math.round(Color.red(color) * factor);
		int g = Math.round(Color.green(color) * factor);
		int b = Math.round(Color.blue(color) * factor);
		return Color.argb(a, clamp(r, 0, 255), clamp(g, 0, 255), clamp(b, 0, 255));
	}
	
	/**
	 * Multiplies the lightness value of a color
	 */
	public static int multiplyColorLightness(int color, float factor) {
		float[] hsl = new float[3];
		ColorUtils.colorToHSL(color, hsl);
		hsl[2] = clamp(hsl[2] * factor, 0F, 1F);
		
		return ColorUtils.HSLToColor(hsl);
	}
	
	/**
	 * Adds to the lightness value of a color
	 */
	public static int modifyColorAdd(int color, float amount) {
		float[] hsl = new float[3];
		ColorUtils.colorToHSL(color, hsl);
		hsl[2] = clamp(hsl[2] + amount, 0F, 1F);
		
		return ColorUtils.HSLToColor(hsl);
	}
	
	private static float clamp(float value, float min, float max) {
		return Math.max(Math.min(value, max), min);
	}
	
	private static int clamp(int value, int min, int max) {
		return Math.max(Math.min(value, max), min);
	}
	
	/**
	 * Calculates the overall brightness of a given bitmap
	 * @param bitmap The bitmap to evaluate
	 * @param skipPixel At what interval to sample the bitmap
	 * @return A lightness value of the image, between 0 and 255
	 */
	public static int calculateBrightness(Bitmap bitmap, int skipPixel) {
		int R = 0; int G = 0; int B = 0;
		int height = bitmap.getHeight();
		int width = bitmap.getWidth();
		int n = 0;
		int[] pixels = new int[width * height];
		bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
		for (int i = 0; i < pixels.length; i += skipPixel) {
			int color = pixels[i];
			R += Color.red(color);
			G += Color.green(color);
			B += Color.blue(color);
			n++;
		}
		return (R + B + G) / (n * 3);
	}
}