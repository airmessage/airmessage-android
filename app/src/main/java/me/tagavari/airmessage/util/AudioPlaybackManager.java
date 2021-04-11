package me.tagavari.airmessage.util;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.util.Objects;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import me.tagavari.airmessage.R;

/**
 * A class that helps manage user-initiated foreground playback of audio from multiple sources
 */
public class AudioPlaybackManager {
	private boolean isAttached = false;
	private Object requestObject;
	private final SimpleExoPlayer exoPlayer;
	private final Handler mediaPlayerHandler = new Handler(Looper.getMainLooper());
	private final Runnable mediaPlayerHandlerRunnable = new Runnable() {
		@Override
		public void run() {
			//Notifying the listener
			updateProgress(true);
			
			//Running again
			mediaPlayerHandler.postDelayed(this, 10);
		}
	};
	private BehaviorSubject<Progress> progressSubject = null;
	
	public AudioPlaybackManager(Context context) {
		//Creating the exo player
		exoPlayer = new SimpleExoPlayer.Builder(context).build();
		
		exoPlayer.addListener(new Player.EventListener() {
			@Override
			public void onIsPlayingChanged(boolean isPlaying) {
				if(isPlaying) {
					//Starting the timer
					startTimer();
					
					//Notifying the listener
					updateProgress(true);
				} else {
					//Cancelling the timer
					stopTimer();
					
					//Notifying the listener
					int state = exoPlayer.getPlaybackState();
					if(state == Player.STATE_ENDED) {
						//Completing the session
						stop();
					} else {
						//Notifying the listener
						updateProgress(false);
					}
				}
			}
		});
	}
	
	public Observable<Progress> play(Context context, @NonNull Object requestObject, File file) {
		return play(context, requestObject, Uri.fromFile(file));
	}
	
	/**
	 * Configures and automatically starts a new play request.
	 *
	 * This function returns an {@link Observable<Progress>}.
	 * Progress updates are sent every 10ms while playing, and include
	 * the current playback position, as well as whether playback is
	 * playing or paused.
	 * @param context The context to use
	 * @param requestObject An object to track this request
	 * @param uri The URI to play
	 * @return An observable that emits playback updates
	 */
	public Observable<Progress> play(Context context, @NonNull Object requestObject, Uri uri) {
		//Ignoring if we are already handling this request
		if(this.requestObject != null && this.requestObject == requestObject) return emitter();
		
		//Stopping the current media session
		stop();
		
		//Setting the media player source
		DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context, Util.getUserAgent(context, context.getResources().getString(R.string.app_name)));
		MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uri));
		exoPlayer.setMediaSource(mediaSource);
		exoPlayer.prepare();
		exoPlayer.setPlayWhenReady(true);
		
		//Setting the request information
		this.requestObject = requestObject;
		progressSubject = BehaviorSubject.create();
		isAttached = true;
		
		//Returning the progress observable
		return progressSubject;
	}
	
	/**
	 * Gets the progress emitter for the current playback session.
	 * The result may be NULL if there is no session in progress.
	 */
	public Observable<Progress> emitter() {
		return progressSubject;
	}
	
	/**
	 * Gets if this playback manager is currently playing (not paused and not ended)
	 */
	public boolean isPlaying() {
		return exoPlayer.getPlayWhenReady() && exoPlayer.getPlaybackState() != Player.STATE_ENDED;
	}
	
	/**
	 * Toggles the current player
	 */
	public void togglePlaying() {
		//Ignoring if there is no session
		if(!isAttached) return;
		
		//Checking if the media player is playing
		if(exoPlayer.getPlayWhenReady() && exoPlayer.getPlaybackState() != Player.STATE_ENDED) {
			//Pausing the media player
			exoPlayer.setPlayWhenReady(false);
			
			//Cancelling the playback timer
			//stopTimer();
			
			//Notifying the listener
			//updateProgress(false);
		} else {
			//Resuming the media player
			exoPlayer.setPlayWhenReady(true);
			
			//Starting the playback timer
			//startTimer();
			
			//Notifying the listener
			//updateProgress(true);
		}
	}
	
	/**
	 * Stops and detaches the current playback session
	 */
	public void stop() {
		//Ignoring if there is no session
		if(!isAttached) return;
		
		//Stopping the player and the timer
		exoPlayer.stop();
		stopTimer();
		
		//Completing the observable
		progressSubject.onComplete();
		isAttached = false;
		
		//Clearing the request
		requestObject = null;
	}
	
	/**
	 * Releases all resources
	 */
	public void release() {
		//Cancelling the timer
		if(exoPlayer.getPlaybackState() == Player.STATE_READY) stopTimer();
		
		//Releasing the player
		exoPlayer.release();
	}
	
	/**
	 * Gets if the provided request object matches the current request
	 */
	public boolean compareRequest(@NonNull Object requestObject) {
		return Objects.equals(this.requestObject, requestObject);
	}
	
	private void startTimer() {
		mediaPlayerHandlerRunnable.run();
	}
	
	private void stopTimer() {
		mediaPlayerHandler.removeCallbacks(mediaPlayerHandlerRunnable);
	}
	
	private void updateProgress(boolean isPlaying) {
		progressSubject.onNext(new Progress(exoPlayer.getCurrentPosition(), isPlaying));
	}
	
	/**
	 * A progress payload for playback listeners
	 */
	public static class Progress {
		private final long playbackProgress;
		private final boolean isPlaying;
		
		public Progress(long playbackProgress, boolean isPlaying) {
			this.playbackProgress = playbackProgress;
			this.isPlaying = isPlaying;
		}
		
		/**
		 * Gets the current playback position
		 */
		public long getPlaybackProgress() {
			return playbackProgress;
		}
		
		/**
		 * Gets whether the player is playing or paused
		 */
		public boolean isPlaying() {
			return isPlaying;
		}
	}
}