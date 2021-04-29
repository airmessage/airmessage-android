package me.tagavari.airmessage.helper

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.FileProvider
import me.tagavari.airmessage.R
import me.tagavari.airmessage.helper.AttachmentStorageHelper.getFileAuthority
import java.io.File

object IntentHelper {
	/**
	 * Opens an attachment file in the system's default file viewer
	 * @param context The context to use
	 * @param file The local file to open
	 * @param fileType The MIME type of the local file
	 */
	@JvmStatic
	fun openAttachmentFile(context: Context, file: File, fileType: String?) {
		//Creating a content URI
		val content = FileProvider.getUriForFile(context, getFileAuthority(context), file)
		
		val intent = Intent().apply {
			action = Intent.ACTION_VIEW
			setDataAndType(content, fileType)
			flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
		}
		
		try {
			//Launching the content viewer
			context.startActivity(intent)
		} catch(exception: ActivityNotFoundException) {
			Toast.makeText(context, R.string.message_intenterror_open, Toast.LENGTH_SHORT).show()
		}
	}
	
	/**
	 * Launches the provided URI
	 */
	@JvmStatic
	fun launchUri(context: Context, uri: Uri) {
		val intent = Intent(Intent.ACTION_VIEW, uri)
		try {
			context.startActivity(intent)
		} catch(exception: ActivityNotFoundException) {
			Toast.makeText(context, R.string.message_intenterror_browser, Toast.LENGTH_SHORT).show()
		}
	}
	
	/**
	 * Launches the provided URI in a custom tab
	 */
	@JvmStatic
	fun launchCustomTabs(context: Context, uri: Uri) {
		//Getting the color scheme
		val colorScheme = when(AppCompatDelegate.getDefaultNightMode()) {
			AppCompatDelegate.MODE_NIGHT_NO -> CustomTabsIntent.COLOR_SCHEME_LIGHT
			AppCompatDelegate.MODE_NIGHT_YES -> CustomTabsIntent.COLOR_SCHEME_DARK
			else -> CustomTabsIntent.COLOR_SCHEME_SYSTEM
		}
		
		//Launching the custom tab
		CustomTabsIntent.Builder()
				.setColorScheme(colorScheme)
				.build()
				.launchUrl(context, uri)
	}
}