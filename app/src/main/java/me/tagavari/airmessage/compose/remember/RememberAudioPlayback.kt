package me.tagavari.airmessage.compose.remember

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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
	
	val scope = rememberCoroutineScope()
	fun play(uri: Uri): Flow<AudioPlaybackState> {
		exoPlayer.setMediaItem(MediaItem.fromUri(uri))
		exoPlayer.prepare()
		exoPlayer.play()
		
		return callbackFlow {
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
		}
	}
	
	return object : AudioPlaybackControls {
		override fun play(uri: Uri) = play(uri)
		override fun pause() = exoPlayer.pause()
		override fun resume() = exoPlayer.play()
		override fun stop() = exoPlayer.stop()
	}
}

interface AudioPlaybackControls {
	fun play(uri: Uri): Flow<AudioPlaybackState>
	fun pause()
	fun resume()
	fun stop()
}

/**
 * An instance of AudioPlaybackControls that throws errors
 */
class AudioPlaybackControlsEmpty : AudioPlaybackControls {
	private fun throwError(): Nothing {
		throw IllegalStateException("No-op audio playback controls instance!")
	}
	
	override fun play(uri: Uri) = throwError()
	override fun pause() = throwError()
	override fun resume() = throwError()
	override fun stop() = throwError()
}

sealed class AudioPlaybackState {
	class Playing(
		val time: Long,
		val totalDuration: Long,
		val playing: Boolean
	) : AudioPlaybackState()
	object Stopped : AudioPlaybackState()
}
