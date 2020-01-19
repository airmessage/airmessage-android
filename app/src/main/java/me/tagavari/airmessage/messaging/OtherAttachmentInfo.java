package me.tagavari.airmessage.messaging;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;

import me.tagavari.airmessage.util.ConversationUtils;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.activity.Messaging;

public class OtherAttachmentInfo extends AttachmentInfo<OtherAttachmentInfo.ViewHolder> {
	//Creating the reference values
	public static final int ITEM_VIEW_TYPE = MessageComponent.getNextItemViewType();
	//public static final String MIME_PREFIX = "other";
	public static final int RESOURCE_NAME = R.string.part_content_other;
	
	public OtherAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize) {
		super(localID, guid, message, fileName, fileType, fileSize);
	}
	
	public OtherAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, File file) {
		super(localID, guid, message, fileName, fileType, fileSize, file);
	}
	
	public OtherAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, byte[] fileChecksum) {
		super(localID, guid, message, fileName, fileType, fileSize, fileChecksum);
	}
	
	public OtherAttachmentInfo(long localID, String guid, MessageInfo message, String fileName, String fileType, long fileSize, Uri fileUri) {
		super(localID, guid, message, fileName, fileType, fileSize, fileUri);
	}
	
	public static boolean checkFileApplicability(String fileType, String fileName) {
		return true;
	}
	
	@Override
	public void updateContentView(ViewHolder viewHolder, Context context) {
		//Enforcing the maximum content width
		viewHolder.labelFailed.setMaxWidth(ConversationUtils.getMaxMessageWidth(context.getResources()));
		
		//Configuring the content view
		viewHolder.labelFailed.setText(fileName);
		
		//Showing the content view
		viewHolder.groupFailed.setVisibility(View.VISIBLE);
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
		return new ViewHolder(getEmptyAttachmentView(LayoutInflater.from(context), parent));
	}
	
	public static class ViewHolder extends AttachmentInfo.ViewHolder {
		public ViewHolder(View view) {
			super(view);
		}
	}
}
