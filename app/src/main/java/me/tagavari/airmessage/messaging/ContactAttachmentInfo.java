package me.tagavari.airmessage.messaging;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.property.Photo;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.activity.Messaging;
import me.tagavari.airmessage.util.Constants;

public class ContactAttachmentInfo extends AttachmentInfo<ContactAttachmentInfo.ViewHolder> {
	//Creating the reference values
	public static final int ITEM_VIEW_TYPE = MessageComponent.getNextItemViewType();
	public static final int RESOURCE_NAME = R.string.part_content_contact;
	
	private static final int fileStateIdle = 0;
	private static final int fileStateLoading = 1;
	private static final int fileStateLoaded = 2;
	private static final int fileStateFailed = 3;
	
	//Creating the media values
	private int fileState = fileStateIdle;
	private String contactName;
	private Bitmap contactIcon;
	
	public ContactAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize) {
		super(localID, guid, message, fileName, fileType, fileSize);
	}
	
	public ContactAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, File file) {
		super(localID, guid, message, fileName, fileType, fileSize, file);
	}
	
	public ContactAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, byte[] fileChecksum) {
		super(localID, guid, message, fileName, fileType, fileSize, fileChecksum);
	}
	
	public ContactAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, Uri fileUri) {
		super(localID, guid, message, fileName, fileType, fileSize, fileUri);
	}
	
	public static boolean checkFileApplicability(String fileType, String fileName) {
		return Constants.compareMimeTypes("text/vcard", fileType) || Constants.compareMimeTypes("text/x-vcard", fileType);
	}
	
	@Override
	public void updateContentViewColor(ViewHolder viewHolder, Context context, ColorStateList cslText, ColorStateList cslBackground, ColorStateList cslAccent) {
		viewHolder.groupContent.setBackgroundTintList(cslBackground);
		//viewHolder.iconPlaceholder.setImageTintList(cslText);
		viewHolder.labelName.setTextColor(cslText);
	}
	
	@Override
	public void updateContentView(ViewHolder viewHolder, Context context) {
		//Loading the file data if the state is idle
		if(file != null && fileState == fileStateIdle) {
			new LoadContactTask(this, file).execute();
			fileState = fileStateLoading;
		}
		
		//Checking if the file state is invalid
		if(fileState == fileStateFailed) {
			//Showing the failed view
			viewHolder.groupFailed.setVisibility(View.VISIBLE);
		} else {
			//Showing the content view
			viewHolder.groupContentFrame.setVisibility(View.VISIBLE);
			
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
		//Checking if there is no content available
		if(fileState != fileStateLoaded) {
			//Hiding the content view
			viewHolder.groupContentFrame.setVisibility(View.GONE);
			viewHolder.groupFailed.setVisibility(View.VISIBLE);
			return;
		}
		
		//Showing the content view
		viewHolder.groupContentFrame.setVisibility(View.VISIBLE);
		viewHolder.groupFailed.setVisibility(View.GONE);
		
		//Setting the contact name label
		if(contactName == null) viewHolder.labelName.setText(R.string.part_content_contact);
		else viewHolder.labelName.setText(contactName);
		
		//Setting the contact's picture
		if(contactIcon == null) {
			viewHolder.iconPlaceholder.setVisibility(View.VISIBLE);
			viewHolder.iconProfile.setVisibility(View.GONE);
		} else {
			viewHolder.iconPlaceholder.setVisibility(View.GONE);
			viewHolder.iconProfile.setVisibility(View.VISIBLE);
			viewHolder.iconProfile.setImageBitmap(contactIcon);
		}
	}
	
	private static class LoadContactTask extends AsyncTask<Void, Void, Constants.Tuple2<String, Bitmap>> {
		//Creating the values
		private final WeakReference<ContactAttachmentInfo> attachmentReference;
		private final File file;
		
		LoadContactTask(ContactAttachmentInfo attachmentInfo, File file) {
			//Setting the parameters
			attachmentReference = new WeakReference<>(attachmentInfo);
			this.file = file;
		}
		
		@Override
		protected Constants.Tuple2<String, Bitmap> doInBackground(Void... parameters) {
			try {
				//Parsing the file
				VCard vcard = Ezvcard.parse(file).first();
				String name = null;
				Bitmap bitmap = null;
				
				//Getting the name
				if(vcard.getFormattedName() != null) name = vcard.getFormattedName().getValue();
				
				if(!vcard.getPhotos().isEmpty()) {
					//Reading the profile picture
					Photo photo = vcard.getPhotos().get(0);
					byte[] photoData = photo.getData();
					bitmap = BitmapFactory.decodeByteArray(photoData, 0, photoData.length);
					//photo.getUrl();
				}
				
				return new Constants.Tuple2<>(name, bitmap);
			} catch(IOException exception) {
				exception.printStackTrace();
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(Constants.Tuple2<String, Bitmap> result) {
			//Getting the attachment
			ContactAttachmentInfo attachmentInfo = attachmentReference.get();
			if(attachmentInfo == null) return;
			
			//Setting the data
			if(result == null) {
				attachmentInfo.fileState = fileStateFailed;
			} else {
				attachmentInfo.fileState = fileStateLoaded;
				attachmentInfo.contactName = result.item1;
				attachmentInfo.contactIcon = result.item2;
			}
			
			attachmentInfo.updateViewData();
		}
	}
	
	@Override
	public void updateContentViewEdges(ViewHolder viewHolder, Drawable drawable, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight, int pxCornerAnchored, int pxCornerUnanchored) {
		//Assigning the drawable
		viewHolder.groupContent.setBackground(drawable.getConstantState().newDrawable());
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
		return new ViewHolder(buildAttachmentView(LayoutInflater.from(context), parent, R.layout.listitem_contentcontact));
	}
	
	public static class ViewHolder extends AttachmentInfo.ViewHolder {
		final ViewGroup groupContent;
		final ImageView iconProfile;
		final ImageView iconPlaceholder;
		final TextView labelName;
		
		ViewHolder(View view) {
			super(view);
			
			groupContent = groupContentFrame.findViewById(R.id.content);
			iconProfile = groupContent.findViewById(R.id.image_profile);
			iconPlaceholder = groupContent.findViewById(R.id.icon_placeholder);
			labelName = groupContent.findViewById(R.id.label_name);
		}
	}
}
