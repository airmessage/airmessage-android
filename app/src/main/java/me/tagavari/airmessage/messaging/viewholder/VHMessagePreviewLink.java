package me.tagavari.airmessage.messaging.viewholder;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.Resource;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;

import me.tagavari.airmessage.R;
import me.tagavari.airmessage.helper.MessageShapeHelper;
import me.tagavari.airmessage.helper.ResourceHelper;

public class VHMessagePreviewLink extends VHMessagePreview {
	public final View viewBorder;
	public final ImageView imageHeader;
	public final TextView labelTitle;
	public final TextView labelDescription;
	public final TextView labelAddress;
	
	public VHMessagePreviewLink(@NonNull View itemView, View viewBorder, ImageView imageHeader, TextView labelTitle, TextView labelDescription, TextView labelAddress) {
		super(itemView);
		this.viewBorder = viewBorder;
		this.imageHeader = imageHeader;
		this.labelTitle = labelTitle;
		this.labelDescription = labelDescription;
		this.labelAddress = labelAddress;
	}
	
	@Override
	public void updateViewEdges(Context context, boolean anchoredBottom, boolean alignToRight) {
		//Creating the message shape
		ShapeAppearanceModel messageAppearance = MessageShapeHelper.createRoundedMessageAppearanceBottom(context.getResources(), anchoredBottom, alignToRight);
		
		//Assigning the border shape
		MaterialShapeDrawable borderDrawable = new MaterialShapeDrawable(messageAppearance);
		borderDrawable.setFillColor(ColorStateList.valueOf(0));
		borderDrawable.setStroke(ResourceHelper.dpToPx(1), context.getColor(R.color.colorDivider));
		viewBorder.setBackground(borderDrawable);
		
		//Updating the card shape
		((MaterialCardView) itemView).setShapeAppearanceModel(messageAppearance);
	}
}