package me.tagavari.airmessage.util;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Resources;
import android.database.sqlite.SQLiteException;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.provider.Telephony;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.collection.LongSparseArray;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.activity.Conversations;
import me.tagavari.airmessage.activity.Messaging;
import me.tagavari.airmessage.messaging.AttachmentInfo;
import me.tagavari.airmessage.messaging.AudioAttachmentInfo;
import me.tagavari.airmessage.messaging.ContactAttachmentInfo;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.ConversationItem;
import me.tagavari.airmessage.messaging.ImageAttachmentInfo;
import me.tagavari.airmessage.messaging.LightConversationItem;
import me.tagavari.airmessage.messaging.MemberInfo;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.messaging.OtherAttachmentInfo;
import me.tagavari.airmessage.messaging.VLocationAttachmentInfo;
import me.tagavari.airmessage.messaging.VideoAttachmentInfo;

public class ConversationUtils {
	//Message burst - Sending single messages one after the other
	public static final long conversationBurstTimeMillis = 30 * 1000; //30 seconds
	//Message session - A conversation session, where conversation participants are active
	public static final long conversationSessionTimeMillis = 5 * 60 * 1000; //5 minutes
	//Just now - A message sent just now
	public static final long conversationJustNowTimeMillis = 60 * 1000; //1 minute
	
	public static final Comparator<ConversationInfo> conversationComparator = (conversation1, conversation2) -> {
		//Getting the last conversation item times
		long lastTime1 = conversation1.getLastItem() == null ? Long.MIN_VALUE : conversation1.getLastItem().getDate();
		long lastTime2 = conversation2.getLastItem() == null ? Long.MIN_VALUE : conversation2.getLastItem().getDate();
		
		//Returning the comparison
		return Long.compare(lastTime2, lastTime1);
	};
	public static final Comparator<ConversationItem> conversationItemComparator = ConversationUtils::compareConversationItems;
	public static final Comparator<MemberInfo> memberInfoComparator = (member1, member2) -> {
		//Returning 0 if either of the values are invalid
		if(member1 == null || member2 == null) return 0;
		
		//Returning the comparison (lexicographic comparison)
		return member1.getName().compareTo(member2.getName());
	};
	
	public static final int invisibleInkBlurRadius = 2;
	public static final int invisibleInkBlurSampling = 80;
	
	public static final int permissionRequestWriteStorageDownload = 0;
	
	private static final String shortcutPrefixConversation = "conversation-";
	
	//Creating the conversation list
	//private final ArrayList<ConversationInfo> conversations = new ArrayList<>();
	
	/* public ArrayList<ConversationInfo> getConversations() {
		return conversations;
	}
	ConversationInfo findConversationInfo(long conversationID) {
		//Looping through all of the conversations
		for(ConversationInfo conversation : conversations) {
			//Skipping the remainder of the iteration if the conversation identifiers do not match
			if(conversation.getLocalID() != conversationID) continue;
			
			//Returning the conversation
			return conversation;
		}
		
		//Returning null
		return null;
	}
	
	ConversationInfo findConversationInfo(String conversationGUID) {
		//Looping through all of the conversations
		for(ConversationInfo conversation : conversations) {
			//Skipping the remainder of the iteration if the conversation GUIDs do not match
			if(conversation.getGuid() == null || !conversation.getGuid().equals(conversationGUID)) continue;
			
			//Returning the conversation
			return conversation;
		}
		
		//Returning null
		return null;
	}
	
	ConversationInfo findConversationInfo(String service, ArrayList<String> members) {
		//Sorting the list
		members = (ArrayList<String>) members.clone();
		Collections.sort(members);
		
		//Looping through all of the conversations
		for(ConversationInfo conversation : conversations) {
			//Skipping the remainder of the iteration if the service does not match
			if(!conversation.getService().equals(service)) continue;
			
			//Getting the members
			ArrayList<String> conversationMembers = (ArrayList<String>) conversation.getConversationMembers().clone();
			Collections.sort(conversationMembers);
			
			//Skipping the remainder of the iteration if the members don't match
			if(!conversationMembers.equals(members)) continue;
			
			//Returning the conversation
			return conversation;
		}
		
		//Returning null
		return null;
	}
	
	AttachmentInfo findAttachmentInfoInActiveConversation(String message) {
		//Returning a matching attachment info
		for(long conversationLocalID : Messaging.getForegroundConversations())
			for(ConversationItem conversationItem : findConversationInfo(conversationLocalID).getConversationItems())
				if(conversationItem instanceof MessageInfo)
					for(AttachmentInfo attachmentInfo : ((MessageInfo) conversationItem).getAttachments())
						if(attachmentInfo.message != null && attachmentInfo.message.equals(message))
							return attachmentInfo;
		
		//Returning null
		return null;
	}
	
	AttachmentInfo findAttachmentInfoInActiveConversation(long localID) {
		//Returning a matching attachment info
		for(long conversationLocalID : Messaging.getForegroundConversations())
			for(ConversationItem conversationItem : findConversationInfo(conversationLocalID).getConversationItems())
				if(conversationItem instanceof MessageInfo)
					for(AttachmentInfo attachmentInfo : ((MessageInfo) conversationItem).getAttachments())
						if(attachmentInfo.localID == localID) return attachmentInfo;
		
		//Returning null
		return null;
	} */
	
