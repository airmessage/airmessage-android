package me.tagavari.airmessage.compose

import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.tagavari.airmessage.compose.component.ConversationMessagingPane
import me.tagavari.airmessage.compose.component.SyncDialog
import me.tagavari.airmessage.compose.interop.GestureTrackable
import me.tagavari.airmessage.compose.interop.GestureTracker
import me.tagavari.airmessage.compose.provider.ConnectionServiceLocalProvider
import me.tagavari.airmessage.compose.state.ConversationsDetailPage
import me.tagavari.airmessage.compose.state.ConversationsViewModel
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.container.ConversationReceivedContent
import me.tagavari.airmessage.container.PendingConversationReceivedContent
import me.tagavari.airmessage.data.ForegroundState
import me.tagavari.airmessage.data.SharedPreferencesManager
import me.tagavari.airmessage.helper.NotificationHelper
import me.tagavari.airmessage.helper.PlatformHelper
import me.tagavari.airmessage.helper.ShortcutHelper.shortcutIDToConversationID
import me.tagavari.airmessage.helper.getParcelableArrayListExtraCompat
import me.tagavari.airmessage.helper.getParcelableExtraCompat

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class ConversationsCompose : FragmentActivity(), GestureTrackable {
	private val viewModel: ConversationsViewModel by viewModels()
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		//Apply the launch intent when the activity is created
		if(savedInstanceState == null) {
			applyIntent(intent)
		}
		
		//Redirect if the user needs to configure the app
		if(!SharedPreferencesManager.isConnectionConfigured(this)) {
			startActivity(Intent(this, OnboardingCompose::class.java))
			finish()
			return
		}
		
		//Render edge-to-edge
		WindowCompat.setDecorFitsSystemWindows(window, false)
		
		//Subscribe to window information updates
		val devicePostureFlow = WindowInfoTracker.getOrCreate(this)
			.windowLayoutInfo(this)
			.flowWithLifecycle(lifecycle)
			.map {
				it.displayFeatures
					.filterIsInstance<FoldingFeature>()
					.firstOrNull()
			}
			.stateIn(
				scope = lifecycleScope,
				started = SharingStarted.Eagerly,
				initialValue = null
			)
		
		setContent {
			ConnectionServiceLocalProvider(context = this) {
				AirMessageAndroidTheme {
					PlatformHelper.updateChromeOSTopBarCompose(this)
					
					ConversationMessagingPane(
						devicePosture = devicePostureFlow.collectAsState(initial = null).value,
						windowSizeClass = calculateWindowSizeClass(this)
					)
					
					SyncDialog(
						activity = this,
						conversations = viewModel.conversations?.getOrNull() ?: emptyList()
					)
				}
			}
		}
	}
	
	override fun onResume() {
		super.onResume()
		
		//Clear all message notifications
		ContextCompat.getSystemService(this, NotificationManager::class.java)?.let { notificationManager ->
			notificationManager.activeNotifications.asSequence()
				.filter { it.tag == NotificationHelper.notificationTagMessage
						|| it.id == NotificationHelper.notificationIDMessageSummary }
				.forEach {
					notificationManager.cancel(it.tag, it.id)
				}
		}
		
		//Update the foreground state
		ForegroundState.isInForeground = true
	}
	
	override fun onPause() {
		super.onPause()
		
		//Update the foreground state
		ForegroundState.isInForeground = false
	}
	
	override fun onNewIntent(intent: Intent?) {
		super.onNewIntent(intent)
		
		//Ignore null intents
		if(intent == null) return
		
		applyIntent(intent)
	}
	
	private fun applyIntent(intent: Intent) {
		when(intent.action) {
			Intent.ACTION_DEFAULT -> {
				//Update the selected conversation
				val conversationID = intent.getLongExtra(INTENT_TARGET_ID, -1)
				if(conversationID != -1L) {
					var messageText: String? = null
					
					if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
						//Set the draft content from the notification
						messageText = intent.getStringExtra(Notification.EXTRA_REMOTE_INPUT_DRAFT)
					}
					
					viewModel.setSelectedConversation(conversationID, ConversationReceivedContent(messageText))
				}
			}
			Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE, Intent.ACTION_SENDTO -> {
				var targetNewConversation = false
				var targetConversationID: Long? = null
				var targetSMSParticipants: List<String>? = null
				
				var messageText: String? = null
				var messageAttachments: List<Uri> = listOf()
				
				if(intent.action == Intent.ACTION_SEND || intent.action == Intent.ACTION_SEND_MULTIPLE) {
					//Handle intents towards a specific conversation
					if(intent.hasExtra(INTENT_TARGET_ID)) {
						targetConversationID = intent.getLongExtra(INTENT_TARGET_ID, -1L)
					}
					//Handle intents towards a new conversation
					else if(intent.getBooleanExtra(INTENT_TARGET_NEW, false)) {
						targetNewConversation = true
					}
					//Handle intents from direct share
					else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && intent.hasExtra(Intent.EXTRA_SHORTCUT_ID)) {
						val shortcutID = intent.getStringExtra(Intent.EXTRA_SHORTCUT_ID)!!
						targetConversationID = shortcutIDToConversationID(shortcutID)
					}
					
					if(intent.action == Intent.ACTION_SEND) {
						messageText = intent.getStringExtra(Intent.EXTRA_TEXT)
						intent.getParcelableExtraCompat<Uri>(Intent.EXTRA_STREAM)?.let {
							messageAttachments = listOf(it)
						}
					} else {
						messageText = intent.getStringArrayListExtra(Intent.EXTRA_TEXT)?.joinToString(separator = " ")
						intent.getParcelableArrayListExtraCompat<Uri>(Intent.EXTRA_STREAM)?.let {
							messageAttachments = it
						}
					}
				} else if(intent.action == Intent.ACTION_SENDTO) {
					//Check if the request came from an SMS link
					intent.data?.let { uri ->
						if(setOf("sms", "smsto", "mms", "mmsto").contains(uri.scheme)) {
							targetSMSParticipants = uri.authority?.split(",")
							
							messageText = intent.getStringExtra("sms_body")
							intent.getParcelableExtraCompat<Uri>(Intent.EXTRA_STREAM)?.let {
								messageAttachments = listOf(it)
							}
						}
					}
				}
				
				val receivedContent = ConversationReceivedContent(messageText, messageAttachments)
				
				//Start a new conversation
				if(targetNewConversation) {
					//Set the pending received content for a NULL conversation
					viewModel.setPendingReceivedContent(PendingConversationReceivedContent(null, receivedContent))
					
					//Create a new conversation
					viewModel.detailPage = ConversationsDetailPage.NewConversation
				}
				
				//Resolve the SMS conversation
				targetSMSParticipants?.let { participants ->
					viewModel.selectTextMessageConversation(participants, receivedContent)
				}
				
				//Open the target conversation
				targetConversationID?.let { conversationID ->
					viewModel.setSelectedConversation(conversationID, receivedContent)
				}
			}
		}
	}
	
	//GESTURE HANDLING
	
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
		const val INTENT_TARGET_NEW = "targetNew"
	}
}
