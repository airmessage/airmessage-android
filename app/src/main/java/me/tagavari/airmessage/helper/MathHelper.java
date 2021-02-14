package me.tagavari.airmessage.helper;

public class MathHelper {
	/**
	 * Calculates the linear interpolation between two values
	 * @param val The linear interpolation progress, from 0.0 to 1.0
	 * @param start The minimum value
	 * @param end The maximum value
	 * @return The interpolated value
	 */
	public static float lerp(float val, float start, float end) {
		return val * (end - start) + start;
	}
}