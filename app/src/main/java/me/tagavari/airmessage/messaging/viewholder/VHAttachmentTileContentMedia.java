package me.tagavari.airmessage.messaging.viewholder;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.MediaStoreSignature;
import com.bumptech.glide.signature.ObjectKey;

import java.io.File;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import me.tagavari.airmessage.constants.MIMEConstants;
import me.tagavari.airmessage.enums.AttachmentType;
import me.tagavari.airmessage.helper.FileHelper;
import me.tagavari.airmessage.messaging.FileDisplayMetadata;
import me.tagavari.airmessage.util.AudioPlaybackManager;
import me.tagavari.airmessage.util.Union;

public class VHAttachmentTileContentMedia extends VHAttachmentTileContent {
	public final ImageView imageThumbnail; //The main thumbnail view
	public final ImageView imageFlagGIF; //An indicator if this attachment is a GIF
	public final ViewGroup groupVideo; //The view group if this attachment is a video
	public final TextView labelVideo; //The time label if this attachment is a video
	
	public VHAttachmentTileContentMedia(ImageView imageThumbnail, ImageView imageFlagGIF, ViewGroup groupVideo, TextView labelVideo) {
		this.imageThumbnail = imageThumbnail;
		this.imageFlagGIF = imageFlagGIF;
		this.groupVideo = groupVideo;
		this.labelVideo = labelVideo;
	}
	
	@Override
	public void bind(@NonNull Context context, @NonNull CompositeDisposable compositeDisposable, @NonNull Union<File, Uri> source, @NonNull String fileName, @NonNull String fileType, long fileSize, long draftID, long dateModified) {
		RequestBuilder<Drawable> glideRequest = source.apply(Glide.with(context), RequestManager::load, RequestManager::load);
		
		//Applying the signature
		if(draftID != -1) {
			glideRequest = glideRequest.signature(new ObjectKey(draftID));
		} else if(dateModified != -1) {
			glideRequest = glideRequest.signature(new MediaStoreSignature(fileType, dateModified, 0));
		}
		
		glideRequest.apply(RequestOptions.centerCropTransform())
				.transition(DrawableTransitionOptions.withCrossFade())
				.into(imageThumbnail);
		
		//Setting the image flags
		if(FileHelper.compareMimeTypes(fileType, MIMEConstants.mimeTypeGIF)) {
			imageFlagGIF.setVisibility(View.VISIBLE);
			groupVideo.setVisibility(View.GONE);
		} else if(FileHelper.compareMimeTypes(fileType, MIMEConstants.mimeTypeVideo)) {
			imageFlagGIF.setVisibility(View.GONE);
			groupVideo.setVisibility(View.VISIBLE);
			labelVideo.setVisibility(View.GONE);
		} else {
			imageFlagGIF.setVisibility(View.GONE);
			groupVideo.setVisibility(View.GONE);
		}
	}
	
	/**
	 * Applies contact metadata to this bound view
	 * @param metadata The metadata to apply
	 */
	public void applyMetadata(FileDisplayMetadata.Media metadata) {
		labelVideo.setText(DateUtils.formatElapsedTime(metadata.getMediaDuration() / 1000));
		labelVideo.setVisibility(View.VISIBLE);
	}
	
	@Override
	public int getAttachmentType() {
		return AttachmentType.media;
	}
}