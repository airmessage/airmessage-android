package me.tagavari.airmessage.messaging.viewholder;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import me.tagavari.airmessage.R;
import me.tagavari.airmessage.enums.AttachmentType;
import me.tagavari.airmessage.enums.MessageComponentType;
import me.tagavari.airmessage.helper.ColorMathHelper;
import me.tagavari.airmessage.helper.MessageShapeHelper;

public class VHMessageComponentAudio extends VHMessageComponentAttachment {
	public final ViewGroup groupContent;
	public final ImageView contentIcon;
	public final TextView contentLabel;
	public final ProgressBar contentProgress;
	
	public VHMessageComponentAudio(@NonNull View itemView, ViewGroup groupContainer, ViewGroup stickerContainer, ViewGroup tapbackContainer, ViewGroup groupPrompt, TextView labelPromptSize, TextView labelPromptType, ImageView iconPrompt, ViewGroup groupProgress, ProgressBar progressProgress, ImageView iconProgress, ViewGroup groupOpen, TextView labelOpen, ViewGroup groupContentFrame, ViewGroup groupContent, ImageView contentIcon, TextView contentLabel, ProgressBar contentProgress) {
		super(itemView, groupContainer, stickerContainer, tapbackContainer, groupPrompt, labelPromptSize, labelPromptType, iconPrompt, groupProgress, progressProgress, iconProgress, groupOpen, labelOpen, groupContentFrame);
		this.groupContent = groupContent;
		this.contentIcon = contentIcon;
		this.contentLabel = contentLabel;
		this.contentProgress = contentProgress;
	}
	
	/**
	 * Sets the image of the playback icon
	 * @param isPlay Whether to set the icon as 'play', or otherwise 'pause'
	 */
	public void setPlaybackIcon(boolean isPlay) {
		contentIcon.setImageResource(isPlay ? R.drawable.play_rounded : R.drawable.pause_rounded);
	}
	
	/**
	 * Updates the view to reflect no playback
	 * @param totalDuration The total duration of the audio clip (in milliseconds)
	 */
	public void setPlaybackIdle(long totalDuration) {
		setPlaybackIcon(true);
		contentProgress.setProgress(0);
		contentLabel.setText(DateUtils.formatElapsedTime(((int) Math.floor(totalDuration / 1000))));
	}
	
	/**
	 * Updates the view to reflect playback in progress
	 * @param progressDuration The current playback progression (in milliseconds)
	 * @param totalDuration The total duration of the audio clip (in milliseconds)
	 * @param isPlaying Whether the audio playback is playing or paused
	 */
	public void setPlaybackProgress(long progressDuration, long totalDuration, boolean isPlaying) {
		setPlaybackIcon(!isPlaying);
		contentProgress.setProgress((int) ((float) progressDuration / totalDuration * contentProgress.getMax()));
		contentLabel.setText(DateUtils.formatElapsedTime(((int) Math.floor(progressDuration / 1000))));
	}
	
	@Override
	void updateContentViewEdges(Context context, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight) {
		groupContent.setBackground(MessageShapeHelper.createRoundedMessageDrawable(context.getResources(), anchoredTop, anchoredBottom, alignToRight));
	}
	
	@Override
	void updateContentViewColoring(Context context, int colorTextPrimary, int colorTextSecondary, int colorBackground) {
		ColorStateList cslPrimary = ColorStateList.valueOf(colorTextPrimary);
		//ColorStateList cslSecondary = ColorStateList.valueOf(colorTextSecondary);
		ColorStateList cslBackground = ColorStateList.valueOf(colorBackground);
		
		groupContent.setBackgroundTintList(cslBackground);
		contentIcon.setImageTintList(cslPrimary);
		contentLabel.setTextColor(cslPrimary);
		contentProgress.setProgressTintList(cslPrimary);
		contentProgress.setProgressBackgroundTintList(ColorStateList.valueOf(ColorMathHelper.multiplyColorRaw(colorBackground, 0.9F)));
	}
	
	@Override
	public int getComponentType() {
		return MessageComponentType.attachmentAudio;
	}
}