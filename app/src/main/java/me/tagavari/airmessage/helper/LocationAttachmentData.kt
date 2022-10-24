package me.tagavari.airmessage.helper

import android.net.Uri
import androidx.compose.runtime.Immutable
import ezvcard.Ezvcard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.tagavari.airmessage.util.LatLngInfo
import java.io.File
import java.io.IOException

@Immutable
data class LocationAttachmentData(
	val uri: Uri,
	val coords: LatLngInfo
) {
	companion object {
		/*
		Example of VCard on newer iOS versions:
		
		BEGIN:VCARD
		VERSION:3.0
		PRODID:-//Apple Inc.//iPhone OS 14.3//EN
		N:;Current Location;;;
		FN:Current Location
		URL;type=pref:http://maps.apple.com/?ll=XX.XXXXXX\,-XX.XXXXXX&q=XX.XXXXXX\,-XX.XXXXXX
		END:VCARD
		
		There is always only one URL without a group
		
		Example of location VCard on older iOS versions:
		item1.ADR;type=WORK;type=pref:;;Street address;City;State;ZIP code;Country
		item1.X-ABADR:US
		item1.X-APPLE-SUBLOCALITY:Area
		item1.X-APPLE-SUBADMINISTRATIVEAREA:City
		item2.URL;type=pref:http://www.restaurant.com
		item2.X-ABLabel:_$!<HomePage>!$_
		item3.URL:https://maps.apple.com/?q=...
		item3.X-ABLabel:map url
		
		The X-ABLabel parameter with the value "map url" is used to identify the correct group for the map URL
		In this case, the group is "item3"
		
		 */
		
		/**
		 * Creates a [LocationAttachmentData] from a file, or returns null
		 * if the file couldn't be loaded
		 */
		suspend fun fromVCard(file: File): LocationAttachmentData? {
			//Read the VCard file
			@Suppress("BlockingMethodInNonBlockingContext")
			val vcard = withContext(Dispatchers.IO) {
				try {
					Ezvcard.parse(file).first()
				} catch(exception: IOException) {
					null
				}
			} ?: return null
			
			//Find the Apple Maps URL group
			val mapURLGroup = vcard.extendedProperties.firstOrNull { property ->
				property.propertyName == "X-ABLabel"
						&& property.value == "map url"
			}?.group
			
			//Get the URL from the relevant group
			val mapLink = if(mapURLGroup != null) {
				vcard.urls.firstOrNull { it.group == mapURLGroup }
			} else {
				vcard.urls.firstOrNull()
			}
				?.let { Uri.parse(it.value) }
				?: return null
			
			//Pull the coordinates from the map URL
			//See the following link for more details (Apple Map Links documentation)
			//https://developer.apple.com/library/archive/featuredarticles/iPhoneURLScheme_Reference/MapLinks/MapLinks.html
			val stringMapCoords = mapLink.getQueryParameter("ll")?.split(",")
				?: return null
			
			//Make sure we have 2 coordinates
			if(stringMapCoords.size != 2) return null
			
			//Parse the coordinates
			val locationCoords = LatLngInfo(stringMapCoords[0].toDouble(), stringMapCoords[1].toDouble())
			
			return LocationAttachmentData(
				uri = mapLink,
				coords = locationCoords
			)
		}
	}
}
