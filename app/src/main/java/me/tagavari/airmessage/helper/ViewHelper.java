package me.tagavari.airmessage.helper;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;

import me.tagavari.airmessage.R;

public class ViewHelper {
	public static ArrayList<View> getViewsByTag(ViewGroup root, String tag) {
		ArrayList<View> views = new ArrayList<>();
		final int childCount = root.getChildCount();
		for(int i = 0; i < childCount; i++) {
			final View child = root.getChildAt(i);
			if (child instanceof ViewGroup) {
				views.addAll(getViewsByTag((ViewGroup) child, tag));
			}
			
			final Object tagObj = child.getTag();
			if (tagObj != null && tagObj.equals(tag)) {
				views.add(child);
			}
			
		}
		return views;
	}
	
	/**
	 * Colors all view with the tag @string/tag_primarytint the specified color
	 */
	public static void colorTaggedUI(Resources resources, ViewGroup viewRoot, int color) {
		for(View view : getViewsByTag(viewRoot, resources.getString(R.string.tag_primarytint))) {
			if(view instanceof ImageView) ((ImageView) view).setColorFilter(color, android.graphics.PorterDuff.Mode.MULTIPLY);
			else if(view instanceof SwitchMaterial) {
				SwitchMaterial switchView = (SwitchMaterial) view;
				switchView.setThumbTintList(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_checked}, new int[]{android.R.attr.state_checked}}, new int[]{0xFFFAFAFA, color}));
				switchView.setTrackTintList(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_checked}, new int[]{android.R.attr.state_checked}}, new int[]{0x61000000, color}));
			} else if(view instanceof MaterialButton) {
				MaterialButton buttonView = (MaterialButton) view;
				buttonView.setTextColor(color);
				buttonView.setIconTint(ColorStateList.valueOf(color));
				buttonView.setRippleColor(ColorStateList.valueOf(color));
			} else if(view instanceof TextView) ((TextView) view).setTextColor(color);
			else if(view instanceof RelativeLayout) view.setBackground(new ColorDrawable(color));
			else if(view instanceof FrameLayout) view.setBackgroundTintList(ColorStateList.valueOf(color));
		}
	}
}