	public static MainApplication.LoadFlagArrayList<ConversationInfo> getConversations() {
		MainApplication app = MainApplication.getInstance();
		if(app == null) return null;
		
		return app.getConversations();
	}
	
	public static void sortConversation(ConversationInfo conversation) {
		//Getting the conversations
		ArrayList<ConversationInfo> conversations = getConversations();
		if(conversations == null) return;
		
		//Removing the matching conversation
		Iterator<ConversationInfo> iterator = conversations.iterator();
		while(iterator.hasNext()) {
			if(iterator.next().getLocalID() == conversation.getLocalID()) {
				iterator.remove();
				break;
			}
		}
		
		//Re-inserting the conversation
		insertConversation(conversations, conversation);
	}
	
	public static void addConversation(ConversationInfo conversation) {
		//Getting the conversations
		ArrayList<ConversationInfo> conversations = getConversations();
		if(conversations == null) return;
		
		//Inserting the conversation into the list
		insertConversation(conversations, conversation);
	}
	
	private static void insertConversation(ArrayList<ConversationInfo> conversations, ConversationInfo conversationInfo) {
		//Adding the item if the list is empty or it has no last item
		if(conversations.isEmpty() || conversationInfo.getLastItem() == null) {
			conversations.add(conversationInfo);
			return;
		}
		
		//Iterating over the conversation items backwards (more recent items appear at the end of the list, and new items are more likely to be new than old)
		for(int i = 0; i < conversations.size(); i++) {
			//Getting the conversation at the index
			ConversationInfo indexConversation = conversations.get(i);
			
			//Skipping the remainder of the iteration if the item is older
			if(indexConversation.getLastItem() == null || conversationInfo.getLastItem().getDate() < indexConversation.getLastItem().getDate())
				continue;
			
			//Adding the item
			conversations.add(i, conversationInfo);
			
			//Returning
			return;
		}
		
		//Placing the item at the bottom of the list
		conversations.add(conversationInfo);
	}
	
	public static ConversationInfo findConversationInfo(long localID) {
		//Getting the conversations
		ArrayList<ConversationInfo> conversations = getConversations();
		if(conversations == null) return null;
		
		//Returning the conversation info
		for(ConversationInfo conversationInfo : conversations) {
			if(conversationInfo.getLocalID() == localID) return conversationInfo;
		}
		
		//Returning null
		return null;
	}
	
	public static ConversationInfo findConversationInfoExternalID(long externalID, int serviceHandler, String service) {
		//Getting the conversations
		ArrayList<ConversationInfo> conversations = getConversations();
		if(conversations == null) return null;
		
		//Returning the conversation info
		for(ConversationInfo conversationInfo : conversations) {
			if(conversationInfo.getExternalID() == externalID && conversationInfo.getServiceHandler() == serviceHandler && conversationInfo.getService().equals(service)) return conversationInfo;
		}
		
		//Returning null
		return null;
	}
	
	public static ArrayList<ConversationInfo> getForegroundConversations() {
		//Creating the list
		ArrayList<ConversationInfo> list = new ArrayList<>();
		
		//Iterating over the loaded conversation IDs
		for(long conversationID : Messaging.getForegroundConversations()) {
			//Adding the conversation
			ConversationInfo conversationInfo = findConversationInfo(conversationID);
			if(conversationInfo != null) list.add(conversationInfo);
		}
		
		//Returning the list
		return list;
	}
	
	public static ArrayList<ConversationInfo> getLoadedConversations() {
		//Creating the list
		ArrayList<ConversationInfo> list = new ArrayList<>();
		
		//Getting the conversations
		ArrayList<ConversationInfo> conversations = getConversations();
		if(conversations == null) return list;
		
		//Iterating over the conversations
		for(ConversationInfo item : conversations) {
			//Skipping the remainder of the iteration if the conversation's items aren't loaded
			if(!item.isDataAvailable()) continue;
			
			//Adding the conversation to the list
			list.add(item);
		}
		
		//Returning the list
		return list;
	}
	
	public static int compareConversationItems(ConversationItem item1, ConversationItem item2) {
		//Returning 0 if either of the arguments are invalid
		if(item1 == null || item2 == null) return 0;
		
		//Returning the comparison
		if(item1.getServerID() != -1 && item2.getServerID() != -1) return Long.compare(item1.getServerID(), item2.getServerID());
		if(item1.getLocalID() != -1 && item2.getLocalID() != -1) return Long.compare(item1.getLocalID(), item2.getLocalID());
		if(item1.getLocalID() == -1 && item2.getLocalID() == -1) return Long.compare(item1.getDate(), item2.getDate());
		if(item1.getLocalID() == -1) return 1;
		return -1; //Item 2's local ID is -1
	}
	
