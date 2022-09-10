package me.tagavari.airmessage.compose

import android.app.NotificationManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import me.tagavari.airmessage.compose.component.ConversationPane
import me.tagavari.airmessage.compose.provider.ConnectionServiceLocalProvider
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.connection.ConnectionManager
import me.tagavari.airmessage.fragment.FragmentSync
import me.tagavari.airmessage.helper.NotificationHelper

class ConversationsCompose : FragmentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		WindowCompat.setDecorFitsSystemWindows(window, false)
		
		setContent {
			ConnectionServiceLocalProvider(context = this) {
				AirMessageAndroidTheme {
					fun showSyncFragment(connectionManager: ConnectionManager, deleteMessages: Boolean) {
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
					
					ConversationPane(onShowSyncDialog = ::showSyncFragment)
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
	
	companion object {
		private const val keyFragmentSync = "fragment_sync"
	}
}
