package me.tagavari.airmessage.helper

import android.telephony.PhoneNumberUtils
import io.reactivex.rxjava3.annotations.NonNull
import me.tagavari.airmessage.constants.RegexConstants

object AddressHelper {
	private val regexPhoneNumber = Regex("^\\+?[ \\d().-]+$")
	
	/**
	 * Format an address to be user-friendly
	 * @param address The address to format
	 * @return The user-friendly String representation of the address
	 */
	fun formatAddress(address: String): String {
		//Returning the E-Mail if the address is one (can't be formatted)
		if(address.contains("@")) return address
		
		//Returning the formatted phone number if the address is one
		if(PhoneNumberUtils.isWellFormedSmsAddress(address)) {
			val formattedNumber = PhoneNumberUtils.formatNumber(address, "US")
			if(formattedNumber != null) return formattedNumber
		}
		
		//Returning the address directly (unknown type)
		return address
	}
	
	/**
	 * Normalize an address for storage or comparison purposes
	 * @param address The address to normalize
	 * @return The normalized String representation of the address
	 */
	fun normalizeAddress(address: String): String {
		//Returning the E-Mail if the address is one (can't be normalized)
		if(address.contains("@")) return address
		
		//Formatting phone numbers to E164
		if(PhoneNumberUtils.isWellFormedSmsAddress(address)) {
			val formattedNumber = PhoneNumberUtils.formatNumberToE164(address, "US")
			if(formattedNumber != null) return formattedNumber
		}
		
		//Returning the address directly (unknown type)
		return address
	}
	
	/**
	 * Normalizes an array of addresses
	 */
	fun normalizeAddresses(addresses: Array<String>): Array<String> {
		//Normalizing the addresses
		for(i in addresses.indices) addresses[i] = normalizeAddress(addresses[i])
		
		//Returning the addresses
		return addresses
	}
	
	/**
	 * Gets if the provided address is a valid address
	 */
	fun validateAddress(address: String): Boolean {
		return validateEmail(address) || validatePhoneNumber(address)
	}
	
	/**
	 * Gets if the provided address is a valid email address
	 */
	fun validateEmail(address: String): Boolean {
		return RegexConstants.email.matcher(address).find()
	}
	
	/**
	 * Gets if the provided address is a valid phone number
	 */
	fun validatePhoneNumber(address: String): Boolean {
		return address.replace("[^\\d+]".toRegex(), "").length >= 3 && address.matches(regexPhoneNumber)
	}
	
	/**
	 * Removes all non-phone number characters from a string
	 */
	fun stripPhoneNumber(address: String): String {
		return address.replace("[^\\d]".toRegex(), "")
	}
}