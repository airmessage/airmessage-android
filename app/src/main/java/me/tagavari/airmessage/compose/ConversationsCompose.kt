package me.tagavari.airmessage.compose

import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import me.tagavari.airmessage.R
import me.tagavari.airmessage.activity.NewMessage
import me.tagavari.airmessage.activity.Preferences
import me.tagavari.airmessage.compose.component.ConversationPane
import me.tagavari.airmessage.compose.component.MessagingScreen
import me.tagavari.airmessage.compose.interop.GestureTrackable
import me.tagavari.airmessage.compose.interop.GestureTracker
import me.tagavari.airmessage.compose.provider.ConnectionServiceLocalProvider
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.connection.ConnectionManager
import me.tagavari.airmessage.data.SharedPreferencesManager
import me.tagavari.airmessage.fragment.FragmentSync
import me.tagavari.airmessage.helper.NotificationHelper
import me.tagavari.airmessage.helper.ShortcutHelper
import me.tagavari.airmessage.helper.ShortcutHelper.shortcutIDToConversationID
import me.tagavari.airmessage.helper.getParcelableArrayListExtraCompat
import me.tagavari.airmessage.helper.getParcelableExtraCompat
import soup.compose.material.motion.MaterialFadeThrough
import soup.compose.material.motion.MaterialSharedAxisX

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class ConversationsCompose : FragmentActivity(), GestureTrackable {
	private var selectedConversationID by mutableStateOf<Long?>(null)
	
	@OptIn(ExperimentalAnimationApi::class)
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		//Load the selected conversation ID
		selectedConversationID = savedInstanceState
			?.getLong(saveStateKeySelectedConversationID, -1L)
			?.let { if(it == -1L) null else it }
		
		//Apply intent values
		applyIntent(intent)
		
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
					val windowSizeClass = calculateWindowSizeClass(this)
					val isExpandedScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
					
					if(isExpandedScreen) {
						val devicePosture by devicePostureFlow.collectAsState(initial = null)
						val hingeBounds = devicePosture?.bounds
						
						val hingeOffset: Dp
						val hingeWidth: Dp
						if(hingeBounds != null) {
							with(LocalDensity.current) {
								hingeOffset = hingeBounds.left.toDp()
								hingeWidth = hingeBounds.width().toDp()
							}
						} else {
							//Hardcode for non-foldables
							hingeOffset = 384.dp
							hingeWidth = 0.dp
						}
						
						Row(
							modifier = Modifier
								.background(MaterialTheme.colorScheme.inverseOnSurface)
								.fillMaxSize()
						) {
							ConversationPane(
								modifier = Modifier.width(hingeOffset),
								floatingPane = true,
								activeConversationID = selectedConversationID,
								onShowSyncDialog = ::showSyncFragment,
								onSelectConversation = { selectConversation(it) },
								onNavigateSettings = { startActivity(Intent(this@ConversationsCompose, Preferences::class.java)) },
								onNewConversation = { startActivity(Intent(this@ConversationsCompose, NewMessage::class.java)) }
							)
							
							selectedConversationID?.let { selectedConversationID ->
								Spacer(modifier = Modifier.width(hingeWidth))
								
								val useFloatingPane = devicePosture?.isSeparating != true
								
								MaterialFadeThrough(
									targetState = selectedConversationID,
								) { localSelectedConversationID ->
									Box(
										modifier = if(useFloatingPane) {
											Modifier
												.statusBarsPadding()
												.padding(end = 16.dp)
												.clip(
													RoundedCornerShape(
														topStart = 20.dp,
														topEnd = 20.dp
													)
												)
										} else {
											Modifier
										}
									) {
										key(localSelectedConversationID) {
											MessagingScreen(
												conversationID = localSelectedConversationID,
												floatingPane = useFloatingPane
											)
										}
									}
								}
							}
						}
					} else {
						//Handle back presses
						BackHandler(enabled = selectedConversationID != null) {
							selectedConversationID = null
						}
						
						MaterialSharedAxisX(
							modifier = Modifier.background(MaterialTheme.colorScheme.background),
							targetState = selectedConversationID,
							forward = selectedConversationID != null,
						) { localSelectedConversationID ->
							if(localSelectedConversationID == null) {
								ConversationPane(
									activeConversationID = null,
									onShowSyncDialog = ::showSyncFragment,
									onSelectConversation = { selectConversation(it) },
									onNavigateSettings = { startActivity(Intent(this@ConversationsCompose, Preferences::class.java)) },
									onNewConversation = { startActivity(Intent(this@ConversationsCompose, NewMessage::class.java)) }
								)
							} else {
								key(localSelectedConversationID) {
									MessagingScreen(
										conversationID = localSelectedConversationID,
										navigationIcon = {
											IconButton(onClick = { selectedConversationID = null }) {
												Icon(
													imageVector = Icons.Filled.ArrowBack,
													contentDescription = stringResource(id = R.string.action_back)
												)
											}
										}
									)
								}
							}
						}
					}
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
	}
	
	override fun onNewIntent(intent: Intent?) {
		super.onNewIntent(intent)
		
		//Ignore null intents
		if(intent == null) return
		
		applyIntent(intent)
	}
	
	/**
	 * Handles setting the selected conversation, or NULL to deselect
	 */
	private fun selectConversation(conversationID: Long?) {
		selectedConversationID = conversationID
		
		if(conversationID != null) {
			ShortcutHelper.reportShortcutUsed(this, conversationID)
		}
	}
	
	private fun showSyncFragment(connectionManager: ConnectionManager, deleteMessages: Boolean) {
		//Ignore if we're already showing the fragment
		if(supportFragmentManager.findFragmentByTag(keyFragmentSync) != null) return
		
		//Create and show the sync fragment
		val fragmentSync = FragmentSync(
			connectionManager.serverDeviceName,
			connectionManager.serverInstallationID,
			deleteMessages
		)
		fragmentSync.isCancelable = false
		fragmentSync.show(supportFragmentManager, keyFragmentSync)
	}
	
	private fun applyIntent(intent: Intent) {
		//Update the selected conversation
		if(intent.hasExtra(INTENT_TARGET_ID)) {
			val conversationID = intent.getLongExtra(INTENT_TARGET_ID, -1)
			if(conversationID != -1L) {
				selectedConversationID = conversationID
			}
		}
		
		var targetConversationID: Long? = null
		var targetSMSParticipants: Collection<String>? = null
		
		var messageText: String? = null
		var messageAttachments: Collection<Uri>? = null
		
		if(intent.action == Intent.ACTION_SEND || intent.action == Intent.ACTION_SEND_MULTIPLE) {
			//Check if the request came from direct share
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				intent.getStringExtra(Intent.EXTRA_SHORTCUT_ID)?.let { shortcutID ->
					targetConversationID = shortcutIDToConversationID(shortcutID)
					
					if(intent.action == Intent.ACTION_SEND) {
						messageText = intent.getStringExtra(Intent.EXTRA_TEXT)
						messageAttachments = intent.getParcelableExtraCompat<Uri>(Intent.EXTRA_STREAM)
							?.let { listOf(it) }
					} else {
						messageText = intent.getStringArrayListExtra(Intent.EXTRA_TEXT)?.joinToString(separator = " ")
						messageAttachments = intent.getParcelableArrayListExtraCompat(Intent.EXTRA_STREAM)
					}
				}
			}
		} else if(intent.action == Intent.ACTION_SENDTO) {
			//Check if the request came from an SMS link
			intent.data?.let { uri ->
				if(setOf("sms", "smsto", "mms", "mmsto").contains(uri.scheme)) {
					targetSMSParticipants = uri.authority?.split(",")
					
					messageText = intent.getStringExtra("sms_body")
					messageAttachments = intent.getParcelableExtraCompat<Uri>(Intent.EXTRA_STREAM)
						?.let { listOf(it) }
				}
			}
		}
	}
	
	//STATE PERSISTENCE
	
	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		
		outState.putLong(saveStateKeySelectedConversationID, selectedConversationID ?: -1)
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
		private const val keyFragmentSync = "fragment_sync"
		
		private const val saveStateKeySelectedConversationID = "selectedConversationID"
		
		const val INTENT_TARGET_ID = "targetID"
		const val INTENT_DATA_TEXT = "dataText"
		const val INTENT_DATA_FILE = "dataFile"
		const val INTENT_BUBBLE = "bubble"
	}
}
