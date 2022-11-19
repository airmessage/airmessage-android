package me.tagavari.airmessage.common.util

import android.content.res.Resources
import androidx.compose.runtime.Immutable
import me.tagavari.airmessage.R
import me.tagavari.airmessage.common.helper.AddressHelper

//Represents a presentable and searchable user address
@Immutable
class AddressInfo(
	val address: String, //Address as-is
	val addressLabel: String? //Gets the label of the address
) {
	/**
	 * Gets a normalized representation of the address
	 */
	val normalizedAddress: String by lazy {
		AddressHelper.normalizeAddress(address)
	}
	
	/**
	 * Gets a user-presentable representation of this address with its label
	 */
	fun getDisplay(resources: Resources): String {
		return if(addressLabel == null) {
			address
		} else {
			resources.getString(R.string.label_addressdetails, addressLabel, address)
		}
	}
}