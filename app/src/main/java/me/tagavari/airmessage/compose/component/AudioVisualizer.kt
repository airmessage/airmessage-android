package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.Canvas
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.ceil

private const val amplitudeMin = 100
private const val amplitudeMax = 10000
private const val amplitudeFallStep = 1000

private const val lineWidth = 6F
private const val lineSpacing = 2F

enum class AudioVisualizerDisplayType {
	STREAM,
	SUMMARY
}

@Composable
fun AudioVisualizer(
	modifier: Modifier = Modifier,
	amplitudeList: List<Int>,
	displayType: AudioVisualizerDisplayType = AudioVisualizerDisplayType.STREAM
) {
	val color = LocalContentColor.current
	
	Canvas(modifier = modifier) {
		val canvasWidth = size.width
		val canvasHeight = size.height
		
		val lineCount = ceil(canvasWidth / (lineWidth + lineSpacing)).toInt()
		
		val displayList = when(displayType) {
			AudioVisualizerDisplayType.STREAM -> {
				//Take the last n items
				amplitudeList.subList(
					(amplitudeList.size - lineCount).coerceAtLeast(0),
					amplitudeList.size
				)
			}
			AudioVisualizerDisplayType.SUMMARY -> {
				//Stretch or compress the items to fit
				val itemCount = amplitudeList.size
				
				if(itemCount < lineCount) {
					val newList = amplitudeList.toMutableList()
					
					if(amplitudeList.isNotEmpty()) {
						//Copy items at even intervals
						val missingItemCount = lineCount - itemCount
						for(i in missingItemCount - 1 downTo 0) {
							val index = ((i.toFloat() / missingItemCount.toFloat()) * (amplitudeList.size - 1)).toInt()
							newList.add(index, newList[index])
						}
					}
					
					newList
				} else if(itemCount > lineCount) {
					val newList = amplitudeList.toMutableList()
					
					if(amplitudeList.isNotEmpty()) {
						//Remove items at even intervals
						val excessItemCount = itemCount - lineCount
						for(i in excessItemCount - 1 downTo 0) {
							val index = ((i.toFloat() / excessItemCount.toFloat()) * (amplitudeList.size - 1)).toInt()
							newList.removeAt(index)
						}
					}
					
					newList
				} else {
					amplitudeList
				}
			}
		}
		
		/* val sanitizedList = mutableListOf<Int>().also { list ->
			var lastValue = 0
			for(amplitude in amplitudeList) {
				val clampedAmplitude = amplitude.coerceIn(amplitudeMin, amplitudeMax)
				val smoothedAmplitude = clampedAmplitude.coerceAtLeast(lastValue - amplitudeFallStep)
				
				lastValue = smoothedAmplitude
				list.add(smoothedAmplitude)
			}
		} */
		val sanitizedList = displayList.map { it.coerceIn(amplitudeMin, amplitudeMax) }
		
		sanitizedList
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
