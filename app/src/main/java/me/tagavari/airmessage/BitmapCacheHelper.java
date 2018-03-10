package me.tagavari.airmessage;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.telephony.PhoneNumberUtils;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageView;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class BitmapCacheHelper {
	//Creating the reference values
	private static final String cachePrefixAttachment = "attachment-";
	private static final String cachePrefixContact = "contact-";
	private static final String cachePrefixSticker = "sticker-";
	//Creating the values
	private LruCache<String, Bitmap> bitmapCache;
	private List<String> failedBitmapCache = new ArrayList<>();
	private Map<String, List<ImageDecodeResult>> callbackList = new HashMap<>();
	
	BitmapCacheHelper() {
		//Setting the bitmap cache
		bitmapCache = new LruCache<String, Bitmap>((int) (Runtime.getRuntime().maxMemory() / 1024 / 8)) {
			@Override
			protected int sizeOf(String key, Bitmap bitmap) {
				//Returning the bitmap's size
				return bitmap.getByteCount() / 1024;
			}
		};
	}
	
	void assignContactImage(Context context, String name, Constants.TaskedViewSource viewSource) {
		//Creating the result listener
		ImageDecodeResult callbacks = new ImageDecodeResult(viewSource) {
			@Override
			void onImageMeasured(int width, int height) {}
			
			@Override
			void onImageDecoded(Bitmap result, boolean wasTasked) {
				//Returning if the bitmap is invalid
				if(result == null) return;
				
				//Getting the image view
				ImageView imageView = (ImageView) viewSource.get(wasTasked);
				if(imageView == null) return;
				
				//Setting the bitmap
				imageView.setImageBitmap(result);
				
				//Fading in the view
				if(wasTasked) {
					imageView.setAlpha(0F);
					imageView.animate().alpha(1).setDuration(300).start();
				}
			}
		};
		
		//Getting the bitmap
		getBitmapFromContact(context, name, name, callbacks);
	}
	
	void assignContactImage(Context context, String name, View view) {
		//Creating the result listener
		ImageDecodeResult callbacks = new ImageDecodeResult(view) {
			@Override
			void onImageMeasured(int width, int height) {}
			
			@Override
			void onImageDecoded(Bitmap result, boolean wasTasked) {
				//Returning if the bitmap is invalid
				if(result == null) return;
				
				//Getting the image view
				ImageView imageView = (ImageView) viewReference.get();
				if(imageView == null) return;
				
				//Setting the bitmap
				imageView.setImageBitmap(result);
				
				//Fading in the view
				if(wasTasked) {
					imageView.setAlpha(0F);
					imageView.animate().alpha(1).setDuration(300).start();
				}
			}
		};
		
		//Getting the bitmap
		getBitmapFromContact(context, name, name, callbacks);
	}
	
	void getBitmapFromImageFile(String id, File file, ImageDecodeResult callbacks, boolean resize, int pxMinX, int pxMinY) {
		//Prefixing the identifier
		id = cachePrefixAttachment + id;
		
		//Checking if there is an entry in the cache
		Bitmap bitmap = bitmapCache.get(id);
		
		//Checking if the bitmap is not cached
		if(bitmap == null && !failedBitmapCache.contains(id)) {
			//Adding the listener
			if(callbackList.containsKey(id))
				callbackList.get(id).add(callbacks);
			else {
				ArrayList<ImageDecodeResult> resultList = new ArrayList<>();
				resultList.add(callbacks);
				callbackList.put(id, resultList);
			}
			
			//Starting the task
			new DecodeImageFileTask(id, this, resize, pxMinX, pxMinY).execute(file);
		}
		//Otherwise immediately telling the callback listener
		else callbacks.onImageDecoded(bitmap, false);
	}
	
	void getBitmapFromContact(Context context, String id, long contactID, ImageDecodeResult callbacks) {
		//Returning if contacts are not enabled
		if(!MainApplication.canUseContacts(MainApplication.getInstance())) {
			callbacks.onImageDecoded(null, false);
			return;
		}
		
		//Prefixing the identifier
		id = cachePrefixContact + id;
		
		//Checking if there is an entry in the cache
		Bitmap bitmap = bitmapCache.get(id);
		
		//Checking if the bitmap is not cached
		if(bitmap == null && !failedBitmapCache.contains(id)) {
			//Adding the listener
			if(callbackList.containsKey(id))
				callbackList.get(id).add(callbacks);
			else {
				ArrayList<ImageDecodeResult> resultList = new ArrayList<>();
				resultList.add(callbacks);
				callbackList.put(id, resultList);
			}
			
			//Starting the task
			new DecodeContactThumbnailTask(id, this, context.getContentResolver(), contactID).execute();
		}
		//Otherwise immediately telling the callback listener
		else callbacks.onImageDecoded(bitmap, false);
	}
	
	void getBitmapFromContact(Context context, String id, String contactName, ImageDecodeResult callbacks) {
		//Returning if contacts are not enabled
		if(!MainApplication.canUseContacts(MainApplication.getInstance())) {
			callbacks.onImageDecoded(null, false);
			return;
		}
		
		//Prefixing the identifier
		id = cachePrefixContact + id;
		
		//Checking if there is an entry in the cache
		Bitmap bitmap = bitmapCache.get(id);
		
		//Checking if the bitmap is not cached
		if(bitmap == null && !failedBitmapCache.contains(id)) {
			//Adding the listener
			if(callbackList.containsKey(id))
				callbackList.get(id).add(callbacks);
			else {
				ArrayList<ImageDecodeResult> resultList = new ArrayList<>();
				resultList.add(callbacks);
				callbackList.put(id, resultList);
			}
			
			//Starting the task
			new DecodeContactThumbnailTask(id, this, context.getContentResolver(), contactName).execute();
		}
		//Otherwise immediately telling the callback listener
		else callbacks.onImageDecoded(bitmap, false);
	}
	
	void getBitmapFromVideoFile(String id, File file, ImageDecodeResult callbacks) {
		//Prefixing the identifier
		id = cachePrefixAttachment + id;
		
		//Checking if there is an entry in the cache
		Bitmap bitmap = bitmapCache.get(id);
		
		//Checking if the bitmap is not cached
		if(bitmap == null && !failedBitmapCache.contains(id)) {
			//Adding the listener
			if(callbackList.containsKey(id))
				callbackList.get(id).add(callbacks);
			else {
				ArrayList<ImageDecodeResult> resultList = new ArrayList<>();
				resultList.add(callbacks);
				callbackList.put(id, resultList);
			}
			
			//Starting the task
			new DecodeVideoFileTask(id, this).execute(file);
		}
		//Otherwise immediately telling the callback listener
		else callbacks.onImageDecoded(bitmap, false);
	}
	
	void getBitmapFromDBSticker(String id, long identifier, ImageDecodeResult callbacks) {
		//Prefixing the identifier
		id = cachePrefixSticker + id;
		
		//Checking if there is an entry in the cache
		Bitmap bitmap = bitmapCache.get(id);
		
		//Checking if the bitmap is not cached
		if(bitmap == null && !failedBitmapCache.contains(id)) {
			//Adding the listener
			if(callbackList.containsKey(id))
				callbackList.get(id).add(callbacks);
			else {
				ArrayList<ImageDecodeResult> resultList = new ArrayList<>();
				resultList.add(callbacks);
				callbackList.put(id, resultList);
			}
			
			//Starting the task
			new DecodeStickerTask(MainApplication.getInstance(), id, this).execute(identifier);
		}
		//Otherwise immediately telling the callback listener
		else callbacks.onImageDecoded(bitmap, false);
	}
	
	private static class DecodeContactThumbnailTask extends AsyncTask<Void, Integer, Bitmap> {
		//Creating the values
		private final String requestKey;
		private final WeakReference<BitmapCacheHelper> superclassReference;
		private final ContentResolver contentResolver;
		
		private long contactID;
		private String contactName;
		
		private DecodeContactThumbnailTask(String requestKey, BitmapCacheHelper superclass, ContentResolver contentResolver) {
			//Setting the values
			this.requestKey = requestKey;
			superclassReference = new WeakReference<>(superclass);
			this.contentResolver = contentResolver;
		}
		
		DecodeContactThumbnailTask(String requestKey, BitmapCacheHelper superclass, ContentResolver contentResolver, long contactID) {
			//Calling the main constructor
			this(requestKey, superclass, contentResolver);
			
			//Setting the request values
			this.contactID = contactID;
			this.contactName = null;
		}
		
		DecodeContactThumbnailTask(String requestKey, BitmapCacheHelper superclass, ContentResolver contentResolver, String contactName) {
			//Calling the main constructor
			this(requestKey, superclass, contentResolver);
			
			//Setting the request values
			this.contactID = -1;
			this.contactName = contactName;
		}
		
		@Override
		protected Bitmap doInBackground(Void... parameters) {
			//Checking if an ID has been provided
			if(contactID != -1) {
				//Querying the user for a profile image
				try(Cursor cursor = contentResolver.query(ContactsContract.Data.CONTENT_URI,
						new String[]{ContactsContract.Contacts.PHOTO_ID},
						ContactsContract.Data.CONTACT_ID + " = ? ", new String[]{Long.toString(contactID)},
						null)) {
					//Returning if the cursor couldn't be created
					if(cursor == null) return null;
					
					//Returning if there are no results
					if(!cursor.moveToFirst()) return null;
					
					//Getting the data
					boolean hasProfileImage = !cursor.isNull(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_ID));
					
					//Returning if the user has no profile image
					if(!hasProfileImage) return null;
					
					//Closing the cursor
					cursor.close();
				}
			} else {
				//Getting the contact info
				ContactInfo contactInfo = getContactInfoFromName(contentResolver, contactName);
				
				//Returning null if no contact info could be found or the user has no profile image
				if(contactInfo == null || !contactInfo.hasImage) return null;
				
				//Setting the contact ID
				contactID = contactInfo.contactID;
			}
			
			//Querying for the user icon
			Uri photoUri = Uri.withAppendedPath(ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactID), ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
			try(Cursor cursor = contentResolver.query(photoUri, new String[]{ContactsContract.Contacts.Photo.PHOTO}, null, null, null)) {
				//Returning if the cursor couldn't be created
				if(cursor == null) return null;
				
				//Returning if there is no data
				if(!cursor.moveToFirst()) return null;
				
				//Getting the data
				byte[] data = cursor.getBlob(0);
				if(data == null) return null;
				
				//Returning the bitmap
				return BitmapFactory.decodeStream(new ByteArrayInputStream(data));
			}
		}
		
		private ContactInfo getContactInfoFromName(ContentResolver contentResolver, String name) {
			//Querying the database
			try(Cursor cursor = contentResolver.query(ContactsContract.Data.CONTENT_URI,
					new String[]{ContactsContract.Data.CONTACT_ID, ContactsContract.Contacts.PHOTO_ID},
					ContactsContract.CommonDataKinds.Email.ADDRESS + " = ? OR " + ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER + " = ?", new String[]{name, PhoneNumberUtils.normalizeNumber(name)},
					null)) {
				//Returning if the cursor is invalid
				if(cursor == null) return null;
				
				//Returning if there are no results
				if(!cursor.moveToFirst()) return null;
				
				//Getting the data
				long contactIdentifier = cursor.getLong(cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID));
				boolean hasProfileImage = !cursor.isNull(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_ID));
				
				//Returning the data
				return new ContactInfo(contactIdentifier, hasProfileImage);
			}
		}
		
		private static class ContactInfo {
			final long contactID;
			final boolean hasImage;
			
			ContactInfo(long contactID, boolean hasImage) {
				this.contactID = contactID;
				this.hasImage = hasImage;
			}
		}
		
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			//Getting the superclass
			BitmapCacheHelper superclass = superclassReference.get();
			if(superclass == null) return;
			
			//Caching the bitmap
			if(bitmap == null) superclass.failedBitmapCache.add(requestKey);
			else if(superclass.bitmapCache.get(requestKey) == null)
				superclass.bitmapCache.put(requestKey, bitmap);
			
			//Telling the result listeners
			if(superclass.callbackList.containsKey(requestKey)) {
				for(ImageDecodeResult callback : superclass.callbackList.get(requestKey)) callback.onImageDecoded(bitmap, true);
				superclass.callbackList.remove(requestKey);
			}
		}
	}
	
	private static class DecodeImageFileTask extends AsyncTask<File, Integer, Bitmap> {
		//Creating the values
		private final String requestKey;
		private final WeakReference<BitmapCacheHelper> superclassReference;
		private final boolean resize;
		private final int pxMinX, pxMinY;
		
		DecodeImageFileTask(String requestKey, BitmapCacheHelper superclass, boolean resize, int pxMinX, int pxMinY) {
			//Setting the values
			this.requestKey = requestKey;
			superclassReference = new WeakReference<>(superclass);
			
			this.resize = resize;
			this.pxMinX = pxMinX;
			this.pxMinY = pxMinY;
		}
		
		@Override
		protected Bitmap doInBackground(File... parameters) {
			//Getting the file
			File file = parameters[0];
			
			//Creating the EXIF flags
			boolean useExif = false;
			int exifOrientation = -1;
			
			//Checking if the image is a JPEG file (contains EXIF data)
			if("image/jpeg".equals(Constants.getMimeType(file))) {
				//Reading the image's EXIF data
				ExifInterface exif = null;
				try {
					exif = new ExifInterface(file.getAbsolutePath());
					useExif = true;
				} catch(IOException exception) {
					//Printing the stack trace
					exception.printStackTrace();
				}
				
				//Checking if EXIF should be used
				if(useExif) {
					//Getting the EXIF orientation
					exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
				}
			}
			
			//Creating the bitmap options
			BitmapFactory.Options options = new BitmapFactory.Options();
			
			if(resize) {
				//Reading the dimensions of the image
				options.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(file.getPath(), options);
				
				//Calculating the optimal image dimensions
				options.inSampleSize = calculateInSampleSize(options, pxMinX, pxMinY);
				publishProgress(options.outWidth / options.inSampleSize, options.outHeight / options.inSampleSize);
				
				//Decoding the entire bitmap
				options.inJustDecodeBounds = false;
			}
			
			//Decoding the file
			Bitmap bitmap = BitmapFactory.decodeFile(file.getPath(), options);
			if(bitmap == null) return null;
			
			//Rotating the bitmap
			if(useExif) bitmap = rotateBitmap(bitmap, exifOrientation);
			
			//Returning the bitmap
			return bitmap;
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			//Getting the superclass
			BitmapCacheHelper superclass = superclassReference.get();
			if(superclass == null) return;
			
			//Telling the result listeners
			if(superclass.callbackList.containsKey(requestKey))
				for(ImageDecodeResult callback : superclass.callbackList.get(requestKey)) callback.onImageMeasured(values[0], values[1]);
		}
		
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			//Getting the superclass
			BitmapCacheHelper superclass = superclassReference.get();
			if(superclass == null) return;
			
			//Caching the bitmap
			if(bitmap == null) superclass.failedBitmapCache.add(requestKey);
			else if(superclass.bitmapCache.get(requestKey) == null)
				superclass.bitmapCache.put(requestKey, bitmap);
			
			//Telling the result listeners
			if(superclass.callbackList.containsKey(requestKey)) {
				for(ImageDecodeResult callback : superclass.callbackList.get(requestKey)) callback.onImageDecoded(bitmap, true);
				superclass.callbackList.remove(requestKey);
			}
		}
		
		private static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
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
	
	private static class DecodeVideoFileTask extends AsyncTask<File, Integer, Bitmap> {
		//Creating the values
		private final String requestKey;
		private final WeakReference<BitmapCacheHelper> superclassReference;
		
		DecodeVideoFileTask(String requestKey, BitmapCacheHelper superclass) {
			//Setting the values
			this.requestKey = requestKey;
			superclassReference = new WeakReference<>(superclass);
		}
		
		@Override
		protected Bitmap doInBackground(File... parameters) {
			//Returning the decoded file
			return ThumbnailUtils.createVideoThumbnail(parameters[0].getPath(), MediaStore.Images.Thumbnails.MINI_KIND);
		}
		
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			//Getting the superclass
			BitmapCacheHelper superclass = superclassReference.get();
			if(superclass == null) return;
			
			//Caching the bitmap
			if(bitmap == null) superclass.failedBitmapCache.add(requestKey);
			else if(superclass.bitmapCache.get(requestKey) == null)
				superclass.bitmapCache.put(requestKey, bitmap);
			
			//Telling the result listeners
			if(superclass.callbackList.containsKey(requestKey)) {
				for(ImageDecodeResult callback : superclass.callbackList.get(requestKey)) callback.onImageDecoded(bitmap, true);
				superclass.callbackList.remove(requestKey);
			}
		}
	}
	
	private static class DecodeStickerTask extends AsyncTask<Long, Integer, Bitmap> {
		//Creating the references
		private final WeakReference<Context> contextReference;
		private final WeakReference<BitmapCacheHelper> superclassReference;
		
		//Creating the values
		private final String requestKey;
		
		DecodeStickerTask(Context context, String requestKey, BitmapCacheHelper superclass) {
			//Setting the values
			contextReference = new WeakReference<>(context);
			this.requestKey = requestKey;
			superclassReference = new WeakReference<>(superclass);
		}
		
		@Override
		protected Bitmap doInBackground(Long... parameters) {
			//Getting the context
			Context context = contextReference.get();
			if(context == null) return null;
			
			//Fetching the bytes from the database
			byte[] imageBlob = DatabaseManager.getStickerBlob(DatabaseManager.getReadableDatabase(context), parameters[0]);
			if(imageBlob == null) return null;
			
			//Returning the bitmap
			return BitmapFactory.decodeByteArray(imageBlob, 0, imageBlob.length);
		}
		
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			//Getting the superclass
			BitmapCacheHelper superclass = superclassReference.get();
			if(superclass == null) return;
			
			//Caching the bitmap
			if(bitmap == null) superclass.failedBitmapCache.add(requestKey);
			else if(superclass.bitmapCache.get(requestKey) == null)
				superclass.bitmapCache.put(requestKey, bitmap);
			
			//Telling the result listeners
			if(superclass.callbackList.containsKey(requestKey)) {
				for(ImageDecodeResult callback : superclass.callbackList.get(requestKey)) callback.onImageDecoded(bitmap, true);
				superclass.callbackList.remove(requestKey);
			}
		}
	}
	
	static abstract class ImageDecodeResult {
		//Creating the view values (for subclass reference)
		final Constants.TaskedViewSource viewSource;
		final WeakReference<View> viewReference;
		
		ImageDecodeResult() {
			viewSource = null;
			viewReference = null;
		}
		
		ImageDecodeResult(Constants.TaskedViewSource viewSource) {
			this.viewSource = viewSource;
			this.viewReference = null;
		}
		
		ImageDecodeResult(View view) {
			this.viewSource = null;
			this.viewReference = new WeakReference<>(view);
		}
		
		abstract void onImageMeasured(int width, int height);
		
		abstract void onImageDecoded(Bitmap result, boolean wasTasked);
	}
	
	private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		//Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;
		
		if(height > reqHeight || width > reqWidth) {
			final int halfHeight = height / 2;
			final int halfWidth = width / 2;
			
			//Calculate the largest inSampleSize value that is a power of 2 and keeps both
			//height and width larger than the requested height and width.
			while((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) inSampleSize *= 2;
		}
		
		return inSampleSize;
	}
}