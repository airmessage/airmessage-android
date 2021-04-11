package me.tagavari.airmessage.helper;

import android.telephony.PhoneNumberUtils;

import java.util.List;
import java.util.ListIterator;

import io.reactivex.rxjava3.annotations.NonNull;
import me.tagavari.airmessage.constants.RegexConstants;

public class AddressHelper {
	/**
	 * Format an address to be user-friendly
	 * @param address The address to format
	 * @return The user-friendly String representation of the address
	 */
	public static String formatAddress(String address) {
		//Returning the E-Mail if the address is one (can't be formatted)
		if(address.contains("@")) return address;
		
		//Returning the formatted phone number if the address is one
		if(PhoneNumberUtils.isWellFormedSmsAddress(address)) {
			String formattedNumber = PhoneNumberUtils.formatNumber(address, "US");
			if(formattedNumber != null) return formattedNumber;
		}
		
		//Returning the address directly (unknown type)
		return address;
	}
	
	/**
	 * Normalize an address for storage or comparison purposes
	 * @param address The address to normalize
	 * @return The normalized String representation of the address
	 */
	public static String normalizeAddress(String address) {
		//Returning the E-Mail if the address is one (can't be normalized)
		if(address.contains("@")) return address;
		
		//Formatting phone numbers to E164
		if(PhoneNumberUtils.isWellFormedSmsAddress(address)) {
			String formattedNumber = PhoneNumberUtils.formatNumberToE164(address, "US");
			if(formattedNumber != null) return formattedNumber;
		}
		
		//Returning the address directly (unknown type)
		return address;
	}
	
	/**
	 * Normalizes an array of addresses
	 */
	public static String[] normalizeAddresses(String[] addresses) {
		//Normalizing the addresses
		for(int i = 0; i < addresses.length; i++) addresses[i] = normalizeAddress(addresses[i]);
		
		//Returning the addresses
		return addresses;
	}
	
	/**
	 * Gets if the provided address is a valid address
	 */
	public static boolean validateAddress(String address) {
		return validateEmail(address) || validatePhoneNumber(address);
	}
	
	/**
	 * Gets if the provided address is a valid email address
	 */
	public static boolean validateEmail(@NonNull String address) {
		return RegexConstants.email.matcher(address).find();
	}
	
	/**
	 * Gets if the provided address is a valid phone number
	 */
	public static boolean validatePhoneNumber(@NonNull String address) {
		return address.replaceAll("[^\\d+]", "").length() >= 3 && address.matches("^\\+?[ \\d().-]+$");
	}
	
	/**
	 * Removes all non-phone number characters from a string
	 */
	public static String stripPhoneNumber(@NonNull String address) {
		return address.replaceAll("[^\\d]", "");
	}
}