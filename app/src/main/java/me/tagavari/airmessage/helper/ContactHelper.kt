package me.tagavari.airmessage.helper

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import io.reactivex.rxjava3.core.Maybe
import me.tagavari.airmessage.MainApplication

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
					.onErrorReturnItem(address)
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
}