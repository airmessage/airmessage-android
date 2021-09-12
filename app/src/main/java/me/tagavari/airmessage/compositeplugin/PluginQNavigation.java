package me.tagavari.airmessage.compositeplugin;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import me.tagavari.airmessage.composite.AppCompatActivityPlugin;

public class PluginQNavigation extends AppCompatActivityPlugin {
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Ignoring if the system version is below Q
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return;
		
		//Configuring the window
		WindowCompat.setDecorFitsSystemWindows(getActivity().getWindow(), false);
	}
	
	public static void setViewForInsets(View rootView, View... listViews) {
		//Ignoring if the system version is below Q
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return;
		
		//Adding an inset listener
		ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
			Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
			
			for(View view : listViews) {
				((ViewGroup.MarginLayoutParams) view.getLayoutParams()).bottomMargin = -insets.bottom;
				view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), insets.bottom);
			}
			
			return windowInsets;
		});
	}
}