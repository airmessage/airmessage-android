package me.tagavari.airmessage.util

import android.content.res.Resources
import android.net.Uri

/**
 * Represents data of a local Android contact
 * @param contactID The database identifier of this contact
 * @param name The name of this contact
 * @param thumbnailURI A URI to display the thumbnail of this contact
 * @param addresses The list of addresses associated with this contact
 */
class ContactInfo(
	val contactID: Long,
	val name: String?,
	val thumbnailURI: Uri?,
	val addresses: MutableList<AddressInfo>
) {
	fun addAddress(address: AddressInfo) {
		addresses.add(address)
	}
	
	fun getAddressDisplayList(resources: Resources): List<String> {
		return addresses.map { it.getDisplay(resources) }
	}
	
	fun clone(): ContactInfo {
		return ContactInfo(contactID, name, thumbnailURI, addresses.toMutableList())
	}
}