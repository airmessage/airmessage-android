package me.tagavari.airmessage.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class BitmapHelper {
	/**
	 * Loads a bitmap from a URI
	 */
	public static Single<Bitmap> loadBitmap(Context context, Uri uri, boolean useMemory) {
		return Single.fromCallable(() -> {
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				//Bitmaps loaded with ImageDecoder are by default hardware-accelerated, and can't be used on software canvases
				Bitmap bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getContentResolver(), uri));
				if(useMemory) return bitmap.copy(Bitmap.Config.ARGB_8888, true);
				else return bitmap;
			} else {
				return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
			}
		}).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
	}
	
	/**
	 * Loads a circular bitmap from a URI
	 */
	public static Single<Bitmap> loadBitmapCircular(Context context, Uri uri) {
		return Single.fromCallable(() -> {
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				return ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getContentResolver(), uri), (decoder, info, source) -> {
					decoder.setPostProcessor(canvas -> {
						//Mask circle
						int width = canvas.getWidth();
						int height = canvas.getHeight();
						
						Path path = new Path();
						path.setFillType(Path.FillType.INVERSE_EVEN_ODD);
						path.addRoundRect(0, 0, width, height, width / 2, height / 2, Path.Direction.CW);
						
						Paint paint = new Paint();
						paint.setAntiAlias(true);
						paint.setColor(Color.TRANSPARENT);
						paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
						
						canvas.drawPath(path, paint);
						
						return PixelFormat.TRANSLUCENT;
					});
				});
			} else {
				return maskCircularBitmap(MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri));
			}
		}).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
	}
	
	/**
	 * Masks a square bitmap to be a circle
	 */
	public static Bitmap maskCircularBitmap(Bitmap bitmap) {
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
		
		Canvas canvas = new Canvas(output);
		
		final Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(0xFF424242);
		
		final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		
		canvas.drawARGB(0, 0, 0, 0);
		canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2, bitmap.getWidth() / 2, paint);
		
		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);
		return output;
	}
}