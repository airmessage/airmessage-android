package me.tagavari.airmessage.common.compose.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Find the closest Activity in a given Context.
 */
fun Context.findActivity(): Activity {
	var context = this
	while(context is ContextWrapper) {
		if(context is Activity) return context
		context = context.baseContext
	}
	
	throw IllegalStateException("Should be called in the context of an Activity")
}
