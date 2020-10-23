package me.tagavari.airmessage.util;

import android.app.Person;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.activity.Conversations;
import me.tagavari.airmessage.activity.Messaging;
import me.tagavari.airmessage.messaging.ConversationInfo;

@RequiresApi(api = Build.VERSION_CODES.N_MR1)
public class ShortcutUtils {
	private static final String shortcutPrefixConversation = "conversation-";
	private static final Set<String> shareCategorySet = Collections.singleton("me.tagavari.airmessage.directshare.category.DEFAULT");
	private static final int dynamicShortcutLimit = 3;
	
	/**
	 * Create a list of shortcut infos from a list of conversations
	 * @param context The context to use
	 * @param conversationList The list of conversations
	 * @param result A callback to receive the list of shortcut infos
	 */
	private static void generateShortcutInfo(Context context, List<ConversationInfo> conversationList, Constants.ResultCallback<List<ShortcutInfo>> result) {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			generateShortcutInfoAndroidQ(context, conversationList, result);
		} else {
			generateShortcutInfoAndroidN(context, conversationList, result);
		}
	}
	
	private static void generateShortcutInfoAndroidN(Context context, List<ConversationInfo> originalConversations, Constants.ResultCallback<List<ShortcutInfo>> result) {
		//Removing invalid conversations
		List<ConversationInfo> conversationList = originalConversations.stream().filter(conversation -> !conversation.getConversationMembers().isEmpty()).collect(Collectors.toList());
		
		//Creating the shortcuts
		List<ShortcutInfo> shortcutList = new ArrayList<>(conversationList.size());
		String[] titleArray = new String[conversationList.size()];
		Icon[] iconArray = new Icon[conversationList.size()];
		Constants.ValueWrapper<Integer> requestsCompleted = new Constants.ValueWrapper<>(0);
		
		final Consumer<Boolean> completionRunnable = (wasTasked) -> {
			//Adding to the completion count
			requestsCompleted.value++;
			
			//Checking if all requests have been completed
			if(requestsCompleted.value == conversationList.size()) {
				//Building the shortcuts
				for(ListIterator<ConversationInfo> shortcutIterator = conversationList.listIterator(); shortcutIterator.hasNext();) {
					int indexShortcut = shortcutIterator.nextIndex();
					ConversationInfo conversationShortcut = shortcutIterator.next();
					
					//Creating the intents
					Intent intentConversations = new Intent(context, Conversations.class);
					intentConversations.setAction(Intent.ACTION_VIEW);
					intentConversations.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
					
					Intent intentMessaging = new Intent(context, Messaging.class);
					intentMessaging.setAction(Intent.ACTION_VIEW);
					intentMessaging.putExtra(Constants.intentParamTargetID, conversationShortcut.getLocalID());
					
					//Building and adding the shortcuts
					shortcutList.add(new ShortcutInfo.Builder(context, conversationToShortcutID(conversationShortcut))
							.setShortLabel(titleArray[indexShortcut])
							.setIcon(iconArray[indexShortcut])
							.setIntents(new Intent[]{intentConversations, intentMessaging})
							.setCategories(shareCategorySet)
							.setRank(getConversationShortcutRank(conversationShortcut))
							.build());
				}
				
				//Returning the list
				result.onResult(wasTasked, shortcutList);
			}
		};
		
		for(ListIterator<ConversationInfo> buildIterator = conversationList.listIterator(); buildIterator.hasNext();) {
			int indexBuild = buildIterator.nextIndex();
			ConversationInfo indexConversation = buildIterator.next();
			
			//Building the title
			indexConversation.buildTitle(context, (titleResult, titleWasTasked) -> {
				titleArray[indexBuild] = titleResult;
				
				//Building the shortcut icon
				indexConversation.generateShortcutIcon(context, (iconResult, iconWasTasked) -> {
					iconArray[indexBuild] = Icon.createWithBitmap(iconResult);
					
					//Building the people list
					completionRunnable.accept(titleWasTasked || iconWasTasked);
				});
			});
		}
	}
	
	@RequiresApi(api = Build.VERSION_CODES.Q)
	private static void generateShortcutInfoAndroidQ(Context context, List<ConversationInfo> originalConversations, Constants.ResultCallback<List<ShortcutInfo>> result) {
		//Removing invalid conversations
		List<ConversationInfo> conversationList = originalConversations.stream().filter(conversation -> !conversation.getConversationMembers().isEmpty()).collect(Collectors.toList());
		
		//Creating the shortcuts
		List<ShortcutInfo> shortcutList = new ArrayList<>(conversationList.size());
		String[] titleArray = new String[conversationList.size()];
		Icon[] iconArray = new Icon[conversationList.size()];
		Person[][] personArray = new Person[conversationList.size()][];
		Constants.ValueWrapper<Integer> requestsCompleted = new Constants.ValueWrapper<>(0);
		
		final Consumer<Boolean> completionRunnable = (wasTasked) -> {
			//Adding to the completion count
			requestsCompleted.value++;
			
			//Checking if all requests have been completed
			if(requestsCompleted.value == conversationList.size()) {
				//Building the shortcuts
				for(ListIterator<ConversationInfo> shortcutIterator = conversationList.listIterator(); shortcutIterator.hasNext();) {
					int indexShortcut = shortcutIterator.nextIndex();
					ConversationInfo conversationShortcut = shortcutIterator.next();
					
					//Creating the intents
					Intent intentConversations = new Intent(context, Conversations.class);
					intentConversations.setAction(Intent.ACTION_VIEW);
					intentConversations.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
					
					Intent intentMessaging = new Intent(context, Messaging.class);
					intentMessaging.setAction(Intent.ACTION_VIEW);
					intentMessaging.putExtra(Constants.intentParamTargetID, conversationShortcut.getLocalID());
					
					//Building and adding the shortcuts
					shortcutList.add(new ShortcutInfo.Builder(context, conversationToShortcutID(conversationShortcut))
							.setShortLabel(titleArray[indexShortcut])
							.setIcon(iconArray[indexShortcut])
							.setIntents(new Intent[]{intentConversations, intentMessaging})
							.setCategories(shareCategorySet)
							.setLongLived(true)
							.setPersons(personArray[indexShortcut])
							.setRank(getConversationShortcutRank(conversationShortcut))
							.build());
				}
				
				//Returning the list
				result.onResult(wasTasked, shortcutList);
			}
		};
		
		for(ListIterator<ConversationInfo> buildIterator = conversationList.listIterator(); buildIterator.hasNext();) {
			int indexBuild = buildIterator.nextIndex();
			ConversationInfo indexConversation = buildIterator.next();
			
			//Building the title
			indexConversation.buildTitle(context, (titleResult, titleWasTasked) -> {
				titleArray[indexBuild] = titleResult;
				
				//Building the shortcut icon
				indexConversation.generateShortcutIcon(context, (iconResult, iconWasTasked) -> {
					iconArray[indexBuild] = Icon.createWithBitmap(iconResult);
					
					//Building the people list
					indexConversation.generatePersonList(context, (peopleResult, peopleWasTasked) -> {
						personArray[indexBuild] = peopleResult;
						
						completionRunnable.accept(titleWasTasked || iconWasTasked || peopleWasTasked);
					});
				});
			});
		}
	}
	
	/**
	 * Report a shortcut as used
	 * @param context The context to use
	 * @param conversationID The local ID of the conversation
	 */
	public static void reportShortcutUsed(Context context, long conversationID) {
		ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
		shortcutManager.reportShortcutUsed(conversationIDToShortcutID(conversationID));
	}
	
	/**
	 * Update the top conversations as dynamic shortcuts
	 * @param context The context to use
	 */
	public static void updateTopConversations(Context context) {
		//Getting the conversations
		MainApplication.LoadFlagArrayList<ConversationInfo> conversations = ConversationUtils.getConversations();
		if(conversations == null || !conversations.isLoaded()) return;
		
		//Generate and rank the top 3 conversations
		generateShortcutInfo(context, conversations.stream().limit(dynamicShortcutLimit).collect(Collectors.toList()), (wasTasked, list) -> {
			ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
			shortcutManager.setDynamicShortcuts(list);
		});
	}
	
	/**
	 * Rebuilds and updates the shortcuts of list of conversations
	 * @param context The context to use
	 * @param conversationList The conversations to update the shortcuts of
	 */
	public static void updateShortcuts(Context context, List<ConversationInfo> conversationList) {
		//Getting the shortcut manager
		ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
		
		List<ConversationInfo> filteredList;
		//If we're running Android 11 or later, include all conversations for messaging notifications
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) filteredList = conversationList;
		//Filtering out conversations that aren't in any shortcuts
		else filteredList = getActiveShortcutConversations(shortcutManager, conversationList);
		
		//Creating the shortcuts
		generateShortcutInfo(context, filteredList, (wasTasked, shortcutList) -> {
			//Setting the shortcuts
			shortcutManager.updateShortcuts(shortcutList);
		});
	}
	
	/**
	 * Enables the shortcuts associated with the list of conversations
	 * @param context The context to use
	 * @param conversationList The conversations to enable the shortcuts of
	 */
	public static void enableShortcuts(Context context, List<ConversationInfo> conversationList) {
		//Mapping the conversations to shortcut IDs
		List<String> idList = conversationList.stream().map(ShortcutUtils::conversationToShortcutID).collect(Collectors.toList());
		
		//Enabling the shortcuts
		ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
		shortcutManager.enableShortcuts(idList);
	}
	
	/**
	 * Disables the shortcuts associated with the list of conversations
	 * Also removes long-lived shortcuts on Android 11
	 * @param context The context to use
	 * @param conversationList The conversations to disable the shortcuts of
	 */
	public static void disableShortcuts(Context context, Collection<ConversationInfo> conversationList) {
		//Mapping the conversations to shortcut IDs
		List<String> idList = conversationList.stream().map(ShortcutUtils::conversationToShortcutID).collect(Collectors.toList());
		
		//Disabling the shortcuts
		ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
		shortcutManager.disableShortcuts(idList);
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) shortcutManager.removeLongLivedShortcuts(idList);
	}
	
	/**
	 * Filters and converts a list of conversations to active shortcuts
	 * @param shortcutManager The shortcut manager to use
	 * @param conversationList The list of conversations
	 * @return A filtered list of conversations that are either dynamic or pinned
	 */
	private static List<ConversationInfo> getActiveShortcutConversations(ShortcutManager shortcutManager, List<ConversationInfo> conversationList) {
		return conversationList.stream()
				.filter((conversation) -> {
					Predicate<ShortcutInfo> matcher = shortcut -> shortcutToConversationID(shortcut) == conversation.getLocalID();
					return shortcutManager.getDynamicShortcuts().stream().anyMatch(matcher) || shortcutManager.getPinnedShortcuts().stream().anyMatch(matcher);
				}).collect(Collectors.toList());
	}
	
	/**
	 * Given a list of conversations, finds the conversation in the provided list that corresponds to the shortcut
	 * @param conversationList The conversation list to shortcut
	 * @param shortcutInfo The shortcut to match
	 * @return A matching conversation from the list, or NULL if none was found
	 */
	private static ConversationInfo conversationInfoFromShortcut(List<ConversationInfo> conversationList, ShortcutInfo shortcutInfo) {
		long conversationID = shortcutToConversationID(shortcutInfo);
		if(conversationID == -1) return null; //Not a conversation shortcut
		return conversationList.stream().filter((conversation) -> conversation.getLocalID() == conversationID).findAny().orElse(null);
	}
	
	/**
	 * Gets the rank that should be assigned to a conversation shortcut
	 * Ranges 1 to the dynamic shortcut limit, defaults to 0
	 * @param conversationInfo The conversation to check
	 * @return The conversation's rank
	 */
	private static int getConversationShortcutRank(ConversationInfo conversationInfo) {
		//Load conversations from memory
		MainApplication.LoadFlagArrayList<ConversationInfo> conversations = ConversationUtils.getConversations();
		if(conversations != null && conversations.isLoaded()) {
			//Find the index of the matching conversation in the top list
			OptionalInt indexOpt = IntStream.range(0, Math.min(conversations.size(), dynamicShortcutLimit))
					.filter(i -> conversations.get(i).getLocalID() == conversationInfo.getLocalID())
					.findAny();
			
			//If we found a match, return the rank in ascending order, starting at 1 (since our default is 0)
			if(indexOpt.isPresent()) {
				return indexOpt.getAsInt() + 1;
			}
		}
		
		//Default to 0
		return 0;
	}
	
	
	/**
	 * Converts a conversation to a shortcut ID
	 * @param conversation The conversation
	 * @return The shortcut ID of the conversation
	 */
	public static String conversationToShortcutID(ConversationInfo conversation) {
		return conversationIDToShortcutID(conversation.getLocalID());
	}
	
	/**
	 * Converts a conversation ID to a shortcut ID
	 * @param conversationID The ID of the conversation
	 * @return The shortcut ID of the conversation
	 */
	public static String conversationIDToShortcutID(long conversationID) {
		return shortcutPrefixConversation + conversationID;
	}
	
	/**
	 * Converts a shortcut to a conversation ID
	 * @param shortcutInfo The shortcut
	 * @return The ID of the conversation
	 */
	public static long shortcutToConversationID(ShortcutInfo shortcutInfo) {
		return shortcutIDToConversationID(shortcutInfo.getId());
	}
	
	/**
	 * Converts a shortcut ID to a conversation ID
	 * @param shortcutID The ID of the shortcut
	 * @return The ID of the conversation
	 */
	public static long shortcutIDToConversationID(String shortcutID) {
		if(!shortcutID.startsWith(shortcutPrefixConversation)) return -1;
		try {
			return Long.parseLong(shortcutID.substring(shortcutPrefixConversation.length()));
		} catch(NumberFormatException exception) {
			exception.printStackTrace();
			return -1;
		}
	}
}