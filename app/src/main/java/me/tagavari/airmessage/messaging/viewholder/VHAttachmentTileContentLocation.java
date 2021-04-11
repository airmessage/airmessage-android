package me.tagavari.airmessage.messaging.viewholder;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.view.ViewStub;
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

public class VHAttachmentTileContentLocation extends VHAttachmentTileContent {
	public final TextView labelName; //The name of the location
	
	public VHAttachmentTileContentLocation(TextView labelName) {
		this.labelName = labelName;
	}
	
	@Override
	public void bind(@NonNull Context context, @NonNull CompositeDisposable compositeDisposable, @NonNull Union<File, Uri> source, @NonNull String fileName, @NonNull String fileType, long fileSize, long draftID, long dateModified) {
		//Resetting the view
		labelName.setText(R.string.part_content_location);
	}
	
	/**
	 * Applies contact metadata to this bound view
	 * @param metadata The metadata to apply
	 */
	public void applyMetadata(FileDisplayMetadata.LocationSimple metadata) {
		labelName.setText(metadata.getLocationName());
	}
	
	@Override
	public int getAttachmentType() {
		return AttachmentType.location;
	}
}