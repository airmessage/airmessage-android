package me.tagavari.airmessage.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.util.LruCache;
import android.view.View;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.tagavari.airmessage.util.Constants;
import me.tagavari.airmessage.MainApplication;

public class UserCacheHelper {
	//Creating the values
	private final LruCache<String, UserInfo> userCache;
	private final List<String> failedCache = new ArrayList<>();
	private final Map<String, List<UserFetchResult>> callbackList = new HashMap<>();
	
	public UserCacheHelper() {
		//Setting the user cache
		userCache = new LruCache<String, UserInfo>((int) (Runtime.getRuntime().maxMemory() / 1024 / 8 / 2)) {
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
	
	public void assignUserInfo(Context context, String name, Constants.TaskedViewSource labelViewSource) {
		//Getting the user info
		getUserInfo(context, name, new UserFetchResult(labelViewSource) {
			@Override
			public void onUserFetched(UserInfo userInfo, boolean wasTasked) {
				//Returning if the user info is invalid
				if(userInfo == null) return;
				
				//Assigning the view data
				TextView userLabel = (TextView) viewSource.get(wasTasked);
				if(userLabel != null) userLabel.setText(userInfo.getContactName()); //TODO animate text change
			}
		});
	}
	
	public void assignUserInfo(Context context, String name, View labelView) {
		//Getting the user info
		getUserInfo(context, name, new UserFetchResult(labelView) {
			@Override
			public void onUserFetched(UserInfo userInfo, boolean wasTasked) {
				//Returning if the user info is invalid
				if(userInfo == null) return;
				
				//Assigning the view data
				TextView userLabel = (TextView) viewReference.get();
				if(userLabel != null) userLabel.setText(userInfo.getContactName()); //TODO animate text change
			}
		});
	}
	
	public void getUserInfo(Context context, String name, UserFetchResult callbacks) {
		//Returning if contacts cannot be used
		if(!MainApplication.canUseContacts(context)) {
			callbacks.onUserFetched(null, false);
			return;
		}
		
		//Normalizing the name
		name = Constants.normalizeAddress(name);
		
		//Checking if there is an entry in the cache
		UserInfo userInfo = userCache.get(name);
		
		//Checking if the user info is not cached
		if(userInfo == null && !failedCache.contains(name)) {
			//Adding the listener
			if(callbackList.containsKey(name)) callbackList.get(name).add(callbacks);
			else {
				ArrayList<UserFetchResult> resultList = new ArrayList<>();
				resultList.add(callbacks);
				callbackList.put(name, resultList);
			}
			
			//Starting the task
			new FetchUserInfoTask(name, this, context).execute();
		}
		//Otherwise immediately telling the callback listener
		else callbacks.onUserFetched(userInfo, false);
	}
	
	public UserInfo getUserInfoSync(Context context, String name) {
		//Returning null if contacts cannot be used
		if(!MainApplication.canUseContacts(context)) return null;
		
		//Normalizing the name
		name = Constants.normalizeAddress(name);
		
		//Checking if there is an entry in the cache
		UserInfo userInfo = userCache.get(name);
		
		//Checking if the user info is not cached
		if(userInfo == null && !failedCache.contains(name)) {
			//Fetching the user info
			userInfo = FetchUserInfoTask.getUserInfo(context, name);
			
			//Caching the user info
			if(userInfo == null) synchronized(failedCache) {
				failedCache.add(name);
			}
			else if(userCache.get(name) == null) synchronized(userCache) {
				userCache.put(name, userInfo);
			}
			
			//Returning the user info
			return userInfo;
		}
		
		//Returning the user info
		return userInfo;
	}
	
	private static class FetchUserInfoTask extends AsyncTask<Void, Void, UserInfo> {
		//Creating the values
		private final String name;
		private final WeakReference<UserCacheHelper> superclassReference;
		private final WeakReference<Context> contextReference;
		
		FetchUserInfoTask(String name, UserCacheHelper superclass, Context context) {
			//Setting the values
			this.name = name;
			
			superclassReference = new WeakReference<>(superclass);
			contextReference = new WeakReference<>(context);
		}
		
		@Override
		protected UserInfo doInBackground(Void... parameters) {
			//Returning the user info
			Context context = contextReference.get();
			if(context != null) return getUserInfo(context, name);
			return null;
		}
		
		static UserInfo getUserInfo(Context context, String name) {
			//Getting the content resolver
			ContentResolver contentResolver = context.getContentResolver();
			
			//Querying the database
			Cursor cursor = contentResolver.query(ContactsContract.Data.CONTENT_URI,
					new String[]{ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts.DISPLAY_NAME},
					ContactsContract.CommonDataKinds.Email.ADDRESS + " = ? OR " + ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER + " = ?", new String[]{name, name},
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
			String lookupKey = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY));
			//long contactIdentifier = cursor.getLong(cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID));
			String contactName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
			
			//Closing the cursor
			cursor.close();
			
			//Returning the user info
			return new UserInfo(lookupKey, contactName);
		}
		
		/* private Bitmap getBitmap(ContentResolver contentResolver, long contactID) {
			//Querying for the user icon
			Uri photoUri = Uri.withAppendedPath(ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactID), ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
			Cursor cursor = contentResolver.query(photoUri, new String[]{ContactsContract.Contacts.Photo.PHOTO}, null, null, null);
			if(cursor == null) return null;
			
			//Returning if there is no data
			if(!cursor.moveToFirst()) {
				cursor.close();
				return null;
			}
			
			//Getting the data
			byte[] data = cursor.getBlob(0);
			if(data == null) {
				cursor.close();
				return null;
			}
			
			//Returning the bitmap
			Bitmap bitmap = BitmapFactory.decodeStream(new ByteArrayInputStream(data));
			cursor.close();
			return bitmap;
		} */
		
		@Override
		protected void onPostExecute(UserInfo userInfo) {
			//Getting the superclass
			UserCacheHelper superclass = superclassReference.get();
			if(superclass == null) return;
			
			//Caching the user info
			if(userInfo == null) synchronized(superclass.failedCache) {
				superclass.failedCache.add(name);
			}
			else if(superclass.userCache.get(name) == null) synchronized(superclass.userCache) {
				superclass.userCache.put(name, userInfo);
			}
			
			//Telling the result listeners
			if(superclass.callbackList.containsKey(name)) {
				for(UserFetchResult callback : superclass.callbackList.get(name)) callback.onUserFetched(userInfo, true);
				superclass.callbackList.remove(name);
			}
		}
	}
	
	public static class UserInfo {
		//Creating the values
		private final String lookupKey;
		private final String contactName;
		//private final Uri photoUri;
		
		UserInfo(String lookupKey, String contactName) {
			//Setting the values
			this.lookupKey = lookupKey;
			this.contactName = contactName;
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
	
	public static abstract class UserFetchResult {
		//Creating the view source (for subclass reference)
		protected final Constants.TaskedViewSource viewSource;
		protected final WeakReference<View> viewReference;
		
		public UserFetchResult() {
			viewSource = null;
			viewReference = null;
		}
		
		public UserFetchResult(Constants.TaskedViewSource viewSource) {
			this.viewSource = viewSource;
			viewReference = null;
		}
		
		public UserFetchResult(View view) {
			viewSource = null;
			viewReference = new WeakReference<>(view);
		}
		
		public abstract void onUserFetched(UserInfo userInfo, boolean wasTasked);
	}
	
	public void clearCache() {
		userCache.evictAll();
		failedCache.clear();
	}
}