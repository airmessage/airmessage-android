package me.tagavari.airmessage.common.helper

import android.content.Context
import android.os.Build
import java.io.File

/**
 * Helper class for managing the organization of attachment files
 */
object AttachmentStorageHelper {
	private const val dirLayoutIsolated = 0 //Each file gets its own folder
	private const val dirLayoutFlat = 1 //All files share the same folder, collision-proof
	private const val dirLayoutDirect = 2 //All files share the same folder, overwrites allowed
	private val directories = listOf("attachments", "stickers", "draft", "draftprepare")
	const val dirNameAttachment = "attachments"
	const val dirNameSticker = "stickers"
	const val dirNameDraft = "draft"
	const val dirNameDraftPrepare = "draftprepare"
	
	/**
	 * Get the file authority of this app, useful for URI intents
	 * @param context The context to use
	 * @return The file authority
	 */
	@JvmStatic
	fun getFileAuthority(context: Context): String {
		return context.packageName + ".fileprovider"
	}
	
	/**
	 * Get a writable file to place a content file in
	 * @param context The context to use
	 * @param directory The ID of the directory to use
	 * @param fileName The name of the file
	 * @return The file to write to
	 */
	@JvmStatic
	fun prepareContentFile(context: Context, directory: String, fileName: String): File {
		val dir = getSubDirectory(context, directory)
		return when(getDirectoryLayout(directory)) {
			dirLayoutIsolated -> getFileTarget(dir, fileName)
			dirLayoutFlat -> FileHelper.findFreeFile(dir, fileName, true)
			dirLayoutDirect -> File(dir, fileName)
			else -> throw IllegalStateException("Unknown directory type " + getDirectoryLayout(directory))
		}
	}
	
	/**
	 * Deletes a file
	 * @param directoryID The ID of the directory of the file
	 * @param file The file to delete
	 * @return Whether the file was deleted
	 */
	@JvmStatic
	fun deleteContentFile(directoryID: String, file: File): Boolean {
		return if(getDirectoryLayout(directoryID) == dirLayoutIsolated) {
			//Delete the file and its parent folder
			(!file.exists() || file.delete())
					&& file.parentFile!!.let { !it.exists() || it.delete() }
		} else {
			//Just delete the file
			!file.exists() || file.delete()
		}
	}
	
	/**
	 * Gets the relative path of an attachment file
	 * @param context The context to use
	 * @param file The file
	 * @return The relative path of the file
	 */
	@JvmStatic
	fun getRelativePath(context: Context, file: File): String {
		return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			getAttachmentDirectory(context).toPath().relativize(file.toPath()).toString()
		} else {
			file.absolutePath.substring((getAttachmentDirectory(context).absolutePath + "/").length)
		}
	}
	
	/**
	 * Gets the absolute path of an attachment file
	 * @param context The context to use
	 * @param path The relative path of the file
	 * @return The file
	 */
	@JvmStatic
	fun getAbsolutePath(context: Context, path: String): File {
		return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			getAttachmentDirectory(context).toPath().resolve(path).toFile()
		} else {
			File(getAttachmentDirectory(context), path)
		}
	}
	
	/**
	 * Gets if this is a valid directory ID
	 * @param directory The directory ID to check
	 * @return Whether or not this directory ID is valid
	 */
	private fun validateDirectoryID(directory: String) = directories.contains(directory)
	
	/**
	 * Gets the layout type of this directory
	 * @param directory The directory to check
	 * @return The layout type of this directory
	 */
	private fun getDirectoryLayout(directory: String): Int {
		return if(directory == dirNameSticker) {
			dirLayoutDirect
		} else {
			dirLayoutIsolated
		}
	}
	
	/**
	 * Gets if this directory is a cache directory
	 */
	private fun isDirectoryCache(directory: String) = directory == dirNameDraftPrepare
	
	private fun getAttachmentDirectory(context: Context): File {
		//Getting the media directory
		val file = File(context.filesDir, "attachments")
		
		//Preparing the media directory
		prepareDirectory(file)
		
		//Returning the media directory
		return file
	}
	
	private fun getCacheDirectory(context: Context): File {
		//Getting the cache directory
		val file = context.cacheDir
		
		//Preparing the media directory
		prepareDirectory(file)
		
		//Returning the media directory
		return file
	}
	
	private fun getSubDirectory(context: Context, name: String): File {
		require(validateDirectoryID(name)) { "Unknown directory $name" }
		val parentDir = if(isDirectoryCache(name)) {
			getCacheDirectory(context)
		} else {
			getAttachmentDirectory(context)
		}
		
		val file = File(parentDir, name)
		prepareDirectory(file)
		return file
	}
	
	private fun getFileTarget(parentDir: File, fileName: String): File {
		val directory = FileHelper.findFreeFile(parentDir, System.currentTimeMillis().toString(), false)
		prepareDirectory(directory)
		return File(directory, fileName)
	}
	
	private fun prepareDirectory(file: File): Boolean {
		//Creating the directory if the file doesn't exist
		if(!file.exists()) return file.mkdir()
		
		//Checking if the path is a file
		if(file.isFile) {
			//Deleting the file
			if(!file.delete()) return false
			
			//Creating the directory
			return file.mkdir()
		}
		
		//Returning true
		return true
	}
}