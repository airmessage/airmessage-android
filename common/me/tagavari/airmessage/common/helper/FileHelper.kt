package me.tagavari.airmessage.common.helper

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import me.tagavari.airmessage.common.constants.MIMEConstants
import me.tagavari.airmessage.common.enums.AttachmentType
import me.tagavari.airmessage.flavor.CrashlyticsBridge
import java.io.File

object FileHelper {
	fun findFreeFile(directory: File, splitFileExtension: Boolean): File {
		return findFreeFile(directory, "", splitFileExtension, "", 0)
	}
	
	/**
	 * Finds a free file in the specified directory based on the file name by appending a counter to the end, increasing it until a suitable option is found
	 * @param directory the directory to find a file in
	 * @param fileName the name of the file
	 * @param splitFileExtension if the counter should be placed between the file's name and the file's extension
	 * @param separator a string of characters to place between the file's name and the file's counter
	 * @param startIndex the number to start the counter at
	 * @return the first available file found
	 */
	@JvmStatic
	@JvmOverloads
	fun findFreeFile(directory: File, fileName: String, splitFileExtension: Boolean, separator: String = "_", startIndex: Int = 0): File {
		//Creating the default file
		var startIndex = startIndex
		var file = File(directory, if(fileName.isEmpty()) separator + startIndex else fileName)
		
		//Checking if the file directory doesn't exist
		if(!directory.exists()) {
			//Creating the directory
			directory.mkdir()
			
			//Returning the file
			return file
		}
		
		//Returning the file if it doesn't exist
		if(!file.exists()) return file
		if(splitFileExtension) {
			//Getting the file name and extension
			val fileData = fileName.split("\\.(?=[^.]+$)".toRegex()).toTypedArray()
			val baseFileName = fileData[0]
			val fileExtension = if(fileData.size > 1) fileData[1] else ""
			
			//Finding the first free file
			do {
				file = File(directory, baseFileName + separator + startIndex++ + '.' + fileExtension)
			} while(file.exists())
		} else {
			//Finding the first free file
			do {
				file = File(directory, fileName + separator + startIndex++)
			} while(file.exists())
		}
		
		//Returning the file
		return file
	}
	
	/**
	 * Gets the content MIME type of a URI
	 */
	@JvmStatic
	fun getMimeType(context: Context, uri: Uri): String {
		return StringHelper.defaultEmptyString(context.contentResolver.getType(uri), MIMEConstants.defaultMIMEType)
	}
	
	/**
	 * Gets the content MIME type of a file
	 */
	@JvmStatic
	fun getMimeType(file: File): String {
		val extension = fileExtensionFromURL(file.path)
		return if(extension != null) {
			MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: MIMEConstants.defaultMIMEType
		} else MIMEConstants.defaultMIMEType
	}
	
	/**
	 * Gets the file extension from a URL
	 */
	@JvmStatic
	fun fileExtensionFromURL(url: String): String? {
		val separatorIndex = url.lastIndexOf(".")
		val queryIndex = url.lastIndexOf("?")
		return when {
			separatorIndex == -1 -> null
			separatorIndex < queryIndex -> url.substring(separatorIndex + 1, queryIndex)
			else -> url.substring(separatorIndex + 1)
		}
	}
	
	/**
	 * Compares two mime types to check if they are equivalent
	 */
	@JvmStatic
	fun compareMimeTypes(one: String?, two: String?): Boolean {
		if(one == null || two == null) return false
		if(one == "*/*" || two == "*/*") return true
		if(!one.contains("/") || !two.contains("/")) {
			CrashlyticsBridge.recordException(IllegalArgumentException("Couldn't compare MIME types. Attempting to compare $one and $two"))
			return false
		}
		val oneComponents = one.split("/".toRegex()).toTypedArray()
		val twoComponents = two.split("/".toRegex()).toTypedArray()
		return if(oneComponents[1] == "*" || twoComponents[1] == "*") {
			oneComponents[0] == twoComponents[0]
		} else {
			one == two
		}
	}
	
	/**
	 * Converts a MIME type to an [AttachmentType]
	 */
	@JvmStatic
	@AttachmentType
	fun getAttachmentType(type: String?): Int {
		return if(type == null) AttachmentType.document
		else when {
			compareMimeTypes(type, MIMEConstants.mimeTypeImage) || compareMimeTypes(type, MIMEConstants.mimeTypeVideo) -> AttachmentType.media
			compareMimeTypes(type, MIMEConstants.mimeTypeAudio) -> AttachmentType.audio
			compareMimeTypes(type, MIMEConstants.mimeTypeVCard) -> AttachmentType.contact
			compareMimeTypes(type, MIMEConstants.mimeTypeVLocation) -> AttachmentType.location
			else -> AttachmentType.document
		}
	}
	
	/**
	 * Gets if a file's MIME type should be recognized as a document file
	 */
	@JvmStatic
	fun isAttachmentDocument(type: String): Boolean {
		return listOf(MIMEConstants.mimeTypeImage,
				MIMEConstants.mimeTypeVideo,
				MIMEConstants.mimeTypeAudio,
				MIMEConstants.mimeTypeVLocation,
				MIMEConstants.mimeTypeVCard)
				.none { compareMimeTypes(type, it) }
	}
	
	/**
	 * Removes common illegal characters from a file name
	 */
	@JvmStatic
	fun cleanFileName(fileName: String): String {
		return fileName.replace('\u0000', '?').replace('/', '-')
	}
}