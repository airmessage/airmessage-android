package me.tagavari.airmessage.connection.task;

import android.content.Context;
import androidx.annotation.Nullable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.CheckReturnValue;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kotlin.Pair;
import me.tagavari.airmessage.common.Blocks;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.enums.ConversationItemType;
import me.tagavari.airmessage.enums.ConversationState;
import me.tagavari.airmessage.enums.GroupAction;
import me.tagavari.airmessage.helper.ConversationColorHelper;
import me.tagavari.airmessage.helper.ConversationHelper;
import me.tagavari.airmessage.messaging.*;
import me.tagavari.airmessage.redux.ReduxEventMessaging;
import me.tagavari.airmessage.util.ConversationValueUpdateResult;
import me.tagavari.airmessage.util.ReplaceInsertResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MessageUpdateTask {
	/**
	 * Updates the status of messages in the database
	 * @param context The context to use
	 * @param foregroundConversationIDs A collection of the IDs of conversations that are currently in the foreground
	 * @param conversationItems The new conversation items
	 * @param collectAttachments Whether to return all found attachments in a list
	 */
	@CheckReturnValue
	public static Single<Response> create(Context context, Collection<Long> foregroundConversationIDs, Collection<Blocks.ConversationItem> conversationItems, boolean collectAttachments) {
		return Single.fromCallable(() -> {
			//Creating the collector lists
			List<ReduxEventMessaging> events = new ArrayList<>();
			List<Pair<ConversationInfo, List<ReplaceInsertResult>>> updatedCompleteConversations = new ArrayList<>();
			List<ConversationInfo> incompleteServerConversations = new ArrayList<>();
			List<Pair<MessageInfo, AttachmentInfo>> collectedAttachments = collectAttachments ? new ArrayList<>() : null;
			
			//Grouping the messages by conversation and iterating
			for(Map.Entry<String, List<Blocks.ConversationItem>> entry : conversationItems.stream().collect(Collectors.groupingBy(item -> item.chatGuid)).entrySet()) {
				//Retrieving / creating the conversation from the database
				ConversationInfo conversationInfo = DatabaseManager.getInstance().addRetrieveServerCreatedConversationInfo(context, entry.getKey());
				if(conversationInfo == null) continue;
				
				int newIncomingMessageCount = 0;
				List<ReplaceInsertResult> newItems = conversationInfo.getState() == ConversationState.ready ? new ArrayList<>(entry.getValue().size()) : null;
				for(Blocks.ConversationItem conversationItem : entry.getValue()) {
					//Adding the conversation item to the database
					ReplaceInsertResult replaceInsertResult = DatabaseManager.getInstance().mergeOrWriteConversationItem(context, conversationInfo.getLocalID(), conversationItem, false);
					if(replaceInsertResult == null) continue;
					
					ConversationItem targetItem = replaceInsertResult.getTargetItem();
					
					if(conversationInfo.getState() == ConversationState.ready) {
						//Adding the conversation item to the complete list
						newItems.add(replaceInsertResult);
						
						//Checking the conversation item's influence
						if(targetItem.getItemType() == ConversationItemType.member) {
							//Converting the item to a group action info
							ChatMemberAction groupActionInfo = (ChatMemberAction) targetItem;
							
							//Adding or removing the member on disk
							if(groupActionInfo.getOther() != null) {
								//Creating the member
								int otherColor = ConversationColorHelper.getNextUserColor(conversationInfo);
								MemberInfo otherMember = new MemberInfo(groupActionInfo.getOther(), otherColor);
								
								if(groupActionInfo.getActionType() == GroupAction.join) {
									DatabaseManager.getInstance().addConversationMember(conversationInfo.getLocalID(), groupActionInfo.getOther(), otherMember.getColor());
									events.add(new ReduxEventMessaging.ConversationMember(conversationInfo, otherMember, true));
								} else if(groupActionInfo.getActionType() == GroupAction.leave) {
									DatabaseManager.getInstance().removeConversationMember(conversationInfo.getLocalID(), groupActionInfo.getOther());
									events.add(new ReduxEventMessaging.ConversationMember(conversationInfo, otherMember, false));
								}
							}
						} else if(targetItem.getItemType() == ConversationItemType.chatRename) {
							String title = ((ChatRenameAction) targetItem).getTitle();
							
							//Writing the new title to the database
							DatabaseManager.getInstance().updateConversationTitle(conversationInfo.getLocalID(), title);
							
							//Adding the event
							events.add(new ReduxEventMessaging.ConversationTitle(conversationInfo, title));
						}
					}
					
					//Counting the new incoming message count
					if(targetItem.getItemType() == ConversationItemType.message && !((MessageInfo) targetItem).isOutgoing()) {
						newIncomingMessageCount++;
					}
					
					if(collectAttachments && targetItem.getItemType() == ConversationItemType.message) {
						//Adding attachments
						MessageInfo messageInfo = (MessageInfo) targetItem;
						collectedAttachments.addAll(messageInfo.getAttachments().stream().map(attachment -> new Pair<>(messageInfo, attachment)).collect(Collectors.toList()));
					}
				}
				
				//Updating the conversation values
				ConversationValueUpdateResult updateResult = ConversationHelper.updateConversationValues(foregroundConversationIDs, conversationInfo, newIncomingMessageCount);
				events.addAll(updateResult.getEvents(conversationInfo));
				
				//Adding the results
				if(conversationInfo.getState() == ConversationState.ready) {
					updatedCompleteConversations.add(new Pair<>(conversationInfo, newItems));
				} else {
					incompleteServerConversations.add(conversationInfo);
				}
			}
			
			//Adding the message update event
			events.add(new ReduxEventMessaging.Message(updatedCompleteConversations));
			
			//Finishing
			return new Response(events, incompleteServerConversations, collectedAttachments);
		}).subscribeOn(Schedulers.single()).observeOn(AndroidSchedulers.mainThread());
	}
	
	public static class Response {
		//Events to be emitted
		private final List<ReduxEventMessaging> events;
		
		//Conversations found from incoming messages that aren't available
		private final List<ConversationInfo> incompleteServerConversations;
		
		//A list of all attachments passed to this task
		private final List<Pair<MessageInfo, AttachmentInfo>> collectedAttachments;
		
		public Response(List<ReduxEventMessaging> events, List<ConversationInfo> incompleteServerConversations, @Nullable List<Pair<MessageInfo, AttachmentInfo>> collectedAttachments) {
			this.events = events;
			this.incompleteServerConversations = incompleteServerConversations;
			this.collectedAttachments = collectedAttachments;
		}
		
		public List<ReduxEventMessaging> getEvents() {
			return events;
		}
		
		public List<ConversationInfo> getIncompleteServerConversations() {
			return incompleteServerConversations;
		}
		
		public List<Pair<MessageInfo, AttachmentInfo>> getCollectedAttachments() {
			return collectedAttachments;
		}
	}
}