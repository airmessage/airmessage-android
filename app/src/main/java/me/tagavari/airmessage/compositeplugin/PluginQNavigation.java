package me.tagavari.airmessage.compositeplugin;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import me.tagavari.airmessage.composite.AppCompatActivityPlugin;

public class PluginQNavigation extends AppCompatActivityPlugin {
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Ignoring if the system version is below Q
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return;
		
		//Configuring the window
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			getActivity().getWindow().setDecorFitsSystemWindows(false);
		} else {
			getActivity().getWindow().getDecorView().setSystemUiVisibility(getActivity().getWindow().getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
		}
	}
	
	public static void setViewForInsets(View rootView, View... listViews) {
		//Ignoring if the system version is below Q
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return;
		
		//Adding an inset listener
		ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
			for(View view : listViews) {
				((ViewGroup.MarginLayoutParams) view.getLayoutParams()).bottomMargin = -insets.getSystemWindowInsetBottom();
				view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), insets.getSystemWindowInsetBottom());
			}
			return insets;
		});
	}
}