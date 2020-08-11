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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.activity.Conversations;
import me.tagavari.airmessage.activity.Messaging;
import me.tagavari.airmessage.messaging.ConversationInfo;

public class ShortcutUtils {
	private static final String shortcutPrefixConversation = "conversation-";
	private static final Set<String> shareCategorySet = Collections.singleton("me.tagavari.airmessage.directshare.category.DEFAULT");
	
	@RequiresApi(api = Build.VERSION_CODES.N_MR1)
	private static void generateShortcutInfo(Context context, List<ConversationInfo> conversationList, Constants.ResultCallback<List<ShortcutInfo>> result) {
		//Removing invalid conversations
		for(ListIterator<ConversationInfo> iterator = conversationList.listIterator(); iterator.hasNext();) {
			if(iterator.next().getConversationMembers().isEmpty()) iterator.remove();
		}
		//Creating the shortcuts
		List<ShortcutInfo> shortcutList = new ArrayList<>(conversationList.size());
		String[] titleArray = new String[conversationList.size()];
		Icon[] iconArray = new Icon[conversationList.size()];
		Person[][] personArray = new Person[conversationList.size()][];
		Constants.ValueWrapper<Integer> titleRequestsCompleted = new Constants.ValueWrapper<>(0);
		
		final Consumer<Boolean> completionRunnable = (wasTasked) -> {
			//Adding to the completion count
			titleRequestsCompleted.value++;
			
			//Checking if all requests have been completed
			if(titleRequestsCompleted.value == conversationList.size()) {
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
					ShortcutInfo.Builder shortcutBuilder = new ShortcutInfo.Builder(context, conversationToShortcutID(conversationShortcut))
							.setShortLabel(titleArray[indexShortcut])
							.setIcon(iconArray[indexShortcut])
							.setIntents(new Intent[]{intentConversations, intentMessaging})
							.setCategories(shareCategorySet);
					if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
						shortcutBuilder
								.setLongLived(true)
								.setPersons(personArray[indexShortcut]);
					}
					shortcutList.add(shortcutBuilder.build());
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
					iconArray[indexBuild] = iconResult;
					
					//Building the people list
					if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
						indexConversation.generatePersonList(context, (peopleResult, peopleWasTasked) -> {
							personArray[indexBuild] = peopleResult;
							
							completionRunnable.accept(titleWasTasked || iconWasTasked || peopleWasTasked);
						});
					} else {
						completionRunnable.accept(titleWasTasked || iconWasTasked);
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
		MainApplication.LoadFlagArrayList<ConversationInfo> conversations = ConversationUtils.getConversations();
		if(conversations == null || !conversations.isLoaded()) return;
		List<ConversationInfo> selectedConversations;
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			//Get all conversations
			selectedConversations = conversations;
		} else {
			//Get the top 3 conversations
			selectedConversations = new ArrayList<>(conversations.subList(0, Math.min(conversations.size(), 3)));
		}
		
		//Creating the shortcuts
		generateShortcutInfo(context, selectedConversations, (wasTasked, shortcutList) -> {
			//Setting the shortcuts
			ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				//Push all dynamic shortcuts for conversation notifications (loop backwards to keep recent conversations at the top)
				for(ListIterator<ShortcutInfo> iterator = shortcutList.listIterator(); iterator.hasPrevious();) shortcutManager.pushDynamicShortcut(iterator.previous());
			} else {
				//Only set the top 3 conversations
				shortcutManager.setDynamicShortcuts(shortcutList);
			}
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
	
	public static String conversationToShortcutID(ConversationInfo conversation) {
		return conversationIDToShortcutID(conversation.getLocalID());
	}
	
	public static String conversationIDToShortcutID(long conversationID) {
		return shortcutPrefixConversation + conversationID;
	}
	
	@RequiresApi(api = Build.VERSION_CODES.N_MR1)
	public static long shortcutToConversationID(ShortcutInfo shortcutInfo) {
		return shortcutIDToConversationID(shortcutInfo.getId());
	}
	
	public static long shortcutIDToConversationID(String shortcutID) {
		if(!shortcutID.startsWith(shortcutPrefixConversation)) return -1;
		try {
			return Long.parseLong(shortcutID.substring(shortcutPrefixConversation.length()));
		} catch(NumberFormatException exception) {
			exception.printStackTrace();
			return -1;
		}
	}
	
	@RequiresApi(api = Build.VERSION_CODES.N_MR1)
	private static ConversationInfo conversationInfoFromShortcut(List<ConversationInfo> conversationList, ShortcutInfo shortcutInfo) {
		long conversationID = shortcutToConversationID(shortcutInfo);
		if(conversationID == -1) return null; //Not a conversation shortcut
		for(ConversationInfo conversationInfo : conversationList) {
			if(conversationID != conversationInfo.getLocalID()) return conversationInfo;
		}
		return null;
	}
	
	public static void updateShortcuts(Context context, List<ConversationInfo> conversationList) {
		//Shortcuts require Android 7.1 Nougat or above
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return;
		
		//Getting the shortcut manager
		ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
		
		//Filtering out conversations that aren't in any shortcuts
		List<ConversationInfo> filteredList = getActiveShortcutConversations(conversationList, shortcutManager);
		
		//Creating the shortcuts
		generateShortcutInfo(context, filteredList, (wasTasked, shortcutList) -> {
			//Setting the shortcuts
			shortcutManager.updateShortcuts(shortcutList);
		});
	}
	
	public static void disableShortcuts(Context context, Collection<ConversationInfo> conversationList) {
		//Shortcuts require Android 7.1 Nougat or above
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return;
		
		//Mapping the conversations to shortcut IDs
		List<String> idList = conversationList.stream().map(ShortcutUtils::conversationToShortcutID).collect(Collectors.toList());
		
		//Disabling the shortcuts
		ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
		shortcutManager.disableShortcuts(idList);
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) shortcutManager.removeLongLivedShortcuts(idList);
	}
	
	public static void enableShortcuts(Context context, List<ConversationInfo> conversationList) {
		//Shortcuts require Android 7.1 Nougat or above
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return;
		
		//Mapping the conversations to shortcut IDs
		List<String> idList = conversationList.stream().map(ShortcutUtils::conversationToShortcutID).collect(Collectors.toList());
		
		//Enabling the shortcuts
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
}