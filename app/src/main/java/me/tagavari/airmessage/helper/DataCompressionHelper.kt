package me.tagavari.airmessage.helper

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import com.otaliastudios.transcoder.Transcoder
import com.otaliastudios.transcoder.TranscoderListener
import com.otaliastudios.transcoder.TranscoderOptions
import com.otaliastudios.transcoder.resize.AtMostResizer
import com.otaliastudios.transcoder.strategy.DefaultAudioStrategy
import com.otaliastudios.transcoder.strategy.DefaultVideoStrategy
import com.otaliastudios.transcoder.strategy.TrackStrategy
import java.io.*
import java.util.*
import java.util.concurrent.ExecutionException
import kotlin.math.sqrt

/**
 * A set of utilities for compressing standard file types
 */
object DataCompressionHelper {
	private const val bitmapQuality = 90 //90%
	private val compressableTypes = listOf("image/jpeg", "image/webp", "image/png", "video/mp4")
	
	/**
	 * Checks if compressing the specified file type is supported
	 * @param mimeType The file type
	 * @return Whether or not this file type is supported
	 */
	@JvmStatic
	fun isCompressable(mimeType: String) = compressableTypes.contains(mimeType)
	
	/**
	 * Compresses a stream to a file
	 * @param fileDescriptor The file source to compress
	 * @param mimeType The file's type
	 * @param maxBytes The upper limit to compress to
	 * @param output The output file to save to
	 * @param streamToOutput Whether to write directly to the target file as we read, or rename the file on top of the output file once we're done
	 * This is useful when we're streaming the input file back and want to write back to the same file
	 */
	@JvmStatic
	@Throws(IllegalArgumentException::class, IOException::class)
	fun compressFile(fileDescriptor: FileDescriptor, mimeType: String, maxBytes: Int, output: File, streamToOutput: Boolean) {
		when(mimeType) {
			"image/jpeg", "image/webp" -> {
				val data = compressBitmapLossy(
						loadCorrectBitmap(fileDescriptor),
						if(mimeType == "image/jpeg") CompressFormat.JPEG
						else {
							if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) CompressFormat.WEBP_LOSSY
							else CompressFormat.WEBP
						},
						maxBytes)
				BufferedOutputStream(FileOutputStream(output)).use { outputStream -> outputStream.write(data) }
			}
			"image/png" -> {
				val data = compressBitmapLossless(loadCorrectBitmap(fileDescriptor), maxBytes)
				BufferedOutputStream(FileOutputStream(output)).use { outputStream -> outputStream.write(data) }
			}
			"video/mp4" -> {
				try {
					compressVideo(fileDescriptor, output, streamToOutput)
				} catch(exception: InterruptedException) {
					throw IOException(exception)
				} catch(exception: ExecutionException) {
					throw IOException(exception)
				}
			}
			else -> throw IllegalArgumentException("Unknown MIME type: $mimeType")
		}
	}
	
	/**
	 * Compresses a bitmap
	 * @param fileBytes The bytes of the bitmap to compress
	 * @param mimeType The data type of the bitmap
	 * @param maxBytes The upper limit to compress to
	 * @return The compressed bitmap
	 */
	@JvmStatic
	@Throws(IOException::class)
	fun compressBitmap(fileBytes: ByteArray, mimeType: String, maxBytes: Int): ByteArray? {
		return when(mimeType) {
			"image/jpeg", "image/webp" -> compressBitmapLossy(
					loadCorrectBitmap(fileBytes),
					if(mimeType == "image/jpeg") CompressFormat.JPEG
					else {
						if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) CompressFormat.WEBP_LOSSY
						else CompressFormat.WEBP
					},
					maxBytes)
			"image/png" -> compressBitmapLossless(loadCorrectBitmap(fileBytes), maxBytes)
			else -> throw IllegalArgumentException("Unknown MIME type: $mimeType")
		}
	}
	
	/**
	 * Rotates an image to be upright based on its EXIF data
	 * @param fileDescriptor The file descriptor to read from
	 * @return The bitmap in an upright position
	 */
	private fun loadCorrectBitmap(fileDescriptor: FileDescriptor): Bitmap? {
		//Getting the bitmap from the image
		var bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor) ?: return null
		
		//Reading the image's EXIF data
		var exif: ExifInterface? = null
		try {
			exif = ExifInterface(fileDescriptor)
		} catch(exception: IOException) {
			//Printing the stack trace
			exception.printStackTrace()
		}
		
		//Fixing the bitmap orientation
		if(exif != null) {
			bitmap = rotateBitmap(bitmap, exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL))
		}
		
		//Returning the bitmap
		return bitmap
	}
	
	/**
	 * Rotates an image to be upright based on its EXIF data
	 * @param fileBytes The bytes to read from
	 * @return The bitmap in an upright position
	 */
	private fun loadCorrectBitmap(fileBytes: ByteArray): Bitmap? {
		//Reading the image's EXIF data
		var exif: ExifInterface? = null
		try {
			exif = ExifInterface(ByteArrayInputStream(fileBytes))
		} catch(exception: IOException) {
			//Printing the stack trace
			exception.printStackTrace()
		}
		
		//Getting the bitmap from the image
		var bitmap = BitmapFactory.decodeByteArray(fileBytes, 0, fileBytes.size) ?: return null
		
		//Fixing the bitmap orientation
		if(exif != null) {
			bitmap = rotateBitmap(bitmap, exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL))
		}
		
		//Returning the bitmap
		return bitmap
	}
	
	/**
	 * Compresses a bitmap in a lossy format
	 * @param bitmap The bitmap to compress
	 * @param compressFormat The format to compress the data
	 * @param maxBytes The upper limit to compress to
	 */
	@Throws(IOException::class)
	private fun compressBitmapLossy(bitmap: Bitmap?, compressFormat: CompressFormat, maxBytes: Int): ByteArray? {
		if(bitmap == null) return null
		
		//Getting the bitmap dimensions
		val width = bitmap.width
		val height = bitmap.height
		ByteArrayOutputStream().use { outputStream ->
			//Compressing the bitmap with default settings immediately
			bitmap.compress(compressFormat, bitmapQuality, outputStream)
			
			//Counting the amount of bytes without scaling
			val unscaledBytes = outputStream.size()
			
			//Further scaling the image until it fits under the max byte count
			var attempts = 0
			var bytes = unscaledBytes
			while(bytes > maxBytes) {
				val scale = sqrt(maxBytes.toDouble() / unscaledBytes) * (1 - attempts * 0.1)
				outputStream.reset()
				Bitmap.createScaledBitmap(bitmap, (width * scale).toInt(), (height * scale).toInt(), true).compress(compressFormat, bitmapQuality, outputStream)
				attempts++
				bytes = outputStream.size()
			}
			
			//Returning the bytes
			return outputStream.toByteArray()
		}
	}
	
	/**
	 * Compresses a bitmap in a lossless format (PNG)
	 * @param bitmap The bitmap to compress
	 * @param maxBytes The upper limit to compress to
	 * @return The bytes of the compressed bitmap
	 */
	@Throws(IOException::class)
	private fun compressBitmapLossless(bitmap: Bitmap?, maxBytes: Int): ByteArray? {
		if(bitmap == null) return null
		
		//Getting the bitmap dimensions
		val width = bitmap.width
		val height = bitmap.height
		ByteArrayOutputStream().use { outputStream ->
			//Compressing the bitmap with default settings immediately
			bitmap.compress(CompressFormat.PNG, 0, outputStream)
			
			//Counting the amount of bytes without scaling
			val unscaledBytes = outputStream.size()
			
			//Further scaling the image until it fits under the max byte count
			var attempts = 0
			var bytes = unscaledBytes
			while(bytes > maxBytes) {
				val scale = sqrt(maxBytes.toDouble() / unscaledBytes) * (1 - attempts * 0.1)
				outputStream.reset()
				Bitmap.createScaledBitmap(bitmap, (width * scale).toInt(), (height * scale).toInt(), true).compress(CompressFormat.PNG, 0, outputStream)
				attempts++
				bytes = outputStream.size()
			}
			
			//Returning the bytes
			return outputStream.toByteArray()
		}
	}
	
	/**
	 * Rotates a bitmap to the specified EXIF orientation
	 * @param bitmap The bitmap to rotate
	 * @param orientation The orientation to rotate to
	 * @return The rotated bitmap
	 */
	private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
		val matrix = Matrix()
		when(orientation) {
			ExifInterface.ORIENTATION_NORMAL -> return bitmap
			ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1F, 1F)
			ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180F)
			ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
				matrix.setRotate(180F)
				matrix.postScale(-1F, 1F)
			}
			ExifInterface.ORIENTATION_TRANSPOSE -> {
				matrix.setRotate(90F)
				matrix.postScale(-1F, 1F)
			}
			ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90F)
			ExifInterface.ORIENTATION_TRANSVERSE -> {
				matrix.setRotate(-90F)
				matrix.postScale(-1F, 1F)
			}
			ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90F)
			else -> return bitmap
		}
		
		val bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
		bitmap.recycle()
		return bmRotated
	}
	
	/**
	 * Compresses a video file
	 * @param fileDescriptor The file descriptor of the video to compress
	 * @param outputFile The file to write the output to
	 * @param streamToOutput Whether to stream to the output file, or rename over it once we're done
	 */
	@Throws(InterruptedException::class, ExecutionException::class)
	fun compressVideo(fileDescriptor: FileDescriptor?, outputFile: File, streamToOutput: Boolean) {
		val audioStrategy: TrackStrategy = DefaultAudioStrategy.Builder()
				.channels(1)
				.bitRate(24000)
				.build()
		val videoStrategy: TrackStrategy = DefaultVideoStrategy.Builder()
				.frameRate(12)
				.bitRate(240000)
				.addResizer(AtMostResizer(144, 176))
				.build()
		val temporaryFile: File?
		val transcoder: TranscoderOptions.Builder
		if(streamToOutput) {
			//Write directly to the output file
			temporaryFile = null
			transcoder = Transcoder.into(outputFile.path)
		} else {
			//Write to the temporary file
			temporaryFile = FileHelper.findFreeFile(outputFile.parentFile, "transcoder_temp", false)
			transcoder = Transcoder.into(temporaryFile.path)
		}
		transcoder.addDataSource(fileDescriptor!!)
				.setAudioTrackStrategy(audioStrategy)
				.setVideoTrackStrategy(videoStrategy)
				.setListener(object : TranscoderListener {
					override fun onTranscodeProgress(progress: Double) {}
					override fun onTranscodeCompleted(successCode: Int) {}
					override fun onTranscodeCanceled() {}
					override fun onTranscodeFailed(exception: Throwable) {}
				})
				.transcode().get()
		
		//Cleaning up the temporary file
		if(!streamToOutput) {
			outputFile.delete()
			temporaryFile!!.renameTo(outputFile)
		}
	}
}