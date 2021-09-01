package me.tagavari.airmessage.redux

import me.tagavari.airmessage.enums.ConnectionErrorCode
import me.tagavari.airmessage.enums.ServiceHandler
import me.tagavari.airmessage.messaging.*
import me.tagavari.airmessage.util.ModifierMetadata
import me.tagavari.airmessage.util.ReplaceInsertResult
import me.tagavari.airmessage.util.TransferredConversation
import java.io.File

typealias MessageStateEnum = me.tagavari.airmessage.enums.MessageState

//An event to represent updates to messages and conversations
abstract class ReduxEventMessaging {
	//An abstract class for an action performed on a conversation
	abstract class ReduxConversationAction(val conversationInfo: ConversationInfo) : ReduxEventMessaging()
	
	//An abstract class for an action performed on a message
	abstract class ReduxMessageAction(conversationInfo: ConversationInfo, val messageInfo: MessageInfo) :
		ReduxConversationAction(conversationInfo)
	
	//When new conversation items are received
	class Message(
		//Conversation items that were added to an available conversation
		val conversationItems: List<Pair<ConversationInfo, List<ReplaceInsertResult>>>
	) : ReduxEventMessaging()
	
	//When a message's state changes
	class MessageState @JvmOverloads constructor(
		val messageID: Long,
		@field:MessageStateEnum @get:MessageStateEnum @param:MessageStateEnum val stateCode: Int,
		val dateRead: Long = -1
	) : ReduxEventMessaging()
	
	//When a message's error changes
	class MessageError(
		conversationInfo: ConversationInfo,
		messageInfo: MessageInfo,
		@field:ConnectionErrorCode @get:ConnectionErrorCode val errorCode: Int,
		val errorDetails: String?
	) : ReduxMessageAction(conversationInfo, messageInfo)
	
	//When a message is deleted
	class MessageDelete(conversationInfo: ConversationInfo, messageInfo: MessageInfo) :
		ReduxMessageAction(conversationInfo, messageInfo)
	
	//When an attachment's file is updated
	class AttachmentFile(val messageID: Long, val attachmentID: Long, val file: File?, val downloadName: String?, val downloadType: String?) : ReduxEventMessaging()
	
	/**
	 * When a tapback is updated
	 * @param tapbackInfo The tapback info that is being updated
	 * @param metadata The tapback's positioning metadata
	 * @param isAddition TRUE if this update is an addition, FALSE if this update is a removal
	 */
	class TapbackUpdate(val tapbackInfo: TapbackInfo, val metadata: ModifierMetadata, val isAddition: Boolean) : ReduxEventMessaging()
	
	//When a sticker is added
	class StickerAdd(val stickerInfo: StickerInfo, val metadata: ModifierMetadata) : ReduxEventMessaging()
	
	/**
	 * Constructs a new conversation update message
	 * @param newConversations A map of conversations to conversation items for freshly updated server-created conversations (that didn't need transfers)
	 * @param transferredConversations A list of transferred conversation details
	 */
	class ConversationUpdate(
		val newConversations: Map<ConversationInfo, List<ConversationItem>>,
		val transferredConversations: Collection<TransferredConversation>
	) : ReduxEventMessaging()
	
	/**
	 * Constructs a new sync trigger update (when a sync is determined to be needed)
	 * @param serverInstallationID The installation ID of the server to sync from
	 * @param serverName The name of the server to sync from
	 */
	class Sync(val serverInstallationID: String, val serverName: String) : ReduxEventMessaging()
	
	//When a conversation's unread count changes
	class ConversationUnread(conversationInfo: ConversationInfo, val unreadCount: Int) :
		ReduxConversationAction(conversationInfo)
	
	//When a conversation member joins or leaves
	class ConversationMember(conversationInfo: ConversationInfo, val member: MemberInfo, val isJoin: Boolean) :
		ReduxConversationAction(conversationInfo)
	
	//When a conversation is muted or unmuted
	class ConversationMute(conversationInfo: ConversationInfo, val isMuted: Boolean) :
		ReduxConversationAction(conversationInfo)
	
	//When a conversation is archived or unarchived
	class ConversationArchive(conversationInfo: ConversationInfo, val isArchived: Boolean) :
		ReduxConversationAction(conversationInfo)
	
	//When a conversation is deleted
	class ConversationDelete(conversationInfo: ConversationInfo) : ReduxConversationAction(conversationInfo)
	
	/**
	 * When a bunch of conversations are deleted by service handler
	 * @param serviceHandler The service handler of the deleted conversations
	 * @param deletedIDs A list of the local IDs of all deleted conversations
	 */
	class ConversationServiceHandlerDelete(
		@field:ServiceHandler @get:ServiceHandler @param:ServiceHandler val serviceHandler: Int,
		val deletedIDs: List<Long>
	) : ReduxEventMessaging()
	
	//When a conversation's title changes
	class ConversationTitle(conversationInfo: ConversationInfo, val title: String?) :
		ReduxConversationAction(conversationInfo)
	
	//When a conversation's draft message changes
	class ConversationDraftMessageUpdate(
		conversationInfo: ConversationInfo,
		val draftMessage: String?,
		val updateTime: Long
	) : ReduxConversationAction(conversationInfo)
	
	//When a conversation's draft files changes
	class ConversationDraftFileUpdate(
		conversationInfo: ConversationInfo,
		val draft: FileDraft,
		val isAddition: Boolean,
		val updateTime: Long
	) : ReduxConversationAction(conversationInfo)
	
	//When a conversation's draft files are cleared
	class ConversationDraftFileClear(conversationInfo: ConversationInfo) : ReduxConversationAction(conversationInfo)
	
	//When a conversation's color changes
	class ConversationColor(conversationInfo: ConversationInfo, val color: Int) :
		ReduxConversationAction(conversationInfo)
	
	//When a conversation member's color changes
	class ConversationMemberColor(conversationInfo: ConversationInfo, val memberInfo: MemberInfo, val color: Int) :
		ReduxConversationAction(conversationInfo)
}