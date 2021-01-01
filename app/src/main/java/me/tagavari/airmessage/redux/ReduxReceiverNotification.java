package me.tagavari.airmessage.redux;

import android.content.Context;
import android.util.Pair;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.activity.Messaging;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.enums.ConversationItemType;
import me.tagavari.airmessage.enums.MessageSendErrorCode;
import me.tagavari.airmessage.helper.LanguageHelper;
import me.tagavari.airmessage.helper.NotificationHelper;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.ConversationItem;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.messaging.TapbackInfo;
import me.tagavari.airmessage.util.ReplaceInsertResult;

//A receiver that handles creating notifications in response to new events
public final class ReduxReceiverNotification {
	private final Context context;
	private final CompositeDisposable compositeDisposable = new CompositeDisposable();
	
	public ReduxReceiverNotification(Context context) {
		this.context = context;
	}
	
	public void initialize() {
		compositeDisposable.add(ReduxEmitterNetwork.getMessageUpdateSubject().subscribe((event) -> {
			if(event instanceof ReduxEventMessaging.ConversationUpdate) {
				ReduxEventMessaging.ConversationUpdate conversationEvent = (ReduxEventMessaging.ConversationUpdate) event;
				
				//Loading a list of conversations in the foreground for later filtering
				List<Long> foregroundConversations = Messaging.getForegroundConversations();
				
				//Gathering updated conversations
				Stream.concat(
						conversationEvent.getTransferredConversations().stream()
								.map(transfer -> new Pair<ConversationInfo, Collection<ConversationItem>>(
										transfer.getClientConversation(),
										transfer.getServerConversationItems().stream().map(ReplaceInsertResult::getTargetItem).collect(Collectors.toList())
								)),
						conversationEvent.getNewConversations().entrySet().stream().map(entry -> new Pair<ConversationInfo, Collection<ConversationItem>>(entry.getKey(), entry.getValue()))
				).forEach(pair -> {
					//Skipping muted and foreground conversations
					if(pair.first.isMuted() || foregroundConversations.contains(pair.first.getLocalID())) return;
					
					//Sending notifications for message items
					for(ConversationItem conversationItem : pair.second) {
						if(conversationItem.getItemType() != ConversationItemType.message) continue;
						
						MessageInfo messageInfo = (MessageInfo) conversationItem;
						if(messageInfo.isOutgoing()) continue;
						
						NotificationHelper.sendNotification(context, pair.first, messageInfo);
					}
				});
			} else if(event instanceof ReduxEventMessaging.Message) {
				ReduxEventMessaging.Message messageEvent = (ReduxEventMessaging.Message) event;
				
				//Getting foreground conversations
				List<Long> foregroundConversations = Messaging.getForegroundConversations();
				
				//Sending notifications for received messages
				for(Pair<ConversationInfo, List<ReplaceInsertResult>> pair : messageEvent.getConversationItems()) {
					ConversationInfo conversationInfo = pair.first;
					
					//Ignoring if the conversation is muted or is loaded the foreground
					if(conversationInfo.isMuted() || foregroundConversations.contains(conversationInfo.getLocalID())) continue;
					
					for(ReplaceInsertResult result : pair.second) {
						//Getting the item
						ConversationItem conversationItem = result.getTargetItem();
						
						//Ignoring if the item is not a message
						if(conversationItem.getItemType() != ConversationItemType.message) continue;
						MessageInfo messageInfo = (MessageInfo) conversationItem;
						
						//Ignoring if the message is outgoing
						if(messageInfo.isOutgoing()) continue;
						
						//Sending the notification
						NotificationHelper.sendNotification(MainApplication.getInstance(), conversationInfo, messageInfo);
					}
				}
			} else if(event instanceof ReduxEventMessaging.MessageError) {
				ReduxEventMessaging.MessageError errorEvent = (ReduxEventMessaging.MessageError) event;
				if(errorEvent.getErrorCode() != MessageSendErrorCode.none) {
					NotificationHelper.sendErrorNotification(MainApplication.getInstance(), errorEvent.getConversationInfo());
				}
			} else if(event instanceof ReduxEventMessaging.TapbackUpdate) {
				ReduxEventMessaging.TapbackUpdate tapbackEvent = (ReduxEventMessaging.TapbackUpdate) event;
				
				Single.create((SingleEmitter<Pair<ConversationItem, ConversationInfo>> emitter) -> {
					Pair<ConversationItem, ConversationInfo> pair = DatabaseManager.getInstance().loadConversationItemWithChat(MainApplication.getInstance(), tapbackEvent.getMetadata().getMessageID());
					if(pair == null || pair.first.getItemType() != ConversationItemType.message) throw new Exception("Failed to load message ID " + tapbackEvent.getMetadata().getMessageID() + " for tapback");
					else emitter.onSuccess(pair);
				}).subscribeOn(Schedulers.single())
						.observeOn(AndroidSchedulers.mainThread())
						.onErrorComplete()
						.flatMapSingle(pair -> {
							//Getting the tapback summary
							TapbackInfo tapbackInfo = tapbackEvent.getTapbackInfo();
							String messageSummary = LanguageHelper.messageToString(MainApplication.getInstance().getResources(), (MessageInfo) pair.first);
							return LanguageHelper.getTapbackSummary(MainApplication.getInstance(), tapbackInfo.getSender(), tapbackInfo.getCode(), messageSummary)
									//Preserving the conversation info
									.map(summary -> new Pair<>(summary, pair.second));
						})
						.subscribe(pair -> {
							//Sending the notification
							NotificationHelper.sendNotification(MainApplication.getInstance(), pair.first, tapbackEvent.getTapbackInfo().getSender(), System.currentTimeMillis(), pair.second);
						});
			}
		}));
	}
	
	public void dispose() {
		compositeDisposable.clear();
	}
}