package me.tagavari.airmessage.helper;

import android.app.Person;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.reactivex.rxjava3.annotations.CheckReturnValue;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleSource;
import me.tagavari.airmessage.activity.Conversations;
import me.tagavari.airmessage.activity.Messaging;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.util.Triplet;

@RequiresApi(api = Build.VERSION_CODES.N_MR1)
public class ShortcutHelper {
	public static final int dynamicShortcutLimit = 3;
	
	private static final String shortcutPrefixConversation = "conversation-";
	private static final Set<String> shareCategorySet = Collections.singleton("me.tagavari.airmessage.directshare.category.DEFAULT");
	
	/**
	 * Creates a shortcut from a conversation
	 * @param context The context to use
	 * @param conversation The conversation to use
	 * @return The completed shortcut
	 */
	@CheckReturnValue
	private static Single<ShortcutInfo> generateShortcutInfo(Context context, ConversationInfo conversation) {
		SingleSource<String> singleTitle = ConversationBuildHelper.buildConversationTitle(context, conversation);
		SingleSource<Bitmap> singleIcon = ConversationBuildHelper.generateShortcutIcon(context, conversation);
		return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
				Single.zip(singleTitle, singleIcon, ConversationBuildHelper.generatePersonList(context, conversation), Triplet::new) :
				Single.zip(singleTitle, singleIcon, (title, icon) -> new Triplet<String, Bitmap, List<Person>>(title, icon, null)))
				.map(result -> {
					//Creating the intents
					Intent intentConversations = new Intent(context, Conversations.class)
							.setAction(Intent.ACTION_VIEW)
							.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
					
					Intent intentMessaging = new Intent(context, Messaging.class)
							.setAction(Intent.ACTION_VIEW)
							.putExtra(Messaging.intentParamTargetID, conversation.getLocalID());
					
					//Building the shortcut
					ShortcutInfo.Builder builder = new ShortcutInfo.Builder(context, conversationToShortcutID(conversation))
							.setShortLabel(result.first)
							.setIntents(new Intent[]{intentConversations, intentMessaging})
							.setCategories(shareCategorySet);
					
					//Using an adaptive bitmap on Android 8+, otherwise defaulting to a regular bitmap
					if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
						builder = builder.setIcon(Icon.createWithAdaptiveBitmap(result.second));
					} else {
						builder = builder.setIcon(Icon.createWithBitmap(result.second));
					}
					
					//Add relevant people and enable long lived on Android 11+
					if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
						builder = builder.setPersons(result.third == null ? null : result.third.toArray(new Person[0]));
						builder = builder.setLongLived(true);
						//builder.setLocusId(new LocusId(Long.toString(conversation.getLocalID())));
					}
					
					return builder.build();
				});
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
	 * Replaces the dynamic shortcut list with the provided conversations
	 * @param context The context to use
	 * @param conversationList The conversations to display in the dynamic shortcuts list
	 * @return A completable to represent this task
	 */
	public static Completable assignShortcuts(Context context, List<ConversationInfo> conversationList) {
		return Observable.fromIterable(conversationList)
				.flatMapSingle(conversation -> generateShortcutInfo(context, conversation))
				.toList()
				.doOnSuccess(shortcuts -> context.getSystemService(ShortcutManager.class).setDynamicShortcuts(shortcuts))
				.ignoreElement();
	}
	
	/**
	 * Pushes a new shortcut to the top of the dynamic shortcuts
	 * @param context The context to use
	 * @param conversationInfo The conversation to push
	 * @return A completable to represent this task
	 */
	@CheckReturnValue
	public static Completable pushShortcut(Context context, ConversationInfo conversationInfo) {
		return generateShortcutInfo(context, conversationInfo)
				.doOnSuccess(conversationShortcut -> {
					ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
					
					//Getting the existing dynamic shortcuts
					List<ShortcutInfo> dynamicShortcuts = shortcutManager.getDynamicShortcuts();
					
					//Checking if this conversation is already published as a shortcut
					if(dynamicShortcuts.stream().anyMatch(shortcut -> shortcut.getId().equals(conversationShortcut.getId()))) {
						//Updating the shortcut
						shortcutManager.updateShortcuts(Collections.singletonList(conversationShortcut));
					} else {
						//Removing the oldest shortcut if we are at the shortcut limit
						if(dynamicShortcuts.size() == dynamicShortcutLimit) {
							shortcutManager.removeDynamicShortcuts(Collections.singletonList(dynamicShortcuts.get(dynamicShortcuts.size() - 1).getId()));
						}
						
						//Adding the new shortcut
						shortcutManager.addDynamicShortcuts(Collections.singletonList(conversationShortcut));
					}
				}).ignoreElement();
	}
	
	/**
	 * Updates a shortcut's information
	 * @param context The context to use
	 * @param conversationInfo The conversation whose shortcut to update
	 * @return A completable to represent this task
	 */
	public static Completable updateShortcut(Context context, ConversationInfo conversationInfo) {
		//Ignoring if the shortcut isn't active
		if(!isShortcutRegistered(context, conversationToShortcutID(conversationInfo))) return Completable.complete();
		
		//Updating the shortcut
		return generateShortcutInfo(context, conversationInfo)
				.doOnSuccess(shortcut -> context.getSystemService(ShortcutManager.class).updateShortcuts(Collections.singletonList(shortcut)))
				.ignoreElement();
	}
	
	/**
	 * Checks if a shortcut ID is in use by the system
	 * @param context The context to use
	 * @param shortcutID The ID of the shortcut to check
	 * @return Whether a shortcut with this ID is currently in use
	 */
	private static boolean isShortcutRegistered(Context context, String shortcutID) {
		ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
		
		Stream<ShortcutInfo> stream;
		if(Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
			stream = shortcutManager.getShortcuts(ShortcutManager.FLAG_MATCH_DYNAMIC | ShortcutManager.FLAG_MATCH_PINNED | ShortcutManager.FLAG_MATCH_CACHED).stream();
		} else {
			stream = Stream.concat(shortcutManager.getDynamicShortcuts().stream(), shortcutManager.getPinnedShortcuts().stream());
		}
		
		return stream.anyMatch(shortcut -> shortcut.getId().equals(shortcutID));
	}
	
	/**
	 * Enables the shortcuts associated with the list of conversations
	 * @param context The context to use
	 * @param conversationList The conversations to enable the shortcuts of
	 */
	public static void enableShortcuts(Context context, List<ConversationInfo> conversationList) {
		//Mapping the conversations to shortcut IDs
		List<String> idList = conversationList.stream().map(ShortcutHelper::conversationToShortcutID).collect(Collectors.toList());
		
		//Enabling the shortcuts
		ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
		shortcutManager.enableShortcuts(idList);
	}
	
	/**
	 * Disables the shortcuts associated with the list of conversations
	 * Also removes long-lived shortcuts on Android 11
	 * @param context The context to use
	 * @param conversationIDs The array of conversation IDs whose shortcuts to disable
	 */
	public static void disableShortcuts(Context context, long[] conversationIDs) {
		//Mapping the conversations to shortcut IDs
		List<String> idList = Arrays.stream(conversationIDs).mapToObj(ShortcutHelper::conversationIDToShortcutID).collect(Collectors.toList());
		
		//Disabling the shortcuts
		ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
		shortcutManager.disableShortcuts(idList);
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) shortcutManager.removeLongLivedShortcuts(idList);
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