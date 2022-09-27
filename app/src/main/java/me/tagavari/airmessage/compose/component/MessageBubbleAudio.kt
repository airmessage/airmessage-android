package me.tagavari.airmessage.compose.component

import android.text.format.DateUtils
import android.util.LruCache
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.remember.AudioPlaybackState
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.helper.AudioDecodeHelper
import me.tagavari.airmessage.helper.AudioPreviewData
import me.tagavari.airmessage.util.MessagePartFlow
import java.io.File

private val audioPreviewCacheMutex = Mutex()
private val audioPreviewCache = object : LruCache<File, AudioPreviewData>(1024 * 1024) {
	override fun sizeOf(key: File, value: AudioPreviewData)
			= Long.SIZE_BYTES + (value.amplitude.size * Int.SIZE_BYTES)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleAudio(
	flow: MessagePartFlow,
	file: File,
	audioPlaybackState: AudioPlaybackState,
	onTogglePlayback: () -> Unit,
	onSetSelected: (Boolean) -> Unit
) {
	val haptic = LocalHapticFeedback.current
	val colors = flow.colors
	
	CompositionLocalProvider(
		LocalMinimumTouchTargetEnforcement provides false
	) {
		Surface(
			modifier = Modifier
				.width(200.dp)
				.combinedClickable(
					onClick = {
						if(flow.isSelected) {
							onSetSelected(false)
						} else {
							onTogglePlayback()
						}
					},
					onLongClick = {
						haptic.performHapticFeedback(HapticFeedbackType.LongPress)
						onSetSelected(!flow.isSelected)
					}
				),
			color = colors.background,
			shape = flow.bubbleShape,
			contentColor = colors.foreground
		) {
			Row(
				modifier = Modifier
					.height(40.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				val isPlaying =
					audioPlaybackState is AudioPlaybackState.Playing && audioPlaybackState.playing
				
				//Get the amplitude list
				val audioPreview by if(LocalInspectionMode.current) {
					remember { mutableStateOf<AudioPreviewData?>(AudioPreviewData.Preview) }
				} else {
					produceState<AudioPreviewData?>(null) {
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
}

@Preview
@Composable
private fun PreviewMessageBubbleAudio() {
	AirMessageAndroidTheme {
		MessageBubbleAudio(
			flow = MessagePartFlow(
				isOutgoing = false,
				isSelected = false,
				anchorBottom = false,
				anchorTop = false,
				tintRatio = 0F
			),
			file = File(""),
			audioPlaybackState = AudioPlaybackState.Stopped,
			onTogglePlayback = {},
			onSetSelected = {}
		)
	}
}
