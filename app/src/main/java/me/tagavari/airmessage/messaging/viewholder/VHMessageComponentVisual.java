package me.tagavari.airmessage.messaging.viewholder;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.imageview.ShapeableImageView;

import me.tagavari.airmessage.enums.MessageComponentType;
import me.tagavari.airmessage.helper.MessageShapeHelper;
import me.tagavari.airmessage.view.InvisibleInkView;

//Image or video
public class VHMessageComponentVisual extends VHMessageComponentAttachment {
	public final ViewGroup groupContent;
	public final ShapeableImageView imageView;
	public final InvisibleInkView inkView;
	public final ImageView playIndicator;
	
	public VHMessageComponentVisual(@NonNull View itemView, ViewGroup groupContainer, ViewGroup stickerContainer, ViewGroup tapbackContainer, ViewGroup groupPrompt, TextView labelPromptSize, TextView labelPromptType, ImageView iconPrompt, ViewGroup groupProgress, ProgressBar progressProgress, ImageView iconProgress, ViewGroup groupOpen, TextView labelOpen, ViewGroup groupContentFrame, ViewGroup groupContent, ShapeableImageView imageView, InvisibleInkView inkView, ImageView playIndicator) {
		super(itemView, groupContainer, stickerContainer, tapbackContainer, groupPrompt, labelPromptSize, labelPromptType, iconPrompt, groupProgress, progressProgress, iconProgress, groupOpen, labelOpen, groupContentFrame);
		this.groupContent = groupContent;
		this.imageView = imageView;
		this.inkView = inkView;
		this.playIndicator = playIndicator;
	}
	
	@Override
	void updateContentViewEdges(Context context, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight) {
		imageView.setShapeAppearanceModel(MessageShapeHelper.createRoundedMessageAppearance(context.getResources(), anchoredTop, anchoredBottom, alignToRight));
		inkView.setRadii(MessageShapeHelper.createStandardRadiusArray(context.getResources(), anchoredTop, anchoredBottom, alignToRight));
	}
	
	@Override
	void updateContentViewColoring(Context context, int colorTextPrimary, int colorTextSecondary, int colorBackground) {
	
	}
	
	@Override
	public int getComponentType() {
		return MessageComponentType.attachmentVisual;
	}
}