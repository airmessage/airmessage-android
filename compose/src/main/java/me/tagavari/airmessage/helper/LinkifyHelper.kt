package me.tagavari.airmessage.helper

import android.content.Context
import android.os.Build
import android.text.SpannableString
import android.text.util.Linkify
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextLinks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LinkifyHelper {
	/**
	 * Recognizes links in text, using the most optimized solution for the system
	 */
	suspend fun linkifyText(context: Context, text: String): SpannableString {
		val spannable = SpannableString(text)
		
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			//Smart Linkify is preferred on Android 9+
			withContext(Dispatchers.IO) {
				val textClassifier = context.getSystemService(TextClassificationManager::class.java).textClassifier
				val textLinks = textClassifier.generateLinks(TextLinks.Request.Builder(text).build())
				textLinks.apply(spannable, TextLinks.APPLY_STRATEGY_REPLACE, null)
			}
		} else {
			//Use regular ol' Linkify
			Linkify.addLinks(
				spannable,
				Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES or Linkify.PHONE_NUMBERS
			)
		}
		
		return spannable
	}
}