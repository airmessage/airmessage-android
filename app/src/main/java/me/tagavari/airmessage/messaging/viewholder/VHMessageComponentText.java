package me.tagavari.airmessage.messaging.viewholder;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import me.tagavari.airmessage.R;
import me.tagavari.airmessage.enums.MessageComponentType;
import me.tagavari.airmessage.helper.MessageShapeHelper;
import me.tagavari.airmessage.view.InvisibleInkView;

public class VHMessageComponentText extends VHMessageComponent {
	public final ViewGroup content;
	public final ViewGroup groupMessage;
	public final TextView labelBody;
	public final TextView labelSubject;
	public final InvisibleInkView inkView;
	
	//The container for the preview
	public final ViewGroup messagePreviewContainer;
	public VHMessagePreviewLink messagePreviewViewHolder;
	
	public VHMessageComponentText(@NonNull View itemView, ViewGroup groupContainer, ViewGroup stickerContainer, ViewGroup tapbackContainer, ViewGroup content, ViewGroup groupMessage, TextView labelBody, TextView labelSubject, InvisibleInkView inkView, ViewGroup messagePreviewContainer) {
		super(itemView, groupContainer, stickerContainer, tapbackContainer);
		this.content = content;
		this.groupMessage = groupMessage;
		this.labelBody = labelBody;
		this.labelSubject = labelSubject;
		this.inkView = inkView;
		this.messagePreviewContainer = messagePreviewContainer;
	}
	
	@Override
	public int getComponentType() {
		return MessageComponentType.text;
	}
	
	@Override
	public void updateViewEdges(Context context, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight) {
		//Checking if we have a preview
		if(messagePreviewViewHolder != null) {
			//Updating the message text bubble's background
			groupMessage.setBackground(MessageShapeHelper.createRoundedMessageDrawableTop(context.getResources(), anchoredTop, alignToRight));
			
			//Updating the ink view's radius
			inkView.setRadii(MessageShapeHelper.createStandardRadiusArrayTop(context.getResources(), anchoredTop, alignToRight));
			
			//Updating the preview view's background
			messagePreviewViewHolder.updateViewEdges(context, anchoredBottom, alignToRight);
		} else {
			//Updating the message text bubble's background
			groupMessage.setBackground(MessageShapeHelper.createRoundedMessageDrawable(context.getResources(), anchoredTop, anchoredBottom, alignToRight));
			
			//Updating the ink view's radius
			inkView.setRadii(MessageShapeHelper.createStandardRadiusArray(context.getResources(), anchoredTop, anchoredBottom, alignToRight));
		}
	}
	
	@Override
	public void updateViewColoring(Context context, int colorTextPrimary, int colorTextSecondary, int colorBackground) {
		labelBody.setTextColor(colorTextPrimary);
		labelBody.setLinkTextColor(colorTextPrimary);
		labelSubject.setTextColor(colorTextPrimary);
		groupMessage.setBackgroundTintList(ColorStateList.valueOf(colorBackground));
		
		inkView.setBackgroundColor(colorBackground);
	}
}