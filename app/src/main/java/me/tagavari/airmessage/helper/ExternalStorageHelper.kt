package me.tagavari.airmessage.helper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import me.tagavari.airmessage.service.FileExportService
import me.tagavari.airmessage.service.UriExportService
import java.io.File

object ExternalStorageHelper {
	/**
	 * Prompts the user to select a location to save a file to disk
	 * @param activity The activity to launch the intent from
	 * @param requestCode The request code for the result
	 * @param mimeType The MIME type of the file
	 * @param fileName The name of the file
	 */
	@JvmStatic
	fun createFileSAF(activity: Activity, requestCode: Int, mimeType: String?, fileName: String?) {
		val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
			// Filter to only show results that can be "opened", such as
			// a file (as opposed to a list of contacts or timezones).
			addCategory(Intent.CATEGORY_OPENABLE)
			
			// Create a file with the requested MIME type.
			type = mimeType
			putExtra(Intent.EXTRA_TITLE, fileName)
		}
		activity.startActivityForResult(intent, requestCode)
	}
	
	/**
	 * Exports a file to the app's public directory in a user-friendly way
	 */
	fun exportFile(context: Context, file: File) {
		val intent = Intent(context, FileExportService::class.java).apply {
			putExtra(FileExportService.intentParamPath, file.path)
		}
		context.startService(intent)
	}
	
	/**
	 * Exports a file to the specified URI in a user-friendly way
	 */
	@JvmStatic
	fun exportFile(context: Context, sourceFile: File?, targetUri: Uri?) {
		val intent = Intent(context, UriExportService::class.java).apply {
			putExtra(UriExportService.intentParamPath, sourceFile)
			putExtra(UriExportService.intentParamDestination, targetUri)
		}
		context.startService(intent)
	}
	
	/**
	 * Writes a string of text to the specified URI in a user-friendly way
	 */
	@JvmStatic
	fun exportText(context: Context, sourceText: String?, targetUri: Uri?) {
		val intent = Intent(context, UriExportService::class.java).apply {
			putExtra(UriExportService.intentParamText, sourceText)
			putExtra(UriExportService.intentParamDestination, targetUri)
		}
		context.startService(intent)
	}
}