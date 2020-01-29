package me.tagavari.airmessage.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Build;
import android.os.FileUtils;

import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DataTransformUtils {
	public static final int standardBuffer = 8192; //8 kB
	
	private static final int bitmapQuality = 90; //90%
	private static final List<String> compressableTypes = Collections.unmodifiableList(Arrays.asList("image/jpeg", "image/webp", "image/png"));
	
	public static boolean isCompressable(String mimeType) {
		return compressableTypes.contains(mimeType);
	}
	
	public static byte[] compressFile(byte[] fileBytes, String mimeType, int maxBytes) {
		switch(mimeType) {
			case "image/jpeg":
			case "image/webp":
				return compressBitmapLossy(convertCorrectBitmap(fileBytes), "image/jpeg".equals(mimeType) ? Bitmap.CompressFormat.JPEG : Bitmap.CompressFormat.WEBP, maxBytes);
			case "image/png":
				return compressBitmapLossless(convertCorrectBitmap(fileBytes), maxBytes);
			default:
				throw new IllegalArgumentException("Unknown MIME type: " + mimeType);
		}
	}
	
	private static Bitmap convertCorrectBitmap(byte[] fileBytes) {
		//Reading the image's EXIF data
		ExifInterface exif = null;
		try {
			exif = new ExifInterface(new ByteArrayInputStream(fileBytes));
		} catch(IOException exception) {
			//Printing the stack trace
			exception.printStackTrace();
		}
		
		//Getting the bitmap from the image
		Bitmap bitmap = BitmapFactory.decodeByteArray(fileBytes, 0, fileBytes.length);
		
		//Fixing the bitmap orientation
		if(exif != null) bitmap = DataTransformUtils.rotateBitmap(bitmap, exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL));
		
		//Returning the bitmap
		return bitmap;
	}
	
	//Based off of QKSMS's ImageUtils
	//https://github.com/moezbhatti/qksms/blob/b7f5cd2fa271efe7c419cd9dc78df57ac6e1f33c/data/src/main/java/com/moez/QKSMS/util/ImageUtils.kt
	private static byte[] compressBitmapLossy(Bitmap bitmap, Bitmap.CompressFormat compressFormat, int maxBytes) {
		//Getting the bitmap dimensions
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		
		//Creating the output stream for the bitmap
		try(ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			//Compressing the bitmap with default settings immediately
			bitmap.compress(compressFormat, bitmapQuality, outputStream);
			
			//Counting the amount of bytes without scaling
			int unscaledBytes = outputStream.size();
			
			//Further scaling the image until it fits under the max byte count
			int attempts = 0;
			int bytes = unscaledBytes;
			while(bytes > maxBytes) {
				double scale = Math.sqrt((double) maxBytes / unscaledBytes) * (1 - attempts * 0.1);
				outputStream.reset();
				Bitmap.createScaledBitmap(bitmap, (int) (width * scale), (int) (height * scale), true).compress(compressFormat, bitmapQuality, outputStream);
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
	
	private static byte[] compressBitmapLossless(Bitmap bitmap, int maxBytes) {
		//Getting the bitmap dimensions
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		
		//Creating the output stream for the bitmap
		try(ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			//Compressing the bitmap with default settings immediately
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
			
			//Counting the amount of bytes without scaling
			int unscaledBytes = outputStream.size();
			
			//Further scaling the image until it fits under the max byte count
			int attempts = 0;
			int bytes = unscaledBytes;
			while(bytes > maxBytes) {
				double scale = Math.sqrt((double) maxBytes / unscaledBytes) * (1 - attempts * 0.1);
				outputStream.reset();
				Bitmap.createScaledBitmap(bitmap, (int) (width * scale), (int) (height * scale), true).compress(Bitmap.CompressFormat.PNG, bitmapQuality, outputStream);;
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
	
	public static long copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			//Using Android Q's optimized solution
			return FileUtils.copy(inputStream, outputStream);
		} else {
			//Copying the stream manually
			byte[] buf = new byte[standardBuffer];
			int len;
			long totalLength = 0;
			while((len = inputStream.read(buf)) > 0) {
				outputStream.write(buf, 0, len);
				totalLength += len;
			}
			return totalLength;
		}
	}
}