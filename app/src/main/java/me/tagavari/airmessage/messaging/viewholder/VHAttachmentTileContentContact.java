package me.tagavari.airmessage.messaging.viewholder;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.enums.AttachmentType;
import me.tagavari.airmessage.messaging.FileDisplayMetadata;
import me.tagavari.airmessage.util.AudioPlaybackManager;
import me.tagavari.airmessage.util.Union;

public class VHAttachmentTileContentContact extends VHAttachmentTileContent {
	public final ImageView iconPlaceholder; //An icon to use as a placeholder when no profile picture is present
	public final ImageView iconProfile; //The contact's profile picture
	public final TextView labelName; //The contact's name
	
	public VHAttachmentTileContentContact(ImageView iconPlaceholder, ImageView iconProfile, TextView labelName) {
		this.iconPlaceholder = iconPlaceholder;
		this.iconProfile = iconProfile;
		this.labelName = labelName;
	}
	
	@Override
	public void bind(@NonNull Context context, @NonNull CompositeDisposable compositeDisposable, @NonNull Union<File, Uri> source, @NonNull String fileName, @NonNull String fileType, long fileSize, long draftID, long dateModified) {
		//Resetting the view
		labelName.setText(R.string.part_content_contact);
		iconPlaceholder.setVisibility(View.VISIBLE);
		iconProfile.setVisibility(View.GONE);
	}
	
	/**
	 * Applies contact metadata to this bound view
	 * @param metadata The metadata to apply
	 */
	public void applyMetadata(FileDisplayMetadata.Contact metadata) {
		//Setting the contact name label
		if(metadata.getContactName() != null) labelName.setText(metadata.getContactName());
		else labelName.setText(R.string.part_content_contact);
		
		//Setting the contact's picture
		if(metadata.getContactIcon() == null) {
			iconPlaceholder.setVisibility(View.VISIBLE);
			iconProfile.setVisibility(View.GONE);
		} else {
			iconPlaceholder.setVisibility(View.GONE);
			iconProfile.setVisibility(View.VISIBLE);
			iconProfile.setImageBitmap(metadata.getContactIcon());
		}
	}
	
	@Override
	public int getAttachmentType() {
		return AttachmentType.contact;
	}
}