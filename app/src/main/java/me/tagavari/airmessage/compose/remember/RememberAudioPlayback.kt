package me.tagavari.airmessage.compose.remember

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

private var sharedExoPlayer: ExoPlayer? = null
private var sharedExoPlayerCount = 0

private fun retainExoPlayer(context: Context): ExoPlayer {
	if(sharedExoPlayerCount == 0) {
		sharedExoPlayer = ExoPlayer.Builder(context).build()
	} else {
		sharedExoPlayerCount += 1
	}
	
	return sharedExoPlayer!!
}

private fun releaseExoPlayer() {
	sharedExoPlayerCount -= 1
	if(sharedExoPlayerCount == 0) {
		sharedExoPlayer!!.release()
		sharedExoPlayer = null
	}
}

@Composable
fun rememberAudioPlayback(): AudioPlaybackControls {
	val context = LocalContext.current
	
	//Reference count the player
	val exoPlayer = remember { retainExoPlayer(context) }
	DisposableEffect(Unit) {
		onDispose {
			releaseExoPlayer()
		}
	}
	
	var currentSession by remember { mutableStateOf<AudioPlaybackSession?>(null) }
	
	val scope = rememberCoroutineScope()
	fun play(key: Any?, uri: Uri) {
		exoPlayer.setMediaItem(MediaItem.fromUri(uri))
		exoPlayer.prepare()
		exoPlayer.play()
		
		scope.launch {
			callbackFlow {
				var timerJob: Job? = null
				
				val listener = object : Player.Listener {
					override fun onIsPlayingChanged(isPlaying: Boolean) {
						scope.launch {
							if(isPlaying) {
								timerJob = launch {
									//Emit a time update recently
									while(true) {
										delay(10)
										
										send(AudioPlaybackState.Playing(
											time = exoPlayer.currentPosition,
											totalDuration = exoPlayer.duration,
											playing = true
										))
									}
								}
								
								send(AudioPlaybackState.Playing(
									time = exoPlayer.currentPosition,
									totalDuration = exoPlayer.duration,
									playing = true
								))
							} else {
								//Cancel the timer
								timerJob?.cancel()
								
								if(exoPlayer.playbackState == Player.STATE_ENDED) {
									send(AudioPlaybackState.Stopped)
									close()
								} else {
									send(AudioPlaybackState.Playing(
										time = exoPlayer.currentPosition,
										totalDuration = exoPlayer.duration,
										playing = false
									))
								}
							}
						}
					}
				}
				
				exoPlayer.addListener(listener)
				awaitClose {
					exoPlayer.removeListener(listener)
				}
			}.collect { currentSession = AudioPlaybackSession(key, it) }
		}
	}
	
	return object : AudioPlaybackControls {
		override fun play(key: Any?, uri: Uri) = play(key, uri)
		override fun pause(key: Any?) {
			if(key == null || key == currentSession?.key) {
				exoPlayer.pause()
			}
		}
		override fun resume(key: Any?) {
			if(key == null || key == currentSession?.key) {
				exoPlayer.play()
			}
		}
		override fun stop(key: Any?) {
			if(key == null || key == currentSession?.key) {
				exoPlayer.stop()
			}
		}
		
		@Composable
		override fun stateForKey(key: Any?): State<AudioPlaybackState> {
			return remember(key) {
				derivedStateOf {
					//Get the current session
					val session = currentSession
						?: return@derivedStateOf AudioPlaybackState.Stopped
					
					//Compare the session key
					if(key == null || session.key != key) {
						return@derivedStateOf AudioPlaybackState.Stopped
					}
					
					//Return the session state
					return@derivedStateOf session.state
				}
			}
		}
	}
}

interface AudioPlaybackControls {
	/**
	 * Plays the audio at the specified URI
	 * @param key A key used to keep track of this playback session
	 * @param uri The URI of the audio file to play
	 */
	fun play(key: Any?, uri: Uri)
	
	/**
	 * Pauses an audio session
	 * @param key The key of the audio session to pause, or
	 * NULL to pause any active session
	 */
	fun pause(key: Any? = null)
	
	/**
	 * Resumes an audio session
	 * @param key The key of the audio session to resume, or
	 * NULL to resume any active session
	 */
	fun resume(key: Any? = null)
	
	/**
	 * Stops an audio session
	 * @param key The key of the audio session to stop, or
	 * NULL to stop any active session
	 */
	fun stop(key: Any? = null)
	
	/**
	 * Gets the audio playback state for the session with the specified key.
	 * If the key is NULL, this function will always return [AudioPlaybackState.Stopped].
	 * @param key The key of the audio session
	 * @return The audio playback state of the specified session
	 */
	@Composable fun stateForKey(key: Any?): State<AudioPlaybackState>
}

/**
 * An instance of AudioPlaybackControls that throws errors
 */
class AudioPlaybackControlsEmpty : AudioPlaybackControls {
	private fun throwError(): Nothing {
		throw IllegalStateException("No-op audio playback controls instance!")
	}
	
	override fun play(key: Any?, uri: Uri) = throwError()
	override fun pause(key: Any?) = throwError()
	override fun resume(key: Any?) = throwError()
	override fun stop(key: Any?) = throwError()
	
	@Composable
	override fun stateForKey(key: Any?): State<AudioPlaybackState> {
		return remember { mutableStateOf(AudioPlaybackState.Stopped) }
	}
}

data class AudioPlaybackSession(
	val key: Any?,
	val state: AudioPlaybackState
)

sealed class AudioPlaybackState {
	class Playing(
		val time: Long,
		val totalDuration: Long,
		val playing: Boolean
	) : AudioPlaybackState()
	object Stopped : AudioPlaybackState()
}
