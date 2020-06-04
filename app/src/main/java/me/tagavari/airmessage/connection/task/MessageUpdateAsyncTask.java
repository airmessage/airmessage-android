package me.tagavari.airmessage.connection.task;

import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.LongSparseArray;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.tagavari.airmessage.R;
import me.tagavari.airmessage.activity.ConversationsBase;
import me.tagavari.airmessage.activity.Messaging;
import me.tagavari.airmessage.common.Blocks;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.connection.request.ConversationInfoRequest;
import me.tagavari.airmessage.service.ConnectionService;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.messaging.AttachmentInfo;
import me.tagavari.airmessage.messaging.ChatRenameActionInfo;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.ConversationItem;
import me.tagavari.airmessage.messaging.GroupActionInfo;
import me.tagavari.airmessage.messaging.MemberInfo;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.util.Constants;
import me.tagavari.airmessage.util.ConversationUtils;
import me.tagavari.airmessage.util.NotificationUtils;

public class MessageUpdateAsyncTask extends QueueTask<Void, Void> {
	//Creating the reference values
	private final WeakReference<ConnectionManager> managerReference;
	private final WeakReference<Context> contextReference;
	
	//Creating the request values
	private final List<Blocks.ConversationItem> structConversationItems;
	private final boolean sendNotifications;
	
	//Creating the conversation lists
	private final List<ConversationItem> newCompleteConversationItems = new ArrayList<>();
	private final List<ConversationInfo> completeConversations = new ArrayList<>();
	
	//Creating the caches
	private List<Long> foregroundConversationsCache;
	
	public MessageUpdateAsyncTask(ConnectionManager managerInstance, Context context, List<Blocks.ConversationItem> structConversationItems, boolean sendNotifications) {
		//Setting the references
		managerReference = new WeakReference<>(managerInstance);
		contextReference = new WeakReference<>(context);
		
		//Setting the values
		this.structConversationItems = structConversationItems;
		this.sendNotifications = sendNotifications;
		
		//Getting the caches
		foregroundConversationsCache = Messaging.getForegroundConversations();
	}
	
