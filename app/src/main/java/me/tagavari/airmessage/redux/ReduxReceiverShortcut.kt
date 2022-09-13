package me.tagavari.airmessage.redux

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*
import kotlinx.coroutines.rx3.asFlow
import me.tagavari.airmessage.data.DatabaseManager
import me.tagavari.airmessage.helper.ConversationHelper
import me.tagavari.airmessage.helper.ShortcutHelper
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.redux.ReduxEventMessaging.*

//A receiver that handles creating and updating shortcuts
@RequiresApi(api = Build.VERSION_CODES.N_MR1)
class ReduxReceiverShortcut(private val context: Context) {
	@OptIn(DelicateCoroutinesApi::class)
	fun initialize() {
		GlobalScope.launch {
			launch { ReduxEmitterNetwork.messageUpdateSubject.asFlow().collect(::handleMessaging) }
			launch { ReduxEmitterNetwork.massRetrievalUpdateSubject.asFlow().collect(::handleMassRetrieval) }
			launch { ReduxEmitterNetwork.textImportUpdateSubject.asFlow().collect(::handleTextImport) }
		}
	}
	
	private suspend fun handleMessaging(event: ReduxEventMessaging) {
		when(event) {
			is Message -> {
				pushConversations(event.conversationItems.map { it.first })
			}
			is ConversationUpdate -> {
				pushConversations(
					event.newConversations.keys.sortedWith(ConversationHelper.conversationComparator)
				)
			}
			is ReduxConversationAction -> {
				ShortcutHelper.updateShortcut(context, event.conversationInfo)
			}
			is ConversationDelete -> {
				ShortcutHelper.disableShortcuts(context, listOf(event.conversationInfo.localID))
			}
			is ConversationServiceHandlerDelete -> {
				ShortcutHelper.disableShortcuts(context, event.deletedIDs)
			}
			else -> {}
		}
	}
	
	private suspend fun handleMassRetrieval(event: ReduxEventMassRetrieval) {
		if(event is ReduxEventMassRetrieval.Complete) {
			updateTopConversations()
		}
	}
	
	private suspend fun handleTextImport(event: ReduxEventTextImport) {
		if(event is ReduxEventTextImport.Complete) {
			updateTopConversations()
		}
	}
	
	private suspend fun pushConversations(conversationList: List<ConversationInfo>) {
		//Push new received conversations to the top of the dynamic shortcut list, ignoring archived conversations
		for(conversationInfo in conversationList) {
			if(conversationInfo.isArchived) continue
			ShortcutHelper.pushShortcut(context, conversationInfo)
		}
	}
	
	private suspend fun updateTopConversations() {
		//Pull the most recent conversations from the database and assign them as dynamic shortcuts
		val topConversations = withContext(Dispatchers.IO) {
			DatabaseManager.getInstance().fetchSummaryConversations(context, false, ShortcutHelper.dynamicShortcutLimit)
		}
		ShortcutHelper.assignShortcuts(context, topConversations)
	}
}