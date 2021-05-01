package me.tagavari.airmessage.messaging.viewholder

import android.content.Context
import android.content.res.ColorStateList
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import me.tagavari.airmessage.R
import me.tagavari.airmessage.enums.MessageComponentType
import me.tagavari.airmessage.helper.ColorMathHelper.multiplyColorRaw
import me.tagavari.airmessage.helper.MessageShapeHelper.createRoundedMessageDrawable
import kotlin.math.floor

class VHMessageComponentAudio(
	itemView: View,
	groupContainer: ViewGroup,
	stickerContainer: ViewGroup,
	tapbackContainer: ViewGroup,
	groupPrompt: ViewGroup,
	labelPromptSize: TextView,
	labelPromptType: TextView,
	iconPrompt: ImageView,
	groupProgress: ViewGroup,
	progressProgress: ProgressBar,
	iconProgress: ImageView,
	groupOpen: ViewGroup,
	labelOpen: TextView,
	groupContentFrame: ViewGroup,
	val groupContent: ViewGroup,
	val contentIcon: ImageView,
	val contentLabel: TextView,
	val contentProgress: ProgressBar
) : VHMessageComponentAttachment(
	itemView,
	groupContainer,
	stickerContainer,
	tapbackContainer,
	groupPrompt,
	labelPromptSize,
	labelPromptType,
	iconPrompt,
	groupProgress,
	progressProgress,
	iconProgress,
	groupOpen,
	labelOpen,
	groupContentFrame
) {
	override val componentType = MessageComponentType.attachmentAudio
	
	/**
	 * Sets the image of the playback icon
	 * @param isPlay Whether to set the icon as 'play', or otherwise 'pause'
	 */
	fun setPlaybackIcon(isPlay: Boolean) {
		contentIcon.setImageResource(if(isPlay) R.drawable.play_rounded else R.drawable.pause_rounded)
	}
	
	/**
	 * Updates the view to reflect no playback
	 * @param totalDuration The total duration of the audio clip (in milliseconds)
	 */
	fun setPlaybackIdle(totalDuration: Long) {
		setPlaybackIcon(true)
		contentProgress.progress = 0
		contentLabel.text = DateUtils.formatElapsedTime(floor((totalDuration / 1000.0)).toLong())
	}
	
	/**
	 * Updates the view to reflect playback in progress
	 * @param progressDuration The current playback progression (in milliseconds)
	 * @param totalDuration The total duration of the audio clip (in milliseconds)
	 * @param isPlaying Whether the audio playback is playing or paused
	 */
	fun setPlaybackProgress(progressDuration: Long, totalDuration: Long, isPlaying: Boolean) {
		setPlaybackIcon(!isPlaying)
		contentProgress.progress = (progressDuration.toFloat() / totalDuration * contentProgress.max).toInt()
		contentLabel.text = DateUtils.formatElapsedTime(floor((progressDuration / 1000.0)).toLong())
	}
	
	override fun updateContentViewEdges(context: Context, anchoredTop: Boolean, anchoredBottom: Boolean, alignToRight: Boolean) {
		groupContent.background = createRoundedMessageDrawable(context.resources, anchoredTop, anchoredBottom, alignToRight)
	}
	
	override fun updateContentViewColoring(context: Context, colorTextPrimary: Int, colorTextSecondary: Int, colorBackground: Int) {
		val cslPrimary = ColorStateList.valueOf(colorTextPrimary)
		//ColorStateList cslSecondary = ColorStateList.valueOf(colorTextSecondary);
		val cslBackground = ColorStateList.valueOf(colorBackground)
		
		groupContent.backgroundTintList = cslBackground
		contentIcon.imageTintList = cslPrimary
		contentLabel.setTextColor(cslPrimary)
		contentProgress.progressTintList = cslPrimary
		contentProgress.progressBackgroundTintList = ColorStateList.valueOf(multiplyColorRaw(colorBackground, 0.9f))
	}
}