	public static int compareConversationItems(LightConversationItem item1, ConversationItem item2) {
		//Returning 0 if either of the values are invalid
		if(item1 == null || item2 == null) return 0;
		
		//Returning the comparison
		if(item1.getServerID() != -1 && item2.getServerID() != -1) return Long.compare(item1.getServerID(), item2.getServerID());
		if(item1.getLocalID() != -1 && item2.getLocalID() != -1) return Long.compare(item1.getLocalID(), item2.getLocalID());
		if(item1.getLocalID() == -1 && item2.getLocalID() == -1) return Long.compare(item1.getDate(), item2.getDate());
		if(item1.getLocalID() == -1) return 1;
		return -1; //Item 2's local ID is -1
	}
	
	public static int compareConversationItems(LightConversationItem item1, LightConversationItem item2) {
		//Returning 0 if either of the values are invalid
		if(item1 == null || item2 == null) return 0;
		
		//Returning the comparison
		if(item1.getServerID() != -1 && item2.getServerID() != -1) return Long.compare(item1.getServerID(), item2.getServerID());
		if(item1.getLocalID() != -1 && item2.getLocalID() != -1) return Long.compare(item1.getLocalID(), item2.getLocalID());
		if(item1.getLocalID() == -1 && item2.getLocalID() == -1) return Long.compare(item1.getDate(), item2.getDate());
		if(item1.getLocalID() == -1) return 1;
		return -1; //Item 2's local ID is -1
	}
	
	public static class ActionLineViewHolder extends RecyclerView.ViewHolder {
		public final TextView labelMessage;
		
		public ActionLineViewHolder(View view) {
			super(view);
			
			labelMessage = view.findViewById(R.id.message);
		}
	}
	
	public static void setupConversationItemRelations(List<ConversationItem> conversationItems, ConversationInfo conversationInfo) {
		//Iterating over the items
		for(int i = 0; i < conversationItems.size(); i++) {
			//Getting the item
			ConversationItem item = conversationItems.get(i);
			if(!(item instanceof MessageInfo)) continue;
			MessageInfo messageItem = (MessageInfo) item;
			
			//Checking if there is a less recent item
			if(i > 0) {
				//Getting the item
				ConversationItem adjacentItem = conversationItems.get(i - 1);
				
				//Checking if the item is a valid anchor point (is a message and is within the burst time)
				if(adjacentItem instanceof MessageInfo && Math.abs(item.getDate() - adjacentItem.getDate()) < ConversationUtils.conversationBurstTimeMillis) {
					//Updating the anchorage
					messageItem.setAnchoredTop(messageItem.getSender() == null ? ((MessageInfo) adjacentItem).getSender() == null : messageItem.getSender().equals(((MessageInfo) adjacentItem).getSender()));
				}
				
				//Finding the last message
				int currentIndex = i - 1;
				while(!(adjacentItem instanceof MessageInfo)) {
					currentIndex--;
					if(currentIndex < 0) break;
					adjacentItem = conversationItems.get(currentIndex);
				}
						/* if(!(adjacentItem instanceof MessageInfo)) {
							do {
								currentIndex--;
								adjacentItem = conversationItems.get(currentIndex);
							} while(!(adjacentItem instanceof MessageInfo) && currentIndex > 0);
						} */
				
				if(currentIndex >= 0) messageItem.setHasTimeDivider(Math.abs(item.getDate() - adjacentItem.getDate()) >= ConversationUtils.conversationSessionTimeMillis);
				else messageItem.setHasTimeDivider(true); //The item is the first message (not conversation item)
			} else messageItem.setHasTimeDivider(true); //The item is at the beginning of the conversation
			
			//Checking if there is a more recent item
			if(i < conversationItems.size() - 1) {
				//Getting the item
				ConversationItem adjacentItem = conversationItems.get(i + 1);
				
				//Checking if the item is a valid anchor point (is a message and is within the burst time)
				if(adjacentItem instanceof MessageInfo && Math.abs(item.getDate() - adjacentItem.getDate()) < ConversationUtils.conversationBurstTimeMillis) {
					//Updating the anchorage
					messageItem.setAnchoredBottom(messageItem.getSender() == null ? ((MessageInfo) adjacentItem).getSender() == null : messageItem.getSender().equals(((MessageInfo) adjacentItem).getSender()));
				}
			}
		}
		
		//Finding the message to show the state on
		boolean targetDeliveredSet = false;
		//boolean targetReadSet = false;
		for(int i = conversationItems.size() - 1; i >= 0; i--) {
			//Getting the item
			ConversationItem item = conversationItems.get(i);
			
			//Skipping the remainder of the iteration if the item is not a message
			if(!(item instanceof MessageInfo)) continue;
			
			//Getting the message
			MessageInfo messageItem = (MessageInfo) item;
			
			//Skipping the remainder of the iteration if the message is incoming
			if(!messageItem.isOutgoing()) continue;
			
			//Setting the conversation's active message state list ID
			if(!targetDeliveredSet && messageItem.getMessageState() == Constants.messageStateCodeDelivered) {
				conversationInfo.setActivityStateTargetDelivered(messageItem);
				targetDeliveredSet = true;
			}
			if(/*!targetReadSet && */messageItem.getMessageState() == Constants.messageStateCodeRead) {
				if(!targetDeliveredSet) conversationInfo.setActivityStateTargetDelivered(messageItem); //The delivered and read message would be the same thing
				conversationInfo.setActivityStateTargetRead(messageItem);
				//targetReadSet = true;
				break; //Break on the first instance of a read message; if no delivered message has been found, then this takes priority anyways (the delivered message will overlap with this one, or be someplace above due to an awkward update)
			}
			
			//Breaking from the loop
			//if(targetDeliveredSet && targetReadSet) break;
		}
	}
	
