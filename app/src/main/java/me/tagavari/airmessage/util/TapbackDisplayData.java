package me.tagavari.airmessage.util;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

public class TapbackDisplayData {
	@DrawableRes private final int iconResource;
	@ColorRes private final int color;
	@StringRes private final int label;
	
	/**
	 * A class that holds display data required to render a tapback
	 * @param iconResource The tapback's icon
	 * @param color The tapback's color
 	 * @param label The tapback's title
	 */
	public TapbackDisplayData(@DrawableRes int iconResource, @ColorRes int color, @StringRes int label) {
		this.iconResource = iconResource;
		this.color = color;
		this.label = label;
	}
	
	@DrawableRes
	public int getIconResource() {
		return iconResource;
	}
	
	@ColorRes
	public int getColor() {
		return color;
	}
	
	@StringRes
	public int getLabel() {
		return label;
	}
}