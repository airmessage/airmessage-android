package me.tagavari.airmessage.common.util

import android.net.Uri
import android.text.style.URLSpan
import android.view.View
import android.widget.TextView
import me.tagavari.airmessage.common.helper.IntentHelper

class CustomTabsURLSpan(url: String?) : URLSpan(url) {
	override fun onClick(widget: View) {
		//Ignoring if the widget has disabled link clicking
		if(!(widget as TextView).linksClickable) return
		
		//Launching the custom tab
		IntentHelper.launchCustomTabs(widget.getContext(), Uri.parse(url))
	}
}