	public static void addConversationItemRelation(ConversationInfo conversation, List<ConversationItem> conversationItems, MessageInfo messageInfo, Context context, boolean update) {
		//Getting the index
		int index = conversationItems.indexOf(messageInfo);
		
		//Checking if there is a less recent item
		if(index > 0) {
			//Getting the item
			ConversationItem adjacentItem = conversationItems.get(index - 1);
			
			//Checking if the item is a valid anchor point (is a message and is within the burst time)
			if(adjacentItem instanceof MessageInfo && Math.abs(messageInfo.getDate() - adjacentItem.getDate()) < ConversationUtils.conversationBurstTimeMillis) {
				//Updating the anchorage
				boolean isAnchored = messageInfo.getSender() == null ? ((MessageInfo) adjacentItem).getSender() == null : messageInfo.getSender().equals(((MessageInfo) adjacentItem).getSender());
				messageInfo.setAnchoredTop(isAnchored);
				((MessageInfo) adjacentItem).setAnchoredBottom(isAnchored);
				
				//Updating the views
				if(update) {
					messageInfo.updateViewEdges(context.getResources().getBoolean(R.bool.is_left_to_right));
					((MessageInfo) adjacentItem).updateViewEdges(context.getResources().getBoolean(R.bool.is_left_to_right));
				}
			}
			
			//Finding the last message
			int currentIndex = index - 1;
			while(!(adjacentItem instanceof MessageInfo)) {
				currentIndex--;
				if(currentIndex < 0) break;
				adjacentItem = conversationItems.get(currentIndex);
			}
			
			if(currentIndex >= 0) messageInfo.setHasTimeDivider(Math.abs(messageInfo.getDate() - adjacentItem.getDate()) >= ConversationUtils.conversationSessionTimeMillis);
			else messageInfo.setHasTimeDivider(true); //The item is the first message (not conversation item)
		} else messageInfo.setHasTimeDivider(true); //The item is at the beginning of the conversation
		
		//Updating the view
		if(update) messageInfo.updateTimeDivider(context);
		
		//Checking if there is a more recent item
		if(index < conversationItems.size() - 1) {
			//Getting the item
			ConversationItem adjacentItem = conversationItems.get(index + 1);
			
			//Checking if the item is a valid anchor point (is a message and is within the burst time)
			if(adjacentItem instanceof MessageInfo && Math.abs(messageInfo.getDate() - adjacentItem.getDate()) < ConversationUtils.conversationBurstTimeMillis) {
				//Updating the anchorage
				boolean isAnchored = messageInfo.getSender() == null ? ((MessageInfo) adjacentItem).getSender() == null : messageInfo.getSender().equals(((MessageInfo) adjacentItem).getSender());
				messageInfo.setAnchoredBottom(isAnchored);
				((MessageInfo) adjacentItem).setAnchoredTop(isAnchored);
				
				//Updating the views
				if(update) {
					messageInfo.updateViewEdges(context.getResources().getBoolean(R.bool.is_left_to_right));
					((MessageInfo) adjacentItem).updateViewEdges(context.getResources().getBoolean(R.bool.is_left_to_right));
				}
			}
		}
		
		//Comparing (and replacing) the activity state target
		conversation.tryActivityStateTarget(messageInfo, update, context);
		
		//Comparing the conversation item to the active message state listing item
		/* {
			//Getting the current item
			MessageInfo activeMessage = conversation.getActivityStateTarget();
			
			//Replacing the item if it is invalid
			if(activeMessage == null) {
				conversation.setActivityStateTarget(messageInfo);
				
				//Updating the view
				if(update) messageInfo.updateActivityStateDisplay(context);
			} else {
				//Replacing the item if the new one is outgoing and more recent
				if(messageInfo.isOutgoing() &&
						messageInfo.getDate() >= activeMessage.getDate() &&
						(messageInfo.getMessageState() == Constants.messageStateCodeDelivered || messageInfo.getMessageState() == Constants.messageStateCodeRead)) {
					conversation.setActivityStateTarget(messageInfo);
					
					//Updating the views
					if(update) {
						messageInfo.updateActivityStateDisplay(context);
						activeMessage.updateActivityStateDisplay(context);
					}
				}
			}
		} */
	}
	
