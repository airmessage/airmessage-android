package me.tagavari.airmessage.helper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;

import me.tagavari.airmessage.service.FileExportService;
import me.tagavari.airmessage.service.UriExportService;

public class ExternalStorageHelper {
	/**
	 * Prompts the user to select a location to save a file to disk
	 * @param activity The activity to launch the intent from
	 * @param requestCode The request code for the result
	 * @param mimeType The MIME type of the file
	 * @param fileName The name of the file
	 */
	public static void createFileSAF(Activity activity, int requestCode, String mimeType, String fileName) {
		Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
		
		// Filter to only show results that can be "opened", such as
		// a file (as opposed to a list of contacts or timezones).
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		
		// Create a file with the requested MIME type.
		intent.setType(mimeType);
		intent.putExtra(Intent.EXTRA_TITLE, fileName);
		activity.startActivityForResult(intent, requestCode);
	}
	
	/**
	 * Exports a file to the app's public directory in a user-friendly way
	 */
	public static void exportFile(Context context, File file) {
		Intent intent = new Intent(context, FileExportService.class);
		intent.putExtra(FileExportService.intentParamPath, file.getPath());
		context.startService(intent);
	}
	
	/**
	 * Exports a file to the specified URI in a user-friendly way
	 */
	public static void exportFile(Context context, File sourceFile, Uri targetUri) {
		Intent intent = new Intent(context, UriExportService.class);
		intent.putExtra(UriExportService.intentParamPath, sourceFile);
		intent.putExtra(UriExportService.intentParamDestination, targetUri);
		context.startService(intent);
	}
	
	/**
	 * Writes a string of text to the specified URI in a user-friendly way
	 */
	public static void exportText(Context context, String sourceText, Uri targetUri) {
		Intent intent = new Intent(context, UriExportService.class);
		intent.putExtra(UriExportService.intentParamText, sourceText);
		intent.putExtra(UriExportService.intentParamDestination, targetUri);
		context.startService(intent);
	}
}