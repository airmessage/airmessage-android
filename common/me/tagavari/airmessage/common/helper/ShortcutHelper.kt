package me.tagavari.airmessage.common.helper

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import kotlinx.coroutines.rx3.await
import me.tagavari.airmessage.activity.ConversationsCompose
import me.tagavari.airmessage.common.messaging.ConversationInfo

object ShortcutHelper {
	const val dynamicShortcutLimit = 3
	private const val shortcutPrefixConversation = "conversation-"
	private val shareCategorySet = setOf("me.tagavari.airmessage.directshare.category.DEFAULT")
	
	/**
	 * Creates a shortcut from a conversation
	 * @param context The context to use
	 * @param conversation The conversation to use
	 * @return The completed shortcut
	 */
	private suspend fun generateShortcutInfo(context: Context, conversation: ConversationInfo): ShortcutInfoCompat {
		val conversationTitle = ConversationBuildHelper.buildConversationTitle(context, conversation).await()
		val shortcutIcon = ConversationBuildHelper.generateShortcutIcon(context, conversation).await()
		val persons = ConversationBuildHelper.generatePersonListCompat(context, conversation)
		
		return ShortcutInfoCompat.Builder(context, conversationToShortcutID(conversation)).apply {
			setShortLabel(conversationTitle)
			setCategories(shareCategorySet)
			setIntent(
				Intent(context, ConversationsCompose::class.java).apply {
					action = Intent.ACTION_VIEW
					addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
					putExtra(ConversationsCompose.INTENT_TARGET_ID, conversation.localID)
				}
			)
			setIcon(IconCompat.createWithAdaptiveBitmap(shortcutIcon))
			setPersons(persons.toTypedArray())
			setLongLived(true)
			//setLocusId(LocusId(conversation.localID.toString()))
		}.build()
	}
	
	/**
	 * Report a shortcut as used
	 * @param context The context to use
	 * @param conversationID The local ID of the conversation
	 */
	fun reportShortcutUsed(context: Context, conversationID: Long) {
		ShortcutManagerCompat.reportShortcutUsed(context, conversationIDToShortcutID(conversationID))
	}
	
	/**
	 * Replaces the dynamic shortcut list with the provided conversations
	 * @param context The context to use
	 * @param conversationList The conversations to display in the dynamic shortcuts list
	 */
	suspend fun assignShortcuts(context: Context, conversationList: List<ConversationInfo>) {
		ShortcutManagerCompat.setDynamicShortcuts(context, conversationList.map { generateShortcutInfo(context, it) })
	}
	
	/**
	 * Pushes a new shortcut to the top of the dynamic shortcuts
	 * @param context The context to use
	 * @param conversationInfo The conversation to push
	 */
	suspend fun pushShortcut(context: Context, conversationInfo: ConversationInfo) {
		//Generate a shortcut for the conversation
		val conversationShortcut = generateShortcutInfo(context, conversationInfo)
		
		//Get existing dynamic shortcuts
		val dynamicShortcuts = ShortcutManagerCompat.getDynamicShortcuts(context)
		
		//Checking if this conversation is already published as a shortcut
		if(dynamicShortcuts.any { it.id == conversationShortcut.id }) {
			//Updating the shortcut
			ShortcutManagerCompat.updateShortcuts(context, listOf(conversationShortcut))
		} else {
			//Remove the oldest shortcut if we are at the shortcut limit
			while(dynamicShortcuts.size >= dynamicShortcutLimit) {
				ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(dynamicShortcuts[dynamicShortcuts.size - 1].id))
				dynamicShortcuts.removeAt(dynamicShortcuts.size - 1)
			}
			
			//Add the new shortcut
			ShortcutManagerCompat.addDynamicShortcuts(context, listOf(conversationShortcut))
		}
	}
	
	/**
	 * Updates a shortcut's information
	 * @param context The context to use
	 * @param conversationInfo The conversation whose shortcut to update
	 */
	suspend fun updateShortcut(context: Context, conversationInfo: ConversationInfo) {
		//Ignore if the shortcut isn't active
		if(!isShortcutRegistered(context, conversationToShortcutID(conversationInfo))) {
			return
		}
		
		//Update the shortcut
		ShortcutManagerCompat.updateShortcuts(context, listOf(
			generateShortcutInfo(context, conversationInfo)
		))
	}
	
	/**
	 * Checks if a shortcut ID is in use by the system
	 * @param context The context to use
	 * @param shortcutID The ID of the shortcut to check
	 * @return Whether a shortcut with this ID is currently in use
	 */
	private fun isShortcutRegistered(context: Context, shortcutID: String): Boolean {
		return ShortcutManagerCompat.getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_DYNAMIC or ShortcutManagerCompat.FLAG_MATCH_PINNED or ShortcutManagerCompat.FLAG_MATCH_CACHED)
			.any { it.id == shortcutID }
	}
	
	/**
	 * Enables the shortcuts associated with the list of conversations
	 * @param context The context to use
	 * @param conversationIDs The array of conversation IDs whose shortcuts to enable
	 */
	fun enableShortcuts(context: Context, conversationIDs: List<Long>) {
		val shortcutList = conversationIDs.map { id ->
			ShortcutInfoCompat.Builder(context, conversationIDToShortcutID(id)).build()
		}
		ShortcutManagerCompat.enableShortcuts(context, shortcutList)
	}
	
	/**
	 * Disables the shortcuts associated with the list of conversations
	 * Also removes long-lived shortcuts on Android 11
	 * @param context The context to use
	 * @param conversationIDs The array of conversation IDs whose shortcuts to disable
	 */
	fun disableShortcuts(context: Context, conversationIDs: List<Long>) {
		val shortcutIDs = conversationIDs.map { conversationIDToShortcutID(it) }
		ShortcutManagerCompat.disableShortcuts(context, shortcutIDs, null)
		ShortcutManagerCompat.removeLongLivedShortcuts(context, shortcutIDs)
	}
	
	/**
	 * Converts a conversation to a shortcut ID
	 * @param conversation The conversation
	 * @return The shortcut ID of the conversation
	 */
	fun conversationToShortcutID(conversation: ConversationInfo): String {
		return conversationIDToShortcutID(conversation.localID)
	}
	
	/**
	 * Converts a conversation ID to a shortcut ID
	 * @param conversationID The ID of the conversation
	 * @return The shortcut ID of the conversation
	 */
	fun conversationIDToShortcutID(conversationID: Long): String {
		return shortcutPrefixConversation + conversationID
	}
	
	/**
	 * Converts a shortcut to a conversation ID
	 * @param shortcutInfo The shortcut
	 * @return The ID of the conversation
	 */
	fun shortcutToConversationID(shortcutInfo: ShortcutInfoCompat): Long {
		return shortcutIDToConversationID(shortcutInfo.id)
	}
	
	/**
	 * Converts a shortcut ID to a conversation ID
	 * @param shortcutID The ID of the shortcut
	 * @return The ID of the conversation
	 */
	fun shortcutIDToConversationID(shortcutID: String): Long {
		if(!shortcutID.startsWith(shortcutPrefixConversation)) return -1
		
		return try {
			shortcutID.substring(shortcutPrefixConversation.length).toLong()
		} catch(exception: NumberFormatException) {
			exception.printStackTrace()
			-1
		}
	}
}