	public static void removeConversationItemRelation(ConversationInfo conversation, List<ConversationItem> conversationItems, int index, Context context, boolean update) {
		//Getting the items adjacent to the message
		ConversationItem itemOlder = index > 0 ? conversationItems.get(index - 1) : null;
		MessageInfo messageOlder = itemOlder != null && itemOlder instanceof MessageInfo ? (MessageInfo) itemOlder : null;
		ConversationItem itemNewer = index < conversationItems.size() ? conversationItems.get(index) : null;
		MessageInfo messageNewer = itemNewer != null && itemNewer instanceof MessageInfo ? (MessageInfo) itemNewer : null;
		
		//Updating the items individually if there is only one of the two
		if(messageOlder == null && messageNewer == null) return;
		if(messageOlder != null && messageNewer == null) {
			//The item is at the end of the conversation (the removed message was at the bottom of the chat)
			messageOlder.setAnchoredBottom(false);
			if(update) messageOlder.updateViewEdges(Constants.isLTR(context.getResources()));
			
			//Replacing the activity state target
			conversation.tryActivityStateTarget(messageOlder, update, context);
		} else if(messageOlder == null) { //messageNewer will always be not null
			//The item is at the beginning of the conversation (the removed message was at the top of the chat)
			messageNewer.setHasTimeDivider(true);
			if(update) messageNewer.updateTimeDivider(context);
			
			messageNewer.setAnchoredTop(false);
			if(update) messageNewer.updateViewEdges(Constants.isLTR(context.getResources()));
		} else { //Both the older message and newer message are valid
			//Checking if the item is a valid anchor point (is a message and is within the burst time)
			boolean isAnchored = Math.abs(messageOlder.getDate() - messageNewer.getDate()) < ConversationUtils.conversationBurstTimeMillis && Objects.equals(messageOlder.getSender(), messageNewer.getSender());
			messageOlder.setAnchoredBottom(isAnchored);
			messageNewer.setAnchoredTop(isAnchored);
			
			//Recalculating the time divider visibility
			messageNewer.setHasTimeDivider(Math.abs(messageNewer.getDate() - messageOlder.getDate()) >= ConversationUtils.conversationSessionTimeMillis);
			
			//Updating the views
			if(update) {
				boolean isLTR = Constants.isLTR(context.getResources());
				messageOlder.updateViewEdges(isLTR);
				messageNewer.updateViewEdges(isLTR);
				
				messageNewer.updateTimeDivider(context);
			}
		}
	}
	
	public static void addConversationItemRelations(ConversationInfo conversation, List<ConversationItem> conversationItems, List<ConversationItem> newConversationItems, Context context, boolean update) {
		int highestIndex = -1;
		
		//Iterating over the new items
		for(ConversationItem conversationItem : newConversationItems) {
			//Skipping the remainder of the iteration if the item is not a message
			if(!(conversationItem instanceof MessageInfo)) continue;
			
			//Getting the message info
			MessageInfo messageInfo = (MessageInfo) conversationItem;
			
			//Getting the item's positioning
			int index = conversationItems.indexOf(messageInfo);
			if(index > highestIndex) highestIndex = index;
			
			//Used to skip updating other items in the chunk, as they will be iterated over too
			//int chunkIndex = newConversationItems.indexOf(conversationItem);
			//boolean isOldestInChunk = chunkIndex == 0;
			//boolean isNewestInChunk = newConversationItems.indexOf(conversationItem) == newConversationItems.size() - 1;
			
			//Checking if there is a less recent item
			if(index > 0) {
				//Getting the item
				ConversationItem adjacentItem = conversationItems.get(index - 1);
				
				//Checking if the item is a valid anchor point (is a message and is within the burst time)
				if(adjacentItem instanceof MessageInfo && Math.abs(messageInfo.getDate() - adjacentItem.getDate()) < ConversationUtils.conversationBurstTimeMillis) {
					//Updating the anchorage
					boolean isAnchored = messageInfo.getSender() == null ? ((MessageInfo) adjacentItem).getSender() == null : messageInfo.getSender().equals(((MessageInfo) adjacentItem).getSender());
					messageInfo.setAnchoredTop(isAnchored);
					((MessageInfo) adjacentItem).setAnchoredBottom(isAnchored);
					
					//Updating the views
					if(update) {
						messageInfo.updateViewEdges(context.getResources().getBoolean(R.bool.is_left_to_right));
						if(!newConversationItems.contains(adjacentItem)) ((MessageInfo) adjacentItem).updateViewEdges(context.getResources().getBoolean(R.bool.is_left_to_right));
					}
				}
				
				//Finding the last message
				int currentIndex = index - 1;
				while(!(adjacentItem instanceof MessageInfo)) {
					currentIndex--;
					if(currentIndex < 0) break;
					adjacentItem = conversationItems.get(currentIndex);
				}
				
				if(currentIndex >= 0) messageInfo.setHasTimeDivider(Math.abs(messageInfo.getDate() - adjacentItem.getDate()) >= ConversationUtils.conversationSessionTimeMillis);
				else messageInfo.setHasTimeDivider(true); //The item is the first message (not conversation item)
			} else messageInfo.setHasTimeDivider(true); //The item is at the beginning of the conversation
			
			//Updating the view
			if(update) messageInfo.updateTimeDivider(context);
			
			//Checking if there is a more recent item
			if(index < conversationItems.size() - 1) {
				//Getting the item
				ConversationItem adjacentItem = conversationItems.get(index + 1);
				
				//Checking if the item is a valid anchor point (is a message and is within the burst time)
				if(adjacentItem instanceof MessageInfo && Math.abs(messageInfo.getDate() - adjacentItem.getDate()) < ConversationUtils.conversationBurstTimeMillis) {
					//Updating the anchorage
					boolean isAnchored = messageInfo.getSender() == null ? ((MessageInfo) adjacentItem).getSender() == null : messageInfo.getSender().equals(((MessageInfo) adjacentItem).getSender());
					messageInfo.setAnchoredBottom(isAnchored);
					((MessageInfo) adjacentItem).setAnchoredTop(isAnchored);
					
					//Updating the views
					if(update) {
						messageInfo.updateViewEdges(context.getResources().getBoolean(R.bool.is_left_to_right));
						if(!newConversationItems.contains(adjacentItem)) ((MessageInfo) adjacentItem).updateViewEdges(context.getResources().getBoolean(R.bool.is_left_to_right));
					}
				}
			}
			
			//Comparing (and replacing) the activity state target if the message is the newest in the chunk
			if(newConversationItems.indexOf(conversationItem) == newConversationItems.size() - 1) conversation.tryActivityStateTarget(messageInfo, update, context);
		}
		
		//Updating the time divider of the item below the group
		if(highestIndex != -1 && conversationItems.size() > highestIndex + 1) {
			ConversationItem groupItem = conversationItems.get(highestIndex);
			ConversationItem adjacentItem = conversationItems.get(highestIndex + 1);
			
			//Checking if both items are messages
			if(groupItem instanceof MessageInfo && adjacentItem instanceof MessageInfo) {
				MessageInfo adjacentMessage = (MessageInfo) adjacentItem;
				adjacentMessage.setHasTimeDivider(Math.abs(groupItem.getDate() - adjacentItem.getDate()) >= ConversationUtils.conversationSessionTimeMillis);
				if(update) adjacentMessage.updateTimeDivider(context);
			}
		}
	}
	
