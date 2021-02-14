package me.tagavari.airmessage.util;

import android.graphics.Rect;
import android.text.Spannable;
import android.text.Spanned;
import android.text.method.TransformationMethod;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.TextView;

public class CustomTabsLinkTransformationMethod implements TransformationMethod {
	@Override
	public CharSequence getTransformation(CharSequence source, View view) {
		if(view instanceof TextView) {
			TextView textView = (TextView) view;
			if(textView.getText() == null || !(textView.getText() instanceof Spannable)) {
				return source;
			}
			Spannable text = (Spannable) textView.getText();
			URLSpan[] spans = text.getSpans(0, textView.length(), URLSpan.class);
			for(int i = spans.length - 1; i >= 0; i--) {
				URLSpan oldSpan = spans[i];
				int start = text.getSpanStart(oldSpan);
				int end = text.getSpanEnd(oldSpan);
				String url = oldSpan.getURL();
				text.removeSpan(oldSpan);
				text.setSpan(new CustomTabsURLSpan(url), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			return text;
		}
		return source;
	}
	
	@Override
	public void onFocusChanged(View view, CharSequence sourceText, boolean focused, int direction, Rect previouslyFocusedRect) {
	
	}
}