package me.tagavari.airmessage.connection.task;

import android.content.Context;
import android.content.Intent;
import android.util.LongSparseArray;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import me.tagavari.airmessage.activity.ConversationsBase;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.connection.request.ConversationInfoRequest;
import me.tagavari.airmessage.service.ConnectionService;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.ConversationItem;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.util.ConversationUtils;
import me.tagavari.airmessage.util.NotificationUtils;

public class SaveConversationInfoAsyncTask extends QueueTask<Void, Void> {
	private final WeakReference<Context> contextReference;
	private final List<ConversationInfo> unavailableConversations;
	private final List<ConversationInfoRequest> availableConversations;
	private final LongSparseArray<List<ConversationItem>> availableConversationItems = new LongSparseArray<>();
	private final HashMap<ConversationInfo, ConnectionManager.TransferConversationStruct> transferredConversations = new HashMap<>();
	
	public SaveConversationInfoAsyncTask(Context context, List<ConversationInfo> unavailableConversations, List<ConversationInfoRequest> availableConversations) {
		//Setting the references
		contextReference = new WeakReference<>(context);
		
		//Setting the lists
		this.unavailableConversations = unavailableConversations;
		this.availableConversations = availableConversations;
	}
	
	@Override
	public Void doInBackground() {
		//Getting the context
		Context context = contextReference.get();
		if(context == null) return null;
		
		//Removing the unavailable conversations from the database
		for(ConversationInfo conversation : unavailableConversations) DatabaseManager.getInstance().deleteConversation(conversation);
		
		//Checking if there are any available conversations
		if(!availableConversations.isEmpty()) {
			//Iterating over the conversations
			for(ListIterator<ConversationInfoRequest> iterator = availableConversations.listIterator(); iterator.hasNext();) {
				//Getting the conversation
				ConversationInfoRequest availableConversationRequest = iterator.next();
				ConversationInfo availableConversation = availableConversationRequest.getConversationInfo();
				
				//Reading and recording the conversation's items
				List<ConversationItem> conversationItems = DatabaseManager.getInstance().loadConversationItems(availableConversation);
				
				//Searching for a matching conversation in the database
				ConversationInfo clientConversation = DatabaseManager.getInstance().findConversationInfoWithMembers(context, availableConversation.getConversationMembersAsCollection(), availableConversation.getServiceHandler(), availableConversation.getService(), true);
				
				//Checking if a client conversation has not been found (the conversation is a new conversation from the server)
				if(clientConversation == null) {
					//Recording the available conversation items
					availableConversationItems.put(availableConversation.getLocalID(), conversationItems);
					
					//Updating the available conversation
					DatabaseManager.getInstance().updateConversationInfo(availableConversation, true);
				} else { //The newly fetched server conversation is merged into the client conversation (the one that originally resided on the device with no link data)
					//Switching the conversation item ownership to the new client conversation
					DatabaseManager.getInstance().switchMessageOwnership(availableConversation, clientConversation);
					//for(ConversationItem item : conversationItems) item.setConversationInfo(clientConversation); //Doesn't work, because the client conversation info isn't actually the one in shared memory
					
					//Recording the conversation details
					transferredConversations.put(clientConversation, new ConnectionManager.TransferConversationStruct(availableConversation.getGuid(),
							ConversationInfo.ConversationState.READY,
							availableConversation.getStaticTitle(),
							conversationItems));
					
					//Deleting the available conversation
					DatabaseManager.getInstance().deleteConversation(availableConversation);
					unavailableConversations.add(availableConversation);
					
					//Updating the client conversation
					DatabaseManager.getInstance().copyConversationInfo(availableConversation, clientConversation, false);
					
					//Removing the available conversation from the list (as it has now been merged into the original conversation)
					iterator.remove();
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
		
		//Sorting the available conversations
		Collections.sort(availableConversations, (request1, request2) -> {
			long date1 = request1.getConversationInfo().getLastItem() == null ? Long.MIN_VALUE : request1.getConversationInfo().getLastItem().getDate();
			long date2 = request2.getConversationInfo().getLastItem() == null ? Long.MIN_VALUE : request2.getConversationInfo().getLastItem().getDate();
			return Long.compare(date1, date2);
		});
		
		//Sending notifications
		if(context != null)
			for(ConversationInfoRequest conversationInfoRequest : availableConversations)
				if(conversationInfoRequest.isSendNotifications())
					for(ConversationItem conversationItem : availableConversationItems.get(conversationInfoRequest.getConversationInfo().getLocalID()))
						if(conversationItem instanceof MessageInfo)
							NotificationUtils.sendNotification(context, (MessageInfo) conversationItem);
		
		//Checking if the conversations are available in memory
		ArrayList<ConversationInfo> conversations = ConversationUtils.getConversations();
		if(conversations != null) {
			//Removing the unavailable conversations from memory
			for(ConversationInfo unavailableConversation : unavailableConversations) {
				for(Iterator<ConversationInfo> iterator = conversations.iterator(); iterator.hasNext(); ) {
					if(unavailableConversation.getGuid().equals(iterator.next().getGuid())) {
						iterator.remove();
						break;
					}
				}
			}
			
			//Iterating over the available conversations
			List<ConversationInfo> availableConversationInfoList = new ArrayList<>();
			for(ConversationInfoRequest conversationInfoRequest : availableConversations) {
				//Adding the available conversations in memory
				ConversationUtils.addConversation(conversationInfoRequest.getConversationInfo());
				
				//Adding the unread messages
				conversationInfoRequest.getConversationInfo().setUnreadMessageCount(ConnectionManager.countUnreadMessages(availableConversationItems.get(conversationInfoRequest.getConversationInfo().getLocalID())));
				conversationInfoRequest.getConversationInfo().updateUnreadStatus(context);
				//availableConversation.updateView(ConnectionService.this);
				
				//Mapping the items
				availableConversationInfoList.add(conversationInfoRequest.getConversationInfo());
			}
			
			//Updating shortcuts
			ConversationUtils.updateShortcuts(context, availableConversationInfoList);
			ConversationUtils.enableShortcuts(context, availableConversationInfoList);
			
			//Updating the transferred conversations
			if(context != null) {
				for(Map.Entry<ConversationInfo, ConnectionManager.TransferConversationStruct> pair : transferredConversations.entrySet()) {
					//Retrieving the pair values
					ConversationInfo conversationInfo = ConversationUtils.findConversationInfo(pair.getKey().getLocalID());
					if(conversationInfo == null) continue;
					
					//Updating the conversation details
					ConnectionManager.TransferConversationStruct transferData = pair.getValue();
					conversationInfo.setGuid(transferData.getGuid());
					conversationInfo.setState(transferData.getState());
					conversationInfo.setTitle(context, transferData.getName());
					if(conversationInfo.isDataAvailable()) {
						for(ConversationItem item : transferData.getConversationItems()) item.setConversationInfo(conversationInfo);
						conversationInfo.addConversationItems(context, transferData.getConversationItems());
					}
					
					//Adding the unread messages
					if(!transferData.getConversationItems().isEmpty()) {
						conversationInfo.setUnreadMessageCount(conversationInfo.getUnreadMessageCount() + ConnectionManager.countUnreadMessages(transferData.getConversationItems()));
						conversationInfo.updateUnreadStatus(context);
						conversationInfo.trySetLastItemUpdate(context, transferData.getConversationItems().get(transferData.getConversationItems().size() - 1), false);
					}
					
					//Sending notifications
					for(ConversationItem conversationItem : transferData.getConversationItems())
						if(conversationItem instanceof MessageInfo)
							NotificationUtils.sendNotification(context, (MessageInfo) conversationItem);
				}
			}
		}
		
		//Updating the conversation activity list
		if(context != null) LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ConversationsBase.localBCConversationUpdate));
	}
}