	public static int getNameFromContent(String fileType, String fileName) {
		//Returning the type
		if(fileType == null || fileType.isEmpty()) return OtherAttachmentInfo.RESOURCE_NAME;
		else if(ImageAttachmentInfo.checkFileApplicability(fileType, fileName)) return ImageAttachmentInfo.RESOURCE_NAME;
		else if(VideoAttachmentInfo.checkFileApplicability(fileType, fileName)) return VideoAttachmentInfo.RESOURCE_NAME;
		else if(AudioAttachmentInfo.checkFileApplicability(fileType, fileName)) return AudioAttachmentInfo.RESOURCE_NAME;
		else if(VLocationAttachmentInfo.checkFileApplicability(fileType, fileName)) return VLocationAttachmentInfo.RESOURCE_NAME;
		else if(ContactAttachmentInfo.checkFileApplicability(fileType, fileName)) return ContactAttachmentInfo.RESOURCE_NAME;
		else return OtherAttachmentInfo.RESOURCE_NAME;
	}
	
	@RequiresApi(api = Build.VERSION_CODES.N_MR1)
	private static void generateShortcutInfo(Context context, List<ConversationInfo> conversationList, Constants.ResultCallback<List<ShortcutInfo>> result) {
		//Removing invalid conversations
		for(ListIterator<ConversationInfo> iterator = conversationList.listIterator(); iterator.hasNext();) {
			if(iterator.next().getConversationMembers().isEmpty()) iterator.remove();
		}
		//Creating the shortcuts
		List<ShortcutInfo> shortcutList = new ArrayList<>(conversationList.size());
		LongSparseArray<String> titleMap = new LongSparseArray<>();
		LongSparseArray<Icon> iconMap = new LongSparseArray<>();
		Constants.ValueWrapper<Integer> titleRequestsCompleted = new Constants.ValueWrapper<>(0);
		
		for(ConversationInfo conversationBuild : conversationList) {
			conversationBuild.buildTitle(context, (titleResult, titleWasTasked) -> {
				//Recording the title
				titleMap.put(conversationBuild.getLocalID(), titleResult);
				
				conversationBuild.generateShortcutIcon(context, (iconResult, iconWasTasked) -> {
					//Recording the icon
					iconMap.put(conversationBuild.getLocalID(), iconResult);
					
					//Adding to the completion count
					titleRequestsCompleted.value++;
					
					//Checking if all requests have been completed
					if(titleRequestsCompleted.value == conversationList.size()) {
						//Building the shortcuts
						for(ConversationInfo conversationShortcut : conversationList) {
							//Creating the intents
							Intent intentConversations = new Intent(context, Conversations.class);
							intentConversations.setAction(Intent.ACTION_VIEW);
							intentConversations.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
							
							Intent intentMessaging = new Intent(context, Messaging.class);
							intentMessaging.setAction(Intent.ACTION_VIEW);
							intentMessaging.putExtra(Constants.intentParamTargetID, conversationShortcut.getLocalID());
							
							//Building and adding the shortcuts
							ShortcutInfo shortcut = new ShortcutInfo.Builder(context, shortcutPrefixConversation + conversationShortcut.getGuid())
									.setShortLabel(titleMap.get(conversationShortcut.getLocalID()))
									.setIcon(iconMap.get(conversationShortcut.getLocalID()))
									.setIntents(new Intent[]{intentConversations, intentMessaging})
									.build();
							shortcutList.add(shortcut);
						}
						
						//Returning the list
						result.onResult(iconWasTasked || titleWasTasked, shortcutList);
					}
				});
			});
		}
	}
	
