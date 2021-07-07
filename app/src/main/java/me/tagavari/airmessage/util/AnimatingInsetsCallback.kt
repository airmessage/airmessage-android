package me.tagavari.airmessage.util

import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import android.view.WindowInsetsAnimation
import android.view.WindowInsets

@RequiresApi(api = Build.VERSION_CODES.R)
class AnimatingInsetsCallback(private val view: View) : WindowInsetsAnimation.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE), View.OnApplyWindowInsetsListener {
	private var isAnimating = false
	
	override fun onApplyWindowInsets(view: View, windowInsets: WindowInsets): WindowInsets {
		if(isAnimating) return windowInsets
		val insets = windowInsets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.ime())
		view.setPadding(insets.left, view.paddingTop, insets.right, insets.bottom)
		
		return WindowInsets.CONSUMED
	}
	
	override fun onProgress(windowInsets: WindowInsets, list: List<WindowInsetsAnimation>): WindowInsets {
		view.setPadding(
			view.paddingLeft,
			view.paddingTop,
			view.paddingRight,
			windowInsets.getInsets(WindowInsets.Type.ime() or WindowInsets.Type.systemBars()).bottom
		)
		
		return WindowInsets.CONSUMED
	}
	
	override fun onPrepare(animation: WindowInsetsAnimation) {
		isAnimating = true
	}
	
	override fun onEnd(animation: WindowInsetsAnimation) {
		isAnimating = false
	}
}