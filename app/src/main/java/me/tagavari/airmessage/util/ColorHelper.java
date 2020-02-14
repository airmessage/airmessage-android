package me.tagavari.airmessage.util;

import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.core.graphics.ColorUtils;

public class ColorHelper {
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
	
	public static int lightenColor(int color) {
		return modifyColorRaw(color, lightColorRatio);
	}
	
	public static int darkenColor(int color) {
		return modifyColorRaw(color, darkColorRatio);
	}
	
	public static int darkModeLightenColor(int color) {
		return modifyColorMultiply(color, 1.5F);
	}
	
	public static int modifyColorRaw(int color, float factor) {
		int a = Color.alpha(color);
		int r = Math.round(Color.red(color) * factor);
		int g = Math.round(Color.green(color) * factor);
		int b = Math.round(Color.blue(color) * factor);
		return Color.argb(a, clamp(r, 0, 255), clamp(g, 0, 255), clamp(b, 0, 255));
	}
	
	public static int modifyColorMultiply(int color, float factor) {
		float[] hsl = new float[3];
		ColorUtils.colorToHSL(color, hsl);
		hsl[2] = clamp(hsl[2] * factor, 0F, 1F);
		
		return ColorUtils.HSLToColor(hsl);
	}
	
	public static int modifyColorAdd(int color, float amount) {
		float[] hsl = new float[3];
		ColorUtils.colorToHSL(color, hsl);
		hsl[2] = clamp(hsl[2] + amount, 0F, 1F);
		
		return ColorUtils.HSLToColor(hsl);
	}
	
	/* private static int modifyColorHSB(int color, float difference) {
		float[] hsv = new float[3];
		Color.colorToHSV(color, hsv);
		hsv[1] = clamp(hsv[1] + difference, 0, 1);
		color = Color.HSVToColor(hsv);
		return color;
	} */
	
	private static float clamp(float value, float min, float max) {
		return Math.max(Math.min(value, max), min);
	}
	
	private static int clamp(int value, int min, int max) {
		return Math.max(Math.min(value, max), min);
	}
	
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