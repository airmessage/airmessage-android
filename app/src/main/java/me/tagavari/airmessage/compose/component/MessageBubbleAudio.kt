package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.remember.AudioPlaybackState
import me.tagavari.airmessage.helper.AudioDecodeHelper
import me.tagavari.airmessage.util.MessagePartFlow
import java.io.File

@Composable
fun MessageBubbleAudio(
	flow: MessagePartFlow,
	file: File,
	audioPlaybackState: AudioPlaybackState
) {
	val colors = flow.colors
	val scope = rememberCoroutineScope()
	var playbackState by remember { mutableStateOf<AudioPlaybackState>(AudioPlaybackState.Stopped) }
	
	Surface(
		modifier = Modifier.width(200.dp),
		color = colors.background,
		shape = flow.bubbleShape,
		contentColor = colors.foreground
	) {
		Row(modifier = Modifier.height(40.dp)) {
			val localPlaybackState = playbackState
			val isPlaying = localPlaybackState !is AudioPlaybackState.Playing || !localPlaybackState.playing
			
			//Get the amplitude list
			val amplitudeList by produceState<List<Int>?>(null) {
				@Suppress("BlockingMethodInNonBlockingContext")
				value = withContext(Dispatchers.IO) {
					AudioDecodeHelper.getAmplitudeList(file)
				}
			}
			
			Icon(
				painter = painterResource(
					if(isPlaying) R.drawable.pause_circle_rounded
					else R.drawable.play_circle_rounded
				),
				contentDescription = null
			)
			
			AudioVisualizer(
				modifier = Modifier
					.weight(1F)
					.fillMaxHeight()
					.padding(vertical = 1.dp),
				amplitudeList = amplitudeList ?: emptyList(),
				displayType = AudioVisualizerDisplayType.SUMMARY
			)
		}
	}
}
