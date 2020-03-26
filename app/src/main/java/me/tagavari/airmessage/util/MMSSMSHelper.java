package me.tagavari.airmessage.util;

import android.content.Context;
import android.content.res.Resources;
import android.provider.ContactsContract;
import android.telephony.CarrierConfigManager;

import me.tagavari.airmessage.R;

public class MMSSMSHelper {
	/**
	 * Get the maximum size for an attachment to be sent over MMS
	 * @param context The context to use
	 * @return The maximum file size in bytes
	 */
	public static int getMaxMessageSize(Context context) {
		return 300 * 1024;
		//return context.getSystemService(CarrierConfigManager.class).getConfig().getInt(CarrierConfigManager.KEY_MMS_MAX_MESSAGE_SIZE_INT);
	}
	
	/**
	 * Get a human-readable label for a contact
	 * @param resources The resources to use to retrieve the string
	 * @param mimeType The MIME type of this contact entry
	 * @param addressType The address type ID of this entry
	 * @return The label for this address type
	 */
	public static String getAddressLabel(Resources resources, String mimeType, int addressType) {
		if(mimeType.equals(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)) {
			int resourceID;
			switch(addressType) {
				case ContactsContract.CommonDataKinds.Email.TYPE_HOME:
					resourceID = R.string.label_email_home;
					break;
				case ContactsContract.CommonDataKinds.Email.TYPE_WORK:
					resourceID = R.string.label_email_work;
					break;
				case ContactsContract.CommonDataKinds.Email.TYPE_OTHER:
					resourceID = R.string.label_email_other;
					break;
				default:
					return null;
			}
			
			return resources.getString(resourceID);
		} else if(mimeType.equals(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
			int resourceID;
			switch(addressType) {
				case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
					resourceID = R.string.label_phone_mobile;
					break;
				case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
					resourceID = R.string.label_email_work;
					break;
				case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
					resourceID = R.string.label_phone_home;
					break;
				case ContactsContract.CommonDataKinds.Phone.TYPE_MAIN:
					resourceID = R.string.label_phone_main;
					break;
				case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK:
					resourceID = R.string.label_phone_workfax;
					break;
				case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME:
					resourceID = R.string.label_phone_homefax;
					break;
				case ContactsContract.CommonDataKinds.Phone.TYPE_PAGER:
					resourceID = R.string.label_phone_pager;
					break;
				case ContactsContract.CommonDataKinds.Phone.TYPE_OTHER:
					resourceID = R.string.label_phone_other;
					break;
				default:
					return null;
			}
			
			return resources.getString(resourceID);
		} else {
			return null;
		}
	}
}