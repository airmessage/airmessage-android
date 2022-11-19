package me.tagavari.airmessage.common.helper

import android.net.Uri
import android.os.Build
import ezvcard.Ezvcard
import ezvcard.VCard
import ezvcard.VCardDataType
import ezvcard.VCardVersion
import ezvcard.io.text.VCardWriter
import ezvcard.property.FormattedName
import ezvcard.property.ProductId
import ezvcard.property.StructuredName
import ezvcard.property.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.tagavari.airmessage.common.util.LatLngInfo
import java.io.File
import java.io.IOException
import java.sql.Struct
import java.util.*

object MapLocationHelper {
	/**
	 * Gets an Apple Maps URI for the specified location
	 */
	fun getMapUri(location: LatLngInfo): Uri {
		return Uri.Builder()
			.scheme("https")
			.authority("maps.apple.com")
			.appendQueryParameter("ll", "${location.latitude},${location.longitude}")
			.build()
	}
	
	/**
	 * Writes a location VCard to a file
	 */
	suspend fun writeLocationVCard(location: LatLngInfo, file: File) {
		@Suppress("BlockingMethodInNonBlockingContext")
		withContext(Dispatchers.IO) {
			//Create the vCard
			val vcard = VCard()
			vcard.productId = ProductId("-//${Build.MANUFACTURER}//Android ${Build.VERSION.RELEASE}//${Locale.getDefault().language.uppercase()}")
			vcard.structuredName = StructuredName().apply {
				given = "Current Location"
			}
			vcard.formattedName = FormattedName("Current Location")
			
			//Add the URL
			vcard.addUrl(
				Url(getMapUri(location).toString()).apply {
					type = "pref"
				}
			)
			
			//Write the vCard
			Ezvcard.write(vcard).prodId(false).go(file)
		}
	}
}