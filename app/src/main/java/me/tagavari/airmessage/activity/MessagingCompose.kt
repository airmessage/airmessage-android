package me.tagavari.airmessage.activity

import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.flowOf
import me.tagavari.airmessage.common.compose.component.MessagingScreen
import me.tagavari.airmessage.common.compose.interop.GestureTrackable
import me.tagavari.airmessage.common.compose.interop.GestureTracker
import me.tagavari.airmessage.common.compose.provider.ConnectionServiceLocalProvider
import me.tagavari.airmessage.common.compose.state.MessagingViewModel
import me.tagavari.airmessage.common.compose.state.MessagingViewModelFactory
import me.tagavari.airmessage.common.compose.ui.theme.AirMessageAndroidTheme

class MessagingCompose : ComponentActivity(), GestureTrackable {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		WindowCompat.setDecorFitsSystemWindows(window, false)
		
		//Get the conversation ID
		val conversationID = intent.getLongExtra(INTENT_TARGET_ID, -1)
		
		setContent {
			ConnectionServiceLocalProvider(context = this) {
				AirMessageAndroidTheme {
					val viewModel = viewModel<MessagingViewModel>(
						factory = MessagingViewModelFactory(application, conversationID),
						key = conversationID.toString()
					).data
					
					MessagingScreen(
						conversationID = conversationID,
						viewModel = viewModel,
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
