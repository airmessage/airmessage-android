package me.tagavari.airmessage.util

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import me.tagavari.airmessage.R
import java.io.File

/**
 * A class that helps manage user-initiated foreground playback of audio from multiple sources
 */
class AudioPlaybackManager(context: Context) {
	private var isAttached = false
	private var requestObject: Any? = null
	
	private val exoPlayer = SimpleExoPlayer.Builder(context).build().apply {
		addListener(object : Player.Listener {
			override fun onIsPlayingChanged(isPlaying: Boolean) {
				if(isPlaying) {
					//Starting the timer
					this@AudioPlaybackManager.startTimer()
					
					//Notifying the listener
					this@AudioPlaybackManager.updateProgress(true)
				} else {
					//Cancelling the timer
					this@AudioPlaybackManager.stopTimer()
					
					//Notifying the listener
					if(playbackState == Player.STATE_ENDED) {
						//Completing the session
						this@AudioPlaybackManager.stop()
					} else {
						//Notifying the listener
						this@AudioPlaybackManager.updateProgress(false)
					}
				}
			}
		})
	}
	private val mediaPlayerHandler = Handler(Looper.getMainLooper())
	private val mediaPlayerHandlerRunnable = object : Runnable {
		override fun run() {
			//Notifying the listener
			updateProgress(true)
			
			//Running again
			mediaPlayerHandler.postDelayed(this, 10)
		}
	}
	private var progressSubject: BehaviorSubject<Progress>? = null
	
	fun play(context: Context, requestObject: Any, file: File): Observable<Progress> {
		return play(context, requestObject, Uri.fromFile(file))
	}
	
	/**
	 * Configures and automatically starts a new play request.
	 *
	 * This function returns an [Observable<Progress>].
	 * Progress updates are sent every 10ms while playing, and include
	 * the current playback position, as well as whether playback is
	 * playing or paused.
	 * @param context The context to use
	 * @param requestObject An object to track this request
	 * @param uri The URI to play
	 * @return An observable that emits playback updates
	 */
	fun play(context: Context, requestObject: Any, uri: Uri): Observable<Progress> {
		//Ignoring if we are already handling this request
		if(this.requestObject != null && this.requestObject === requestObject) return emitter!!
		
		//Stopping the current media session
		stop()
		
		//Setting the media player source
		val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(context, Util.getUserAgent(context, context.resources.getString(R.string.app_name)))
		val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uri))
		exoPlayer.setMediaSource(mediaSource)
		exoPlayer.prepare()
		exoPlayer.playWhenReady = true
		
		//Setting the request information
		this.requestObject = requestObject
		progressSubject = BehaviorSubject.create()
		isAttached = true
		
		//Returning the progress observable
		return progressSubject!!
	}
	
	/**
	 * Gets the progress emitter for the current playback session.
	 * The result may be NULL if there is no session in progress.
	 */
	val emitter: Observable<Progress>?
		get() = progressSubject
	
	/**
	 * Gets if this playback manager is currently playing (not paused and not ended)
	 */
	val isPlaying: Boolean
		get() = exoPlayer.playWhenReady && exoPlayer.playbackState != Player.STATE_ENDED
	
	/**
	 * Toggles the current player
	 */
	fun togglePlaying() {
		//Ignoring if there is no session
		if(!isAttached) return
		
		//Updating the playback state if playback has ended
		if(!exoPlayer.playWhenReady) {
			exoPlayer.playWhenReady = exoPlayer.playbackState == Player.STATE_ENDED
		}
	}
	
	/**
	 * Stops and detaches the current playback session
	 */
	fun stop() {
		//Ignoring if there is no session
		if(!isAttached) return
		
		//Stopping the player and the timer
		exoPlayer.stop()
		stopTimer()
		
		//Completing the observable
		progressSubject!!.onComplete()
		isAttached = false
		
		//Clearing the request
		requestObject = null
	}
	
	/**
	 * Releases all resources
	 */
	fun release() {
		//Cancelling the timer
		if(exoPlayer.playbackState == Player.STATE_READY) stopTimer()
		
		//Releasing the player
		exoPlayer.release()
	}
	
	/**
	 * Gets if the provided request object matches the current request
	 */
	fun compareRequest(requestObject: Any): Boolean {
		return this.requestObject == requestObject
	}
	
	private fun startTimer() {
		mediaPlayerHandlerRunnable.run()
	}
	
	private fun stopTimer() {
		mediaPlayerHandler.removeCallbacks(mediaPlayerHandlerRunnable)
	}
	
	private fun updateProgress(isPlaying: Boolean) {
		progressSubject!!.onNext(Progress(exoPlayer.currentPosition, isPlaying))
	}
	
	/**
	 * A progress payload for playback listeners
	 */
	data class Progress(
		val playbackProgress: Long, //The current playback position
		val isPlaying: Boolean //Whether the player is playing or paused
	)
}