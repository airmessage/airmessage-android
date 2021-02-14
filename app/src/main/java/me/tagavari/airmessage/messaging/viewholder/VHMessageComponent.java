package me.tagavari.airmessage.messaging.viewholder;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import me.tagavari.airmessage.enums.MessageComponentType;
import me.tagavari.airmessage.util.DisposableViewHolder;

public abstract class VHMessageComponent extends DisposableViewHolder {
	//The container of the entire group
	public final ViewGroup groupContainer;
	
	//Containers for stickers and tapbacks
	public final ViewGroup stickerContainer;
	public final ViewGroup tapbackContainer;
	
	public VHMessageComponent(@NonNull View itemView, ViewGroup groupContainer, ViewGroup stickerContainer, ViewGroup tapbackContainer) {
		super(itemView);
		this.groupContainer = groupContainer;
		this.stickerContainer = stickerContainer;
		this.tapbackContainer = tapbackContainer;
	}
	
	@MessageComponentType
	public abstract int getComponentType();
	
	/**
	 * Updates the rounded corners of this component
	 * @param anchoredTop Whether this view is anchored to a message above
	 * @param anchoredBottom Whether this view is anchored to a message below
	 * @param alignToRight Whether this view is aligned to the right edge of the screen
	 */
	public abstract void updateViewEdges(Context context, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight);
	
	/**
	 * Updates the colors of the view
	 * @param context The context to use
	 * @param colorTextPrimary The color to use for primary text and foreground elements
	 * @param colorTextSecondary The color to use for secondary foreground elements
	 * @param colorBackground The color to use for background elements
	 */
	public abstract void updateViewColoring(Context context, int colorTextPrimary, int colorTextSecondary, int colorBackground);
}