package me.tagavari.airmessage.data

import me.tagavari.airmessage.helper.AddressHelper.normalizeAddress
import me.tagavari.airmessage.MainApplication
import me.tagavari.airmessage.helper.AddressHelper
import io.reactivex.rxjava3.core.SingleOnSubscribe
import io.reactivex.rxjava3.core.SingleEmitter
import me.tagavari.airmessage.data.UserCacheHelper
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import android.provider.ContactsContract
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.LruCache
import io.reactivex.rxjava3.core.Single
import java.lang.Exception
import java.lang.IllegalStateException
import java.lang.RuntimeException
import java.util.*

class UserCacheHelper {
	//Creating the values
	private val cache = object : LruCache<String, UserInfo>((Runtime.getRuntime().maxMemory() / 1024 / 8 / 2).toInt()) {
		override fun sizeOf(key: String, userInfo: UserInfo): Int {
			//Calculating the bitmap's size
			var size = 0
			if(userInfo.contactName != null) size += userInfo.contactName.toByteArray().size
			size += 8 //Size of long (user ID)
			
			//Returning the size
			return size
		}
	}
	private val failedCache = Collections.synchronizedList(mutableListOf<String>())
	
	/**
	 * Asynchronously fetches a user's information
	 * @param context The context to use
	 * @param address The address of the user
	 * @return A single for the user's information, or an error if the user wasn't found
	 */
	fun getUserInfo(context: Context, address: String): Single<UserInfo> {
		//Returning if contacts cannot be used
		if(!MainApplication.canUseContacts(context)) {
			return Single.error(IllegalStateException("Contacts permission not granted"))
		}
		
		//Normalizing the address
		val normalizedAddress = normalizeAddress(address)
		
		//Checking if there is an entry in the cache
		val cachedUserInfo = cache[normalizedAddress]
		return if(cachedUserInfo != null) {
			Single.just(cachedUserInfo)
		} else if(!failedCache.contains(normalizedAddress)) {
			Single.create { emitter: SingleEmitter<UserInfo> ->
				try {
					val userInfo = fetchUserInfo(context, normalizedAddress)
					if(userInfo != null) {
						emitter.onSuccess(userInfo)
					} else {
						emitter.tryOnError(RuntimeException("User $address not found"))
					}
				} catch(exception: Exception) {
					FirebaseCrashlytics.getInstance().recordException(exception)
					emitter.tryOnError(exception)
				}
			}
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.doOnSuccess { userInfo: UserInfo ->
					//Caching the user info
					cache.put(normalizedAddress, userInfo)
				}.doOnError {
					//Marking the user as failed
					failedCache.add(normalizedAddress)
				}
		} else {
			Single.error(Throwable("User $address not found"))
		}
	}
	
	/**
	 * Holds a user's name and lookup key
	 */
	data class UserInfo(val contactID: Long, val lookupKey: String, val contactName: String?) {
		val contactLookupUri: Uri
			get() = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey)
	}
	
	/**
	 * Clears all cached data, and forces data to be re-fetched the next time it is requested
	 */
	fun clearCache() {
		cache.evictAll()
		failedCache.clear()
	}
	
	companion object {
		/**
		 * Fetches user information directly from Android's contacts database
		 * Only to be called from this helper
		 * @param context The context to use
		 * @param address The address of the user
		 * @return A UserInfo object representing this user, or NULL if not found
		 */
		private fun fetchUserInfo(context: Context, address: String): UserInfo? {
			//Getting the content resolver
			val contentResolver = context.contentResolver
			
			//Querying the database
			(contentResolver.query(
				ContactsContract.Data.CONTENT_URI,
				arrayOf(ContactsContract.Data.CONTACT_ID, ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts.DISPLAY_NAME),
				ContactsContract.CommonDataKinds.Email.ADDRESS + " = ? OR " + ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER + " = ?",
				arrayOf(address, address),
				null
			) ?: return null).use { cursor ->
				//Returning null if there are no results
				if(!cursor.moveToFirst()) return null
				
				//Returning the user info
				return UserInfo(
					contactID = cursor.getLong(cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)),
					lookupKey = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)),
					contactName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
				)
			}
		}
	}
}