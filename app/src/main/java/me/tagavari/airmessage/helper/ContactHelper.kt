package me.tagavari.airmessage.helper

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import io.reactivex.rxjava3.core.Maybe
import me.tagavari.airmessage.MainApplication
import java.io.InputStream

object ContactHelper {
	/**
	 * Accepts a nullable address to produce an optional of the user's name or address
	 */
	@JvmStatic
	fun getUserDisplayName(context: Context, address: String?): Maybe<String> {
		return if(address != null) {
			MainApplication.getInstance().userCacheHelper.getUserInfo(context, address)
					.map { userInfo -> userInfo.contactName }
					.toMaybe()
					.onErrorReturnItem(address) as Maybe<String>
		} else {
			Maybe.empty()
		}
	}
	
	/**
	 * Gets a URI of a contact image from a contact ID
	 */
	@JvmStatic
	fun getContactImageURI(contactID: Long): Uri {
		val person = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactID)
		return Uri.withAppendedPath(person, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY)
		//return Uri.withAppendedPath(person, ContactsContract.Contacts.Photo.DISPLAY_PHOTO)
	}
	
	/**
	 * Gets an [InputStream] for a contact's thumbnail,
	 * or null if none is available
	 */
	fun getContactImageThumbnailStream(context: Context, contactID: Long): InputStream? {
		val contactURI = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactID)
		return ContactsContract.Contacts.openContactPhotoInputStream(context.contentResolver, contactURI, false)
		
		/* val photoURI = Uri.withAppendedPath(contactURI, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY)
		val cursor = context.contentResolver.query(
			photoURI,
			arrayOf(ContactsContract.Contacts.Photo.PHOTO),
			null,
			null,
			null
		) ?: return null
		
		cursor.use {
			return it.getBlob(0)
		} */
	}
}