package me.tagavari.airmessage.helper;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.FileProvider;

import java.io.File;

import me.tagavari.airmessage.R;

public class IntentHelper {
	/**
	 * Opens an attachment file in the system's default file viewer
	 * @param context The context to use
	 * @param file The local file to open
	 * @param fileType The MIME type of the local file
	 */
	public static void openAttachmentFile(Context context, File file, String fileType) {
		//Returning if there is no content
		if(file == null) return;
		
		//Creating a content URI
		Uri content = FileProvider.getUriForFile(context, AttachmentStorageHelper.getFileAuthority(context), file);
		
		//Launching the content viewer
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		intent.setDataAndType(content, fileType);
		intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		try {
			context.startActivity(intent);
		} catch(ActivityNotFoundException exception) {
			Toast.makeText(context, R.string.message_intenterror_open, Toast.LENGTH_SHORT).show();
		}
	}
	
	/**
	 * Launches the provided URI
	 */
	public static void launchUri(Context context, Uri uri) {
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		
		try {
			context.startActivity(intent);
		} catch(ActivityNotFoundException exception) {
			Toast.makeText(context, R.string.message_intenterror_browser, Toast.LENGTH_SHORT).show();
		}
	}
	
	/**
	 * Launches the provided URI in a custom tab
	 */
	public static void launchCustomTabs(Context context, Uri uri) {
		//Getting the color scheme
		int colorScheme;
		switch(AppCompatDelegate.getDefaultNightMode()) {
			case AppCompatDelegate.MODE_NIGHT_NO:
				colorScheme = CustomTabsIntent.COLOR_SCHEME_LIGHT;
				break;
			case AppCompatDelegate.MODE_NIGHT_YES:
				colorScheme = CustomTabsIntent.COLOR_SCHEME_DARK;
				break;
			default:
				colorScheme = CustomTabsIntent.COLOR_SCHEME_SYSTEM;
				break;
		}
		
		//Launching the custom tab
		new CustomTabsIntent.Builder()
				.setColorScheme(colorScheme)
				.build()
				.launchUrl(context, uri);
	}
}