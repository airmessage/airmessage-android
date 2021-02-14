package me.tagavari.airmessage.util;

import android.graphics.Insets;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsAnimation;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.R)
public class AnimatingInsetsCallback extends WindowInsetsAnimation.Callback implements View.OnApplyWindowInsetsListener {
	private final View view;
	
	private boolean isAnimating = false;
	
	public AnimatingInsetsCallback(View view) {
		super(DISPATCH_MODE_CONTINUE_ON_SUBTREE);
		
		this.view = view;
	}
	
	@Override
	public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
		if(isAnimating) return windowInsets;
		Insets insets = windowInsets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
		view.setPadding(insets.left, view.getPaddingTop(), insets.right, insets.bottom);
		
		return WindowInsets.CONSUMED;
	}
	
	@NonNull
	@Override
	public WindowInsets onProgress(@NonNull WindowInsets windowInsets, @NonNull List<WindowInsetsAnimation> list) {
		view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), windowInsets.getInsets(WindowInsets.Type.ime() | WindowInsets.Type.systemBars()).bottom);
		return WindowInsets.CONSUMED;
	}
	
	@Override
	public void onPrepare(@NonNull WindowInsetsAnimation animation) {
		isAnimating = true;
	}
	
	@Override
	public void onEnd(@NonNull WindowInsetsAnimation animation) {
		isAnimating = false;
	}
}
