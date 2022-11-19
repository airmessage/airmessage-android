package me.tagavari.airmessage.common.compose.remember

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.rx3.await
import me.tagavari.airmessage.common.helper.ConversationBuildHelper
import me.tagavari.airmessage.common.messaging.ConversationInfo

/**
 * Creates a conversation title from a conversation
 */
@Composable
fun produceConversationTitle(conversation: ConversationInfo): State<String> {
	val context = LocalContext.current
	return produceState(
		initialValue = ConversationBuildHelper.buildConversationTitleDirect(context, conversation),
		conversation,
		deriveContactUpdates()
	) {
		value = ConversationBuildHelper.buildConversationTitle(context, conversation).await()
	}
}
