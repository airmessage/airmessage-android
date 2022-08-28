package me.tagavari.airmessage.compose.component

import android.text.format.DateUtils
import android.util.LruCache
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.remember.AudioPlaybackState
import me.tagavari.airmessage.helper.AudioDecodeHelper
import me.tagavari.airmessage.helper.AudioPreviewData
import me.tagavari.airmessage.util.MessagePartFlow
import java.io.File

private val audioPreviewCacheMutex = Mutex()
private val audioPreviewCache = object : LruCache<File, AudioPreviewData>(1024 * 1024) {
	override fun sizeOf(key: File, value: AudioPreviewData)
			= Long.SIZE_BYTES + (value.amplitude.size * Int.SIZE_BYTES)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageBubbleAudio(
	flow: MessagePartFlow,
	file: File,
	audioPlaybackState: AudioPlaybackState,
	onTogglePlayback: () -> Unit
) {
	val colors = flow.colors
	
	Surface(
		modifier = Modifier.width(200.dp),
		color = colors.background,
		shape = flow.bubbleShape,
		contentColor = colors.foreground,
		onClick = onTogglePlayback
	) {
		Row(
			modifier = Modifier
				.height(40.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			val isPlaying =
				audioPlaybackState is AudioPlaybackState.Playing && audioPlaybackState.playing
			
			//Get the amplitude list
			val audioPreview by produceState<AudioPreviewData?>(null) {
				audioPreviewCacheMutex.withLock {
					//Look up a previous value in the cache
					audioPreviewCache[file]?.let {
						value = it
						return@withLock
					}
					
					//Process the file
					@Suppress("BlockingMethodInNonBlockingContext")
					val amplitudeList = withContext(Dispatchers.IO) {
						AudioDecodeHelper.getAudioPreviewData(file)
					}
					
					//Save the value in the cache
					audioPreviewCache.put(file, amplitudeList)
					value = amplitudeList
				}
			}
			
			Icon(
				modifier = Modifier.padding(start = 8.dp),
				painter = painterResource(
					if(isPlaying) R.drawable.pause_circle_rounded
					else R.drawable.play_circle_rounded
				),
				contentDescription = null
			)
			
			Spacer(modifier = Modifier.width(8.dp))
			
			AudioVisualizer(
				modifier = Modifier
					.weight(1F)
					.fillMaxHeight()
					.padding(vertical = 1.dp),
				amplitudeList = audioPreview?.amplitude ?: emptyList(),
				displayType = AudioVisualizerDisplayType.SUMMARY,
				progress = if(audioPlaybackState is AudioPlaybackState.Playing)
					audioPlaybackState.time.toFloat() / audioPlaybackState.totalDuration.toFloat()
				else 1F
			)
			
			Spacer(modifier = Modifier.width(8.dp))
			
			Text(
				modifier = Modifier.padding(end = 12.dp),
				text = remember(audioPlaybackState, audioPreview) {
					if(audioPlaybackState is AudioPlaybackState.Playing) {
						DateUtils.formatElapsedTime(audioPlaybackState.time / 1000)
					} else {
						audioPreview?.let { audioPreview ->
							DateUtils.formatElapsedTime(audioPreview.duration)
						} ?: "-"
					}
				}
			)
		}
	}
}
