package me.tagavari.airmessage.helper;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import java.io.File;

public class MediaFileHelper {
	/**
	 * Retrieves the duration of a media file (ie. audio)
	 * @param file the location of the file to check
	 * @return the duration of the media file in milliseconds
	 */
	public static long getMediaDuration(File file) {
		//Creating a new media metadata retriever
		MediaMetadataRetriever mmr = new MediaMetadataRetriever();
		
		try {
			//Setting the source file
			mmr.setDataSource(file.getPath());
			
			//Getting the duration
			return Long.parseLong(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
		} catch(RuntimeException exception) {
			//Printing the stack trace
			exception.printStackTrace();
		} finally {
			//Releasing the media metadata retriever
			mmr.release();
		}
		
		//Returning an invalid value
		return -1;
	}
	
	/**
	 * Retrieves the duration of a media file (ie. audio)
	 * @param context context to use to resolve the URI
	 * @param uri the uri of the file to check
	 * @return the duration of the media file in milliseconds
	 */
	public static long getMediaDuration(Context context, Uri uri) {
		//Creating a new media metadata retriever
		MediaMetadataRetriever mmr = new MediaMetadataRetriever();
		
		try {
			//Setting the source file
			mmr.setDataSource(context, uri);
			
			//Getting the duration
			return Long.parseLong(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
		} catch(RuntimeException exception) {
			//Printing the stack trace
			exception.printStackTrace();
		} finally {
			//Releasing the media metadata retriever
			mmr.release();
		}
		
		//Returning an invalid value
		return -1;
	}
}