	@Override
	public Void doInBackground() {
		//Getting the context
		Context context = contextReference.get();
		if(context == null) return null;
		
		//Iterating over the conversations from the received messages
		//Collections.sort(structConversationItems, (value1, value2) -> Long.compare(value1.date, value2.date));
		List<String> processedConversations = new ArrayList<>(); //Conversations that have been marked for updating (with new messages)
		List<ConversationInfo> incompleteServerConversations = new ArrayList<>();
		for(Blocks.ConversationItem conversationItemStruct : structConversationItems) {
			//Cleaning the conversation item
			ConnectionManager.cleanConversationItem(conversationItemStruct);
			
			//Creating the parent conversation variable
			ConversationInfo parentConversation = null;
			
			//Checking if the message is contained in a pending conversation
			{
				ConnectionManager connectionManager = ConnectionService.getConnectionManager();
				if(connectionManager != null) {
					synchronized(connectionManager.getPendingConversations()) {
						for(ConversationInfoRequest request : connectionManager.getPendingConversations())
							if(conversationItemStruct.chatGuid.equals(request.getConversationInfo().getGuid())) {
								parentConversation = request.getConversationInfo();
								break;
							}
					}
				}
			}
			
			//Otherwise checking if the conversation is contained in a current conversation
			if(parentConversation == null)
				for(ConversationInfo conversationInfo : completeConversations)
					if(conversationItemStruct.chatGuid.equals(conversationInfo.getGuid())) {
						parentConversation = conversationInfo;
						break;
					}
			if(parentConversation == null)
				for(ConversationInfo conversationInfo : incompleteServerConversations)
					if(conversationItemStruct.chatGuid.equals(conversationInfo.getGuid())) {
						parentConversation = conversationInfo;
						break;
					}
			
			//Otherwise retrieving / creating the conversation from the database
			if(parentConversation == null) {
				parentConversation = DatabaseManager.getInstance().addRetrieveServerCreatedConversationInfo(context, conversationItemStruct.chatGuid);
				//Skipping the remainder of the iteration if the conversation is still invalid (a database error occurred)
				if(parentConversation == null) continue;
			}
			
			//Checking if the conversation hasn't yet been processed
			if(!processedConversations.contains(conversationItemStruct.chatGuid)) {
				//Marking the conversation as processed
				processedConversations.add(conversationItemStruct.chatGuid);
				
				//Fetching the conversation info
				//ConversationInfo conversationInfo = DatabaseManager.fetchConversationInfo(ConnectionService.this, writableDatabase, conversationItemStruct.chatGuid);
				//if(conversationInfo == null) continue;
				
				//Sorting the conversation
				if(parentConversation.getState() == ConversationInfo.ConversationState.READY) {
					completeConversations.add(parentConversation);
					
					//Unarchiving the conversation if it is archived
					if(parentConversation.isArchived()) DatabaseManager.getInstance().updateConversationArchived(parentConversation.getLocalID(), false);
				}
				else if(parentConversation.getState() == ConversationInfo.ConversationState.INCOMPLETE_SERVER) incompleteServerConversations.add(parentConversation);
			}
			
			//Adding the conversation item to the database
			ConversationItem conversationItem = DatabaseManager.getInstance().addConversationItemReplaceGhost(context, conversationItemStruct, parentConversation);
			
			//Skipping the remainder of the iteration if the conversation item is invalid
			if(conversationItem == null) continue;
			
			//Checking the conversation item's influence
			if(conversationItem instanceof GroupActionInfo) {
				//Converting the item to a group action info
				GroupActionInfo groupActionInfo = (GroupActionInfo) conversationItem;
				
				//Adding or removing the member on disk
				if(groupActionInfo.getOther() != null) {
					if(groupActionInfo.getActionType() == Constants.groupActionJoin) DatabaseManager.getInstance().addConversationMember(parentConversation.getLocalID(), groupActionInfo.getOther(), groupActionInfo.color = parentConversation.getNextUserColor());
					else if(groupActionInfo.getActionType() == Constants.groupActionLeave) DatabaseManager.getInstance().removeConversationMember(parentConversation.getLocalID(), groupActionInfo.getOther());
				}
			} else if(conversationItem instanceof ChatRenameActionInfo) {
				//Writing the new title to the database
				DatabaseManager.getInstance().updateConversationTitle(parentConversation.getLocalID(), ((ChatRenameActionInfo) conversationItem).getTitle());
			}
			
			//Checking if the conversation is complete
			if(parentConversation.getState() == ConversationInfo.ConversationState.READY) {
				//Recording the conversation item
				newCompleteConversationItems.add(conversationItem);
				
				//Incrementing the unread count
				if(!foregroundConversationsCache.contains(parentConversation.getLocalID()) && (conversationItem instanceof MessageInfo && !((MessageInfo) conversationItem).isOutgoing())) DatabaseManager.getInstance().incrementUnreadMessageCount(parentConversation.getLocalID());
			}
			//Otherwise updating the last conversation item
			else parentConversation.trySetLastItem(conversationItem.toLightConversationItemSync(context), false);
		}
		
		{
			ConnectionManager connectionManager = ConnectionService.getConnectionManager();
			if(connectionManager != null) {
				//Checking if there are incomplete conversations
				if(!incompleteServerConversations.isEmpty()) {
					//Adding the incomplete conversations to the pending conversations
					synchronized(connectionManager.getPendingConversations()) {
						for(ConversationInfo conversation : incompleteServerConversations) connectionManager.getPendingConversations().add(new ConversationInfoRequest(conversation, sendNotifications));
					}
				}
			}
		}
		
		//Returning
		return null;
	}
	
