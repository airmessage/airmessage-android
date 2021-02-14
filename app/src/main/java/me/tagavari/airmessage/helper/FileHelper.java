package me.tagavari.airmessage.helper;

import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.io.File;

import me.tagavari.airmessage.constants.MIMEConstants;
import me.tagavari.airmessage.enums.AttachmentType;

public class FileHelper {
	public static File findFreeFile(File directory, boolean splitFileExtension) {
		return findFreeFile(directory, "", splitFileExtension, "", 0);
	}
	
	public static File findFreeFile(File directory, String fileName, boolean splitFileExtension) {
		return findFreeFile(directory, fileName, splitFileExtension, "_", 0);
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
	public static File findFreeFile(File directory, String fileName, boolean splitFileExtension, String separator, int startIndex) {
		//Creating the default file
		File file = new File(directory, fileName.isEmpty() ? separator + startIndex : fileName);
		
		//Checking if the file directory doesn't exist
		if(!directory.exists()) {
			//Creating the directory
			directory.mkdir();
			
			//Returning the file
			return file;
		}
		
		//Returning the file if it doesn't exist
		if(!file.exists()) return file;
		
		if(splitFileExtension) {
			//Getting the file name and extension
			String[] fileData = fileName.split("\\.(?=[^.]+$)");
			String baseFileName = fileData[0];
			String fileExtension = fileData.length > 1 ? fileData[1] : "";
			
			//Finding the first free file
			do {
				file = new File(directory, baseFileName + separator + startIndex++ + '.' + fileExtension);
			} while(file.exists());
		} else {
			//Finding the first free file
			do {
				file = new File(directory, fileName + separator + startIndex++);
			} while(file.exists());
		}
		
		//Returning the file
		return file;
	}
	
	/**
	 * Gets the content MIME type of a URI
	 */
	public static String getMimeType(Context context, Uri uri) {
		return StringHelper.defaultEmptyString(context.getContentResolver().getType(uri), MIMEConstants.defaultMIMEType);
	}
	
	/**
	 * Gets the content MIME type of a file
	 */
	public static String getMimeType(File file) {
		String extension = fileExtensionFromURL(file.getPath());
		if(extension != null) {
			String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
			return type == null ? MIMEConstants.defaultMIMEType : type;
		}
		return MIMEConstants.defaultMIMEType;
	}
	
	/**
	 * Gets the file extension from a URL
	 */
	public static String fileExtensionFromURL(String url) {
		int separatorIndex = url.lastIndexOf(".");
		int queryIndex = url.lastIndexOf("?");
		if(separatorIndex == -1) return null;
		else if(separatorIndex < queryIndex) return url.substring(separatorIndex + 1, queryIndex);
		else return url.substring(separatorIndex + 1);
	}
	
	/**
	 * Compares two mime types to check if they are equivalent
	 */
	public static boolean compareMimeTypes(String one, String two) {
		if(one.equals("*/*") || two.equals("*/*")) return true;
		if(!one.contains("/") || !two.contains("/")) {
			FirebaseCrashlytics.getInstance().recordException(new IllegalArgumentException("Couldn't compare MIME types. Attempting to compare " + one + " and " + two));
			return false;
		}
		String[] oneComponents = one.split("/");
		String[] twoComponents = two.split("/");
		if(oneComponents[1].equals("*") || twoComponents[1].equals("*")) {
			return oneComponents[0].equals(twoComponents[0]);
		}
		return one.equals(two);
	}
	
	/**
	 * Converts a MIME type to an {@link AttachmentType}
	 */
	@AttachmentType
	public static int getAttachmentType(String type) {
		if(type == null) return AttachmentType.document;
		else if(compareMimeTypes(type, MIMEConstants.mimeTypeImage) || compareMimeTypes(type, MIMEConstants.mimeTypeVideo)) return AttachmentType.media;
		else if(compareMimeTypes(type, MIMEConstants.mimeTypeAudio)) return AttachmentType.audio;
		else if(compareMimeTypes(type, MIMEConstants.mimeTypeVCard)) return AttachmentType.contact;
		else if(compareMimeTypes(type, MIMEConstants.mimeTypeVLocation)) return AttachmentType.location;
		else return AttachmentType.document;
	}
	
	/**
	 * Gets if a file's MIME type should be recognized as a document file
	 */
	public static boolean isAttachmentDocument(String type) {
		return !compareMimeTypes(type, MIMEConstants.mimeTypeImage) &&
				!compareMimeTypes(type, MIMEConstants.mimeTypeVideo) &&
				!compareMimeTypes(type, MIMEConstants.mimeTypeAudio) &&
				!compareMimeTypes(type, MIMEConstants.mimeTypeVLocation) &&
				!compareMimeTypes(type, MIMEConstants.mimeTypeVCard);
	}
	
	/**
	 * Removes common illegal characters from a file name
	 */
	public static String cleanFileName(String fileName) {
		return fileName.replace('\u0000', '?').replace('/', '-');
	}
}