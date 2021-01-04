package me.tagavari.airmessage.messaging;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;

import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.property.Photo;
import ezvcard.property.RawProperty;
import ezvcard.property.Url;
import me.tagavari.airmessage.helper.MediaFileHelper;
import me.tagavari.airmessage.util.Union;

/**
 * Represents metadata for certain draft types
 */
public abstract class FileDisplayMetadata {
	/**
	 * Initializes this metadata. Please note that this function may block.
	 * @param context The context to use
	 * @param file The file to load
	 */
	public FileDisplayMetadata(Context context, Union<File, Uri> file) {
	
	}
	
	public static class Media extends FileDisplayMetadata {
		//Creating the attachment info values
		private final long mediaDuration;
		
		public Media(Context context, Union<File, Uri> file) {
			super(context, file);
			
			mediaDuration = file.map(MediaFileHelper::getMediaDuration, uri -> MediaFileHelper.getMediaDuration(context, uri));
		}
		
		public long getMediaDuration() {
			return mediaDuration;
		}
	}
	
	public static class LocationSimple extends FileDisplayMetadata {
		private String locationName = null;
		
		public LocationSimple(Context context, Union<File, Uri> file) {
			super(context, file);
			
			try(InputStream inputStream = file.getA() != null ? new FileInputStream(file.getA()) : context.getContentResolver().openInputStream(file.getB())) {
				if(inputStream == null) return;
				
				//Parsing the file
				VCard vcard = Ezvcard.parse(inputStream).first();
				
				//Getting the name
				if(vcard.getFormattedName() != null) locationName = vcard.getFormattedName().getValue();
			} catch(IOException exception) {
				exception.printStackTrace();
			}
		}
		
		public String getLocationName() {
			return locationName;
		}
	}
	
	public static class LocationDetailed extends FileDisplayMetadata {
		private String locationName = null;
		private Uri mapLink = null;
		private LatLng locationCoords = null;
		private String locationAddress = null;
		
		public LocationDetailed(Context context, Union<File, Uri> file) {
			super(context, file);
			
			try(InputStream inputStream = file.getA() != null ? new FileInputStream(file.getA()) : context.getContentResolver().openInputStream(file.getB())) {
				if(inputStream == null) return;
				
				//Parsing the file
				VCard vcard = Ezvcard.parse(inputStream).first();
				
				//Getting the name
				if(vcard.getFormattedName() != null) locationName = vcard.getFormattedName().getValue();
				
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
				String mapURLGroup = null;
				for(RawProperty property : vcard.getExtendedProperties()) {
					if(!"X-ABLabel".equals(property.getPropertyName()) || !"map url".equals(property.getValue())) continue;
					mapURLGroup = property.getGroup();
					break;
				}
				
				//Getting the URL from the relevant group
				if(mapURLGroup != null) {
					for(Url url : vcard.getUrls()) {
						if(!mapURLGroup.equals(url.getGroup())) continue;
						mapLink = Uri.parse(url.getValue());
						break;
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
					for(Url url : vcard.getUrls()) {
						if(url.getGroup() != null) continue;
						mapLink = Uri.parse(url.getValue());
						break;
					}
				}
				
				if(mapLink != null) {
					//Pulling the coordinates from the map URL
					//See the following link for more details (Apple Map Links documentation)
					//https://developer.apple.com/library/archive/featuredarticles/iPhoneURLScheme_Reference/MapLinks/MapLinks.html
					String[] stringMapCoords = mapLink.getQueryParameter("ll").split(",");
					locationCoords = new LatLng(Double.parseDouble(stringMapCoords[0]), Double.parseDouble(stringMapCoords[1]));
					
					//Reverse-Geocoding the coordinates for a user-friendly location string
					if(Geocoder.isPresent()) {
						List<Address> results = new Geocoder(context).getFromLocation(locationCoords.latitude, locationCoords.longitude, 1);
						if(results != null && !results.isEmpty()) {
							locationAddress = results.get(0).getAddressLine(0);
						}
					}
				}
			} catch(IOException | NullPointerException exception) {
				//Printing the stack trace
				exception.printStackTrace();
			}
		}
		
		public String getLocationName() {
			return locationName;
		}
		
		public Uri getMapLink() {
			return mapLink;
		}
		
		public LatLng getLocationCoords() {
			return locationCoords;
		}
		
		public String getLocationAddress() {
			return locationAddress;
		}
	}
	
	public static class Contact extends FileDisplayMetadata {
		private String contactName;
		private Bitmap contactIcon;
		
		public Contact(Context context, Union<File, Uri> file) {
			super(context, file);
			
			try(InputStream inputStream = file.getA() != null ? new FileInputStream(file.getA()) : context.getContentResolver().openInputStream(file.getB())) {
				if(inputStream == null) return;
				
				//Parsing the file
				VCard vcard = Ezvcard.parse(inputStream).first();
				if(vcard == null) return;
				String name = null;
				Bitmap bitmap = null;
				
				//Getting the name
				if(vcard.getFormattedName() != null) name = vcard.getFormattedName().getValue();
				
				//Getting the bitmap
				if(!vcard.getPhotos().isEmpty()) {
					//Reading the profile picture
					Photo photo = vcard.getPhotos().get(0);
					byte[] photoData = photo.getData();
					if(photoData != null) bitmap = BitmapFactory.decodeByteArray(photoData, 0, photoData.length);
				}
				
				//Setting the information
				contactName = name;
				contactIcon = bitmap;
			} catch(IOException exception) {
				exception.printStackTrace();
			}
		}
		
		public String getContactName() {
			return contactName;
		}
		
		public Bitmap getContactIcon() {
			return contactIcon;
		}
	}
}