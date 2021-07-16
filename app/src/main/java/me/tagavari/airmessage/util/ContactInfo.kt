package me.tagavari.airmessage.util

import android.content.res.Resources

/**
 * Represents data of a local Android contact
 * @param identifier The database identifier of this contact
 * @param name The name of this contact
 * @param addresses The list of addresses associated with this contact
 */
class ContactInfo(val identifier: Long, val name: String?, val addresses: MutableList<AddressInfo>) {
	fun addAddress(address: AddressInfo) {
		addresses.add(address)
	}
	
	fun getAddressDisplayList(resources: Resources): List<String> {
		return addresses.map { it.getDisplay(resources) }
	}
	
	fun clone(): ContactInfo {
		return ContactInfo(identifier, name, addresses.toMutableList())
	}
}