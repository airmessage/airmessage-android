package me.tagavari.airmessage.helper

import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.children
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import me.tagavari.airmessage.R

object ViewHelper {
	/**
	 * Gets all child views with the specified tag
	 * @param root The root view to start crawling at
	 * @param tag The view tag to check for
	 */
	@JvmStatic
	fun getViewsByTag(root: ViewGroup, tag: String): List<View> {
		val views = mutableListOf<View>()
		for(child in root.children) {
			if(child is ViewGroup) {
				views.addAll(getViewsByTag(child, tag))
			}
			
			val tagObj = child.tag
			if(tagObj != null && tagObj == tag) {
				views.add(child)
			}
		}
		
		return views
	}
	
	/**
	 * Colors all view with the tag @string/tag_primarytint the specified color
	 */
	@JvmStatic
	fun colorTaggedUI(resources: Resources, viewRoot: ViewGroup, color: Int) {
		for(view in getViewsByTag(viewRoot, resources.getString(R.string.tag_primarytint))) {
			when(view) {
				is ImageView -> view.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
				is SwitchMaterial -> {
					view.thumbTintList = ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_checked), intArrayOf(android.R.attr.state_checked)), intArrayOf(-0x50506, color))
					view.trackTintList = ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_checked), intArrayOf(android.R.attr.state_checked)), intArrayOf(0x61000000, color))
				}
				is MaterialButton -> {
					view.setTextColor(color)
					view.iconTint = ColorStateList.valueOf(color)
					view.rippleColor = ColorStateList.valueOf(color)
				}
				is TextView -> view.setTextColor(color)
				is RelativeLayout -> view.setBackground(ColorDrawable(color))
				is FrameLayout -> view.backgroundTintList = ColorStateList.valueOf(color)
			}
		}
	}
}