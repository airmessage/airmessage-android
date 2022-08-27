package me.tagavari.airmessage.compose.component

import android.text.format.DateUtils
import android.view.MotionEvent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.interop.GestureTrackable
import me.tagavari.airmessage.compose.interop.GestureTracker
import me.tagavari.airmessage.compose.remember.AudioPlaybackState
import me.tagavari.airmessage.compose.util.findActivity

private data class Positioning(
	val x: Float,
	val y: Float,
	val width: Float,
	val height: Float
) {
	/**
	 * Gets if the provided coordinates fall within the bounds
	 * of this positioning
	 */
	fun isInside(checkX: Float, checkY: Float): Boolean {
		return checkX >= x
				&& checkY >= y
				&& checkX <= x + width
				&& checkY <= y + height
	}
}

@Composable
fun MessageInputBarAudio(
	duration: Int,
	isRecording: Boolean,
	onStopRecording: () -> Unit,
	onSend: () -> Unit,
	onDiscard: () -> Unit,
	onTogglePlay: () -> Unit,
	playbackState: AudioPlaybackState,
	amplitudeList: List<Int>,
) {
	val gestureTrackable = LocalContext.current.findActivity() as? GestureTrackable
		?: throw IllegalStateException("Must be a GestureTrackerActivity")
	
	var sendButtonPositioning by remember { mutableStateOf<Positioning?>(null) }
	var recordButtonPositioning by remember { mutableStateOf<Positioning?>(null) }
	
	var sendButtonHover by remember { mutableStateOf(false) }
	var recordButtonHover by remember { mutableStateOf(false) }
	
	val currentSendButtonHover by rememberUpdatedState(sendButtonHover)
	val currentRecordButtonHover by rememberUpdatedState(recordButtonHover)
	
	DisposableEffect(gestureTrackable, sendButtonPositioning, recordButtonPositioning) {
		val listener: GestureTracker = listener@{ event ->
			when(event.action) {
				MotionEvent.ACTION_MOVE -> {
					//Track hover states
					sendButtonPositioning?.let { positioning ->
						sendButtonHover = positioning.isInside(event.rawX, event.rawY)
					}
					
					recordButtonPositioning?.let { positioning ->
						recordButtonHover = positioning.isInside(event.rawX, event.rawY)
					}
					
					false
				}
				MotionEvent.ACTION_UP -> {
					//Stop recording when the user releases
					onStopRecording()
					
					if(currentSendButtonHover) {
						onSend()
					}
					
					true
				}
				else -> false
			}
		}
		
		gestureTrackable.addGestureTracker(listener)
		onDispose {
			gestureTrackable.removeGestureTracker(listener)
		}
	}
	
	Row(
		modifier = Modifier
			.height(40.dp)
			.fillMaxWidth(),
		horizontalArrangement = Arrangement.End,
		verticalAlignment = Alignment.Bottom
	) {
		Surface(
			modifier = Modifier.fillMaxWidth(0.6F),
			shape = RoundedCornerShape(100),
			tonalElevation = 4.dp
		) {
			Row(
				modifier = Modifier.padding(horizontal = 12.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				if(!isRecording) {
					IconButton(
						onClick = onDiscard
					) {
						Icon(
							painter = painterResource(id = R.drawable.close_circle),
							contentDescription = stringResource(id = android.R.string.cancel)
						)
					}
					
					Spacer(modifier = Modifier.width(4.dp))
				}
				
				AudioVisualizer(
					modifier = Modifier
						.weight(1F)
						.fillMaxHeight()
						.padding(vertical = 1.dp),
					amplitudeList = amplitudeList,
					displayType = if(isRecording) AudioVisualizerDisplayType.STREAM
					else AudioVisualizerDisplayType.SUMMARY
				)
				
				Spacer(modifier = Modifier.width(4.dp))
				
				Text(
					text = remember(duration) {
						DateUtils.formatElapsedTime(duration.toLong())
					}
				)
			}
		}
		Spacer(modifier = Modifier.width(8.dp))
		
		Surface(
			modifier = Modifier.wrapContentHeight(align = Alignment.Bottom, unbounded = true),
			shape = RoundedCornerShape(100),
			tonalElevation = 4.dp
		) {
			Column(modifier = Modifier.padding(8.dp)) {
				IconButton(
					onClick = onSend
				) {
					Icon(
						modifier = Modifier
							.size(48.dp)
							.onGloballyPositioned { coordinates ->
								val position = coordinates.positionInRoot()
								sendButtonPositioning = Positioning(
									x = position.x,
									y = position.y,
									width = coordinates.size.width.toFloat(),
									height = coordinates.size.height.toFloat()
								)
							}
							.alpha(if(sendButtonHover) 0.5F else 1F),
						painter = painterResource(id = R.drawable.push_rounded),
						contentDescription = null,
						tint = MaterialTheme.colorScheme.primary
					)
				}
				
				Spacer(modifier = Modifier.height(48.dp))
				
				IconButton(
					onClick = onTogglePlay
				) {
					Icon(
						modifier = Modifier
							.size(48.dp)
							.onGloballyPositioned { coordinates ->
								val position = coordinates.positionInRoot()
								recordButtonPositioning = Positioning(
									x = position.x,
									y = position.y,
									width = coordinates.size.width.toFloat(),
									height = coordinates.size.height.toFloat()
								)
							}
							.alpha(if(recordButtonHover) 0.5F else 1F),
						painter = painterResource(id = R.drawable.play_circle_rounded),
						contentDescription = null,
						tint = MaterialTheme.colorScheme.primary
					)
				}
			}
		}
	}
}
