package me.tagavari.airmessage.redux

import android.content.Context
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import me.tagavari.airmessage.MainApplication
import me.tagavari.airmessage.data.DatabaseManager
import me.tagavari.airmessage.data.ForegroundState
import me.tagavari.airmessage.enums.ConversationItemType
import me.tagavari.airmessage.enums.MessageSendErrorCode
import me.tagavari.airmessage.helper.LanguageHelper
import me.tagavari.airmessage.helper.NotificationHelper
import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.messaging.ConversationItem
import me.tagavari.airmessage.messaging.MessageInfo
import me.tagavari.airmessage.redux.ReduxEventMessaging.*

//A receiver that handles creating notifications in response to new events
class ReduxReceiverNotification(private val context: Context) {
	private val compositeDisposable = CompositeDisposable()
	
	fun initialize() {
		compositeDisposable.add(ReduxEmitterNetwork.messageUpdateSubject.subscribe { event: ReduxEventMessaging ->
			if(event is ConversationUpdate) {
				//Loading a list of conversations in the foreground for later filtering
				val foregroundConversations = ForegroundState.foregroundConversationIDs
				
				//Gathering updated conversations
				val transferredConversations: List<Pair<ConversationInfo, Collection<ConversationItem>>> =
					event.transferredConversations.map { transfer -> Pair(transfer.clientConversation, transfer.serverConversationItems.map { it.targetItem })
				}
				val newConversations: List<Pair<ConversationInfo, Collection<ConversationItem>>> =
					event.newConversations.map { (conversation, items) -> Pair(conversation, items) }
				
				for((conversation, items) in (transferredConversations + newConversations)) {
					//Skipping muted and foreground conversations
					if(conversation.isMuted || foregroundConversations.contains(conversation.localID)) continue
					
					//Sending notifications for message items
					for(conversationItem in items) {
						//Ignoring if the item is not a message
						if(conversationItem.itemType != ConversationItemType.message) continue
						conversationItem as MessageInfo
						
						//Ignoring if the message is outgoing
						if(conversationItem.isOutgoing) continue
						
						//Sending the notification
						NotificationHelper.sendNotification(context, conversation, conversationItem)
					}
				}
			} else if(event is Message) {
				//Getting foreground conversations
				val foregroundConversations = ForegroundState.foregroundConversationIDs
				
				//Sending notifications for received messages
				for((conversation, results) in event.conversationItems) {
					//Ignoring if the conversation is muted or is loaded the foreground
					if(conversation.isMuted || foregroundConversations.contains(conversation.localID)) continue
					
					for(result in results) {
						//Getting the item
						val conversationItem = result.targetItem
						
						//Ignoring if the item is not a message
						if(conversationItem.itemType != ConversationItemType.message) continue
						val messageInfo = conversationItem as MessageInfo
						
						//Ignoring if the message is outgoing
						if(messageInfo.isOutgoing) continue
						
						//Sending the notification
						NotificationHelper.sendNotification(MainApplication.getInstance(), conversation, messageInfo)
					}
				}
			} else if(event is MessageError) {
				if(event.errorCode != MessageSendErrorCode.none) {
					NotificationHelper.sendErrorNotification(MainApplication.getInstance(), event.conversationInfo)
				}
			} else if(event is TapbackUpdate) {
				Single.fromCallable {
					val pair = DatabaseManager.getInstance().loadConversationItemWithChat(MainApplication.getInstance(), event.metadata.messageID)
					
					if(pair == null || pair.first.itemType != ConversationItemType.message) {
						throw Exception("Failed to load message ID " + event.metadata.messageID + " for tapback")
					} else {
						return@fromCallable pair
					}
				}.subscribeOn(Schedulers.single())
					.observeOn(AndroidSchedulers.mainThread())
					.onErrorComplete()
					.flatMapSingle { (item, conversation) ->
						//Getting the tapback summary
						val tapbackInfo = event.tapbackInfo
						val messageSummary = LanguageHelper.messageToString(MainApplication.getInstance().resources, item as MessageInfo)
						
						return@flatMapSingle LanguageHelper.getTapbackSummary(MainApplication.getInstance(), tapbackInfo.sender, tapbackInfo.code, messageSummary)
							//Preserving the conversation info
							.map { summary: String -> Pair(summary, conversation) }
					}
					.subscribe { (summary, conversation) ->
						//Sending the notification
						NotificationHelper.sendNotification(
							MainApplication.getInstance(),
							summary,
							event.tapbackInfo.sender,
							System.currentTimeMillis(),
							conversation
						)
					}
			}
		})
	}
	
	fun dispose() {
		compositeDisposable.clear()
	}
}