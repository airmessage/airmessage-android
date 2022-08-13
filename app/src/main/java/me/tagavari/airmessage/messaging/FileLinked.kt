package me.tagavari.airmessage.messaging

import android.content.Context
import android.net.Uri
import me.tagavari.airmessage.util.Union
import java.io.File

/**
 * Represents a reference to a file that can be loaded as a draft
 */
@Deprecated("Use QueuedFile instead")
class FileLinked @JvmOverloads constructor(
	val file: Union<File, Uri>,
	val fileName: String,
	val fileSize: Long,
	val fileType: String,
	val metadata: FileDisplayMetadata? = null,
	val mediaStoreData: MediaStore? = null
) {
	
	override fun equals(other: Any?): Boolean {
		if(other == null || other.javaClass != this.javaClass) return false
		other as FileLinked
		
		//If we don't have any mediastore data, we can't compare
		if(mediaStoreData == null || other.mediaStoreData == null) return false
		
		//Compare mediastore data
		return (mediaStoreData.mediaStoreID == other.mediaStoreData.mediaStoreID ||
				fileType == other.fileType && mediaStoreData.modificationDate == other.mediaStoreData.modificationDate)
	}
	
	data class MediaStore(val mediaStoreID: Long, val modificationDate: Long)
}