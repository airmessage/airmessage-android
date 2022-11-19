package me.tagavari.airmessage.common.messaging

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.net.Uri
import ezvcard.Ezvcard
import me.tagavari.airmessage.common.helper.MediaFileHelper
import me.tagavari.airmessage.common.util.LatLngInfo
import me.tagavari.airmessage.common.util.Union
import java.io.File
import java.io.FileInputStream
import java.io.IOException

/**
 * Represents metadata for certain draft types. Please note that the constructor may block.
 * @param context The context to use
 * @param file The file to load
 */
abstract class FileDisplayMetadata(context: Context, file: Union<File, Uri>) {
	class Media(context: Context, file: Union<File, Uri>) : FileDisplayMetadata(context, file) {
		//Creating the attachment info values
		val mediaDuration: Long = file.map(
			{ file -> MediaFileHelper.getMediaDuration(file) },
			{ uri -> MediaFileHelper.getMediaDuration(context, uri) }
		)
	}
	
	class LocationSimple(context: Context, file: Union<File, Uri>) : FileDisplayMetadata(context, file) {
		var locationName: String? = null
			private set
		
		init {
			try {
				(if(file.isA) FileInputStream(file.a) else context.contentResolver.openInputStream(file.b))
					.use { inputStream ->
						if(inputStream != null) {
							//Parsing the file
							val vcard = Ezvcard.parse(inputStream).first()
							
							//Getting the name
							if(vcard.formattedName != null) {
								locationName = vcard.formattedName.value
							}
						}
					}
			} catch(exception: IOException) {
				exception.printStackTrace()
			}
		}
	}
	
	class LocationDetailed(context: Context, file: Union<File, Uri>) : FileDisplayMetadata(context, file) {
		var locationName: String? = null
			private set
		var mapLink: Uri? = null
			private set
		var locationCoords: LatLngInfo? = null
			private set
		var locationAddress: String? = null
			private set
		
		init {
			try {
				(if(file.isA) FileInputStream(file.a) else context.contentResolver.openInputStream(file.b))
					.use { inputStream ->
						if(inputStream != null) {
							//Parsing the file
							val vcard = Ezvcard.parse(inputStream).first()
							
							//Getting the name
							locationName = vcard.formattedName?.value
							
							/*
							The following section extracts the URL from the VCF file
							Here is an example of relevant location data in a VCF file:
								item1.ADR;type=WORK;type=pref:;;Street address;City;State;ZIP code;Country
								item1.X-ABADR:US
								item1.X-APPLE-SUBLOCALITY:Area
								item1.X-APPLE-SUBADMINISTRATIVEAREA:City
								item2.URL;type=pref:http://www.restaurant.com
								item2.X-ABLabel:_$!<HomePage>!$_
								item3.URL:https://maps.apple.com/?q=...
								item3.X-ABLabel:map url
							
							The X-ABLabel parameter, with the value "map url" is used to identify the correct group for the map URL
							In this case, the group is "item3"
							*/
							
							//Finding the Apple Maps URL group
							var mapURLGroup: String? = null
							for(property in vcard.extendedProperties) {
								if("X-ABLabel" != property.propertyName || "map url" != property.value) continue
								mapURLGroup = property.group
								break
							}
							
							//Getting the URL from the relevant group
							if(mapURLGroup != null) {
								for(url in vcard.urls) {
									if(mapURLGroup != url.group) continue
									mapLink = Uri.parse(url.value)
									break
								}
							} else {
								/* On newer iOS versions, all that is included is the URL without a group
								
								Example:
									BEGIN:VCARD
									VERSION:3.0
									PRODID:-//Apple Inc.//iPhone OS 14.3//EN
									N:;Current Location;;;
									FN:Current Location
									URL;type=pref:http://maps.apple.com/?ll=XX.XXXXXX\,-XX.XXXXXX&q=XX.XXXXXX\,-XX.XXXXXX
									END:VCARD
						 		*/
								for(url in vcard.urls) {
									if(url.group != null) continue
									mapLink = Uri.parse(url.value)
									break
								}
							}
							
							if(mapLink != null) {
								//Pulling the coordinates from the map URL
								//See the following link for more details (Apple Map Links documentation)
								//https://developer.apple.com/library/archive/featuredarticles/iPhoneURLScheme_Reference/MapLinks/MapLinks.html
								val stringMapCoords = mapLink!!.getQueryParameter("ll")!!.split(",").toTypedArray()
								locationCoords = LatLngInfo(stringMapCoords[0].toDouble(), stringMapCoords[1].toDouble())
								
								//Reverse-Geocoding the coordinates for a user-friendly location string
								if(Geocoder.isPresent()) {
									val results = Geocoder(context).getFromLocation(
										locationCoords!!.latitude,
										locationCoords!!.longitude,
										1
									)
									if(results != null && results.isNotEmpty()) {
										locationAddress = results[0].getAddressLine(0)
									}
								}
							}
						}
					}
			} catch(exception: IOException) {
				//Printing the stack trace
				exception.printStackTrace()
			} catch(exception: NullPointerException) {
				exception.printStackTrace()
			}
		}
	}
	
	class Contact(context: Context, file: Union<File, Uri>) : FileDisplayMetadata(context, file) {
		var contactName: String? = null
			private set
		var contactIcon: Bitmap? = null
			private set
		
		init {
			try {
				(if(file.isA) FileInputStream(file.a) else context.contentResolver.openInputStream(file.b))
					.use { inputStream ->
						if(inputStream == null) return@use
						
						//Parsing the file
						val vcard = Ezvcard.parse(inputStream).first() ?: return@use
						var name: String? = null
						var bitmap: Bitmap? = null
						
						//Getting the name
						if(vcard.formattedName != null) name = vcard.formattedName.value
						
						//Getting the bitmap
						if(vcard.photos.isNotEmpty()) {
							//Reading the profile picture
							val photo = vcard.photos[0]
							val photoData = photo.data
							if(photoData != null) bitmap = BitmapFactory.decodeByteArray(photoData, 0, photoData.size)
						}
						
						//Setting the information
						contactName = name
						contactIcon = bitmap
					}
			} catch(exception: IOException) {
				exception.printStackTrace()
			}
		}
	}
}