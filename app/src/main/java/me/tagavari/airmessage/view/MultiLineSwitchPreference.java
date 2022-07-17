package me.tagavari.airmessage.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreference;
import androidx.preference.SwitchPreferenceCompat;

public class MultiLineSwitchPreference extends SwitchPreference {
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
	public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
		super.onBindViewHolder(holder);
		TextView textView = (TextView) holder.findViewById(android.R.id.title);
		if(textView != null) textView.setSingleLine(false);
	}
}