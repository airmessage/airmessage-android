package me.tagavari.airmessage.task;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.CheckReturnValue;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.schedulers.Schedulers;
import me.tagavari.airmessage.helper.AddressHelper;
import me.tagavari.airmessage.helper.MMSSMSHelper;
import me.tagavari.airmessage.helper.StringHelper;
import me.tagavari.airmessage.util.AddressInfo;
import me.tagavari.airmessage.util.ContactInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;

public class ContactsTask {
	private static final Predicate<AddressInfo> predicateAddressPhoneNumber = address -> AddressHelper.validatePhoneNumber(address.getNormalizedAddress());
	
	/**
	 * Loads the user's contacts from Android's database
	 */
	@CheckReturnValue
	public static Observable<ContactAddressPart> loadContacts(Context context) {
		return Observable.create((ObservableEmitter<ContactAddressPart> emitter) -> {
			//Getting the content resolver
			ContentResolver contentResolver = context.getContentResolver();
			
			//Querying the database
			Cursor cursor = contentResolver.query(
					ContactsContract.Data.CONTENT_URI,
					new String[]{ContactsContract.Data.CONTACT_ID, ContactsContract.Data.MIMETYPE, ContactsContract.Data.DISPLAY_NAME, ContactsContract.Data.DATA1, ContactsContract.Data.DATA2, ContactsContract.Data.DATA3},
					ContactsContract.Data.MIMETYPE + " = ? OR (" + ContactsContract.Data.HAS_PHONE_NUMBER + "!= 0 AND " + ContactsContract.Data.MIMETYPE + " = ?)",
					new String[]{ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE},
					ContactsContract.Data.DISPLAY_NAME + " ASC");
			
			//Failing if the cursor is invalid
			if(cursor == null) {
				throw new IllegalStateException("Received NULL cursor from content resolver");
			}
			
			//Reading the data
			int indexContactID = cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID);
			int indexMimeType = cursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE);
			int indexDisplayName = cursor.getColumnIndexOrThrow(ContactsContract.Data.DISPLAY_NAME);
			int indexAddress = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA1); //The address itself (email or phone number)
			int indexAddressType = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA2); //The label ID for this address
			int indexAddressLabel = cursor.getColumnIndexOrThrow(ContactsContract.Data.DATA3); //The custom user-assigned label for this address
			
			while(cursor.moveToNext()) {
				//Retrieving and validating the entry's label
				String address = cursor.getString(indexAddress);
				if(address == null || address.isEmpty()) continue;
				
				//Getting the general info
				long contactID = cursor.getLong(indexContactID);
				String contactName = StringHelper.nullifyEmptyString(cursor.getString(indexDisplayName));
				String addressLabel = null;
				if(!cursor.isNull(indexAddressType)) {
					int addressType = cursor.getInt(indexAddressType);
					if(addressType == ContactsContract.CommonDataKinds.BaseTypes.TYPE_CUSTOM) addressLabel = cursor.getString(indexAddressLabel);
					else addressLabel = MMSSMSHelper.getAddressLabel(context.getResources(), cursor.getString(indexMimeType), addressType);
				}
				AddressInfo addressInfo = new AddressInfo(address, addressLabel);
				
				emitter.onNext(new ContactAddressPart(contactID, contactName, addressInfo));
			}
			
			//Closing the cursor
			cursor.close();
		}).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
	}
	
	/**
	 * Represents a part of a contact, that can be merged into a {@link ContactInfo}
	 */
	public static final class ContactAddressPart {
		private final long id;
		@Nullable private final String name;
		private final AddressInfo address;
		
		public ContactAddressPart(long id, @Nullable String name, AddressInfo address) {
			this.id = id;
			this.name = name;
			this.address = address;
		}
		
		public long getID() {
			return id;
		}
		
		@Nullable
		public String getName() {
			return name;
		}
		
		public AddressInfo getAddress() {
			return address;
		}
	}
	
	/**
	 * Filters the contacts collection based on a query and phone number requirement filter
	 * @param contacts The collection of contacts to filter
	 * @param filterText The text to filter
	 * @param phoneNumbersOnly Whether to filter out contacts that don't have phone numbers
	 * @return An observable stream that outputs matching results
	 */
	@CheckReturnValue
	public static Observable<ContactInfo> searchContacts(@NonNull Collection<ContactInfo> contacts, @Nullable String filterText, boolean phoneNumbersOnly) {
		//Creating the filters
		boolean enableFilter = !TextUtils.isEmpty(filterText);
		String nameFilter, addressFilter, phoneFilter;
		if(enableFilter) {
			nameFilter = filterText.toLowerCase();
			addressFilter = filterText; //Email addresses can't be formatted
			phoneFilter = formatPhoneFilter(filterText);
		} else {
			nameFilter = addressFilter = phoneFilter = null;
		}
		
		return Observable.fromIterable(contacts)
				.observeOn(Schedulers.computation())
				.filter(contact -> {
					//Filtering out contacts that don't include phone numbers
					if(phoneNumbersOnly && contact.getAddresses().stream().noneMatch(predicateAddressPhoneNumber)) {
						return false;
					}
					
					//If there's no filter, accept everything
					if(!enableFilter) return true;
					
					return (contact.getName() != null && contact.getName().toLowerCase().contains(nameFilter)) || //The contact's name matches the filter
						    new ArrayList<>(contact.getAddresses()).stream().anyMatch(address ->
									address.getNormalizedAddress().startsWith(addressFilter) || //The contact's email address matches the filter
											(phoneFilter != null && AddressHelper.validatePhoneNumber(address.getNormalizedAddress()) && AddressHelper.stripPhoneNumber(address.getNormalizedAddress()).startsWith(phoneFilter)) //The contact's phone number matches the filter
							);
				}).observeOn(AndroidSchedulers.mainThread());
	}
	
	@Nullable
	private static String formatPhoneFilter(String filter) {
		//Remove all non-phone number characters
		String strippedFilter = AddressHelper.stripPhoneNumber(filter);
		
		//If the input is empty, the user didn't enter a phone number, so just return null
		if(strippedFilter.isEmpty()) return null;
		
		//Ensure that the number begins with a 1
		if(!strippedFilter.startsWith("1")) strippedFilter = "1" + strippedFilter; //All normalized items start with "1"
		
		return strippedFilter;
	}
}