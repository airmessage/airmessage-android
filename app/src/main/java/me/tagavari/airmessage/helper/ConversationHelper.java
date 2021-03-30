package me.tagavari.airmessage.helper;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import io.reactivex.rxjava3.core.Completable;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.ConversationItem;
import me.tagavari.airmessage.messaging.ConversationPreview;
import me.tagavari.airmessage.util.ConversationValueUpdateResult;

public class ConversationHelper {
	//Sorts conversations in descending order by their preview date
	public static final Comparator<ConversationInfo> conversationComparator = (conversation1, conversation2) -> {
		//Getting the last conversation item times
		ConversationPreview dynamicPreview1 = conversation1.getDynamicPreview();
		ConversationPreview dynamicPreview2 = conversation2.getDynamicPreview();
		
		long lastTime1 = dynamicPreview1 == null ? Long.MIN_VALUE : dynamicPreview1.getDate();
		long lastTime2 = dynamicPreview2 == null ? Long.MIN_VALUE : dynamicPreview2.getDate();
		
		//Returning the comparison
		return Long.compare(lastTime2, lastTime1);
	};
	
	public static final Comparator<ConversationItem> conversationItemComparator = (item1, item2) -> {
		//Returning the comparison
		if(item1.getServerID() != -1 && item2.getServerID() != -1) return Long.compare(item1.getServerID(), item2.getServerID());
		else if(item1.getLocalID() != -1 && item2.getLocalID() != -1) return Long.compare(item1.getLocalID(), item2.getLocalID());
		else if(item1.getLocalID() == -1 && item2.getLocalID() == -1) return Long.compare(item1.getDate(), item2.getDate());
		else if(item1.getLocalID() == -1) return 1;
		else return -1; //Item 2's local ID is -1
	};
	
	/**
	 * Finds the index to insert the conversation into a list based on its preview, in descending chronological order
	 * @param conversationInfo The conversation to check
	 * @param list The list of conversations to compare against
	 * @return The index to insert the conversation at to retain the list in chronological order
	 */
	public static int findInsertionIndex(ConversationInfo conversationInfo, List<ConversationInfo> list) {
		//Getting the conversation's dynamic preview
		ConversationPreview dynamicPreview = conversationInfo.getDynamicPreview();
		
		//If the conversation has no dynamic preview, add the item to the end of the list
		if(dynamicPreview == null) {
			return list.size();
		}
		
		//Finding the correct insertion position for this conversation
		for(ListIterator<ConversationInfo> iterator = list.listIterator(); iterator.hasNext();) {
			int i = iterator.nextIndex();
			ConversationInfo listedConversation = iterator.next();
			ConversationPreview listedPreview = listedConversation.getDynamicPreview();
			
			if(listedPreview != null && listedPreview.getDate() < dynamicPreview.getDate()) {
				return i;
			}
		}
		
		//No matches; add this conversation to the end of the list
		return list.size();
	}
	
	/**
	 * Finds the index to re-insert the conversation into a list after its removal based on its preview, in descending chronological order
	 * @param conversationInfo The conversation to check
	 * @param list The list of conversations to compare against
	 * @return The index to insert the conversation at to retain the list in chronological order
	 */
	public static int findReinsertionIndex(ConversationInfo conversationInfo, List<ConversationInfo> list) {
		//Getting the conversation's dynamic preview
		ConversationPreview dynamicPreview = conversationInfo.getDynamicPreview();
		
		//If the conversation has no dynamic preview, add the item to the end of the list
		if(dynamicPreview == null) {
			return list.size() - 1;
		}
		
		//Finding the correct insertion position for this conversation
		boolean pastOriginalIndex = false;
		for(ListIterator<ConversationInfo> iterator = list.listIterator(); iterator.hasNext();) {
			int i = iterator.nextIndex();
			ConversationInfo listedConversation = iterator.next();
			
			//If we hit the original item, mark it down and continue to the next item
			if(conversationInfo.getLocalID() == listedConversation.getLocalID()) {
				pastOriginalIndex = true;
				continue;
			}
			
			ConversationPreview listedPreview = listedConversation.getDynamicPreview();
			
			if(listedPreview != null && listedPreview.getDate() < dynamicPreview.getDate()) {
				//If we're past the point where this conversation will be removed, compensate by going one index down
				if(pastOriginalIndex) {
					return i - 1;
				} else {
					return i;
				}
			}
		}
		
		//No matches; add this conversation to the end of the list
		return list.size() - 1;
	}
	
	/**
	 * Maps a collection of conversations to an array of their IDs
	 */
	public static long[] conversationsToIDArray(Collection<ConversationInfo> conversations) {
		return conversations.stream().mapToLong(ConversationInfo::getLocalID).toArray();
	}
	
	/**
	 * Updates the state of a conversation in response to a message update
	 * @param foregroundConversationIDs A list of the IDs of conversations currently loaded in the foreground
	 * @param conversationInfo The conversation to update
	 * @param incomingMessageCount The amount of incoming messages
	 */
	public static ConversationValueUpdateResult updateConversationValues(Collection<Long> foregroundConversationIDs, ConversationInfo conversationInfo, int incomingMessageCount) {
		//Unarchiving the conversation if it is archived
		if(conversationInfo.isArchived()) DatabaseManager.getInstance().updateConversationArchived(conversationInfo.getLocalID(), false);
		
		//Incrementing the unread count if the conversation is not part of a conversation that is currently in the foreground, and we have been requested to do so
		boolean updateUnread = incomingMessageCount > 0 && !foregroundConversationIDs.contains(conversationInfo.getLocalID());
		if(updateUnread) {
			DatabaseManager.getInstance().incrementUnreadMessageCount(conversationInfo.getLocalID());
		}
		
		return new ConversationValueUpdateResult(conversationInfo.isArchived(), updateUnread ? incomingMessageCount : 0);
	}
	
	/**
	 * Inserts a conversation into a list of conversations sorted by their preview date
	 * @param conversations The list of conversations to insert into
	 * @param conversationInfo The conversation to insert
	 * @return The index the conversation was inserted at
	 */
	public static int insertConversation(List<ConversationInfo> conversations, ConversationInfo conversationInfo) {
		//Adding the item if the list is empty
		if(conversations.isEmpty()) {
			conversations.add(conversationInfo);
			return conversations.size() - 1;
		}
		
		//Getting the conversation's preview
		ConversationPreview preview = conversationInfo.getDynamicPreview();
		
		//If the conversation has no preview, just add it to the end of the list
		if(preview == null) {
			conversations.add(conversationInfo);
			return conversations.size() - 1;
		}
		
		//Iterating over the conversation items
		for(int i = 0; i < conversations.size(); i++) {
			//Getting the conversation at the index
			ConversationInfo indexConversation = conversations.get(i);
			
			//Skipping the remainder of the iteration if the item is older
			ConversationPreview indexPreview = indexConversation.getDynamicPreview();
			if(indexPreview == null || preview.getDate() < indexPreview.getDate())
				continue;
			
			//Adding the item
			conversations.add(i, conversationInfo);
			
			//Returning
			return i;
		}
		
		//Placing the item at the bottom of the list
		conversations.add(conversationInfo);
		return conversations.size() - 1;
	}
}