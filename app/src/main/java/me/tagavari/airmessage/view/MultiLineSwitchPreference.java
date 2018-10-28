package me.tagavari.airmessage.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreference;
import androidx.preference.SwitchPreferenceCompat;

public class MultiLineSwitchPreference extends SwitchPreferenceCompat {
	public MultiLineSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	public MultiLineSwitchPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public MultiLineSwitchPreference(Context context) {
		super(context);
	}
	
	@Override
	public void onBindViewHolder(PreferenceViewHolder holder) {
		super.onBindViewHolder(holder);
		TextView textView = (TextView) holder.findViewById(android.R.id.title);
		if(textView != null) textView.setSingleLine(false);
	}
}