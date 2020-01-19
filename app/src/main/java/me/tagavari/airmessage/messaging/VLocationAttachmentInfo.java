package me.tagavari.airmessage.messaging;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.property.RawProperty;
import ezvcard.property.Url;
import me.tagavari.airmessage.util.ConversationUtils;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.activity.Messaging;
import me.tagavari.airmessage.activity.Preferences;
import me.tagavari.airmessage.util.Constants;
import me.tagavari.airmessage.view.RoundedFrameLayout;

public class VLocationAttachmentInfo extends AttachmentInfo<VLocationAttachmentInfo.ViewHolder> {
	//Creating the reference values
	public static final int ITEM_VIEW_TYPE = MessageComponent.getNextItemViewType();
	public static final String MIME_TYPE = "text/x-vlocation";
	public static final int RESOURCE_NAME = R.string.part_content_location;
	
	private static final int fileStateIdle = 0;
	private static final int fileStateLoading = 1;
	private static final int fileStateLoaded = 2;
	private static final int fileStateFailed = 3;
	
	//Creating the media values
	private int fileState = fileStateIdle;
	private LocationData locationData = null;
	
	public VLocationAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize) {
		super(localID, guid, message, fileName, fileType, fileSize);
	}
	
	public VLocationAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, File file) {
		super(localID, guid, message, fileName, fileType, fileSize, file);
	}
	
	public VLocationAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, byte[] fileChecksum) {
		super(localID, guid, message, fileName, fileType, fileSize, fileChecksum);
	}
	
	public VLocationAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, Uri fileUri) {
		super(localID, guid, message, fileName, fileType, fileSize, fileUri);
	}
	
	public static boolean checkFileApplicability(String fileType, String fileName) {
		return Constants.compareMimeTypes(MIME_TYPE, fileType);
	}
	
	@Override
	public void updateContentViewColor(ViewHolder viewHolder, Context context, ColorStateList cslText, ColorStateList cslBackground, ColorStateList cslAccent) {
		//viewHolder.groupContent.setBackgroundTintList(cslBackground);
		//viewHolder.iconPlaceholder.setImageTintList(cslText);
		//viewHolder.labelTitle.setTextColor(cslText);
		//viewHolder.labelAddress.setTextColor(cslText);
		if(viewHolder.googleMap != null) updateMapTheme(viewHolder.googleMap, context);
	}
	
	@Override
	public void updateContentView(ViewHolder viewHolder, Context context) {
		//Setting the view width
		viewHolder.groupContent.getLayoutParams().width = ConversationUtils.getMaxMessageWidth(context.getResources());
		
		//Loading the file data if the state is idle
		if(file != null && fileState == fileStateIdle) {
			new LoadLocationTask(this, file, context).execute();
			fileState = fileStateLoading;
		}
		
		//Checking if the file state is invalid
		if(fileState == fileStateFailed) {
			//Showing the failed view
			viewHolder.groupFailed.setVisibility(View.VISIBLE);
		} else {
			//Updating the view data
			updateViewData(viewHolder);
		}
	}
	
	public void updateViewData() {
		//Calling the overload method
		ViewHolder viewHolder = getViewHolder();
		if(viewHolder != null) updateViewData(viewHolder);
	}
	
	private void updateViewData(ViewHolder viewHolder) {
		//Checking if there is no data, or there was a problem loading the data
		if(locationData == null || fileState != fileStateLoaded) {
			//Hiding the content view
			viewHolder.groupContentFrame.setVisibility(View.GONE);
			viewHolder.groupFailed.setVisibility(View.VISIBLE);
			return;
		}
		
		//Showing the content view
		viewHolder.groupContentFrame.setVisibility(View.VISIBLE);
		viewHolder.groupFailed.setVisibility(View.GONE);
		
		//Setting the click listener
		viewHolder.groupContent.setOnClickListener(view -> Constants.launchUri(viewHolder.groupContent.getContext(), locationData.mapLink));
		viewHolder.groupContent.setOnLongClickListener(clickView -> {
			//Getting the context
			Context context = clickView.getContext();
			
			//Returning if the view is not an activity
			if(!(context instanceof Activity)) return false;
			
			//Displaying the context menu
			displayContextMenu(clickView, context);
			
			//Returning
			return true;
		});
		
		//Setting the location title
		if(locationData.title != null) viewHolder.labelTitle.setText(locationData.title);
		else viewHolder.labelTitle.setText(R.string.message_locationtitle_unknown);
		
		//Setting the address
		if(locationData.mapLocName != null) viewHolder.labelAddress.setText(locationData.mapLocName);
		else {
			if(locationData.mapLocPos != null) viewHolder.labelAddress.setText(Constants.locationToString(locationData.mapLocPos));
			else viewHolder.labelTitle.setText(R.string.message_locationaddress_unknown);
		}
		
		//Setting the map preview
		if(locationData.mapLocPos == null || !Preferences.getPreferenceMessagePreviews(viewHolder.itemView.getContext())) {
			viewHolder.frameHeader.setVisibility(View.GONE);
		} else {
			//Showing the map view and setting it as non-clickable (so that the card view parent will handle clicks instead)
			viewHolder.frameHeader.setVisibility(View.VISIBLE);
			viewHolder.mapHeader.setClickable(false);
			
			if(viewHolder.googleMap != null) {
				//Updating the current map, if it is available
				updateMapLocation(viewHolder.googleMap);
				updateMapTheme(viewHolder.googleMap, viewHolder.groupContent.getContext());
			} else {
				//Creating the Google map anew
				MapReadyCallback callback = new MapReadyCallback(this);
				callback.tempViewHolder = viewHolder;
				viewHolder.mapHeader.getMapAsync(callback);
				viewHolder.mapHeader.onCreate(null);
				viewHolder.mapHeader.onResume();
				viewHolder.mapHeader.post(() -> callback.tempViewHolder = null);
			}
		}
	}
	
	public static void updateMapTheme(GoogleMap googleMap, Context context) {
		googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, Constants.isNightMode(context.getResources()) ? R.raw.map_dark : R.raw.map_light));
	}
	
	public void updateMapLocation(GoogleMap googleMap) {
		LatLng targetLocation = locationData.mapLocPos;
		googleMap.clear();
		googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(targetLocation, 15));
		MarkerOptions markerOptions = new MarkerOptions().position(targetLocation);
		googleMap.addMarker(markerOptions);
	}
	
	private static class MapReadyCallback implements OnMapReadyCallback {
		private final WeakReference<VLocationAttachmentInfo> attachmentReference;
		ViewHolder tempViewHolder = null;
		
		MapReadyCallback(VLocationAttachmentInfo attachment) {
			//Setting the attachment reference
			attachmentReference = new WeakReference<>(attachment);
		}
		
		@Override
		public void onMapReady(GoogleMap googleMap) {
			//Getting the attachment
			VLocationAttachmentInfo attachment = attachmentReference.get();
			if(attachment == null) return;
			
			ViewHolder viewHolder;
			if(tempViewHolder != null) viewHolder = tempViewHolder;
			else viewHolder = attachment.getViewHolder();
			if(viewHolder == null) return;
			
			//Updating the view holder information
			viewHolder.googleMap = googleMap;
			
			//Configuring the map
			googleMap.getUiSettings().setMapToolbarEnabled(false);
			updateMapTheme(googleMap, viewHolder.mapHeader.getContext());
			attachment.updateMapLocation(googleMap);
		}
	}
	
	private static class LocationData {
		String title = null;
		Uri mapLink = null;
		LatLng mapLocPos = null;
		String mapLocName = null;
	}
	
	private static class LoadLocationTask extends AsyncTask<Void, Void, LocationData> {
		//Creating the values
		private final WeakReference<VLocationAttachmentInfo> attachmentReference;
		private final File file;
		private Geocoder geocoder = null;
		
		LoadLocationTask(VLocationAttachmentInfo attachmentInfo, File file, Context context) {
			//Setting the parameters
			attachmentReference = new WeakReference<>(attachmentInfo);
			this.file = file;
			if(Geocoder.isPresent()) geocoder = new Geocoder(context);
		}
		
		@Override
		protected LocationData doInBackground(Void... parameters) {
			LocationData locationData = new LocationData();
			
			try {
				//Parsing the file
				VCard vcard = Ezvcard.parse(file).first();
				
				//Getting the title
				if(vcard.getFormattedName() != null) locationData.title = vcard.getFormattedName().getValue();
				
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
						locationData.mapLink = Uri.parse(url.getValue());
						break;
					}
				}
				
				//Pulling the coordinates from the map URL
				//See the following link for more details (Apple Map Links documentation)
				//https://developer.apple.com/library/archive/featuredarticles/iPhoneURLScheme_Reference/MapLinks/MapLinks.html
				String[] stringMapCoords = locationData.mapLink.getQueryParameter("ll").split(",");
				locationData.mapLocPos = new LatLng(Double.parseDouble(stringMapCoords[0]), Double.parseDouble(stringMapCoords[1]));
				
				//Reverse-Geocoding the coordinates for a user-friendly location string
				if(Geocoder.isPresent()) {
					List<Address> results = geocoder.getFromLocation(locationData.mapLocPos.latitude, locationData.mapLocPos.longitude, 1);
					if(results != null && !results.isEmpty()) {
						locationData.mapLocName = results.get(0).getAddressLine(0);
					}
				}
			} catch(IOException | NullPointerException exception) {
				//Printing the stack trace
				exception.printStackTrace();
			}
			
			//Returning any available data
			return locationData;
		}
		
		@Override
		protected void onPostExecute(LocationData result) {
			//Getting the attachment
			VLocationAttachmentInfo attachmentInfo = attachmentReference.get();
			if(attachmentInfo == null) return;
			
			//Setting the data
			if(result == null || result.mapLink == null) {
				attachmentInfo.fileState = fileStateFailed;
			} else {
				attachmentInfo.fileState = fileStateLoaded;
				attachmentInfo.locationData = result;
			}
			
			attachmentInfo.updateViewData();
		}
	}
	
	@Override
	public void updateContentViewEdges(ViewHolder viewHolder, Drawable drawable, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight, int pxCornerAnchored, int pxCornerUnanchored) {
		//Assigning the border shape
		viewHolder.viewBorder.setBackground(Constants.createRoundedDrawable((GradientDrawable) viewHolder.viewBorder.getResources().getDrawable(R.drawable.rectangle_chatpreviewfull, null).mutate().getConstantState().newDrawable(), anchoredTop, anchoredBottom, alignToRight, pxCornerUnanchored, pxCornerAnchored));
		
		//Updating the touch ripple
		RippleDrawable rippleDrawable = (RippleDrawable) Constants.resolveDrawableAttr(viewHolder.groupContent.getContext(), android.R.attr.selectableItemBackground); //Ripple drawable from Android attributes
		Drawable shapeDrawable = Constants.createRoundedDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xFFFFFFFF, 0xFFFFFFFF}), anchoredTop, anchoredBottom, alignToRight, pxCornerUnanchored, pxCornerAnchored); //Getting a standard drawable for the shape
		rippleDrawable.setDrawableByLayerId(android.R.id.mask, shapeDrawable); //Applying the drawable to the ripple drawable
		viewHolder.groupContent.setForeground(rippleDrawable);
		
		//Updating the map clip
		int cornerRadius = anchoredTop ? pxCornerAnchored : pxCornerUnanchored;
		int radiusLeft, radiusRight;
		if(alignToRight) {
			radiusLeft = pxCornerUnanchored;
			radiusRight = cornerRadius;
		} else {
			radiusLeft = cornerRadius;
			radiusRight = pxCornerUnanchored;
		}
		viewHolder.frameHeader.setRadii(radiusLeft, radiusRight, 0, 0);
	}
	
	@Override
	public void onClick(Messaging activity) {
		openAttachmentFile(activity);
	}
	
	@Override
	public int getItemViewType() {
		return ITEM_VIEW_TYPE;
	}
	
	@Override
	public int getResourceTypeName() {
		return RESOURCE_NAME;
	}
	
	@Override
	public ViewHolder createViewHolder(Context context, ViewGroup parent) {
		return new ViewHolder(buildAttachmentView(LayoutInflater.from(context), parent, R.layout.listitem_contentlocation));
	}
	
	static class ViewHolder extends AttachmentInfo.ViewHolder {
		final ViewGroup groupContent;
		final View viewBorder;
		final RoundedFrameLayout frameHeader;
		final MapView mapHeader;
		final TextView labelTitle;
		final TextView labelAddress;
		
		GoogleMap googleMap;
		
		public ViewHolder(View view) {
			super(view);
			
			groupContent = groupContentFrame.findViewById(R.id.content);
			viewBorder = groupContent.findViewById(R.id.view_border);
			frameHeader = groupContent.findViewById(R.id.frame_header);
			mapHeader = frameHeader.findViewById(R.id.map_header);
			labelTitle = groupContent.findViewById(R.id.label_title);
			labelAddress = groupContent.findViewById(R.id.label_address);
		}
		
		@Override
		public void pause() {
			if(googleMap != null) mapHeader.onPause();
		}
		
		@Override
		public void resume() {
			if(googleMap != null) mapHeader.onResume();
		}
	}
}
