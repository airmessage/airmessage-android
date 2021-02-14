package me.tagavari.airmessage.messaging.viewholder;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import me.tagavari.airmessage.helper.ColorMathHelper;
import me.tagavari.airmessage.helper.MessageShapeHelper;

public abstract class VHMessageComponentAttachment extends VHMessageComponent {
	public final ViewGroup groupPrompt;
	public final TextView labelPromptSize;
	public final TextView labelPromptType;
	public final ImageView iconPrompt;
	
	public final ViewGroup groupProgress;
	public final ProgressBar progressProgress;
	public final ImageView iconProgress;
	
	public final ViewGroup groupOpen;
	public final TextView labelOpen;
	
	public final ViewGroup groupContentFrame;
	
	public VHMessageComponentAttachment(@NonNull View itemView, ViewGroup groupContainer, ViewGroup stickerContainer, ViewGroup tapbackContainer, ViewGroup groupPrompt, TextView labelPromptSize, TextView labelPromptType, ImageView iconPrompt, ViewGroup groupProgress, ProgressBar progressProgress, ImageView iconProgress, ViewGroup groupOpen, TextView labelOpen, ViewGroup groupContentFrame) {
		super(itemView, groupContainer, stickerContainer, tapbackContainer);
		this.groupPrompt = groupPrompt;
		this.labelPromptSize = labelPromptSize;
		this.labelPromptType = labelPromptType;
		this.iconPrompt = iconPrompt;
		this.groupProgress = groupProgress;
		this.progressProgress = progressProgress;
		this.iconProgress = iconProgress;
		this.groupOpen = groupOpen;
		this.labelOpen = labelOpen;
		this.groupContentFrame = groupContentFrame;
	}
	
	@Override
	public void updateViewEdges(Context context, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight) {
		//Creating the drawable
		Drawable drawable = MessageShapeHelper.createRoundedMessageDrawable(context.getResources(), anchoredTop, anchoredBottom, alignToRight);
		
		//Assigning the drawable
		groupPrompt.setBackground(drawable);
		groupProgress.setBackground(drawable);
		groupOpen.setBackground(drawable);
		
		//Updating the content view's edges
		updateContentViewEdges(context, anchoredTop, anchoredBottom, alignToRight);
	}
	
	abstract void updateContentViewEdges(Context context, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight);
	
	@Override
	public void updateViewColoring(Context context, int colorTextPrimary, int colorTextSecondary, int colorBackground) {
		ColorStateList cslPrimary = ColorStateList.valueOf(colorTextPrimary);
		ColorStateList cslSecondary = ColorStateList.valueOf(colorTextSecondary);
		ColorStateList cslBackground = ColorStateList.valueOf(colorBackground);
		
		//Coloring the views
		groupPrompt.setBackgroundTintList(cslBackground);
		labelPromptSize.setTextColor(cslSecondary);
		labelPromptType.setTextColor(cslPrimary);
		iconPrompt.setImageTintList(cslPrimary);
		
		groupProgress.setBackgroundTintList(cslBackground);
		progressProgress.setProgressTintList(cslPrimary);
		ColorStateList cslProgressBG = ColorStateList.valueOf(ColorMathHelper.multiplyColorRaw(colorBackground, 0.9F));
		progressProgress.setIndeterminateTintList(cslProgressBG);
		progressProgress.setProgressBackgroundTintList(cslProgressBG);
		iconProgress.setImageTintList(cslPrimary);
		
		groupOpen.setBackgroundTintList(cslBackground);
		labelOpen.setTextColor(cslPrimary);
		labelOpen.setCompoundDrawableTintList(cslPrimary);
		
		//Updating the content view's coloring
		updateContentViewColoring(context, colorTextPrimary, colorTextSecondary, colorBackground);
	}
	
	abstract void updateContentViewColoring(Context context, int colorTextPrimary, int colorTextSecondary, int colorBackground);
}