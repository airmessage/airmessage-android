package me.tagavari.airmessage.redux

import me.tagavari.airmessage.enums.MessageSendErrorCode
import me.tagavari.airmessage.enums.ServiceHandler
import me.tagavari.airmessage.messaging.*
import me.tagavari.airmessage.util.ModifierMetadata
import me.tagavari.airmessage.util.ReplaceInsertResult
import me.tagavari.airmessage.util.TransferredConversation
import java.io.File

typealias MessageStateEnum = me.tagavari.airmessage.enums.MessageState

//An event to represent updates to messages and conversations
sealed class ReduxEventMessaging {
	//An abstract class for an action performed on a conversation
	abstract class ReduxConversationAction(val conversationID: Long) : ReduxEventMessaging()
	
	//An abstract class for an action performed on a message
	abstract class ReduxMessageAction(conversationID: Long, val messageInfo: MessageInfo) :
		ReduxConversationAction(conversationID)
	
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
		conversationID: Long,
		messageInfo: MessageInfo,
		@param:MessageSendErrorCode @get:MessageSendErrorCode val errorCode: Int,
		val errorDetails: String?
	) : ReduxMessageAction(conversationID, messageInfo)
	
	//When a message is deleted
	class MessageDelete(conversationID: Long, messageInfo: MessageInfo) :
		ReduxMessageAction(conversationID, messageInfo)
	
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
	class ConversationUnread(conversationID: Long, val unreadCount: Int) :
		ReduxConversationAction(conversationID)
	
	//When a conversation member joins or leaves
	class ConversationMember(conversationID: Long, val member: MemberInfo, val isJoin: Boolean) :
		ReduxConversationAction(conversationID)
	
	//When a conversation is muted or unmuted
	class ConversationMute(conversationID: Long, val isMuted: Boolean) :
		ReduxConversationAction(conversationID)
	
	//When a conversation is archived or unarchived
	class ConversationArchive(conversationID: Long, val isArchived: Boolean) :
		ReduxConversationAction(conversationID)
	
	//When a conversation is deleted
	class ConversationDelete(conversationID: Long) : ReduxConversationAction(conversationID)
	
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
	class ConversationTitle(conversationID: Long, val title: String?) :
		ReduxConversationAction(conversationID)
	
	//When a conversation's draft message changes
	class ConversationDraftMessageUpdate(
		conversationID: Long,
		val draftMessage: String?,
		val updateTime: Long
	) : ReduxConversationAction(conversationID)
	
	//When a conversation's draft files changes
	class ConversationDraftFileUpdate(
		conversationID: Long,
		val draft: FileDraft,
		val isAddition: Boolean,
		val updateTime: Long
	) : ReduxConversationAction(conversationID)
	
	//When a conversation's draft files are cleared
	class ConversationDraftFileClear(conversationID: Long) : ReduxConversationAction(conversationID)
	
	//When a conversation's color changes
	class ConversationColor(conversationID: Long, val color: Int) :
		ReduxConversationAction(conversationID)
	
	//When a conversation member's color changes
	class ConversationMemberColor(conversationID: Long, val memberInfo: MemberInfo, val color: Int) :
		ReduxConversationAction(conversationID)
}