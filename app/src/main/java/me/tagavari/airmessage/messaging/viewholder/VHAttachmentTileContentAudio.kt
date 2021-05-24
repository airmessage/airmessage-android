package me.tagavari.airmessage.messaging.viewholder

import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import me.tagavari.airmessage.R
import me.tagavari.airmessage.enums.AttachmentType
import me.tagavari.airmessage.messaging.FileDisplayMetadata
import me.tagavari.airmessage.util.AudioPlaybackManager
import me.tagavari.airmessage.util.Union
import java.io.File

/**
 * An attachment tile that holds an audio file
 * @param itemView The tile view group
 * @param progressBar The playback progress bar
 * @param imagePlay The play / pause icon
 */
class VHAttachmentTileContentAudio(
	val itemView: View,
	val progressBar: ProgressBar,
	val imagePlay: ImageView
) : VHAttachmentTileContent() {
	override val attachmentType = AttachmentType.audio
	
	private var playbackIconPlay = true
	
	override fun bind(
		context: Context,
		compositeDisposable: CompositeDisposable,
		source: Union<File, Uri>,
		fileName: String,
		fileType: String,
		fileSize: Long,
		draftID: Long,
		dateModified: Long
	) {
		//Clearing the click listener
		itemView.setOnClickListener(null)
		
		//Resetting the view
		setPlaybackIdle()
	}
	
	/**
	 * Hooks up this view model to an audio playback manager
	 * @param context The context to use
	 * @param compositeDisposable A composite disposable linked to the lifecycle of this view
	 * @param playbackManager A playback manager for audio controls
	 * @param source The file, either a [File] or [Uri]
	 * @param metadata The audio metadata for this audio file
	 * @param referenceID A unique ID to represent this draft file
	 */
	fun onLoaded(
		context: Context?,
		compositeDisposable: CompositeDisposable,
		playbackManager: AudioPlaybackManager,
		source: Union<File, Uri>,
		metadata: FileDisplayMetadata.Media,
		referenceID: Int
	) {
		//Checking if we are currently playing this message
		if(playbackManager.compareRequest(referenceID)) {
			//Subscribing to audio playback
			attachAudioPlayback(compositeDisposable, playbackManager.emitter(), metadata.mediaDuration)
		}
		
		//Setting the click listener
		itemView.setOnClickListener {
			if(playbackManager.compareRequest(referenceID)) {
				//Toggling playback if we're already playing this audio file
				playbackManager.togglePlaying()
			} else {
				//Otherwise, starting a new playback session
				val playbackObservable = playbackManager.play(context, referenceID, source.map({ file -> Uri.fromFile(file) }, { uri -> uri }))
				attachAudioPlayback(compositeDisposable, playbackObservable, metadata.mediaDuration)
			}
		}
	}
	
	private fun attachAudioPlayback(compositeDisposable: CompositeDisposable, playbackObservable: Observable<AudioPlaybackManager.Progress>, audioDuration: Long) {
		compositeDisposable.add(playbackObservable.subscribe(
			{ update: AudioPlaybackManager.Progress ->
				setPlaybackProgress(
					update.playbackProgress,
					audioDuration,
					update.isPlaying
				)
			},
			{ },
			{ setPlaybackIdle() }
		))
	}
	
	/**
	 * Sets the image of the playback icon
	 * @param isPlay Whether to set the icon as 'play', or otherwise 'pause'
	 */
	fun setPlaybackIcon(isPlay: Boolean) {
		if(playbackIconPlay == isPlay) return
		playbackIconPlay = isPlay
		imagePlay.setImageResource(if(isPlay) R.drawable.play_rounded else R.drawable.pause_rounded)
	}
	
	/**
	 * Updates the view to reflect no playback
	 */
	fun setPlaybackIdle() {
		setPlaybackIcon(true)
		progressBar.progress = 0
	}
	
	/**
	 * Updates the view to reflect playback in progress
	 * @param progressDuration The current playback progression (in milliseconds)
	 * @param totalDuration The total duration of the audio clip (in milliseconds)
	 * @param isPlaying Whether the audio playback is playing or paused
	 */
	fun setPlaybackProgress(progressDuration: Long, totalDuration: Long, isPlaying: Boolean) {
		setPlaybackIcon(!isPlaying)
		progressBar.progress = (progressDuration.toFloat() / totalDuration * progressBar.max).toInt()
	}
}