package me.tagavari.airmessage.compose

import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import kotlinx.coroutines.flow.flowOf
import me.tagavari.airmessage.compose.component.MessagingScreen
import me.tagavari.airmessage.compose.interop.GestureTrackable
import me.tagavari.airmessage.compose.interop.GestureTracker
import me.tagavari.airmessage.compose.provider.ConnectionServiceLocalProvider
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme

class MessagingCompose : ComponentActivity(), GestureTrackable {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		WindowCompat.setDecorFitsSystemWindows(window, false)
		
		//Get the conversation ID
		val conversationID = intent.getLongExtra(INTENT_TARGET_ID, -1)
		
		setContent {
			ConnectionServiceLocalProvider(context = this) {
				AirMessageAndroidTheme {
					MessagingScreen(
						conversationID = conversationID,
						receivedContentFlow = flowOf(),
						onProcessedReceivedContent = {}
					)
				}
			}
		}
	}
	
	private val gestureTrackers = mutableSetOf<GestureTracker>()
	
	override fun addGestureTracker(gestureTracker: GestureTracker) {
		gestureTrackers.add(gestureTracker)
	}
	
	override fun removeGestureTracker(gestureTracker: GestureTracker) {
		gestureTrackers.remove(gestureTracker)
	}
	
	override fun onTouchEvent(event: MotionEvent): Boolean {
		var consume = false
		for(tracker in gestureTrackers) {
			val trackerResult = tracker(event)
			consume = consume || trackerResult
		}
		
		val superResult = super.onTouchEvent(event)
		return consume || superResult
	}
	
	companion object {
		const val INTENT_TARGET_ID = "targetID"
	}
}
