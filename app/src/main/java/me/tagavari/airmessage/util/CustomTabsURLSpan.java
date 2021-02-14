package me.tagavari.airmessage.util;

import android.net.Uri;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.TextView;

import me.tagavari.airmessage.helper.IntentHelper;

public class CustomTabsURLSpan extends URLSpan {
	public CustomTabsURLSpan(String url) {
		super(url);
	}
	
	@Override
	public void onClick(View widget) {
		//Ignoring if the widget has disabled link clicking
		if(!((TextView) widget).getLinksClickable()) return;
		
		//Launching the custom tab
		IntentHelper.launchCustomTabs(widget.getContext(), Uri.parse(getURL()));
	}
}