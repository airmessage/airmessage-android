package me.tagavari.airmessage.helper

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import com.otaliastudios.transcoder.Transcoder
import com.otaliastudios.transcoder.TranscoderListener
import com.otaliastudios.transcoder.resize.AtMostResizer
import com.otaliastudios.transcoder.strategy.DefaultAudioStrategy
import com.otaliastudios.transcoder.strategy.DefaultVideoStrategy
import me.tagavari.airmessage.container.ReadableBlob
import me.tagavari.airmessage.container.ReadableBlobByteArray
import java.io.*
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
	 * @param file The file source to compress
	 * @param mimeType The MIME type of the file
	 * @param maxBytes The upper limit to compress to
	 * @param output The output file to save to
	 * This is useful when we're streaming the input file back and want to write back to the same file
	 */
	@JvmStatic
	@Throws(IllegalArgumentException::class, IOException::class)
	fun compressFile(file: ReadableBlob, mimeType: String, maxBytes: Int, output: File) {
		when(mimeType) {
			"image/jpeg", "image/webp" -> {
				val format = if(mimeType == "image/jpeg") {
					CompressFormat.JPEG
				} else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
					CompressFormat.WEBP_LOSSY
				} else {
					@Suppress("DEPRECATION")
					CompressFormat.WEBP
				}
				
				val data = compressBitmapLossy(loadCorrectBitmap(file), format, maxBytes)
				BufferedOutputStream(FileOutputStream(output)).use { outputStream -> outputStream.write(data) }
			}
			"image/png" -> {
				val data = compressBitmapLossless(loadCorrectBitmap(file), maxBytes)
				BufferedOutputStream(FileOutputStream(output)).use { outputStream -> outputStream.write(data) }
			}
			"video/mp4" -> {
				try {
					compressVideo(file, output)
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
	 * Rotates an image to be upright based on its EXIF data
	 * @param file The readable blob to read from
	 * @return The bitmap in an upright position
	 */
	@Throws(IOException::class)
	private fun loadCorrectBitmap(file: ReadableBlob): Bitmap {
		//Get the bitmap from the image
		val bitmap = file.openInputStream().use { fileStream ->
			BitmapFactory.decodeStream(fileStream)
		} ?: throw IOException("Failed to load bitmap")
		
		//Read the image's EXIF data
		val exif = try {
			file.openInputStream().use { fileStream ->
				ExifInterface(fileStream)
			}
		} catch(exception: IOException) {
			exception.printStackTrace()
			
			//Ignore any exceptions
			null
		}
		
		return if(exif != null) {
			//Fix the bitmap orientation
			rotateBitmap(bitmap, exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL))
		} else {
			bitmap
		}
	}
	
	/**
	 * Compresses a bitmap in a lossy format
	 * @param bitmap The bitmap to compress
	 * @param compressFormat The format to compress the data
	 * @param maxBytes The upper limit to compress to
	 */
	@Throws(IOException::class)
	private fun compressBitmapLossy(bitmap: Bitmap, compressFormat: CompressFormat, maxBytes: Int): ByteArray {
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
	private fun compressBitmapLossless(bitmap: Bitmap, maxBytes: Int): ByteArray {
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
	 * @param file The readable blob to compress
	 * @param outputFile The file to write the output to
	 */
	@Throws(InterruptedException::class, ExecutionException::class)
	fun compressVideo(file: ReadableBlob, outputFile: File) {
		val transcoderSource = file.asTranscoderSource()
			?: throw IllegalArgumentException("Input does not support transcoding!")
		
		Transcoder.into(outputFile.path).apply {
			addDataSource(transcoderSource)
			.setAudioTrackStrategy(
				DefaultAudioStrategy.Builder()
					.channels(1)
					.bitRate(24000)
					.build()
			)
			
			setVideoTrackStrategy(
				DefaultVideoStrategy.Builder()
					.frameRate(12)
					.bitRate(240000)
					.addResizer(AtMostResizer(144, 176))
					.build()
			)
			
			setListener(object : TranscoderListener {
				override fun onTranscodeProgress(progress: Double) {}
				override fun onTranscodeCompleted(successCode: Int) {}
				override fun onTranscodeCanceled() {}
				override fun onTranscodeFailed(exception: Throwable) {}
			})
		}.transcode().get()
	}
	
	/**
	 * Compresses a bitmap in memory
	 * @param bytes The bytes of the image file to compress
	 * @param mimeType The image format to convert to
	 * @param maxBytes The maximum size to output
	 */
	fun compressBitmap(bytes: ByteArray, mimeType: String, maxBytes: Int): ByteArray {
		val blob = ReadableBlobByteArray(bytes, type = mimeType)
		return when(mimeType) {
			"image/jpeg", "image/webp" -> compressBitmapLossy(
				loadCorrectBitmap(blob),
				if(mimeType == "image/jpeg") CompressFormat.JPEG
				else {
					if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) CompressFormat.WEBP_LOSSY
					else @Suppress("DEPRECATION") CompressFormat.WEBP
				},
				maxBytes)
			"image/png" -> compressBitmapLossless(loadCorrectBitmap(blob), maxBytes)
			else -> throw IllegalArgumentException("Unknown MIME type: $mimeType")
		}
	}
}