package me.tagavari.airmessage.helper

import android.app.Person
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import io.reactivex.rxjava3.annotations.CheckReturnValue
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleSource
import me.tagavari.airmessage.activity.Conversations
import me.tagavari.airmessage.activity.Messaging
import me.tagavari.airmessage.helper.ConversationBuildHelper.buildConversationTitle
import me.tagavari.airmessage.helper.ConversationBuildHelper.generatePersonList
import me.tagavari.airmessage.helper.ConversationBuildHelper.generateShortcutIcon
import me.tagavari.airmessage.messaging.ConversationInfo

@RequiresApi(api = Build.VERSION_CODES.N_MR1)
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
	@CheckReturnValue
	private fun generateShortcutInfo(context: Context, conversation: ConversationInfo): Single<ShortcutInfo> {
		val singleTitle: SingleSource<String> = buildConversationTitle(context, conversation)
		val singleIcon: SingleSource<Bitmap> = generateShortcutIcon(context, conversation)
		
		return (
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Single.zip(singleTitle, singleIcon, generatePersonList(context, conversation), ::Triple)
				else Single.zip(singleTitle, singleIcon) { title, icon -> Triple<String, Bitmap, List<Person>?>(title, icon, null) })
				.map { (title: String, icon: Bitmap, people: List<Person>?) ->
					ShortcutInfo.Builder(context, conversationToShortcutID(conversation)).apply {
						setShortLabel(title)
						setCategories(shareCategorySet)
						setIntents(arrayOf(
								Intent(context, Conversations::class.java).apply {
									action = Intent.ACTION_VIEW
									addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
								},
								Intent(context, Messaging::class.java).apply {
									action = Intent.ACTION_VIEW
									putExtra(Messaging.intentParamTargetID, conversation.localID)
								}
						))
						
						//Using an adaptive bitmap on Android 8+, otherwise defaulting to a regular bitmap
						if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
							setIcon(Icon.createWithAdaptiveBitmap(icon))
						} else {
							setIcon(Icon.createWithBitmap(icon))
						}
						
						//Add relevant people and enable long lived on Android 11+
						if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
							if(people != null) setPersons(people.toTypedArray())
							setLongLived(true)
							//setLocusId(LocusId(conversation.getLocalID().toString()));
						}
					}.build()
				}
	}
	
	/**
	 * Report a shortcut as used
	 * @param context The context to use
	 * @param conversationID The local ID of the conversation
	 */
	@JvmStatic
	fun reportShortcutUsed(context: Context, conversationID: Long) {
		val shortcutManager = context.getSystemService(ShortcutManager::class.java)
		shortcutManager.reportShortcutUsed(conversationIDToShortcutID(conversationID))
	}
	
	/**
	 * Replaces the dynamic shortcut list with the provided conversations
	 * @param context The context to use
	 * @param conversationList The conversations to display in the dynamic shortcuts list
	 * @return A completable to represent this task
	 */
	@JvmStatic
	fun assignShortcuts(context: Context, conversationList: List<ConversationInfo>?): Completable {
		return Observable.fromIterable(conversationList)
				.flatMapSingle { conversation -> generateShortcutInfo(context, conversation) }
				.toList()
				.doOnSuccess { shortcuts -> context.getSystemService(ShortcutManager::class.java).dynamicShortcuts = shortcuts }
				.ignoreElement()
	}
	
	/**
	 * Pushes a new shortcut to the top of the dynamic shortcuts
	 * @param context The context to use
	 * @param conversationInfo The conversation to push
	 * @return A completable to represent this task
	 */
	@JvmStatic
	@CheckReturnValue
	fun pushShortcut(context: Context, conversationInfo: ConversationInfo): Completable {
		return generateShortcutInfo(context, conversationInfo)
				.doOnSuccess { conversationShortcut: ShortcutInfo ->
					val shortcutManager = context.getSystemService(ShortcutManager::class.java)
					
					//Getting the existing dynamic shortcuts
					val dynamicShortcuts = shortcutManager.dynamicShortcuts
					
					//Checking if this conversation is already published as a shortcut
					if(dynamicShortcuts.stream().anyMatch { shortcut: ShortcutInfo -> shortcut.id == conversationShortcut.id }) {
						//Updating the shortcut
						shortcutManager.updateShortcuts(listOf(conversationShortcut))
					} else {
						//Removing the oldest shortcut if we are at the shortcut limit
						while(dynamicShortcuts.size >= dynamicShortcutLimit) {
							shortcutManager.removeDynamicShortcuts(listOf(dynamicShortcuts[dynamicShortcuts.size - 1].id))
							dynamicShortcuts.removeAt(dynamicShortcuts.size - 1)
						}
						
						//Adding the new shortcut
						shortcutManager.addDynamicShortcuts(listOf(conversationShortcut))
					}
				}.ignoreElement()
	}
	
	/**
	 * Updates a shortcut's information
	 * @param context The context to use
	 * @param conversationInfo The conversation whose shortcut to update
	 * @return A completable to represent this task
	 */
	@JvmStatic
	fun updateShortcut(context: Context, conversationInfo: ConversationInfo): Completable {
		//Ignoring if the shortcut isn't active
		if(!isShortcutRegistered(context, conversationToShortcutID(conversationInfo))) {
			return Completable.complete()
		}
		
		//Updating the shortcut
		return generateShortcutInfo(context, conversationInfo)
				.doOnSuccess { shortcut: ShortcutInfo -> context.getSystemService(ShortcutManager::class.java).updateShortcuts(listOf(shortcut)) }
				.ignoreElement()
	}
	
	/**
	 * Checks if a shortcut ID is in use by the system
	 * @param context The context to use
	 * @param shortcutID The ID of the shortcut to check
	 * @return Whether a shortcut with this ID is currently in use
	 */
	private fun isShortcutRegistered(context: Context, shortcutID: String): Boolean {
		val shortcutManager = context.getSystemService(ShortcutManager::class.java)
		
		return if(Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
			shortcutManager.getShortcuts(ShortcutManager.FLAG_MATCH_DYNAMIC or ShortcutManager.FLAG_MATCH_PINNED or ShortcutManager.FLAG_MATCH_CACHED)
		} else {
			shortcutManager.dynamicShortcuts + shortcutManager.pinnedShortcuts
		}.any { it.id == shortcutID }
	}
	
	/**
	 * Enables the shortcuts associated with the list of conversations
	 * @param context The context to use
	 * @param conversationList The conversations to enable the shortcuts of
	 */
	fun enableShortcuts(context: Context, conversationList: List<ConversationInfo>) {
		val shortcutManager = context.getSystemService(ShortcutManager::class.java)
		shortcutManager.enableShortcuts(conversationList.map(ShortcutHelper::conversationToShortcutID))
	}
	
	/**
	 * Disables the shortcuts associated with the list of conversations
	 * Also removes long-lived shortcuts on Android 11
	 * @param context The context to use
	 * @param conversationIDs The array of conversation IDs whose shortcuts to disable
	 */
	@JvmStatic
	fun disableShortcuts(context: Context, conversationIDs: List<Long>) {
		//Mapping the conversations to shortcut IDs
		val idList = conversationIDs.map(ShortcutHelper::conversationIDToShortcutID)
		
		//Disabling the shortcuts
		val shortcutManager = context.getSystemService(ShortcutManager::class.java)
		shortcutManager.disableShortcuts(idList)
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) shortcutManager.removeLongLivedShortcuts(idList)
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
	fun shortcutToConversationID(shortcutInfo: ShortcutInfo): Long {
		return shortcutIDToConversationID(shortcutInfo.id)
	}
	
	/**
	 * Converts a shortcut ID to a conversation ID
	 * @param shortcutID The ID of the shortcut
	 * @return The ID of the conversation
	 */
	@JvmStatic
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