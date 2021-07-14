package me.tagavari.airmessage.connection.task;

import android.content.Context;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.reactivex.rxjava3.schedulers.Schedulers;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.ConversationItem;
import me.tagavari.airmessage.messaging.MemberInfo;
import me.tagavari.airmessage.util.ReplaceInsertResult;
import me.tagavari.airmessage.util.TransferredConversation;

import java.util.*;
import java.util.stream.Collectors;

public class ChatResponseTask {
	/**
	 * Asynchronously processes a chat update
	 * @param context The context to use
	 * @param matchedConversationList A list of conversations that have been matched by the server
	 * @param unmatchedConversationList A list of conversations that have been rejected by the server
	 * @return A completable representing this task
	 */
	public static Single<Response> create(Context context, List<ConversationInfo> matchedConversationList, List<ConversationInfo> unmatchedConversationList) {
		return Single.create((SingleEmitter<Response> emitter) -> {
			//Removing the unmatched conversations from the database
			for(ConversationInfo conversation : unmatchedConversationList) {
				DatabaseManager.getInstance().deleteConversation(context, conversation.getLocalID());
			}
			
			//New conversation items available from this request
			Map<ConversationInfo, List<ConversationItem>> availableConversationItems = new HashMap<>();
			
			//Conversations that were transferred from server-created to client-created
			Collection<TransferredConversation> transferredConversations = new ArrayList<>();
			
			for(ConversationInfo serverConversation : matchedConversationList) {
				//Searching for a matching client-created conversation in the database
				ConversationInfo clientConversation = DatabaseManager.getInstance().findConversationInfoWithMembers(context, serverConversation.getMembers().stream().map(MemberInfo::getAddress).collect(Collectors.toList()), serverConversation.getServiceHandler(), serverConversation.getServiceType(), true);
				
				//Checking if a client conversation was found (the conversation was created on the client and is now being fulfilled)
				if(clientConversation != null) {
					//Switching the conversation item ownership to the new client conversation
					List<ReplaceInsertResult> replaceInsertResults = DatabaseManager.getInstance().switchMessageOwnership(context, serverConversation.getLocalID(), clientConversation.getLocalID());
					
					//Recording the conversation details
					transferredConversations.add(new TransferredConversation(serverConversation, replaceInsertResults, clientConversation));
					
					//Deleting the server-created conversation
					DatabaseManager.getInstance().deleteConversation(context, serverConversation.getLocalID());
					
					//Updating the client conversation
					DatabaseManager.getInstance().copyConversationInfo(serverConversation, clientConversation, false);
				} else { //This conversation was created from a message from the server, now complete and save it
					//Reading and recording the conversation's items
					List<ConversationItem> conversationItems = DatabaseManager.getInstance().loadConversationItems(context, serverConversation.getLocalID());
					
					//Recording the available conversation items
					availableConversationItems.put(serverConversation, conversationItems);
					
					//Updating the available conversation (to set it as available)
					DatabaseManager.getInstance().updateConversationInfo(serverConversation, true);
				}
			}
			
			emitter.onSuccess(new Response(availableConversationItems, transferredConversations));
		}).subscribeOn(Schedulers.single()).observeOn(AndroidSchedulers.mainThread());
	}
	
	public static class Response {
		private final Map<ConversationInfo, List<ConversationItem>> availableConversationItems;
		private final Collection<TransferredConversation> transferredConversations;
		
		/**
		 * Constructs a new chat response task response
		 * @param availableConversationItems A map of conversation IDs to conversation items for server-created conversations (that didn't need transfers)
		 * @param transferredConversations A list of transferred conversation details
		 */
		private Response(Map<ConversationInfo, List<ConversationItem>> availableConversationItems, Collection<TransferredConversation> transferredConversations) {
			this.availableConversationItems = availableConversationItems;
			this.transferredConversations = transferredConversations;
		}
		
		public Map<ConversationInfo, List<ConversationItem>> getAvailableConversationItems() {
			return availableConversationItems;
		}
		
		public Collection<TransferredConversation> getTransferredConversations() {
			return transferredConversations;
		}
	}
}