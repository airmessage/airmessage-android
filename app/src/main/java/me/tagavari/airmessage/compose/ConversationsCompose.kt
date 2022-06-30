package me.tagavari.airmessage.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.tagavari.airmessage.compose.component.ConversationList
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.data.DatabaseManager
import me.tagavari.airmessage.messaging.ConversationInfo

class ConversationsCompose : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		WindowCompat.setDecorFitsSystemWindows(window, false)
		
		setContent {
			val context = LocalContext.current
			
			val conversations by produceState<Result<List<ConversationInfo>>?>(initialValue = null) {
				value = withContext(Dispatchers.IO) {
					try {
						Result.success(DatabaseManager.getInstance().fetchSummaryConversations(context, false))
					} catch(throwable: Throwable) {
						Result.failure(throwable)
					}
				}
			}
			
			AirMessageAndroidTheme {
				ConversationList(conversations = conversations) {
				
				}
			}
		}
	}
}
