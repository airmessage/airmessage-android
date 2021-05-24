package me.tagavari.airmessage.redux

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import me.tagavari.airmessage.data.DatabaseManager
import me.tagavari.airmessage.helper.ConversationHelper
import me.tagavari.airmessage.helper.ShortcutHelper
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.redux.ReduxEventMessaging.*

//A receiver that handles creating and updating shortcuts
@RequiresApi(api = Build.VERSION_CODES.N_MR1)
class ReduxReceiverShortcut(private val context: Context) {
	private val compositeDisposable = CompositeDisposable()
	
	fun initialize() {
		compositeDisposable.addAll(
			ReduxEmitterNetwork.messageUpdateSubject.subscribe(::handleMessaging),
			ReduxEmitterNetwork.massRetrievalUpdateSubject.subscribe(::handleMassRetrieval),
			ReduxEmitterNetwork.textImportUpdateSubject.subscribe(::handleTextImport)
		)
	}
	
	private fun handleMessaging(event: ReduxEventMessaging) {
		if(event is Message) {
			pushConversations(event.conversationItems.map { it.first })
		} else if(event is ConversationUpdate) {
			pushConversations(
				event.newConversations.keys.toList()
					.sortedWith(ConversationHelper.conversationComparator)
			)
		} else if(event is ConversationTitle || event is ConversationMember || event is ConversationMemberColor) {
			ShortcutHelper.updateShortcut(context, (event as ReduxConversationAction).conversationInfo).subscribe()
		} else if(event is ConversationDelete) {
			ShortcutHelper.disableShortcuts(context, listOf(event.conversationInfo.localID))
		} else if(event is ConversationServiceHandlerDelete) {
			ShortcutHelper.disableShortcuts(context, event.deletedIDs)
		}
	}
	
	private fun handleMassRetrieval(event: ReduxEventMassRetrieval) {
		if(event is ReduxEventMassRetrieval.Complete) {
			updateTopConversations()
		}
	}
	
	private fun handleTextImport(event: ReduxEventTextImport) {
		if(event is ReduxEventTextImport.Complete) {
			updateTopConversations()
		}
	}
	
	private fun pushConversations(conversationList: List<ConversationInfo>) {
		//Push new received conversations to the top of the dynamic shortcut list, ignoring archived conversations
		for(conversationInfo in conversationList) {
			if(conversationInfo.isArchived) continue
			ShortcutHelper.pushShortcut(context, conversationInfo).subscribe()
		}
	}
	
	private fun updateTopConversations() {
		//Pull the most recent conversations from the database and assign them as dynamic shortcuts
		Single.fromCallable {
			DatabaseManager.getInstance().fetchSummaryConversations(context, false, ShortcutHelper.dynamicShortcutLimit)
		}
			.subscribeOn(Schedulers.single())
			.observeOn(AndroidSchedulers.mainThread())
			.flatMapCompletable { conversations: List<ConversationInfo> ->
				ShortcutHelper.assignShortcuts(context, conversations)
			}
			.subscribe()
	}
	
	fun dispose() {
		compositeDisposable.clear()
	}
}