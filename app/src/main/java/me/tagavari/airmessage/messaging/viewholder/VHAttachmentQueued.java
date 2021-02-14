package me.tagavari.airmessage.messaging.viewholder;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.io.File;

import me.tagavari.airmessage.util.DisposableViewHolder;
import me.tagavari.airmessage.util.Union;

/**
 * A view holder to handle different types of attachment tiles in the queue
 */
public class VHAttachmentQueued extends DisposableViewHolder {
	//Creating the view values
	public final ImageButton buttonRemove;
	public final ViewGroup contentContainer;
	public final VHAttachmentTileContent content;
	
	/**
	 * Constructs a new attachment tile view holder for a queue list
	 * @param itemView The base item view
	 * @param buttonRemove The button to remove this item from the queue
	 * @param contentContainer The container for the content of this queued item
	 * @param content The primary content of this attachment tile
	 */
	public VHAttachmentQueued(@NonNull View itemView, ImageButton buttonRemove, ViewGroup contentContainer, VHAttachmentTileContent content) {
		super(itemView);
		
		this.buttonRemove = buttonRemove;
		this.contentContainer = contentContainer;
		
		this.content = content;
	}
	
	/**
	 * Binds a file to this view
	 * @param context The context to use
	 * @param source The file, either a {@link File} or {@link Uri}
	 * @param fileName The name of the file
	 * @param fileType The MIME type of the file
	 * @param fileSize The size of the file
	 * @param draftID The file's draft ID (or -1 if unavailable)
	 * @param dateModified The file's media provider modification date (or -1 if unavailable)
	 */
	public void bind(@NonNull Context context, @NonNull Union<File, Uri> source, @NonNull String fileName, @NonNull String fileType, long fileSize, long draftID, long dateModified) {
		content.bind(context, getCompositeDisposable(), source, fileName, fileType, fileSize, draftID, dateModified);
	}
	
	/**
	 * Updates the view to represent whether this attachment has been successfully processed
	 * @param isProcessed Whether this attachment is processed
	 * @param animate Whether to animate the change
	 */
	public void setAppearanceState(boolean isProcessed, boolean animate) {
		if(animate) {
			contentContainer.animate().alpha(isProcessed ? 1 : 0.5F);
		} else {
			contentContainer.setAlpha(isProcessed ? 1 : 0.5F);
		}
	}
}