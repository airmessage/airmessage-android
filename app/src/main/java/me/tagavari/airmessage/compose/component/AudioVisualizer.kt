package me.tagavari.airmessage.compose.component

import android.media.MediaRecorder
import androidx.compose.foundation.Canvas
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import me.tagavari.airmessage.compose.remember.deriveAmplitudeList
import kotlin.math.ceil

private const val amplitudeMin = 100
private const val amplitudeMax = 10000
private const val amplitudeFallStep = 1000

private const val lineWidth = 6F
private const val lineSpacing = 2F

@Composable
fun AudioVisualizer(
	modifier: Modifier = Modifier,
	amplitudeList: List<Int>
) {
	val displayList = mutableListOf<Int>().also { list ->
		var lastValue = 0
		for(amplitude in amplitudeList) {
			val clampedAmplitude = amplitude.coerceIn(amplitudeMin, amplitudeMax)
			val smoothedAmplitude = clampedAmplitude.coerceAtLeast(lastValue - amplitudeFallStep)
			
			lastValue = smoothedAmplitude
			list.add(smoothedAmplitude)
		}
	}
	
	val color = LocalContentColor.current
	Canvas(modifier = modifier) {
		val canvasWidth = size.width
		val canvasHeight = size.height
		
		displayList
			.asReversed()
			.asSequence()
			.take(ceil(canvasWidth / (lineWidth + lineSpacing)).toInt())
			.forEachIndexed { index, amplitude ->
				val lineHeight = (amplitude.toFloat() / amplitudeMax.toFloat()) * canvasHeight
				
				drawRect(
					color = color,
					topLeft = Offset(
						x = canvasWidth - lineWidth - ((lineSpacing + lineWidth) * index),
						y = (canvasHeight - lineHeight) / 2
					),
					size = Size(
						width = lineWidth,
						height = lineHeight
					)
				)
			}
	}
}

@Composable
fun AudioVisualizer(
	modifier: Modifier = Modifier,
	mediaRecorder: MediaRecorder,
	enable: Boolean,
	interval: Long = 100
) {
	AudioVisualizer(
		modifier = modifier,
		amplitudeList = deriveAmplitudeList(mediaRecorder, enable, interval)
	)
}
