package me.tagavari.airmessage.compose.remember

import android.media.MediaRecorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.delay

@Composable
fun deriveAmplitudeList(
	mediaRecorder: MediaRecorder,
	enable: Boolean,
	interval: Long = 50
): SnapshotStateList<Int> {
	val amplitudeList = remember { mutableStateListOf<Int>() }
	
	LaunchedEffect(mediaRecorder, enable, interval) {
		if(enable) {
			amplitudeList.clear()
			
			//Repeatedly add the max amplitude
			while(enable) {
				delay(interval)
				val amplitude = mediaRecorder.maxAmplitude
				amplitudeList.add(amplitude)
			}
		}
	}
	
	return amplitudeList
}
