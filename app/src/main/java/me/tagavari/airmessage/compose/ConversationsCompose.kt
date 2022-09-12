package me.tagavari.airmessage.compose

import android.app.NotificationManager
import android.content.Intent
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import soup.compose.material.motion.MaterialFadeThrough
import soup.compose.material.motion.MaterialSharedAxisX

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class ConversationsCompose : FragmentActivity(), GestureTrackable {
	@OptIn(ExperimentalAnimationApi::class)
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
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
					
					var selectedConversationID by rememberSaveable { mutableStateOf<Long?>(null) }
					
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
								onSelectConversation = { selectedConversationID = it },
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
											Modifier.statusBarsPadding()
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
									onSelectConversation = { selectedConversationID = it },
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
	}
}
