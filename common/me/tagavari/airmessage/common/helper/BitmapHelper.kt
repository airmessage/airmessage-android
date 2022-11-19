package me.tagavari.airmessage.common.helper

import android.content.Context
import android.graphics.*
import android.graphics.ImageDecoder.ImageInfo
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers

object BitmapHelper {
	/**
	 * Loads a bitmap from a URI
	 */
	@JvmStatic
	fun loadBitmap(context: Context, uri: Uri, useMemory: Boolean): Single<Bitmap> {
		return Single.fromCallable {
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				//Bitmaps loaded with ImageDecoder are by default hardware-accelerated, and can't be used on software canvases
				val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
				if(useMemory) return@fromCallable bitmap.copy(Bitmap.Config.ARGB_8888, true) else return@fromCallable bitmap
			} else {
				return@fromCallable MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
			}
		}.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
	}
	
	/**
	 * Loads a circular bitmap from a URI
	 */
	@JvmStatic
	fun loadBitmapCircular(context: Context, uri: Uri): Single<Bitmap> {
		return Single.fromCallable {
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				return@fromCallable ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder: ImageDecoder, _: ImageInfo?, _: ImageDecoder.Source? ->
					decoder.postProcessor = PostProcessor { canvas: Canvas ->
						//Mask circle
						val width = canvas.width
						val height = canvas.height
						
						val path = Path()
						path.fillType = Path.FillType.INVERSE_EVEN_ODD
						path.addRoundRect(0f, 0f, width.toFloat(), height.toFloat(), (width / 2).toFloat(), (height / 2).toFloat(), Path.Direction.CW)
						
						val paint = Paint()
						paint.isAntiAlias = true
						paint.color = Color.TRANSPARENT
						paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
						
						canvas.drawPath(path, paint)
						
						return@PostProcessor PixelFormat.TRANSLUCENT
					}
				}
			} else {
				return@fromCallable maskCircularBitmap(MediaStore.Images.Media.getBitmap(context.contentResolver, uri))
			}
		}.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
	}
	
	/**
	 * Masks a square bitmap to be a circle
	 */
	private fun maskCircularBitmap(bitmap: Bitmap): Bitmap {
		val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
		
		val canvas = Canvas(output)
		
		val paint = Paint()
		paint.isAntiAlias = true
		paint.color = 0xFF424242.toInt()
		
		val rect = Rect(0, 0, bitmap.width, bitmap.height)
		
		canvas.drawARGB(0, 0, 0, 0)
		canvas.drawCircle((bitmap.width / 2).toFloat(), (bitmap.height / 2).toFloat(), (bitmap.width / 2).toFloat(), paint)
		
		paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
		canvas.drawBitmap(bitmap, rect, rect, paint)
		return output
	}
}