	public static void reportShortcutUsed(Context context, String conversationGUID) {
		//Shortcuts require Android 7.1 Nougat or above
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return;
		
		ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
		shortcutManager.reportShortcutUsed(shortcutPrefixConversation + conversationGUID);
	}
	
	public static void rebuildDynamicShortcuts(Context context) {
		//Shortcuts require Android 7.1 Nougat or above
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return;
		
		//Getting the top 3 conversations
		MainApplication.LoadFlagArrayList<ConversationInfo> conversations = getConversations();
		if(conversations == null || !conversations.isLoaded()) return;
		List<ConversationInfo> topConversations = new ArrayList<>(conversations.subList(0, Math.min(conversations.size(), 3)));
		
		//Creating the shortcuts
		generateShortcutInfo(context, topConversations, (wasTasked, shortcutList) -> {
			//Setting the shortcuts
			ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
			shortcutManager.setDynamicShortcuts(shortcutList);
		});
	}
	
	@RequiresApi(api = Build.VERSION_CODES.N_MR1)
	private static boolean isShortcutActive(ShortcutManager shortcutManager, String shortcutID) {
		for(ShortcutInfo shortcut : shortcutManager.getDynamicShortcuts()) if(shortcut.getId().equals(shortcutID)) return true;
		for(ShortcutInfo shortcut : shortcutManager.getPinnedShortcuts()) if(shortcut.getId().equals(shortcutID)) return true;
		return false;
	}
	
	@RequiresApi(api = Build.VERSION_CODES.N_MR1)
	private static List<ConversationInfo> getActiveShortcutConversations(List<ConversationInfo> conversationList, ShortcutManager shortcutManager) {
		//Creating the list
		List<ConversationInfo> list = new ArrayList<>();
		
		//Adding the shortcuts
		ConversationInfo conversationInfo;
		for(ShortcutInfo shortcut : shortcutManager.getDynamicShortcuts()) {
			conversationInfo = conversationInfoFromShortcut(conversationList, shortcut);
			if(conversationInfo != null) list.add(conversationInfo);
		}
		for(ShortcutInfo shortcut : shortcutManager.getPinnedShortcuts()) {
			conversationInfo = conversationInfoFromShortcut(conversationList, shortcut);
			if(conversationInfo != null) list.add(conversationInfo);
		}
		
		//Returning the list
		return list;
	}
	
	@RequiresApi(api = Build.VERSION_CODES.N_MR1)
	private static ConversationInfo conversationInfoFromShortcut(List<ConversationInfo> conversationList, ShortcutInfo shortcutInfo) {
		String shortcutID = shortcutInfo.getId();
		if(!shortcutID.startsWith(shortcutPrefixConversation)) return null; //Not a conversation shortcut
		String chatID = shortcutID.substring(shortcutPrefixConversation.length());
		for(ConversationInfo conversationInfo : conversationList) {
			if(chatID.equals(conversationInfo.getGuid())) return conversationInfo;
		}
		return null;
	}
	
	public static void updateShortcuts(Context context, List<ConversationInfo> conversationList) {
		//Shortcuts require Android 7.1 Nougat or above
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return;
		
		//Getting the shortcut manager
		ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
		
		//Filtering out conversations that aren't in any shortcuts
		/* List<ConversationInfo> filteredList = new ArrayList<>();
		for(ConversationInfo conversation : conversationList) {
			if(!isShortcutActive(shortcutManager, shortcutPrefixConversation + conversation.getGuid())) continue;
			filteredList.add(conversation);
		} */
		List<ConversationInfo> filteredList = getActiveShortcutConversations(conversationList, shortcutManager);
		
		//Creating the shortcuts
		generateShortcutInfo(context, filteredList, (wasTasked, shortcutList) -> {
			//Setting the shortcuts
			shortcutManager.updateShortcuts(shortcutList);
		});
	}
	
	public static void disableShortcuts(Context context, List<ConversationInfo> conversationList) {
		//Shortcuts require Android 7.1 Nougat or above
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return;
		
		//Mapping the conversations to IDs
		List<String> idList = new ArrayList<>(conversationList.size());
		for(ConversationInfo conversation : conversationList) idList.add(shortcutPrefixConversation + conversation.getGuid());
		
		//Disabling the shortcuts
		ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
		shortcutManager.disableShortcuts(idList);
	}
	
	public static void enableShortcuts(Context context, List<ConversationInfo> conversationList) {
		//Shortcuts require Android 7.1 Nougat or above
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return;
		
		//Mapping the conversations to GUIDs
		List<String> idList = new ArrayList<>(conversationList.size());
		for(ConversationInfo conversation : conversationList) idList.add(shortcutPrefixConversation + conversation.getGuid());
		
		//Disabling the shortcuts
		ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
		shortcutManager.enableShortcuts(idList);
	}
	
