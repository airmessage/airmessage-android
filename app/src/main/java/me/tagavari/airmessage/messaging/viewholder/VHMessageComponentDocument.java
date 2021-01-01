package me.tagavari.airmessage.messaging.viewholder;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import me.tagavari.airmessage.enums.AttachmentType;
import me.tagavari.airmessage.enums.MessageComponentType;
import me.tagavari.airmessage.helper.MessageShapeHelper;

public class VHMessageComponentDocument extends VHMessageComponentAttachment {
	public VHMessageComponentDocument(@NonNull View itemView, ViewGroup groupContainer, ViewGroup stickerContainer, ViewGroup tapbackContainer, ViewGroup groupPrompt, TextView labelPromptSize, TextView labelPromptType, ImageView iconPrompt, ViewGroup groupProgress, ProgressBar progressProgress, ImageView iconProgress, ViewGroup groupOpen, TextView labelOpen, ViewGroup groupContentFrame) {
		super(itemView, groupContainer, stickerContainer, tapbackContainer, groupPrompt, labelPromptSize, labelPromptType, iconPrompt, groupProgress, progressProgress, iconProgress, groupOpen, labelOpen, groupContentFrame);
	}
	
	@Override
	void updateContentViewEdges(Context context, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight) {
	
	}
	
	@Override
	void updateContentViewColoring(Context context, int colorTextPrimary, int colorTextSecondary, int colorBackground) {
	
	}
	
	@Override
	public int getComponentType() {
		return MessageComponentType.attachmentDocument;
	}
}