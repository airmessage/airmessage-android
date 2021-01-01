package me.tagavari.airmessage.messaging.viewholder;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.enums.AttachmentType;
import me.tagavari.airmessage.messaging.FileDisplayMetadata;
import me.tagavari.airmessage.util.AudioPlaybackManager;
import me.tagavari.airmessage.util.Union;

public class VHAttachmentTileContentAudio extends VHAttachmentTileContent {
	public final View itemView; //The tile view group
	public final ProgressBar progressBar; //The playback process bar
	public final ImageView imagePlay; //The play / pause icon
	
	private boolean playbackIconPlay = true;
	
	public VHAttachmentTileContentAudio(View itemView, ProgressBar progressBar, ImageView imagePlay) {
		this.itemView = itemView;
		this.progressBar = progressBar;
		this.imagePlay = imagePlay;
	}
	
	@Override
	public void bind(@NonNull Context context, @NonNull CompositeDisposable compositeDisposable, @NonNull Union<File, Uri> source, @NonNull String fileName, @NonNull String fileType, long fileSize, long draftID, long dateModified) {
		//Resetting the view
		setPlaybackIdle();
	}
	
	/**
	 * Hooks up this view model to an audio playback manager
	 * @param context The context to use
	 * @param compositeDisposable A composite disposable linked to the lifecycle of this view
	 * @param playbackManager A playback manager for audio controls
	 * @param source The file, either a {@link File} or {@link Uri}
	 * @param metadata The audio metadata for this audio file
	 * @param referenceID A unique ID to represent this draft file
	 */
	public void onLoaded(Context context, CompositeDisposable compositeDisposable, AudioPlaybackManager playbackManager, Union<File, Uri> source, FileDisplayMetadata.Media metadata, int referenceID) {
		//Checking if we are currently playing this message
		if(playbackManager.compareRequest(referenceID)) {
			//Subscribing to audio playback
			attachAudioPlayback(compositeDisposable, playbackManager.emitter(), metadata.getMediaDuration());
		}
		
		//Setting the click listener
		itemView.setOnClickListener(view -> {
			if(playbackManager.compareRequest(referenceID)) {
				//Toggling playback if we're already playing this audio file
				playbackManager.togglePlaying();
			} else {
				//Otherwise, starting a new playback session
				Observable<AudioPlaybackManager.Progress> playbackObservable = playbackManager.play(context, referenceID, source.map(Uri::fromFile, uri -> uri));
				attachAudioPlayback(compositeDisposable, playbackObservable, metadata.getMediaDuration());
			}
		});
	}
	
	private void attachAudioPlayback(CompositeDisposable compositeDisposable, Observable<AudioPlaybackManager.Progress> playbackObservable, long audioDuration) {
		compositeDisposable.add(playbackObservable.subscribe(
				update -> setPlaybackProgress(update.getPlaybackProgress(), audioDuration, update.isPlaying()),
				error -> {},
				//Returning to idle when playback is finished
				this::setPlaybackIdle)
		);
	}
	
	/**
	 * Sets the image of the playback icon
	 * @param isPlay Whether to set the icon as 'play', or otherwise 'pause'
	 */
	public void setPlaybackIcon(boolean isPlay) {
		if(playbackIconPlay == isPlay) return;
		playbackIconPlay = isPlay;
		
		imagePlay.setImageResource(isPlay ? R.drawable.play_rounded : R.drawable.pause_rounded);
	}
	
	/**
	 * Updates the view to reflect no playback
	 */
	public void setPlaybackIdle() {
		setPlaybackIcon(true);
		progressBar.setProgress(0);
	}
	
	/**
	 * Updates the view to reflect playback in progress
	 * @param progressDuration The current playback progression (in milliseconds)
	 * @param totalDuration The total duration of the audio clip (in milliseconds)
	 * @param isPlaying Whether the audio playback is playing or paused
	 */
	public void setPlaybackProgress(long progressDuration, long totalDuration, boolean isPlaying) {
		setPlaybackIcon(!isPlaying);
		progressBar.setProgress((int) ((float) progressDuration / totalDuration * progressBar.getMax()));
	}
	
	@Override
	public int getAttachmentType() {
		return AttachmentType.audio;
	}
}