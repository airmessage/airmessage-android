package me.tagavari.airmessage.util;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ImageUtils {
	public static final int maxBytes = 300 * 1024; //300 kilobytes
	private static final int bitmapQuality = 90; //90%
	
	//Based off of QKSMS's ImageUtils
	//https://github.com/moezbhatti/qksms/blob/b7f5cd2fa271efe7c419cd9dc78df57ac6e1f33c/data/src/main/java/com/moez/QKSMS/util/ImageUtils.kt
	public static byte[] compressBitmap(Bitmap bitmap) {
		//Getting the bitmap dimensions
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		
		//Creating the output stream for the bitmap
		try(ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			//Compressing the bitmap with default settings immediately
			bitmap.compress(Bitmap.CompressFormat.JPEG, bitmapQuality, outputStream);
			
			//Counting the amount of bytes without scaling
			int unscaledBytes = outputStream.size();
			
			//Further scaling the image until it fits under the max byte count
			int attempts = 0;
			int bytes = unscaledBytes;
			while(bytes > maxBytes) {
				double scale = Math.sqrt((double) maxBytes / unscaledBytes) * (1 - attempts * 0.1);
				outputStream.reset();
				Bitmap.createScaledBitmap(bitmap, (int) (width * scale), (int) (height * scale), true).compress(Bitmap.CompressFormat.JPEG, bitmapQuality, outputStream);
				attempts++;
				bytes = outputStream.size();
			}
			
			//Returning the bytes
			return outputStream.toByteArray();
		} catch(IOException exception) {
			exception.printStackTrace();
			return null;
		}
	}
	
	public static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
		Matrix matrix = new Matrix();
		switch (orientation) {
			case ExifInterface.ORIENTATION_NORMAL:
				return bitmap;
			case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
				matrix.setScale(-1, 1);
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				matrix.setRotate(180);
				break;
			case ExifInterface.ORIENTATION_FLIP_VERTICAL:
				matrix.setRotate(180);
				matrix.postScale(-1, 1);
				break;
			case ExifInterface.ORIENTATION_TRANSPOSE:
				matrix.setRotate(90);
				matrix.postScale(-1, 1);
				break;
			case ExifInterface.ORIENTATION_ROTATE_90:
				matrix.setRotate(90);
				break;
			case ExifInterface.ORIENTATION_TRANSVERSE:
				matrix.setRotate(-90);
				matrix.postScale(-1, 1);
				break;
			case ExifInterface.ORIENTATION_ROTATE_270:
				matrix.setRotate(-90);
				break;
			default:
				return bitmap;
		}
		try {
			Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
			bitmap.recycle();
			return bmRotated;
		}
		catch (OutOfMemoryError exception) {
			exception.printStackTrace();
			return null;
		}
	}
}