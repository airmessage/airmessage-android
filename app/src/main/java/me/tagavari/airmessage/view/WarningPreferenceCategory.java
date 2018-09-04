package me.tagavari.airmessage.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceViewHolder;
import me.tagavari.airmessage.R;

public class WarningPreferenceCategory extends PreferenceCategory {
	public WarningPreferenceCategory(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}
	
	public WarningPreferenceCategory(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public WarningPreferenceCategory(Context context) {
		super(context);
	}
	
	@Override
	public void onBindViewHolder(PreferenceViewHolder holder) {
		super.onBindViewHolder(holder);
		TextView label = (TextView) holder.findViewById(android.R.id.title);
		label.setTextColor(getContext().getColor(R.color.colorExperimental));
	}
}