	public static void clearDynamicShortcuts(Context context) {
		//Shortcuts require Android 7.1 Nougat or above
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return;
		
		//Clearing the shortcuts
		ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
		shortcutManager.removeAllDynamicShortcuts();
	}
	
	public static void deleteMMSSMSConversationSync(Context context, Set<String> recipients) {
		try {
			long threadID = Telephony.Threads.getOrCreateThreadId(context, recipients);
			context.getContentResolver().delete(ContentUris.withAppendedId(Telephony.Threads.CONTENT_URI, threadID), null, null);
			context.getContentResolver().delete(Telephony.Threads.CONTENT_URI, Telephony.Mms._ID + " = ?", new String[]{Long.toString(threadID)});
		} catch(SQLiteException exception) {
			exception.printStackTrace();
		}
	}
	
	public static AttachmentInfo<?> createAttachmentInfoFromType(long fileID, String fileGuid, MessageInfo messageInfo, String fileName, String fileType, long fileSize) {
		if(fileType == null) fileType = Constants.defaultMIMEType;
		if(ImageAttachmentInfo.checkFileApplicability(fileType, fileName)) return new ImageAttachmentInfo(fileID, fileGuid, messageInfo, fileName, fileType, fileSize);
		else if(AudioAttachmentInfo.checkFileApplicability(fileType, fileName)) return new AudioAttachmentInfo(fileID, fileGuid, messageInfo, fileName, fileType, fileSize);
		else if(VideoAttachmentInfo.checkFileApplicability(fileType, fileName)) return new VideoAttachmentInfo(fileID, fileGuid, messageInfo, fileName, fileType, fileSize);
		else if(VLocationAttachmentInfo.checkFileApplicability(fileType, fileName)) return new VLocationAttachmentInfo(fileID, fileGuid, messageInfo, fileName, fileType, fileSize);
		else if(ContactAttachmentInfo.checkFileApplicability(fileType, fileName)) return new ContactAttachmentInfo(fileID, fileGuid, messageInfo, fileName, fileType, fileSize);
		return new OtherAttachmentInfo(fileID, fileGuid, messageInfo, fileName, fileType, fileSize);
	}
	
	public static AttachmentInfo<?> createAttachmentInfoFromType(long fileID, String fileGuid, MessageInfo messageInfo, String fileName, String fileType, long fileSize, File file) {
		if(fileType == null) fileType = Constants.defaultMIMEType;
		if(ImageAttachmentInfo.checkFileApplicability(fileType, fileName)) return new ImageAttachmentInfo(fileID, fileGuid, messageInfo, fileName, fileType, fileSize, file);
		else if(AudioAttachmentInfo.checkFileApplicability(fileType, fileName)) return new AudioAttachmentInfo(fileID, fileGuid, messageInfo, fileName, fileType, fileSize, file);
		else if(VideoAttachmentInfo.checkFileApplicability(fileType, fileName)) return new VideoAttachmentInfo(fileID, fileGuid, messageInfo, fileName, fileType, fileSize, file);
		else if(VLocationAttachmentInfo.checkFileApplicability(fileType, fileName)) return new VLocationAttachmentInfo(fileID, fileGuid, messageInfo, fileName, fileType, fileSize, file);
		else if(ContactAttachmentInfo.checkFileApplicability(fileType, fileName)) return new ContactAttachmentInfo(fileID, fileGuid, messageInfo, fileName, fileType, fileSize, file);
		return new OtherAttachmentInfo(fileID, fileGuid, messageInfo, fileName, fileType, fileSize, file);
	}
	
	public static AttachmentInfo<?> createAttachmentInfoFromType(long fileID, String fileGuid, MessageInfo messageInfo, String fileName, String fileType, long fileSize, Uri fileUri) {
		if(ImageAttachmentInfo.checkFileApplicability(fileType, fileName)) return new ImageAttachmentInfo(fileID, fileGuid, messageInfo, fileName, fileType, fileSize, fileUri);
		else if(AudioAttachmentInfo.checkFileApplicability(fileType, fileName)) return new AudioAttachmentInfo(fileID, fileGuid, messageInfo, fileName, fileType, fileSize, fileUri);
		else if(VideoAttachmentInfo.checkFileApplicability(fileType, fileName)) return new VideoAttachmentInfo(fileID, fileGuid, messageInfo, fileName, fileType, fileSize, fileUri);
		else if(VLocationAttachmentInfo.checkFileApplicability(fileType, fileName)) return new VLocationAttachmentInfo(fileID, fileGuid, messageInfo, fileName, fileType, fileSize, fileUri);
		else if(ContactAttachmentInfo.checkFileApplicability(fileType, fileName)) return new ContactAttachmentInfo(fileID, fileGuid, messageInfo, fileName, fileType, fileSize, fileUri);
		return new OtherAttachmentInfo(fileID, fileGuid, messageInfo, fileName, fileType, fileSize, fileUri);
	}
	
	public static int getMaxMessageWidth(Resources resources) {
		return (int) Math.min(Constants.getMaxContentWidth(resources) * .7F, resources.getDisplayMetrics().widthPixels * 0.7F);
	}
}