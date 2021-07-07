package me.tagavari.airmessage.util

import android.net.Uri
import android.text.style.URLSpan
import android.view.View
import android.widget.TextView
import me.tagavari.airmessage.helper.IntentHelper.launchCustomTabs

class CustomTabsURLSpan(url: String?) : URLSpan(url) {
	override fun onClick(widget: View) {
		//Ignoring if the widget has disabled link clicking
		if(!(widget as TextView).linksClickable) return
		
		//Launching the custom tab
		launchCustomTabs(widget.getContext(), Uri.parse(url))
	}
}