package me.tagavari.airmessage.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.LruCache;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.reactivex.rxjava3.schedulers.Schedulers;
import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.helper.AddressHelper;

public class UserCacheHelper {
	//Creating the values
	private final LruCache<String, UserInfo> cache;
	private final List<String> failedCache = Collections.synchronizedList(new ArrayList<>());
	
	public UserCacheHelper() {
		//Setting the user cache
		cache = new LruCache<String, UserInfo>((int) (Runtime.getRuntime().maxMemory() / 1024 / 8 / 2)) {
			@Override
			protected int sizeOf(String key, UserInfo userInfo) {
				//Calculating the bitmap's size
				int size = 0;
				if(userInfo.contactName != null) size += userInfo.contactName.getBytes().length;
				size += 8; //Size of long (user ID)
				
				//Returning the size
				return size;
			}
		};
	}
	
	/**
	 * Asynchronously fetches a user's information
	 * @param context The context to use
	 * @param address The address of the user
	 * @return A single for the user's information, or an error if the user wasn't found
	 */
	public Single<UserInfo> getUserInfo(Context context, String address) {
		//Returning if contacts cannot be used
		if(!MainApplication.canUseContacts(context)) {
			return Single.error(new IllegalStateException("Contacts permission not granted"));
		}
		
		//Normalizing the address
		String normalizedAddress = AddressHelper.normalizeAddress(address);
		
		//Checking if there is an entry in the cache
		UserInfo cachedUserInfo = cache.get(normalizedAddress);
		if(cachedUserInfo != null) {
			return Single.just(cachedUserInfo);
		}
		//Otherwise checking if we haven't previously confirmed that this user can't be fetched
		else if(!failedCache.contains(normalizedAddress)) {
			return Single.create((SingleEmitter<UserInfo> emitter) -> {
				UserInfo userInfo;
				try {
					userInfo = fetchUserInfo(context, normalizedAddress);
				} catch(Exception exception) {
					FirebaseCrashlytics.getInstance().recordException(exception);
					emitter.tryOnError(exception);
					return;
				}
				if(userInfo != null) emitter.onSuccess(userInfo);
				else emitter.tryOnError(new RuntimeException("User " + address + " not found"));
			}).subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())
					.doOnSuccess((userInfo) -> {
						//Caching the user info
						cache.put(normalizedAddress, userInfo);
					}).doOnError((error) -> {
						//Marking the user as failed
						failedCache.add(normalizedAddress);
					});
		} else {
			return Single.error(new Throwable("User " + address + " not found"));
		}
	}
	
	/**
	 * Fetches user information directly from Android's contacts database
	 * Only to be called from this helper
	 * @param context The context to use
	 * @param address The address of the user
	 * @return A UserInfo object representing this user, or NULL if not found
	 */
	private static UserInfo fetchUserInfo(Context context, String address) {
		//Getting the content resolver
		ContentResolver contentResolver = context.getContentResolver();
		
		//Querying the database
		Cursor cursor = contentResolver.query(ContactsContract.Data.CONTENT_URI,
				new String[]{ContactsContract.Data.CONTACT_ID, ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts.DISPLAY_NAME},
				ContactsContract.CommonDataKinds.Email.ADDRESS + " = ? OR " + ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER + " = ?", new String[]{address, address},
				null);
		
		//Checking if the cursor is invalid
		if(cursor == null) {
			//Returning null
			return null;
		}
		
		//Checking if there are no results
		if(!cursor.moveToFirst()) {
			//Closing the cursor
			cursor.close();
			
			//Returning null
			return null;
		}
		
		//Getting the data
		long contactID = cursor.getLong(cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID));
		String lookupKey = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY));
		String contactName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
		
		//Closing the cursor
		cursor.close();
		
		//Returning the user info
		return new UserInfo(contactID, lookupKey, contactName);
	}
	
	/**
	 * Holds a user's name and lookup key
	 */
	public static class UserInfo {
		//Creating the values
		private final long contactID;
		private final String lookupKey;
		private final String contactName;
		//private final Uri photoUri;
		
		UserInfo(long contactID, String lookupKey, String contactName) {
			//Setting the values
			this.contactID = contactID;
			this.lookupKey = lookupKey;
			this.contactName = contactName;
		}
		
		public long getContactID() {
			return contactID;
		}
		
		public String getContactName() {
			return contactName;
		}
		
		public String getLookupKey() {
			return lookupKey;
		}
		
		/* public Uri getProfileUri() {
			return Uri.withAppendedPath(ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactID), ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
		} */
		
		public Uri getContactLookupUri() {
			return Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);
		}
	}
	
	/**
	 * Clears all cached data, and forces data to be re-fetched the next time it is requested
	 */
	public void clearCache() {
		cache.evictAll();
		failedCache.clear();
	}
}