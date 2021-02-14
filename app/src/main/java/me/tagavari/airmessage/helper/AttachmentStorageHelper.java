package me.tagavari.airmessage.helper;

import android.content.Context;
import android.net.Uri;
import android.os.Build;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Helper class for managing the organization of attachment files
 */
public class AttachmentStorageHelper {
	private static final int dirLayoutIsolated = 0; //Each file gets its own folder
	private static final int dirLayoutFlat = 1; //All files share the same folder, collision-proof
	private static final int dirLayoutDirect = 2; //All files share the same folder, overwrites allowed
	
	private static final String[] directories = new String[]{"attachments", "stickers", "draft", "draftprepare"};
	public static final String dirNameAttachment = "attachments";
	public static final String dirNameSticker = "stickers";
	public static final String dirNameDraft = "draft";
	public static final String dirNameDraftPrepare = "draftprepare";
	
	/**
	 * Get the file authority of this app, useful for URI intents
	 * @param context The context to use
	 * @return The file authority
	 */
	public static String getFileAuthority(Context context) {
		return context.getPackageName() + ".fileprovider";
	}
	
	/**
	 * Get a writable file to place a content file in
	 * @param context The context to use
	 * @param directory The ID of the directory to use
	 * @param fileName The name of the file
	 * @return The file to write to
	 */
	public static File prepareContentFile(Context context, String directory, String fileName) {
		File dir = getSubDirectory(context, directory);
		
		switch(getDirectoryLayout(directory)) {
			case dirLayoutIsolated:
				return getFileTarget(dir, fileName);
			case dirLayoutFlat:
				return FileHelper.findFreeFile(dir, fileName, true);
			case dirLayoutDirect:
				return new File(dir, fileName);
			default:
				throw new IllegalStateException("Unknown directory type " + getDirectoryLayout(directory));
		}
	}
	
	/**
	 * Deletes a file
	 * @param directoryID The ID of the directory of the file
	 * @param file The file to delete
	 * @return Whether the file was deleted
	 */
	public static boolean deleteContentFile(String directoryID, File file) {
		if(getDirectoryLayout(directoryID) == dirLayoutIsolated) {
			//Delete the file and its parent folder
			return file.delete() && file.getParentFile().delete();
		} else {
			//Just delete the file
			return file.delete();
		}
	}
	
	/**
	 * Gets the relative path of an attachment file
	 * @param context The context to use
	 * @param file The file
	 * @return The relative path of the file
	 */
	public static String getRelativePath(Context context, File file) {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			return getAttachmentDirectory(context).toPath().relativize(file.toPath()).toString();
		} else {
			return file.getAbsolutePath().substring((getAttachmentDirectory(context).getAbsolutePath() + "/").length());
		}
	}
	
	/**
	 * Gets the absolute path of an attachment file
	 * @param context The context to use
	 * @param path The relative path of the file
	 * @return The file
	 */
	public static File getAbsolutePath(Context context, String path) {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			return getAttachmentDirectory(context).toPath().resolve(path).toFile();
		} else {
			return new File(getAttachmentDirectory(context), path);
		}
	}
	
	/**
	 * Gets if this is a valid directory ID
	 * @param directory The directory ID to check
	 * @return Whether or not this directory ID is valid
	 */
	public static boolean validateDirectoryID(String directory) {
		return Arrays.asList(directories).contains(directory);
	}
	
	/**
	 * Gets the layout type of this directory
	 * @param directory The directory to check
	 * @return The layout type of this directory
	 */
	private static int getDirectoryLayout(String directory) {
		if(directory.equals(dirNameSticker)) {
			return dirLayoutDirect;
		} else {
			return dirLayoutIsolated;
		}
	}
	
	/**
	 * Gets if this directory is a cache directory
	 */
	private static boolean isDirectoryCache(String directory) {
		return directory.equals(dirNameDraftPrepare);
	}
	
	private static File getAttachmentDirectory(Context context) {
		//Getting the media directory
		File file = new File(context.getFilesDir(), "attachments");
		
		//Preparing the media directory
		prepareDirectory(file);
		
		//Returning the media directory
		return file;
	}
	
	private static File getCacheDirectory(Context context) {
		//Getting the cache directory
		File file = context.getCacheDir();
		
		//Preparing the media directory
		prepareDirectory(file);
		
		//Returning the media directory
		return file;
	}
	
	private static File getSubDirectory(Context context, String name) {
		if(!validateDirectoryID(name)) throw new IllegalArgumentException("Unknown directory " + name);
		
		File parentDir;
		if(isDirectoryCache(name)) {
			parentDir = getCacheDirectory(context);
		} else {
			parentDir = getAttachmentDirectory(context);
		}
		
		File file = new File(parentDir, name);
		prepareDirectory(file);
		return file;
	}
	
	private static File getFileTarget(File parentDir, String fileName) {
		File directory = FileHelper.findFreeFile(parentDir, Long.toString(System.currentTimeMillis()), false);
		prepareDirectory(directory);
		return new File(directory, fileName);
	}
	
	private static boolean prepareDirectory(File file) {
		//Creating the directory if the file doesn't exist
		if(!file.exists()) return file.mkdir();
		
		//Checking if the path is a file
		if(file.isFile()) {
			//Deleting the file
			if(!file.delete()) return false;
			
			//Creating the directory
			return file.mkdir();
		}
		
		//Returning true
		return true;
	}
}