package me.tagavari.airmessage.common.redux

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*
import kotlinx.coroutines.rx3.asFlow
import me.tagavari.airmessage.common.data.DatabaseManager
import me.tagavari.airmessage.common.helper.ConversationHelper
import me.tagavari.airmessage.common.helper.ShortcutHelper
import me.tagavari.airmessage.common.messaging.ConversationInfo

//A receiver that handles creating and updating shortcuts
@RequiresApi(api = Build.VERSION_CODES.N_MR1)
class ReduxReceiverShortcut(private val context: Context) {
	@OptIn(DelicateCoroutinesApi::class)
	fun initialize() {
		GlobalScope.launch { ReduxEmitterNetwork.messageUpdateSubject.asFlow().collect(::handleMessaging) }
		GlobalScope.launch { ReduxEmitterNetwork.massRetrievalUpdateSubject.asFlow().collect(::handleMassRetrieval) }
		GlobalScope.launch { ReduxEmitterNetwork.textImportUpdateSubject.asFlow().collect(::handleTextImport) }
	}
	
	private suspend fun handleMessaging(event: ReduxEventMessaging) {
		when(event) {
			is ReduxEventMessaging.Message -> {
				pushConversations(event.conversationItems.map { it.first })
			}
			is ReduxEventMessaging.ConversationUpdate -> {
				pushConversations(
					event.newConversations.keys.sortedWith(ConversationHelper.conversationComparator)
				)
			}
			is ReduxEventMessaging.ConversationDelete -> {
				ShortcutHelper.disableShortcuts(context, listOf(event.conversationID))
			}
			is ReduxEventMessaging.ReduxConversationAction -> {
				val conversation = withContext(Dispatchers.IO) {
					DatabaseManager.getInstance().fetchConversationInfo(context, event.conversationID)
				}
				
				if(conversation != null) {
					ShortcutHelper.updateShortcut(context, conversation)
				}
			}
			is ReduxEventMessaging.ConversationServiceHandlerDelete -> {
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