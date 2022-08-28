package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.Canvas
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

private const val amplitudeMin = 100
private const val amplitudeMax = 10000
private const val amplitudeFallStep = 1000

private const val lineWidth = 6F
private const val lineSpacing = 2F

/**
 * The display type to use for the audio visualizer.
 *
 * STREAM will display a single bar per entry, and push
 * old entries out of bounds to the left.
 *
 * SUMMARY will stretch or compress the entries to show
 * the entire waveform in the view.
 */
enum class AudioVisualizerDisplayType {
	STREAM,
	SUMMARY
}

@Composable
fun AudioVisualizer(
	modifier: Modifier = Modifier,
	amplitudeList: List<Int>,
	displayType: AudioVisualizerDisplayType = AudioVisualizerDisplayType.STREAM,
	progress: Float = 1F
) {
	val solidColor = LocalContentColor.current
	val transparentColor = solidColor.copy(alpha = 0.3F)
	
	Canvas(modifier = modifier) {
		//Get the dimensions of the canvas
		val canvasWidth = size.width
		val canvasHeight = size.height
		
		//Estimate how many lines we'll be able to render
		val lineCount = (canvasWidth / (lineWidth + lineSpacing)).toInt()
		
		val displayList = when(displayType) {
			AudioVisualizerDisplayType.STREAM -> {
				//Take the last n items
				amplitudeList.subList(
					(amplitudeList.size - lineCount).coerceAtLeast(0),
					amplitudeList.size
				).asReversed()
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
		
		//Make sure that all amplitudes fit within a preset range
		val sanitizedList = displayList.map { it.coerceIn(amplitudeMin, amplitudeMax) }
		
		//Get the pixel that the progress falls on
		val progressPixelX = progress * canvasWidth
		
		sanitizedList.forEachIndexed { index, amplitude ->
			//Calculate the line dimensions
			val lineHeight = (amplitude.toFloat() / amplitudeMax.toFloat()) * canvasHeight
			
			val posX = when(displayType) {
				AudioVisualizerDisplayType.STREAM -> {
					canvasWidth - lineWidth - ((lineSpacing + lineWidth) * index)
				}
				AudioVisualizerDisplayType.SUMMARY -> {
					(lineWidth + lineSpacing) * index
				}
			}
			val posY = (canvasHeight - lineHeight) / 2
			
			//How many pixels wide should be solid
			val widthSolid = (progressPixelX - posX).coerceIn(0F, lineWidth)
			//How many pixels wide should be transparent
			val widthTransparent = ((posX + lineWidth) - progressPixelX).coerceIn(0F, lineWidth)
			
			if(widthSolid > 0) {
				//Draw the line with a solid color
				drawRect(
					color = solidColor,
					topLeft = Offset(
						x = posX,
						y = posY
					),
					size = Size(
						width = widthSolid,
						height = lineHeight
					)
				)
			}
			if(widthTransparent > 0) {
				//Draw the line with a transparent color
				drawRect(
					color = transparentColor,
					topLeft = Offset(
						x = posX + widthSolid,
						y = posY
					),
					size = Size(
						width = widthTransparent,
						height = lineHeight
					)
				)
			}
		}
	}
}
