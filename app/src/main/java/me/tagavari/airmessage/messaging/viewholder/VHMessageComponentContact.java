package me.tagavari.airmessage.messaging.viewholder;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import me.tagavari.airmessage.enums.MessageComponentType;
import me.tagavari.airmessage.helper.MessageShapeHelper;

public class VHMessageComponentContact extends VHMessageComponentAttachment {
	public final ViewGroup groupContent;
	public final ImageView iconProfile;
	public final ImageView iconPlaceholder;
	public final TextView labelName;
	
	public VHMessageComponentContact(@NonNull View itemView, ViewGroup groupContainer, ViewGroup stickerContainer, ViewGroup tapbackContainer, ViewGroup groupPrompt, TextView labelPromptSize, TextView labelPromptType, ImageView iconPrompt, ViewGroup groupProgress, ProgressBar progressProgress, ImageView iconProgress, ViewGroup groupOpen, TextView labelOpen, ViewGroup groupContentFrame, ViewGroup groupContent, ImageView iconProfile, ImageView iconPlaceholder, TextView labelName) {
		super(itemView, groupContainer, stickerContainer, tapbackContainer, groupPrompt, labelPromptSize, labelPromptType, iconPrompt, groupProgress, progressProgress, iconProgress, groupOpen, labelOpen, groupContentFrame);
		this.groupContent = groupContent;
		this.iconProfile = iconProfile;
		this.iconPlaceholder = iconPlaceholder;
		this.labelName = labelName;
	}
	
	@Override
	void updateContentViewEdges(Context context, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight) {
		groupContent.setBackground(MessageShapeHelper.createRoundedMessageDrawable(context.getResources(), anchoredTop, anchoredBottom, alignToRight));
	}
	
	@Override
	void updateContentViewColoring(Context context, int colorTextPrimary, int colorTextSecondary, int colorBackground) {
		groupContent.setBackgroundTintList(ColorStateList.valueOf(colorBackground));
		labelName.setTextColor(ColorStateList.valueOf(colorTextPrimary));
	}
	
	@Override
	public int getComponentType() {
		return MessageComponentType.attachmentContact;
	}
}