	@Override
	public void onPostExecute(Void result) {
		//Getting the context
		Context context = contextReference.get();
		if(context == null) return;
		
		//Sorting the complete conversation items
		Collections.sort(newCompleteConversationItems, ConversationUtils.conversationItemComparator);
		
		//Getting the loaded conversations
		//List<Long> foregroundConversations = Messaging.getForegroundConversations();
		//List<Long> loadedConversations = Messaging.getActivityLoadedConversations();
		
		//Checking if the conversations are loaded in memory
		ArrayList<ConversationInfo> conversations = ConversationUtils.getConversations();
		if(conversations != null) {
			//Sorting the conversation items by conversation
			LongSparseArray<List<ConversationItem>> newCompleteConversationGroups = new LongSparseArray<>();
			for(ConversationItem conversationItem : newCompleteConversationItems) {
				List<ConversationItem> list = newCompleteConversationGroups.get(conversationItem.getConversationInfo().getLocalID());
				if(list == null) {
					list = new ArrayList<>();
					list.add(conversationItem);
					newCompleteConversationGroups.put(conversationItem.getConversationInfo().getLocalID(), list);
				} else list.add(conversationItem);
			}
			
			//Iterating over the conversation groups
			for(int i = 0; i < newCompleteConversationGroups.size(); i++) {
				//Attempting to find the associated parent conversation
				ConversationInfo parentConversation = ConversationUtils.findConversationInfo(newCompleteConversationGroups.keyAt(i));
				
				//Skipping the remainder of the iteration if no parent conversation could be found
				if(parentConversation == null) continue;
				
				//Getting the conversation items
				List<ConversationItem> conversationItems = newCompleteConversationGroups.valueAt(i);
				
				//Adding the conversation items if the conversation is loaded
				//if(loadedConversations.contains(parentConversation.getLocalID()))
				
				//Add items no matter if the conversation is loaded (will simply check if conversation items are still available in memory)
				{
					boolean addItemResult = parentConversation.addConversationItems(context, conversationItems);
					//Setting the last item if the conversation items couldn't be added
					if(!addItemResult) parentConversation.trySetLastItemUpdate(context, conversationItems.get(conversationItems.size() - 1), false);
				}
				
				//Iterating over the conversation items
				for(ConversationItem conversationItem : conversationItems) {
					//Setting the conversation item's parent conversation to the found one (the one provided from the DB is not the same as the one in memory)
					conversationItem.setConversationInfo(parentConversation);
					
					//if(parentConversation.getState() != ConversationInfo.ConversationState.READY) continue;
					
					//Incrementing the conversation's unread count
					if(conversationItem instanceof MessageInfo && !((MessageInfo) conversationItem).isOutgoing()) {
						parentConversation.setUnreadMessageCount(parentConversation.getUnreadMessageCount() + 1);
						parentConversation.updateUnreadStatus(context);
					}
					
					//Renaming the conversation
					if(conversationItem instanceof ChatRenameActionInfo) parentConversation.setTitle(context, ((ChatRenameActionInfo) conversationItem).getTitle());
					else if(conversationItem instanceof GroupActionInfo) {
						//Converting the item to a group action info
						GroupActionInfo groupActionInfo = (GroupActionInfo) conversationItem;
						
						//Finding the conversation member
						MemberInfo member = parentConversation.findConversationMember(groupActionInfo.getOther());
						
						if(groupActionInfo.getActionType() == Constants.groupActionJoin) {
							//Adding the member in memory
							if(member == null) {
								member = new MemberInfo(groupActionInfo.getOther(), groupActionInfo.color);
								parentConversation.addConversationMember(member);
							}
						} else if(groupActionInfo.getActionType() == Constants.groupActionLeave) {
							//Removing the member in memory
							if(member != null && parentConversation.getConversationMembers().contains(member)) parentConversation.removeConversationMember(member);
						}
					}
				}
			}
		}
		
		for(ConversationItem conversationItem : newCompleteConversationItems) {
			//Sending notifications
			if(conversationItem instanceof MessageInfo) {
				MessageInfo messageInfo = (MessageInfo) conversationItem;
				if(messageInfo.isOutgoing()) {
					if(messageInfo.getErrorCode() != Constants.messageErrorCodeOK) NotificationUtils.sendErrorNotification(context, messageInfo.getConversationInfo());
				} else {
					NotificationUtils.sendNotification(context, messageInfo);
				}
			}
			
			//Downloading the items automatically (if requested)
			if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preference_storage_autodownload_key), false) && conversationItem instanceof MessageInfo) {
				for(AttachmentInfo attachmentInfo : ((MessageInfo) conversationItem).getAttachments())
					attachmentInfo.downloadContent(context);
			}
		}
		
		//Updating modified conversations
		for(ConversationInfo conversationInfo : completeConversations) {
			conversationInfo = ConversationUtils.findConversationInfo(conversationInfo.getLocalID());
			if(conversationInfo != null) {
				//Re-sorting the modified conversations
				ConversationUtils.sortConversation(conversationInfo);
				
				//Unarchiving the conversation
				if(conversationInfo.isArchived()) conversationInfo.setArchived(false);
			}
		}
		
		//Updating the conversation activity list
		LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ConversationsBase.localBCConversationUpdate));
		/* for(Conversations.ConversationsCallbacks callbacks : MainApplication.getConversationsActivityCallbacks())
			callbacks.updateList(true); */
		
		//Contacting the server for the pending conversations
		ConnectionManager connectionManager = ConnectionService.getConnectionManager();
		if(connectionManager != null && !connectionManager.getPendingConversations().isEmpty()) connectionManager.retrievePendingConversationInfo();
	}
}
