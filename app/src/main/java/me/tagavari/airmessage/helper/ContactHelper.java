package me.tagavari.airmessage.helper;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.provider.ContactsContract;

import androidx.annotation.Nullable;

import java.util.Optional;

import io.reactivex.rxjava3.core.Single;
import me.tagavari.airmessage.MainApplication;

public class ContactHelper {
	/**
	 * Accepts a nullable address to produce an optional of the user's name or address
	 */
	public static Single<Optional<String>> getUserDisplayName(Context context, @Nullable String address) {
		if(address != null) {
			return MainApplication.getInstance().getUserCacheHelper().getUserInfo(context, address).map(userInfo -> Optional.of(userInfo.getContactName())).onErrorReturnItem(Optional.of(address));
		} else {
			return Single.just(Optional.empty());
		}
	}
	
	/**
	 * Gets a URI of a contact image from a contact ID
	 */
	public static Uri getContactImageURI(long contactID) {
		Uri person = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactID);
		return Uri.withAppendedPath(person, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
		//return Uri.withAppendedPath(person, ContactsContract.Contacts.Photo.DISPLAY_PHOTO);
	}
}