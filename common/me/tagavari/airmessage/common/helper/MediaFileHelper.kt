package me.tagavari.airmessage.common.helper

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File

object MediaFileHelper {
	/**
	 * Retrieves the duration of a media file (ie. audio)
	 * @param file the location of the file to check
	 * @return the duration of the media file in milliseconds
	 */
	@JvmStatic
	fun getMediaDuration(file: File): Long {
		//Creating a new media metadata retriever
		val mmr = MediaMetadataRetriever()
		try {
			//Setting the source file
			mmr.setDataSource(file.path)
			
			//Getting the duration
			return mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: -1L
		} catch(exception: RuntimeException) {
			//Printing the stack trace
			exception.printStackTrace()
			
			//Returning an invalid value
			return -1
		} finally {
			//Releasing the media metadata retriever
			mmr.release()
		}
	}
	
	/**
	 * Retrieves the duration of a media file (ie. audio)
	 * @param context context to use to resolve the URI
	 * @param uri the uri of the file to check
	 * @return the duration of the media file in milliseconds
	 */
	@JvmStatic
	fun getMediaDuration(context: Context, uri: Uri): Long {
		//Creating a new media metadata retriever
		val mmr = MediaMetadataRetriever()
		try {
			//Setting the source file
			mmr.setDataSource(context, uri)
			
			//Getting the duration
			return mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: -1L
		} catch(exception: RuntimeException) {
			//Printing the stack trace
			exception.printStackTrace()
			
			//Returning an invalid value
			return -1
		} finally {
			//Releasing the media metadata retriever
			mmr.release()
		}
	}
}