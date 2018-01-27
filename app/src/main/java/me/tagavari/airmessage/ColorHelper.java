package me.tagavari.airmessage;

import android.graphics.Color;

class ColorHelper {
	//Creating the reference values
	private static final float lightColorRatio = 1.2F;
	private static final float darkColorRatio = 0.8F;
	
	static int lightenColor(int color) {
		return modifyColor(color, lightColorRatio);
	}
	
	static int darkenColor(int color) {
		return modifyColor(color, darkColorRatio);
		//return modifyColorHSB(color, -0.2F);
	}
	
	private static int modifyColor(int color, float factor) {
		int a = Color.alpha(color);
		int r = Math.round(Color.red(color) * factor);
		int g = Math.round(Color.green(color) * factor);
		int b = Math.round(Color.blue(color) * factor);
		return Color.argb(a, Math.min(r, 255), Math.min(g, 255), Math.min(b, 255));
	}
	
	private static int modifyColorHSB(int color, float difference) {
		float[] hsv = new float[3];
		Color.colorToHSV(color, hsv);
		hsv[1] = clamp(hsv[1] + difference, 0, 1);
		color = Color.HSVToColor(hsv);
		return color;
	}
	
	private static float clamp(float value, float min, float max) {
		return Math.max(Math.min(value, max), min);
	}
}