package me.tagavari.airmessage.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.widget.Switch;

import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreference;
import me.tagavari.airmessage.R;

public class WarningSwitchPreference extends SwitchPreference {
	public WarningSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}
	
	public WarningSwitchPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public WarningSwitchPreference(Context context) {
		super(context);
	}
	
	@Override
	public void onBindViewHolder(PreferenceViewHolder holder) {
		super.onBindViewHolder(holder);
		
		Switch switchView = (Switch) holder.findViewById(android.R.id.switch_widget);
		int color = getContext().getColor(R.color.colorExperimental);
		switchView.setThumbTintList(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_checked}, new int[]{android.R.attr.state_checked}}, new int[]{0xFFFAFAFA, color}));
		switchView.setTrackTintList(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_checked}, new int[]{android.R.attr.state_checked}}, new int[]{0x61000000, color}));
	}
}