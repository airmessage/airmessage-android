package me.tagavari.airmessage.helper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import androidx.exifinterface.media.ExifInterface;

import com.otaliastudios.transcoder.Transcoder;
import com.otaliastudios.transcoder.TranscoderOptions;
import com.otaliastudios.transcoder.strategy.DefaultAudioStrategy;
import com.otaliastudios.transcoder.strategy.DefaultVideoStrategy;
import com.otaliastudios.transcoder.strategy.TrackStrategy;
import com.otaliastudios.transcoder.strategy.size.AtMostResizer;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * A set of utilities for compressing standard file types
 */
public class DataCompressionHelper {
	private static final int bitmapQuality = 90; //90%
	private static final List<String> compressableTypes = Collections.unmodifiableList(Arrays.asList("image/jpeg", "image/webp", "image/png", "video/mp4"));
	
	/**
	 * Checks if compressing the specified file type is supported
	 * @param mimeType The file type
	 * @return Whether or not this file type is supported
	 */
	public static boolean isCompressable(String mimeType) {
		return compressableTypes.contains(mimeType);
	}
	
	/**
	 * Compresses a stream to a file
	 * @param fileDescriptor The file source to compress
	 * @param mimeType The file's type
	 * @param maxBytes The upper limit to compress to
	 * @param output The output file to save to
	 * @param streamToOutput Whether to write directly to the target file as we read, or rename the file on top of the output file once we're done
	 *                      This is useful when we're streaming the input file back and want to write back to the same file
	 */
	public static void compressFile(FileDescriptor fileDescriptor, String mimeType, int maxBytes, File output, boolean streamToOutput) throws IllegalArgumentException, IOException {
		switch(mimeType) {
			case "image/jpeg":
			case "image/webp": {
				byte[] data = compressBitmapLossy(convertCorrectBitmap(fileDescriptor), "image/jpeg".equals(mimeType) ? Bitmap.CompressFormat.JPEG : Bitmap.CompressFormat.WEBP, maxBytes);
				try(OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(output))) {
					outputStream.write(data);
				}
				break;
			}
			case "image/png": {
				byte[] data = compressBitmapLossless(convertCorrectBitmap(fileDescriptor), maxBytes);
				try(OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(output))) {
					outputStream.write(data);
				}
				break;
			}
			case "video/mp4":
				try {
					compressVideo(fileDescriptor, output, streamToOutput);
				} catch(InterruptedException | ExecutionException exception) {
					throw new IOException(exception);
				}
				break;
			default:
				throw new IllegalArgumentException("Unknown MIME type: " + mimeType);
		}
	}
	
	/**
	 * Compresses a bitmap
	 * @param fileBytes The bytes of the bitmap to compress
	 * @param mimeType The data type of the bitmap
	 * @param maxBytes The upper limit to compress to
	 * @return The compressed bitmap
	 */
	public static byte[] compressBitmap(byte[] fileBytes, String mimeType, int maxBytes) throws IOException {
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
	
	/**
	 * Rotates an image to be upright based on its EXIF data
	 * @param fileDescriptor The file descriptor to read from
	 * @return The bitmap in an upright position
	 */
	private static Bitmap convertCorrectBitmap(FileDescriptor fileDescriptor) {
		//Getting the bitmap from the image
		Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
		if(bitmap == null) return null;
		
		//Reading the image's EXIF data
		ExifInterface exif = null;
		try {
			exif = new ExifInterface(fileDescriptor);
		} catch(IOException exception) {
			//Printing the stack trace
			exception.printStackTrace();
		}
		
		//Fixing the bitmap orientation
		if(exif != null) bitmap = DataCompressionHelper.rotateBitmap(bitmap, exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL));
		
		//Returning the bitmap
		return bitmap;
	}
	
	/**
	 * Rotates an image to be upright based on its EXIF data
	 * @param fileBytes The bytes to read from
	 * @return The bitmap in an upright position
	 */
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
		if(bitmap == null) return null;
		
		//Fixing the bitmap orientation
		if(exif != null) bitmap = DataCompressionHelper.rotateBitmap(bitmap, exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL));
		
		//Returning the bitmap
		return bitmap;
	}
	
	/**
	 * Compresses a bitmap in a lossy format
	 * @param bitmap The bitmap to compress
	 * @param compressFormat The format to compress the data
	 * @param maxBytes The upper limit to compress to
	 */
	private static byte[] compressBitmapLossy(Bitmap bitmap, Bitmap.CompressFormat compressFormat, int maxBytes) throws IOException {
		if(bitmap == null) return null;
		
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
		}
	}
	
	/**
	 * Compresses a bitmap in a lossless format (PNG)
	 * @param bitmap The bitmap to compress
	 * @param maxBytes The upper limit to compress to
	 * @return The bytes of the compressed bitmap
	 */
	private static byte[] compressBitmapLossless(Bitmap bitmap, int maxBytes) throws IOException {
		if(bitmap == null) return null;
		
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
				Bitmap.createScaledBitmap(bitmap, (int) (width * scale), (int) (height * scale), true).compress(Bitmap.CompressFormat.PNG, bitmapQuality, outputStream);
				attempts++;
				bytes = outputStream.size();
			}
			
			//Returning the bytes
			return outputStream.toByteArray();
		}
	}
	
	/**
	 * Rotates a bitmap to the specified EXIF orientation
	 * @param bitmap The bitmap to rotate
	 * @param orientation The orientation to rotate to
	 * @return The rotated bitmap
	 */
	public static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
		Matrix matrix = new Matrix();
		switch(orientation) {
			default:
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
		}
		
		Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
		bitmap.recycle();
		return bmRotated;
	}
	
	/**
	 * Compresses a video file
	 * @param fileDescriptor The file descriptor of the video to compress
	 * @param outputFile The file to write the output to
	 * @param streamToOutput Whether to stream to the output file, or rename over it once we're done
	 */
	public static void compressVideo(FileDescriptor fileDescriptor, File outputFile, boolean streamToOutput) throws InterruptedException, ExecutionException {
		TrackStrategy audioStrategy = new DefaultAudioStrategy.Builder()
				.channels(1)
				.bitRate(24000)
				.build();
		
		TrackStrategy videoStrategy = new DefaultVideoStrategy.Builder()
				.frameRate(12)
				.bitRate(24000)
				.addResizer(new AtMostResizer(144, 176))
				.build();
		
		File temporaryFile;
		TranscoderOptions.Builder transcoder;
		if(streamToOutput) {
			//Write directly to the output file
			temporaryFile = null;
			transcoder = Transcoder.into(outputFile.getPath());
		} else {
			//Write to the temporary file
			temporaryFile = FileHelper.findFreeFile(outputFile.getParentFile(), "transcoder_temp", false);
			transcoder = Transcoder.into(temporaryFile.getPath());
		}
		
		transcoder.addDataSource(fileDescriptor)
				.setAudioTrackStrategy(audioStrategy)
				.setVideoTrackStrategy(videoStrategy)
				.transcode().get();
		
		//Cleaning up the temporary file
		if(!streamToOutput) {
			outputFile.delete();
			temporaryFile.renameTo(outputFile);
		}
	}
}