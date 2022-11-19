package me.tagavari.airmessage.common.helper

import android.content.Context
import android.content.Intent
import android.net.Uri
import me.tagavari.airmessage.service.UriExportService
import java.io.File

object ExternalStorageHelper {
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