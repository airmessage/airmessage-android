package me.tagavari.airmessage.messaging.viewholder;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public abstract class VHMessagePreview extends RecyclerView.ViewHolder {
	public VHMessagePreview(@NonNull View itemView) {
		super(itemView);
	}
	
	/**
	 * Updates the rounded corners of this component
	 * @param context The context to use
	 * @param anchoredBottom Whether this view is anchored to a message below
	 * @param alignToRight Whether this view is aligned to the right edge of the screen
	 */
	public abstract void updateViewEdges(Context context, boolean anchoredBottom, boolean alignToRight);
}