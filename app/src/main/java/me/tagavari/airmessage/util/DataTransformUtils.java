package me.tagavari.airmessage.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Build;
import android.os.FileUtils;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;

import com.otaliastudios.transcoder.Transcoder;
import com.otaliastudios.transcoder.TranscoderListener;
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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class DataTransformUtils {
	public static final int standardBuffer = 8192; //8 kB
	
	private static final int bitmapQuality = 90; //90%
	private static final List<String> compressableTypes = Collections.unmodifiableList(Arrays.asList("image/jpeg", "image/webp", "image/png", "video/mp4"));
	
	public static boolean isCompressable(String mimeType) {
		return compressableTypes.contains(mimeType);
	}
	
	public static boolean compressFile(FileDescriptor fileDescriptor, String mimeType, int maxBytes, File output, boolean replaceOutput) {
		switch(mimeType) {
			case "image/jpeg":
			case "image/webp": {
				byte[] data = compressBitmapLossy(convertCorrectBitmap(fileDescriptor), "image/jpeg".equals(mimeType) ? Bitmap.CompressFormat.JPEG : Bitmap.CompressFormat.WEBP, maxBytes);
				if(data == null) return false;
				try(InputStream inputStream = new ByteArrayInputStream(data); OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(output))) {
					copyStream(inputStream, outputStream);
					return true;
				} catch(IOException exception) {
					exception.printStackTrace();
					return false;
				}
			}
			case "image/png": {
				byte[] data = compressBitmapLossless(convertCorrectBitmap(fileDescriptor), maxBytes);
				if(data == null) return false;
				try(InputStream inputStream = new ByteArrayInputStream(data); OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(output))) {
					copyStream(inputStream, outputStream);
					return true;
				} catch(IOException exception) {
					exception.printStackTrace();
					return false;
				}
			}
			case "video/mp4":
				return compressVideo(fileDescriptor, output, replaceOutput);
			default:
				throw new IllegalArgumentException("Unknown MIME type: " + mimeType);
		}
	}
	
	public static byte[] compressBitmap(byte[] fileBytes, String mimeType, int maxBytes) {
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
		if(exif != null) bitmap = DataTransformUtils.rotateBitmap(bitmap, exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL));
		
		//Returning the bitmap
		return bitmap;
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
		if(bitmap == null) return null;
		
		//Fixing the bitmap orientation
		if(exif != null) bitmap = DataTransformUtils.rotateBitmap(bitmap, exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL));
		
		//Returning the bitmap
		return bitmap;
	}
	
	private static byte[] compressBitmapLossy(Bitmap bitmap, Bitmap.CompressFormat compressFormat, int maxBytes) {
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
		} catch(IOException exception) {
			exception.printStackTrace();
			return null;
		}
	}
	
	private static byte[] compressBitmapLossless(Bitmap bitmap, int maxBytes) {
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
	
	public static boolean compressVideo(FileDescriptor fileDescriptor, File outputFile, boolean replaceOutput) {
		BlockingQueue<Boolean> blockingQueue = new ArrayBlockingQueue<>(1);
		
		TrackStrategy audioStrategy = new DefaultAudioStrategy.Builder()
				.channels(1)
				.bitRate(24000)
				.build();
		
		TrackStrategy videoStrategy = new DefaultVideoStrategy.Builder()
				.frameRate(12)
				.bitRate(24000)
				.addResizer(new AtMostResizer(144, 176))
				.build();
		
		try {
			File temporaryFile;
			TranscoderOptions.Builder transcoder;
			if(replaceOutput) {
				//Write to the temporary file
				temporaryFile = Constants.findFreeFile(outputFile.getParentFile(), "transcoder_temp", false);
				transcoder = Transcoder.into(temporaryFile.getPath());
			} else {
				//Write directly to the output file
				temporaryFile = null;
				transcoder = Transcoder.into(outputFile.getPath());
			}
			
			transcoder
					.addDataSource(fileDescriptor)
					.setAudioTrackStrategy(audioStrategy)
					.setVideoTrackStrategy(videoStrategy)
					.setListener(new TranscoderListener() {
						@Override
						public void onTranscodeProgress(double progress) {
						
						}
						
						@Override
						public void onTranscodeCompleted(int successCode) {
							blockingQueue.add(true);
						}
						
						@Override
						public void onTranscodeCanceled() {
							blockingQueue.add(false);
						}
						
						@Override
						public void onTranscodeFailed(@NonNull Throwable exception) {
							blockingQueue.add(false);
						}
					})
					.transcode();
			
			//Waiting for the task to finish
			boolean result = blockingQueue.take();
			
			//Cleaning up the temporary file
			if(temporaryFile != null) {
				outputFile.delete();
				temporaryFile.renameTo(outputFile);
			}
			
			//Returning the result
			return result;
		} catch(InterruptedException exception) {
			exception.printStackTrace();
			return false;
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