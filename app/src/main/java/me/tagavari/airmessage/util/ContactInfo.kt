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
data class ContactInfo(
	val contactID: Long,
	val name: String?,
	val thumbnailURI: Uri?,
	val addresses: List<AddressInfo>
) {
	fun getAddressDisplayList(resources: Resources): List<String> {
		return addresses.map { it.getDisplay(resources) }
	}
	
	class Builder(
		var contactID: Long
	) {
		var name: String? = null
		var thumbnailURI: Uri? = null
		var addresses: MutableList<AddressInfo> = mutableListOf()
		
		fun build() = ContactInfo(
			contactID = contactID,
			name = name,
			thumbnailURI = thumbnailURI,
			addresses = addresses
		)
	}
}